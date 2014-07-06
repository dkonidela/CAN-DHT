import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

//remote interface for the peer
public interface remoteInterface extends Remote{
	public String getBootStrapNode(String ipAddress) throws RemoteException,NotBoundException;
	public void insert(float xCoordinate, float yCoordinate, String IPAddress) throws RemoteException, NotBoundException;
	public void remoteUpdateNeighbor(Node peer,String Action) throws RemoteException, NotBoundException;
	public void remoteFinalInsertUpdate(Node newPeer,HashMap<String,String> keywords,ArrayList<Node> neighbours,String Action) 
			throws RemoteException, NotBoundException;
	public void remoteRemoveFromNeighbors(String IPAddress)throws RemoteException, NotBoundException;
	public void remoteUpdateBootStrapServer(String ipAddress)throws RemoteException,NotBoundException;
	public ArrayList<String> search(String keyword, float xCoordinate, float yCoordinate,String Action,ArrayList<String> path) throws RemoteException, NotBoundException;
}
