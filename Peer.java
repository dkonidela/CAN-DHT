import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

class Node implements Serializable
{
	private static final long serialVersionUID = 1L;
	//Coordinates/boundaries of each node
	float lx,ly,ux,uy;
	String IPAddress;	
	Node(float lx, float ly, float ux, float uy, String IPAddress)
	{
		this.lx=lx;this.ly=ly;
		this.ux=ux;this.uy=uy;
		this.IPAddress=IPAddress;	
	}
}

public class Peer extends UnicastRemoteObject implements remoteInterface,Serializable{
	
	Node peerNode;
	private static final long serialVersionUID = 1L;
	ArrayList<Node> neighbours = new ArrayList<Node>();
	HashMap<String, String> keywords = new HashMap<String, String>();
	String BootStrapIPAddress;
	
	protected Peer() throws RemoteException {
		super();
		//Bind the object in the rmi registry
		Registry registry = LocateRegistry.createRegistry(6000);
		registry.rebind("peer", this);
	}
	
	//Check if the Coordinates lies in the Zone of the peer
	boolean sameZone(float xCoordinate, float yCoordinate, Node peer)
	{
		if (xCoordinate >= peer.lx && yCoordinate >= peer.ly
				&& xCoordinate <= peer.ux
				&& yCoordinate <= peer.uy) 
			return true;
		else 
			return false;
	}
	
	//Check whether to split the Zone horizontally
	
	boolean divideHorizantally()
	{
		if(Math.abs(peerNode.lx-peerNode.ux)-Math.abs(peerNode.ly-peerNode.uy) >=0)
		{
			return false;
		}else{
			return true;
		}
	}
	//Check if the two nodes are neighbor are not
	boolean isNeighbor(Node newPeer, Node neighbor)
	{
		if(newPeer.lx==newPeer.ux || newPeer.ly==newPeer.uy)
			return false;
		float breadth=Math.abs(neighbor.ly-neighbor.uy)+Math.abs(newPeer.ly-newPeer.uy);
		float length=Math.abs(neighbor.lx-neighbor.ux)+Math.abs(newPeer.lx-newPeer.ux);
		if(Math.abs(newPeer.ly-neighbor.uy) >breadth || Math.abs(newPeer.uy-neighbor.ly) >breadth)
			return false;
		if(Math.abs(newPeer.lx-neighbor.ux) >length || Math.abs(newPeer.ux-neighbor.lx) >length)
			return false;
		return true;
	}
	//Remote method to update the neighbors of the remote object
	public void remoteUpdateNeighbor(Node peer,String Action) throws RemoteException, NotBoundException
	{
		if(Action.equals("Add"))
		{
			//If a new node joins
			this.neighbours.add(peer);
			return;
		}
		int i;
		for(i=0; i<this.neighbours.size();i++)
		{
			if(this.neighbours.get(i).IPAddress.equals(peer.IPAddress))
				break;
		}
		if(Action.equals("LeaveUpdate"))
		{
			//Updating the neighbors when the node leaves
			if(i==this.neighbours.size()) 
			{
				this.neighbours.add(peer);
				return;
			}
			else
				this.neighbours.remove(i);
		}
		if(Action.equals("Delete"))
		{
			//deleting the node from the neighbor list
			this.neighbours.remove(i);
			return;
		}
		if(Action.equals("Update"))
			this.neighbours.remove(i);
		if(peer.lx!=peer.ux && peer.ly!=peer.uy)
			this.neighbours.add(peer);
	}
	
	//Updating the list of neighbors for the node given in the parameter
	ArrayList<Node> updatePeersNeighbors(Node newPeer,String Action) throws RemoteException, NotBoundException
	{
		ArrayList<Node> newPeerNeighbor=new ArrayList<Node>();
		
		for(int i=0;i<this.neighbours.size();i++)
		{
			System.out.println(this.neighbours.get(i).IPAddress);
			Registry peerRegistry = LocateRegistry.getRegistry(this.neighbours.get(i).IPAddress, 6000);
			remoteInterface peerRemoteObject = (remoteInterface) peerRegistry.lookup("peer");
			if(isNeighbor(newPeer,this.neighbours.get(i)))
			{
				if(Action.equals("Join"))
				{
					System.out.println("Add");
					peerRemoteObject.remoteUpdateNeighbor(newPeer,"Add");
					newPeerNeighbor.add(this.neighbours.get(i));
				}else{
					if(!newPeer.IPAddress.equals(this.neighbours.get(i).IPAddress))
					{
						peerRemoteObject.remoteUpdateNeighbor(newPeer,"LeaveUpdate");
						newPeerNeighbor.add(this.neighbours.get(i));	
					}
				}
			}
			if(isNeighbor(this.peerNode, this.neighbours.get(i)))
				peerRemoteObject.remoteUpdateNeighbor(this.peerNode,"Update");
			else
				System.out.println("Delete");
				peerRemoteObject.remoteUpdateNeighbor(this.peerNode,"Delete");
		}
		return newPeerNeighbor;	
	}
	//Hash the keyword
	float hashX(String keyword) {
		int sum=0;
		for(int i=0; i < keyword.length();i=i+2)
			sum += keyword.charAt(i);
		return (sum%10);
	}
	//Hash the keyword
	float hashY(String keyword) {
		int sum=0;
		for(int i=1; i < keyword.length();i=i+2)
			sum += keyword.charAt(i);
		return (sum%10);
	}
	//Swapping the hashtables  from another node when the new node joins the
	//CAN network or when the node leaves the netowrk
	HashMap<String,String> swapHashTables(Node newPeer)
	{
		HashMap<String,String> newPeerKeywords=new HashMap<String,String>();
		Iterator it = keywords.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> pairs = (Map.Entry<String, String>) it.next();
			if( sameZone(hashX(pairs.getKey()), hashY(pairs.getKey()), newPeer) )
			{
				keywords.remove(pairs.getKey());
				newPeerKeywords.put(pairs.getKey(), pairs.getValue());
			}		
		}
		return newPeerKeywords;
	}
	//Method to handle joining of a new node into the system for the X and Y Coordinates given in the parameters
	public void insert(float xCoordinate, float yCoordinate, String IPAddress) throws RemoteException, NotBoundException
	{
		Node newPeer;
		if(sameZone(xCoordinate,xCoordinate,this.peerNode))
		{
			if(divideHorizantally())
			{
				if(yCoordinate <= (this.peerNode.uy/2))
				{
					newPeer=new Node(this.peerNode.lx,this.peerNode.ly,this.peerNode.ux,this.peerNode.uy/2,IPAddress);
					this.peerNode.ly=this.peerNode.uy/2;
				}else{
					newPeer=new Node(this.peerNode.lx,this.peerNode.uy/2,this.peerNode.ux,this.peerNode.uy,IPAddress);
					this.peerNode.uy=this.peerNode.uy/2;
				}
			}else{
				if(xCoordinate <= (this.peerNode.ux/2))
				{
					newPeer=new Node(this.peerNode.lx,this.peerNode.ly,this.peerNode.ux/2,this.peerNode.uy,IPAddress);
					this.peerNode.lx=this.peerNode.ux/2;
				}else{
					newPeer=new Node(this.peerNode.ux/2,this.peerNode.ly,this.peerNode.ux,this.peerNode.uy,IPAddress);
					this.peerNode.ux=this.peerNode.ux/2;
				}
				
			}
			//Updating the Neighbors
			ArrayList<Node> newPeerNeighbor=updatePeersNeighbors(newPeer,"Join");
			this.neighbours.add(newPeer);
			newPeerNeighbor.add(this.peerNode);
			//Swap hash tables
			HashMap<String,String> newPeerKeywords=swapHashTables(newPeer);
			
			System.out.println(" "+newPeer.IPAddress);
			Registry peerRegistry = LocateRegistry.getRegistry(newPeer.IPAddress, 6000);
			remoteInterface peerRemoteObject = (remoteInterface) peerRegistry.lookup("peer");
			peerRemoteObject.remoteFinalInsertUpdate(newPeer,newPeerKeywords,newPeerNeighbor,"Join");
			
		}else{
			//redirecting
			String temp_IPAddress=this.redirect(xCoordinate, yCoordinate);
			Registry peerRegistry = LocateRegistry.getRegistry(temp_IPAddress, 6000);
			remoteInterface peerRemoteObject = (remoteInterface) peerRegistry.lookup("peer");
			peerRemoteObject.insert(xCoordinate,yCoordinate,IPAddress);
				
		}
	}
	//Method to find the best next hop when  the coordinates are not present in the given Zone.
	String redirect(float xCoordinate, float yCoordinate) throws RemoteException, NotBoundException
	{
		float distance,distanceMin = 100;
		int minIndex = 0;
		for (int i = 0; i < this.neighbours.size(); i++) {
			distance = (float) (Math.pow((xCoordinate - ((this.neighbours.get(i).lx + this.neighbours.get(i).ux) / 2)), 2))
					  + (float) (Math.pow((yCoordinate-((this.neighbours.get(i).ly + this.neighbours.get(i).      uy) / 2)), 2));
			if(distance < distanceMin)
			{
				distanceMin = distance;
				minIndex = i;
			}		
		}
		return this.neighbours.get(minIndex).IPAddress;
		
	}
	public void remoteFinalInsertUpdate(Node newPeer,HashMap<String,String> keywords,ArrayList<Node> neighbours,String Action) 
														throws RemoteException, NotBoundException
	{
		if(Action.equals("Join"))
		{
			this.peerNode=newPeer;
			this.keywords=keywords;
			this.neighbours=neighbours;
		}else if(Action.equals("Leave"))
		{
			this.peerNode.lx=newPeer.lx;this.peerNode.ly=newPeer.ly;
			this.peerNode.ux=newPeer.ux;this.peerNode.uy=newPeer.uy;
			addNewKeywords(keywords);
			leave_UpdateNeighbors(neighbours);
		}
	}
	//Updating the neighbors when the node leaves
	void leave_UpdateNeighbors(ArrayList<Node> updneighbours) throws RemoteException, NotBoundException
	{
		boolean flag;
		for(int i=0; i < updneighbours.size();i++)
		{
			flag=false;
			int j;
			for(j=0;j < this.neighbours.size();j++)
			{
				if(this.neighbours.get(j).IPAddress.equals(updneighbours.get(i).IPAddress))
				{
					this.neighbours.remove(j);
					this.neighbours.add(j,updneighbours.get(i));
					flag=true;
					break;
				}
			}
			if(!flag)
				this.neighbours.add(updneighbours.get(j));
		}
	}
	//Adding new keywords to the hashmap
	void addNewKeywords(HashMap<String,String> keywords)
	{
		Iterator it = keywords.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> pairs = (Map.Entry<String, String>) it.next();
			this.keywords.put(pairs.getKey(), pairs.getValue());
		}
	}
	//Method to handle the search functionality
	public ArrayList<String> search(String keyword, float xCoordinate, float yCoordinate,String Action,ArrayList<String> path) throws RemoteException, NotBoundException
	{
		path.add(this.peerNode.IPAddress);
		if(sameZone(xCoordinate,yCoordinate,this.peerNode))
		{
			if(Action.equals("Insert"))
			{
				this.keywords.put(keyword, " ");
			}else{
				if(this.keywords.containsKey(keyword))
					System.out.println("Keyword Found at" + this.peerNode.IPAddress);
				else
					return null;
			}
		}else{
			String temp_IPAddress=this.redirect(xCoordinate, yCoordinate);
			Registry peerRegistry = LocateRegistry.getRegistry(temp_IPAddress, 6000);
			remoteInterface peerRemoteObject = (remoteInterface) peerRegistry.lookup("peer");
			path=peerRemoteObject.search(keyword, xCoordinate, yCoordinate, Action,path);
		}
		return path;
	}
	//Method to handle the Insert Keyword functionality
	void insertKeyword(String keyword) throws RemoteException, NotBoundException
	{	
		ArrayList<String> path=new ArrayList<String>();
		path=search(keyword,hashX(keyword),hashY(keyword),"Insert",path);
		System.out.println(" Path Traversed :"+path);
	}
	
	//Method to handle the search functionality
	void searchKeyword(String keyword) throws RemoteException, NotBoundException
	{
		ArrayList<String> path=new ArrayList<String>();
		path=search(keyword,hashX(keyword),hashY(keyword),"Search",path);
		if(path!=null)
			System.out.println("Keyword Found \n"+"path Traversed : "+path);
		else
			System.out.println("Keyword not found!");
	}
	//Updating the boundaries of the node
	Node updateNode(Node updNode, float lx, float ly, float ux, float uy)
	{
		updNode.lx=lx;updNode.ly=ly;
		updNode.ux=ux;updNode.uy=uy;
		return updNode;
	}
	//Extending the neighboring nodes when the Node leaves the network
	void canExtend(Node leaveNode, Node node ) throws RemoteException, NotBoundException
	{
		if (leaveNode.lx > node.lx && node.ly >= leaveNode.ly && node.uy <= leaveNode.uy)
		{
			if(node.ly==leaveNode.ly)
			{
				//Update Node
				node=updateNode(node,node.lx,node.ly,leaveNode.ux,node.uy);
				//Update leave Node
				updateNode(leaveNode,leaveNode.lx,node.uy,leaveNode.ux,leaveNode.uy);
			}else if(node.uy==leaveNode.uy)
			{
				node=updateNode(node,node.lx,node.ly,leaveNode.ux,node.uy);
				updateNode(leaveNode,leaveNode.lx,leaveNode.ly,leaveNode.ux,node.ly);
			}
			ArrayList<Node> newPeerNeighbor=updatePeersNeighbors(node,"LeaveUpdate");
			HashMap<String,String> newPeerKeywords=swapHashTables(node);
			leaveFinalUpdate(node,newPeerNeighbor,newPeerKeywords);
			
		}else if(leaveNode.lx < node.lx &&  node.ly >= leaveNode.ly && node.uy <= leaveNode.uy)
		{
			if(node.ly==leaveNode.ly)
			{
				node=updateNode(node,leaveNode.lx,node.ly,node.ux,node.uy);
				updateNode(leaveNode,leaveNode.lx,node.uy,leaveNode.ux,leaveNode.uy);
				
			}else if(node.uy==leaveNode.uy)
			{
				node=updateNode(node,leaveNode.lx,node.ly,node.ux,node.uy);
				updateNode(leaveNode,leaveNode.lx,leaveNode.ly,leaveNode.ux,node.uy);
			}
			
			ArrayList<Node> newPeerNeighbor=updatePeersNeighbors(node,"LeaveUpdate");
			HashMap<String,String> newPeerKeywords=swapHashTables(node);
			leaveFinalUpdate(node,newPeerNeighbor,newPeerKeywords);
		}else if(leaveNode.ly > node.ly && node.lx >= leaveNode.lx && node.ux <= leaveNode.ux)
		{
			if(node.lx==leaveNode.lx)
			{
				node=updateNode(node,node.lx,node.ly,node.ux,leaveNode.uy);
				updateNode(leaveNode,node.ux,leaveNode.ly,leaveNode.ux,leaveNode.uy);
				
			}else if(node.ux==leaveNode.ux)
			{
				node=updateNode(node,node.lx,node.ly,node.ux,leaveNode.uy);
				updateNode(leaveNode,leaveNode.lx,leaveNode.ly,node.lx,leaveNode.uy);
			}
			ArrayList<Node> newPeerNeighbor=updatePeersNeighbors(node,"LeaveUpdate");
			HashMap<String,String> newPeerKeywords=swapHashTables(node);
			leaveFinalUpdate(node,newPeerNeighbor,newPeerKeywords);
			
		}else if(leaveNode.ly < node.ly && node.lx >= leaveNode.lx && node.ux <= leaveNode.ux){
			if(node.lx==leaveNode.lx)
			{
				node=updateNode(node,node.lx,leaveNode.ly,node.ux,node.uy);
				updateNode(leaveNode,node.ux,leaveNode.ly,leaveNode.ux,leaveNode.uy);
				
			}else if(node.ux==leaveNode.ux)
			{
				node=updateNode(node,node.lx,leaveNode.ly,node.ux,node.uy);
				updateNode(leaveNode,leaveNode.lx,leaveNode.ly,leaveNode.lx,node.ly);
			}
			ArrayList<Node> newPeerNeighbor=updatePeersNeighbors(node,"LeaveUpdate");
			HashMap<String,String> newPeerKeywords=swapHashTables(node);
			leaveFinalUpdate(node,newPeerNeighbor,newPeerKeywords);
		}
		
		
	}
	void leaveFinalUpdate(Node node,ArrayList<Node> newPeerNeighbor,HashMap<String,String> newPeerKeywords) throws RemoteException, NotBoundException
	{
		Registry peerRegistry = LocateRegistry.getRegistry(node.IPAddress, 6000);
		remoteInterface peerRemoteObject = (remoteInterface) peerRegistry.lookup("peer");
		peerRemoteObject.remoteFinalInsertUpdate(node,newPeerKeywords,newPeerNeighbor,"Leave");
	}
	//Remote method to remove the leave node from the neighbors
	public void remoteRemoveFromNeighbors(String IPAddress) throws RemoteException, NotBoundException
	{
		for(int i=0;i < this.neighbours.size();i++)
		{
			if(this.neighbours.get(i).IPAddress.equals(IPAddress))
			{
				this.neighbours.remove(i);
				i--;
			}
		}
	}
	//removing the leave node from the neighbors
	void removeFromNeighbors() throws RemoteException, NotBoundException
	{
		for(int i=0;i < this.neighbours.size();i++)
		{
			Registry peerRegistry = LocateRegistry.getRegistry(this.neighbours.get(i).IPAddress, 6000);
			remoteInterface peerRemoteObject = (remoteInterface) peerRegistry.lookup("peer");
			peerRemoteObject.remoteRemoveFromNeighbors(this.peerNode.IPAddress);
			
		}
	}
	//Updating the Bootstrap Server when the node leaves the network
	void updateBootStrapServer()  throws RemoteException, NotBoundException
	{
		Registry registry = LocateRegistry.getRegistry(this.BootStrapIPAddress, 6000);
		remoteInterface otherObj = (remoteInterface) registry.lookup("server");
		otherObj.remoteUpdateBootStrapServer(this.peerNode.IPAddress);
	}
	
	//Method to handle when the node leaves the network
	void leaveNode() throws RemoteException, NotBoundException
	{
		for(int i=0;i < this.neighbours.size();i++)
		{
			canExtend(this.peerNode,this.neighbours.get(i));
		}
		removeFromNeighbors();
		updateBootStrapServer();
	}
	
	//Method to join a new node into the network
	void Join(String IPAddress, int port, float x, float y) throws RemoteException, NotBoundException, UnknownHostException
	{
		Registry registry = LocateRegistry.getRegistry(IPAddress, port);
		remoteInterface otherObj = (remoteInterface) registry.lookup("server");
		String result = otherObj.getBootStrapNode(InetAddress.getLocalHost().getHostAddress());
		if(result.equals("FirstNode"))
		{
			this.peerNode=new Node(0,0,10,10,InetAddress.getLocalHost().getHostAddress());
		}else{
			System.out.println("result : "+result);
			Registry peerRegistry = LocateRegistry.getRegistry(result, port);
			remoteInterface peerRemoteObject = (remoteInterface) peerRegistry.lookup("peer");
			peerRemoteObject.insert(x,y,InetAddress.getLocalHost().getHostAddress());
		}
			
	}
	//Method to display the Information about the Node
	void viewNode()
	{
		System.out.println("( "+this.peerNode.lx+" , "+this.peerNode.ly+" )");
		System.out.println("( "+this.peerNode.ux+" , "+this.peerNode.uy+" )");
		System.out.println("IPAddress :"+this.peerNode.IPAddress);
		System.out.println("---------------------------------------");
		System.out.println("Neighbors");
		for(int i=0; i < this.neighbours.size();i++)
		{
			System.out.println("( "+this.neighbours.get(i).lx+" , "+this.neighbours.get(i).ly+" )");
			System.out.println("( "+this.neighbours.get(i).ux+" , "+this.neighbours.get(i).uy+" )");
			System.out.println("IPAddress : "+this.neighbours.get(i).IPAddress);
			System.out.println("---------------------------------------");
		}
	}
	
	//Method to take the User commands
	void userPrompt() throws RemoteException, NotBoundException, UnknownHostException
	{
	
		while(true)
		{
			Scanner sc = new Scanner(System.in);
			String input = sc.next();
			if (input.equals("join")) {
				
				System.out.println("Enter Bootstrap Server IP Address");
				this.BootStrapIPAddress=sc.next();
				System.out.println("Enter X and Y Coordinate");
				float x=sc.nextFloat();float y=sc.nextFloat();
				Join(this.BootStrapIPAddress, 6000,x,y);
				
			}else if(input.equals("Insert"))
			{
				System.out.println("Enter the keyword");
				insertKeyword(sc.next());
				
			}else if(input.equals("Search"))
			{
				System.out.println("Enter the keyword");
				searchKeyword(sc.next());
				
			}else if(input.equals("Leave")){
				leaveNode();
			}else if(input.equals("view"))
			{
				viewNode();
			}else{
				System.out.println("Wrong command");
			}
		}
	}
	
	public static void main(String[] args) throws RemoteException, NotBoundException, UnknownHostException {
		Peer pr=new Peer();
		pr.userPrompt();
	}
	
	@Override
	public String getBootStrapNode(String ipAddress) throws RemoteException,
			NotBoundException {
		return null;
	}
	
	public void remoteUpdateBootStrapServer(String ipAddress)throws RemoteException,NotBoundException
	{
		return;
	}

}
