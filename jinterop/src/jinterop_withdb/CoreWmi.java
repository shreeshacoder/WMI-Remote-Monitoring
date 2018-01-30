package jinterop_withdb;


import java.util.logging.Level;

import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.jinterop.dcom.impls.automation.IJIEnumVariant;


public class CoreWmi {

	JISession createDComSession(String domain, String user, String password)  throws Exception
	{
	    /**
	    * Please note that the user must have administrator permissions on the target Windows machine
	    */
		JISystem.getLogger().setLevel(Level.OFF);
		JISystem.setAutoRegisteration(true);
	    JISession dcomSession = JISession.createSession(domain, user, password);

	    /**
	     *  NTLM1 config
	     */
	    dcomSession.useSessionSecurity(true);

	    /**
	     * Set the socket timeout for connections in this session.
	     * This is very important - in case the other end shuts down, 
	     * infinite blocking may occur... 
	     */
	    dcomSession.setGlobalSocketTimeout(10000);

	    return dcomSession;
	}
	
	IJIDispatch createWmiLocator(JISession dcomSession, String ipaddress) throws Exception
	{
	    JIComServer wbemLocatorComObj = new JIComServer(JIClsid.valueOf("76A64158-CB41-11D1-8B02-00600806D9B6"), ipaddress, dcomSession);
	    IJIDispatch wmiLocator = (IJIDispatch) JIObjectFactory.narrowObject(wbemLocatorComObj.createInstance().queryInterface(IJIDispatch.IID));
	    return wmiLocator;
	}
	
	public IJIDispatch createNamespaceServer(IJIDispatch wmiLocator, String nameSpace, String ipAddress) throws Exception
	{
	    // create an instance of the WbemScripting.SWbemLocator object to talk to WMI on the target machine.
	    // namespace could be for instance "root\\cimv2"
	    JIVariant results[] = wmiLocator.callMethodA("ConnectServer", new Object[] { new JIString(ipAddress), new JIString(nameSpace),
	                        JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM(), new Integer(0),
	                        JIVariant.OPTIONAL_PARAM()});
	    
	    IJIDispatch wbemService = (IJIDispatch) JIObjectFactory.narrowObject(results[0].getObjectAsComObject());
	    return wbemService;
	}
	public String printDiskIO(IJIDispatch wbemService, String service_name) throws Exception
	{
		String txt=new String();
	    /** 
	     * The WQL query.
	     * WMI Classes are stored in a table. 
	     */
	    final String QUERY = "SELECT * FROM Win32_Service where DisplayName='"+service_name+"'";
	            
	    /**
	     * Flags for the query.
	     */
	    final int RETURN_IMMEDIATE = 16;
	    final int FORWARD_ONLY = 32;
	            
	    /**
	     * Execute the query
	     */
	    JIVariant[] sourceSet = wbemService.callMethodA("ExecQuery", new Object[] { new JIString(QUERY), new JIString("WQL"), new JIVariant(new Integer(RETURN_IMMEDIATE + FORWARD_ONLY))});
	    IJIDispatch wbemSource = (IJIDispatch) JIObjectFactory.narrowObject((sourceSet[0]).getObjectAsComObject());
	    IJIComObject enumSet = wbemSource.get("_NewEnum").getObjectAsComObject();
	    IJIEnumVariant enumVariant = (IJIEnumVariant) JIObjectFactory.narrowObject(enumSet.queryInterface(IJIEnumVariant.IID));
	            
	    /** 
	     * Process the result
	     */
	    
	    //to copy text into a file
	 
	    Object[] values=new Object[100];
	   values=enumVariant.next(1);
	   
	    
	    //Object[] values = enumVariant.next(1);
	    
	    JIArray array = (JIArray) values[0];
	    Object[] arrayObj = (Object[])array.getArrayInstance();
	    
	    /**
	    * Loop through each found disk
	    */
	    
	    
	   for (int j = 0; j < arrayObj.length; j++)
	    {
		   
	        IJIComObject procComObj = ((JIVariant)arrayObj[j]).getObjectAsComObject();
	        IJIDispatch wbemProcObj = (IJIDispatch) JIObjectFactory.narrowObject(procComObj);
	                        
	        
	       JIVariant jj= wbemProcObj.callMethodA("GetObjectText_");
	       
	        txt = jj.getObjectAsString().getString();
	        
	        
	       
	    } 
	 return txt;
	     
	  
	}
	
	
	
	
}
