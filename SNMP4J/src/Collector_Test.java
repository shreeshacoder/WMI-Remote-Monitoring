import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;


import java.util.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Timer;
import java.util.TimerTask;

public class Collector_Test extends TimerTask
{
	   
	protected ArrayList<String> ip=new ArrayList<String>();
	
 
	   static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	   static final String DB_URL = "jdbc:mysql://192.168.50.86:3306/SEMS";
	   static final String USER = "sems";
	   static final String PASS = "";
	
  private static String  port    = "161";

  // OID value= ".1.3.6.1.2.1.25.1.6.0" => Number of processes running on system
  
  //OID value= ".1.3.6.1.2.1.25.4.2.1.2" => Names of processes running on system


  private static int    snmpVersion  = SnmpConstants.version2c;

  private static String  community  = "public";
  
  private static int numberOfProcesses;

  //this method must be inside a a timer
  public void run()
  {
	  //
	  db_getServers();
	  int counter_server=0, memory_used=0;
	  while(counter_server<ip.size())
	  {
		  try {
			memory_used=memory_usage(ip.get(counter_server));
			insert_db(memory_used, ip.get(counter_server));
		} 
		  catch (Exception e)
		  {
			e.printStackTrace();
		}
		  counter_server++;
	  }
  }
  
  
  
  
  public void insert_db(int memory_used, String ipAddress) throws Exception
  {
	  Connection conn = null;
		PreparedStatement stmt = null;
		Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection(DB_URL, USER, PASS);
		String sql = "INSERT INTO MemoryUsage VALUES (?,?,DEFAULT)";
		stmt=conn.prepareStatement(sql);
		stmt.setString(1, ipAddress);
		stmt.setInt(2, memory_used);
		stmt.executeUpdate();
		stmt.close();
		conn.close();
  }
  
  
  public int memory_usage(String ipAddress) throws Exception
  {
    int totalmemory=0, memory;
    String  oidValue  = ".1.3.6.1.2.1.25.1.6.0";
    
    
    // Create TransportMapping and Listen
    TransportMapping transport = new DefaultUdpTransportMapping();
    transport.listen();

    // Create Target Address object
    CommunityTarget comtarget = new CommunityTarget();
    comtarget.setCommunity(new OctetString(community));
    comtarget.setVersion(snmpVersion);
    comtarget.setAddress(new UdpAddress(ipAddress + "/" + port));
    comtarget.setRetries(2);
    comtarget.setTimeout(1000);

    // Create the PDU object
    PDU pdu = new PDU();
    pdu.add(new VariableBinding(new OID(oidValue)));
    pdu.setType(PDU.GET);
    pdu.setRequestID(new Integer32(1));
  

    // Create Snmp object for sending data to Agent
    Snmp snmp = new Snmp(transport);

    
    ResponseEvent response = snmp.get(pdu, comtarget);

    // Process Agent Response
    if (response != null)
    {
      
      PDU responsePDU = response.getResponse();

      if (responsePDU != null)
      {
        int errorStatus = responsePDU.getErrorStatus();
        int errorIndex = responsePDU.getErrorIndex();
        String errorStatusText = responsePDU.getErrorStatusText();

        if (errorStatus == PDU.noError)
        {
          Vector vec=responsePDU.getVariableBindings();
          String xx=(vec.get(0)).toString();
          xx=xx.substring(23);
          numberOfProcesses=Integer.parseInt(xx);
          
          //CODE TO GET LIST OF PROCESSES
          oidValue  = "1.3.6.1.2.1.25.5.1.1.2";  // ends with 0 for scalar object
          pdu.add(new VariableBinding(new OID(oidValue)));
          pdu.setType(PDU.GETBULK);
          pdu.setRequestID(new Integer32(1));
          pdu.setMaxRepetitions(numberOfProcesses);  //47 entries on 192.168.50.90 server
          pdu.setNonRepeaters(0);
          response = snmp.getBulk(pdu, comtarget);
          if (response != null)
          {
            responsePDU = response.getResponse();
            if (responsePDU != null)
            {
              errorStatus = responsePDU.getErrorStatus();
              errorIndex = responsePDU.getErrorIndex();
              errorStatusText = responsePDU.getErrorStatusText();

              if (errorStatus == PDU.noError)
              {
            	  Vector vec1=responsePDU.getVariableBindings();
            	  String result=vec1.toString();
                  result = result.replaceAll("]", ",");
            	  //System.out.println(result.substring(result.indexOf(oidValue)+oidValue.length()));
      	       while((result.indexOf(oidValue))!=-1)
      	        {
      	        	result=result.substring(result.indexOf(oidValue));
      	        	result=result.substring(result.indexOf("=")+2);
      	        	memory=Integer.parseInt(result.substring(0,result.indexOf(",")));
      	        	totalmemory+=memory;
      	        	result=result.substring(result.indexOf(","));
      	        }
      	        totalmemory/=1024;
            	  
              }
              else
              {
                System.out.println("Error: Request Failed");
                System.out.println("Error Status = " + errorStatus);
                System.out.println("Error Index = " + errorIndex);
                System.out.println("Error Status Text = " + errorStatusText);
              }
            }
            else
              System.out.println("Error: Response PDU is null");
          }
          else
            System.out.println("Error: Agent Timeout... ");
         
          }
        
        else
        {
          System.out.println("Error: Request Failed");
          System.out.println("Error Status = " + errorStatus);
          System.out.println("Error Index = " + errorIndex);
          System.out.println("Error Status Text = " + errorStatusText);
        }
      }
      else
      
        System.out.println("Error: Response PDU is null");
      
    }
    else
    System.out.println("Error: Agent Timeout... ");
    
    
    snmp.close();
	return totalmemory;
    
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
			      sql="select IPAddress from SM_Servers where Online='YES'";
			    ResultSet rs=stmt.executeQuery(sql);
			     
			      			    
			      
			      while(rs.next())
			      {
			    	 
					ip.add(rs.getString("IPAddress"));
					
			    	  
			   
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
  
public static void main(String args[])
{
	Timer timer=new Timer();

	timer.schedule(new Collector_Test(), 100, (60000)*2);
}
}