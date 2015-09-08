package maekawa;

import java.util.ArrayList;
import java.util.TreeMap;

public class Server_Thread implements Runnable{

	TreeMap<Integer, String> nodeValue;
	Maekawa_Server server;
	
	public Server_Thread(TreeMap<Integer,String> nodeValue, int index, ArrayList<Integer> quorum, CSCheck csCheck, FIFO fifo){
		this.nodeValue = nodeValue;
		server = new Maekawa_Server(index, quorum, csCheck, fifo);
	}
	
	public void run() {
		try {
			server.live(nodeValue);
		} catch (Exception e) {
			System.out.println("ERROR FROM SERVER_THREAD");
		}
	}

}
