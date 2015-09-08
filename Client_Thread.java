package maekawa;
import maekawa.FIFO;
import maekawa.Maekawa_Client;

public class Client_Thread implements Runnable{

	Maekawa_Client client;
	FIFO fifo;
	String next;
	String controlMsg;
	public Client_Thread(FIFO fifo){
		this.fifo = fifo;
		client = new Maekawa_Client();
	}
	
	public void run() {
		while(true){
			// Gets blocked here if FIFO is empty
			String data = fifo.getFIFO();
			if(!data.equals("NULL")){
				String temp[] = data.split("=");
				next = temp[0];
				controlMsg = temp[1];
				client.live(next, controlMsg);
			}
		}	
	}
}
