//package Server;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;


public class Server extends UnicastRemoteObject implements remoteInterface{
	private static final long serialVersionUID = 1L;
	ArrayList<String> nodesList=new ArrayList<String>();
	//Binding the booststrap server object
	protected Server() throws RemoteException {
		super();
		Registry registry = LocateRegistry.createRegistry(6000);
		registry.rebind("server", this);
	}
	//MEthod to update the bootstrapserver list when the node leaves
	public void remoteUpdateBootStrapServer(String ipAddress) throws RemoteException,NotBoundException
	{
		for(int i=0;i < nodesList.size();i++)
			if(nodesList.equals(ipAddress))
			{
				nodesList.remove(i);
				break;
			}
	}
	//returning the IP Address to route by the Bootstrapserver
	public String getBootStrapNode(String ipAddress) throws RemoteException, NotBoundException
	{
		if(nodesList.isEmpty())
		{
			nodesList.add(ipAddress);
			return "FirstNode";
			
		}else{
			if(nodesList.contains(ipAddress))
				return " ";
			nodesList.add(ipAddress);
			return  nodesList.get(0);
			
		}
	}
	
	public static void main(String[] args) throws RemoteException {
		Server obj=new Server();
		
	}
	
	

}
