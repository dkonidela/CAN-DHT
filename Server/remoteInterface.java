//package Server;

import java.rmi.*;

//remote interface for the Bootstrap Server
public interface remoteInterface extends Remote{
	public String getBootStrapNode(String ipAddress) throws RemoteException,NotBoundException;
	public void remoteUpdateBootStrapServer(String ipAddress) throws RemoteException,NotBoundException;

}
