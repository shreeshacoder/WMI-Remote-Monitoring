package jinterop_withdb;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;


import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.impls.automation.IJIDispatch;


import java.io.*;

public class jinterop_db extends TimerTask{
	
	   static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	   static final String DB_URL = "jdbc:mysql://192.168.50.86:3306/SEMS";
	   static final String USER = "sems";
	   static final String PASS = "sems@123";
	   static final String namespace="root\\cimv2";
	   	
	 protected ArrayList<String> ip=new ArrayList<String>();
	 protected ArrayList<String> serverdomain=new ArrayList<String>();
	 protected ArrayList<String> username=new ArrayList<String>();
	 protected ArrayList<String> pass=new ArrayList<String>();
	 
	 protected ArrayList<String> servicename=new ArrayList<String>();
	 protected ArrayList<String> serviceID=new ArrayList<String>();
	 
	 protected ArrayList<String> propertyname=new ArrayList<String>();
	 protected ArrayList<String> propertyID=new ArrayList<String>();
	 
	public void run()
	{
		//entire code has to exist within a timer and clear the arraylists after all servers are monitored
		//do not place object instantiation statements in the timer
		CoreWmi cw=new CoreWmi();
		server_online();
		db_getServers();
		db_getServices();
		db_getProperties();
		String result=new String();
		
		
		int counter_server=0, counter_service=0;
		while(counter_server<ip.size())
		{
		try
		{
			JISession ses=cw.createDComSession(serverdomain.get(counter_server), username.get(counter_server), pass.get(counter_server));
		
			IJIDispatch dispatch=cw.createWmiLocator(ses,ip.get(counter_server));
			
			IJIDispatch wb=cw.createNamespaceServer(dispatch, namespace, ip.get(counter_server));
			while(counter_service<servicename.size())
			{
				
				
			try
			{
				result="";
				result=cw.printDiskIO(wb, servicename.get(counter_service));
				DB_insert(counter_server, counter_service, result);

			}
				catch(Exception e1)
				{
					System.out.println("Error in method printDiskIO");	
				}
			counter_service++;
		}
			JISession.destroySession(ses);
		}
		catch(Exception e)
		{
			System.out.println("Error obtaining system information using WMI");
		}
		counter_service=0;
		counter_server++;
		}
		//entire code has to exist within a timer and clear the arraylists after all servers are monitored
	}
	
	
	public void server_online()
	{
		Connection conn = null;
		   Statement stmt = null;
		  String ServerIP=new String();
		   try
			{
			
			   Class.forName("com.mysql.jdbc.Driver");

			      //STEP 3: Open a connection
			      System.out.println("Connecting to database...");
			      conn = DriverManager.getConnection(DB_URL,USER,PASS);

			      //STEP 4: Execute a query
			      stmt=conn.createStatement();
			      String sql=new String();
                  sql="select IPAddress from SM_Servers";
                  ResultSet rs=stmt.executeQuery(sql);
		while(rs.next())
		{
			ServerIP=rs.getString("IPAddress");
			check_server(ServerIP);
		}
			
		   rs.close();
		      stmt.close();
		      conn.close();
		      }
		      
		
	    catch(Exception e)
		{
			System.out.println("Error accessing database");
		}
		
	}
	
	public void check_server(String ServerIP)
	{
		String status=new String();
		Connection conn = null;
		   PreparedStatement stmt = null;
		   String command="ping "+ServerIP;
		   try {
				Process p = Runtime.getRuntime().exec(command);
				BufferedReader inputStream = new BufferedReader(
						new InputStreamReader(p.getInputStream()));

				String s = "";
				String p1 = "";
				// reading output stream of the command
				while ((s =inputStream.readLine()) != null) {
					p1+=s;
				}
				if(p1.contains("Request timed out."))
				{
					status="NO";
					delete_from_services(ServerIP);
				}
				else
				{
					status="YES";
					
				}
				
				//to update in the DB
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection(DB_URL,USER,PASS);
				String sql="UPDATE SM_Servers set Online=? where IPAddress=?";
				stmt=conn.prepareStatement(sql);
				stmt.setString(1, status);
				stmt.setString(2, ServerIP);
				stmt.executeUpdate();
				
				stmt.close();
				conn.close();
				}
				catch(Exception e)
				{
					e.notify();
				}
		
	}	
	
	
	public void delete_from_services(String ServerIP)	
	{
		Connection conn = null;
		   Statement stmt = null;
		   try
			{
			
			   Class.forName("com.mysql.jdbc.Driver");

			      //STEP 3: Open a connection
			      conn = DriverManager.getConnection(DB_URL,USER,PASS);
			      stmt=conn.createStatement();
			      String sql="delete from ST_ServerStatus where IPAddress='"+ServerIP+"';";
			      stmt.executeUpdate(sql);
			      stmt.close();
			      conn.close();
			}
		   catch(Exception e)
			{
				System.out.println("Error accessing database");
			}
			      
	}
	
	
	public void db_getServers()
	{
		Connection conn = null;
		   Statement stmt = null;
		  
		   try
			{
			
			   Class.forName("com.mysql.jdbc.Driver");

			      //STEP 3: Open a connection
			      System.out.println("Connecting to database...");
			      conn = DriverManager.getConnection(DB_URL,USER,PASS);

			      //STEP 4: Execute a query
			      stmt=conn.createStatement();
			      String sql;
			      sql="select IPAddress, ServerDomain, Username, Password from SM_Servers where Online='YES'";
			    ResultSet rs=stmt.executeQuery(sql);
			     
			      			    
			      
			      while(rs.next())
			      {
			    	 
					ip.add(rs.getString("IPAddress"));
					
			    	  serverdomain.add(rs.getString("ServerDomain"));
			    	  
			    	  username.add(rs.getString("Username"));
			    	  
			    	  pass.add(rs.getString("Password"));
			   
			      }
			      rs.close();
			      stmt.close();
			      conn.close();
			      }
			      
			
		    catch(Exception e)
			{
				System.out.println("Error accessing database");
			}
		
		
		
	}
		
	public void db_getServices()
	{
		Connection conn = null;
		   Statement stmt = null;
		   
		   try
			{
			
			   Class.forName("com.mysql.jdbc.Driver");

			      //STEP 3: Open a connection
			      
			      conn = DriverManager.getConnection(DB_URL,USER,PASS);

			      //STEP 4: Execute a query
			      stmt=conn.createStatement();
			      String sql;
			     sql="select ServiceID, ServiceName from SM_Services where Monitor='YES'";
			    ResultSet rs=stmt.executeQuery(sql);
			      while(rs.next())
			      {
			    	  serviceID.add(rs.getString("ServiceID"));
			    	  servicename.add(rs.getString("ServiceName"));
			    	  
			      }
			     rs.close();
			      stmt.close();
			      conn.close();
			      }
			
		    catch(Exception e)
			{
				System.out.println("Error accessing database");
			}
			
	}
	
	
	public void db_getProperties()
	{
		Connection conn = null;
		   Statement stmt = null;
		   
		   try
			{
			
			   Class.forName("com.mysql.jdbc.Driver");

			      //STEP 3: Open a connection
			     
			      conn = DriverManager.getConnection(DB_URL,USER,PASS);

			      //STEP 4: Execute a query
			      stmt=conn.createStatement();
			      String sql;
			     sql="select PropertyID, PropertyName from SM_ServiceProperties where Monitor='YES'";
			    ResultSet rs=stmt.executeQuery(sql);
			      while(rs.next())
			      {
			    	  propertyID.add(rs.getString("PropertyID"));
			    	  propertyname.add(rs.getString("PropertyName"));
			    	  
			      }
			     rs.close();
			      stmt.close();
			      conn.close();
			      }
			
		    catch(Exception e)
			{
				System.out.println("Error accessing database");
			}
			
	}
	
	public void DB_insert(int counter_server, int counter_service, String result) throws Exception
	{
		
		//to get current time

		java.util.Date dt = new java.util.Date();

		java.text.SimpleDateFormat sdf = 
		     new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String currentTime = sdf.format(dt);
		
		//for db connection
		PreparedStatement stmt1=null;
		Connection conn = null;
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		
		int ValueStartIndex, ValueEndIndex, i;
		String value=new String();
		String property=new String();
		String sql=new String();
		for(i=0;i<propertyname.size() && i<propertyID.size();i++)
		{
			property=propertyname.get(i);
			ValueStartIndex=result.indexOf(property)+((property).length())+3;
			ValueEndIndex=result.indexOf(';',ValueStartIndex);
			value=result.substring(ValueStartIndex, ValueEndIndex);
			value=value.replaceAll("\"", "");
					    
			//decide whether to insert or update
			
			//sql="select * from ST_ServerStatus where IPAddress='"+(ip.get(counter_server))+"' AND ServiceTagID='"+(serviceID.get(counter_service))+"' AND PropertyID='"+(propertyID.get(i))+"';";
			sql="select * from ST_ServerStatus where IPAddress=? AND ServiceTagID=? AND PropertyID=?";
			stmt1 = conn.prepareStatement(sql);
			stmt1.setString(1,ip.get(counter_server));
			stmt1.setString(2, serviceID.get(counter_service));
			stmt1.setString(3, propertyID.get(i));
			 ResultSet rs=stmt1.executeQuery();
			if(rs.next()) //entry present in table
			{
				if((rs.getString("PropertyStatus")).equals(value))
				{
					System.out.println("entered");
					update_timestamp(ip.get(counter_server),serviceID.get(counter_service), propertyID.get(i), currentTime);
				}
				else
				{
					insert_serverhistory(ip.get(counter_server), serviceID.get(counter_service), propertyID.get(i), value, currentTime);
					//update timestamp and value in st_serverstatus
					update_timestamp_value(ip.get(counter_server),serviceID.get(counter_service), propertyID.get(i), currentTime, value);
				}
					
					
					
						
			}
			else  //entry not present in table
			{
				insert_serverstatus(ip.get(counter_server), serviceID.get(counter_service), propertyID.get(i), value, currentTime);
			
			}
			stmt1.close();
			 rs.close();
		}
		
		
		
		
	      
	      conn.close();
	      
	}
	
	public void insert_serverstatus(String ip, String serviceid, String propertyid, String value, String currentTime)
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try
		{
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		String sql = "INSERT INTO ST_ServerStatus " +
				"VALUES (?,?,?,?,?)";
		stmt=conn.prepareStatement(sql);
		stmt.setString(1, ip);
		stmt.setString(2, serviceid);
		stmt.setString(3, propertyid);
		stmt.setString(4, value);
		stmt.setString(5, currentTime);
		stmt.executeUpdate();
		stmt.close();
		conn.close();
		}
		catch(Exception e)
		{
			e.notify();
		}
	}
	
	public void update_timestamp(String ip, String serviceid, String propertyid, String date)
	{
		Connection conn = null;
		   PreparedStatement stmt = null;
		  
		   try
			{
			   
			   Class.forName("com.mysql.jdbc.Driver");
			  
			      //STEP 3: Open a connection
			      System.out.println("Connecting to database...");
			      conn = DriverManager.getConnection(DB_URL,USER,PASS);

			      //STEP 4: Execute a query
			      
			      String sql;
			      //sql="update ST_ServerStatus set SampleTime='"+date+"' where IPAddress='"+ip+"' AND ServiceTagID='"+serviceid+"' AND PropertyID='"+propertyid+"';";
			    sql="UPDATE ST_ServerStatus set SampleTime=? where IPAddress=? AND ServiceTagID=? AND PropertyID=?";
			    stmt=conn.prepareStatement(sql);
			    stmt.setString(1, date);
			    stmt.setString(2, ip);
			    stmt.setString(3, serviceid);
			    stmt.setString(4, propertyid);
			    stmt.executeUpdate();
			     
			      stmt.close();
			      conn.close();
			      }
			      
			
		    catch(Exception e)
			{
				System.out.println("Error accessing database");
			}
		
	}
	
	
	public void update_timestamp_value(String ip, String serviceid, String propertyid, String date, String value)
	{
		Connection conn = null;
		   PreparedStatement stmt = null;
		  
		   try
			{
			   
			   Class.forName("com.mysql.jdbc.Driver");
			  
			      //STEP 3: Open a connection
			      System.out.println("Connecting to database...");
			      conn = DriverManager.getConnection(DB_URL,USER,PASS);

			      //STEP 4: Execute a query
			      
			      String sql;
			      //sql="update ST_ServerStatus set SampleTime='"+date+"' where IPAddress='"+ip+"' AND ServiceTagID='"+serviceid+"' AND PropertyID='"+propertyid+"';";
			    sql="UPDATE ST_ServerStatus set SampleTime=?, PropertyStatus=? where IPAddress=? AND ServiceTagID=? AND PropertyID=?";
			    stmt=conn.prepareStatement(sql);
			    stmt.setString(1, date);
			    stmt.setString(2, value);
			    stmt.setString(3, ip);
			    stmt.setString(4, serviceid);
			    stmt.setString(5, propertyid);
			    stmt.executeUpdate();
			     
			      stmt.close();
			      conn.close();
			      }
			      
			
		    catch(Exception e)
			{
				System.out.println("Error accessing database");
			}
	}
	
	public void insert_serverhistory(String ip, String serviceid, String propertyid, String value, String currentTime)
	{
		//check if entry is already existing
		
		String sql=new String();
		Connection conn = null;
		PreparedStatement stmt = null;
		try
		{
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		sql="select * from ST_ServerStatusHistory where IPAddress=? AND ServiceTagID=? AND ServicePropertyID=?";
		stmt=conn.prepareStatement(sql);
		stmt.setString(1, ip);
		stmt.setString(2, serviceid);
		stmt.setString(3, propertyid);
		ResultSet rs=stmt.executeQuery();
		if(rs.next())
		{
			PreparedStatement stmt1 = null;
			//entry exists in table. PropertyStatus and SampleTimecolumn has to be updated
			String sql2="UPDATE ST_ServerStatusHistory set PropertyStatus=?, SampleTime=? where IPAddress=? AND ServiceTagID=? AND ServicePropertyID=?";
			stmt1=conn.prepareStatement(sql2);
			stmt1.setString(1, value);
			stmt1.setString(2, currentTime);
			stmt1.setString(3, ip);
			stmt1.setString(4, serviceid);
			stmt1.setString(5, propertyid);
			stmt1.executeUpdate();
			stmt1.close();
			
		}
		else
		{
			PreparedStatement stmt2 = null;
			String sql3 = "INSERT INTO ST_ServerStatusHistory VALUES (?,?,?,?,?)";
			stmt2=conn.prepareStatement(sql3);
			stmt2.setString(1, ip);
			stmt2.setString(2, serviceid);
			stmt2.setString(3, propertyid);
			stmt2.setString(4, value);
			stmt2.setString(5, currentTime);
			stmt2.executeUpdate();
			stmt2.close();
			
		}
		    rs.close();
			stmt.close();
			conn.close();
		}
		catch(Exception e)
		{
			
		}
	}
	
	
	
	public static void main(String args[])
	   {
		   
	  
	  /* try
	   {
	   jinterop_db shree=new jinterop_db();
	   shree.dbconnect_retrieve();
	   JISession ses=shree.createDComSession(domain, user, password);
		IJIDispatch dispatch=shree.createWmiLocator(ses);
		IJIDispatch wb=shree.createNamespaceServer(dispatch, namespace, ipaddress);
		shree.printDiskIO(wb);
		      
	   }
		      catch(Exception e)
		      {
		    	  System.out.println(e.getStackTrace());
		      }*/
		/*jinterop_db shree=new jinterop_db();
		shree.schedule(new jinterop_db(), 100, (60000)*5);*/
		
		Timer jinterop_db=new Timer();

		jinterop_db.schedule(new jinterop_db(), 100, (60000)*2);
	   }
	
}

