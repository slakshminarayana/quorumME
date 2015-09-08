package maekawa;

import java.util.LinkedList;

public class FIFO {

	LinkedList<String> fifo;
	String data;
	
	public FIFO(){
		fifo = new LinkedList<String>();
		data = new String();
	}
	
	public synchronized void putFIFO(String data){
		if(fifo != null){
			fifo.add(data);
			notify();
		}
	}
	
	public synchronized String getFIFO(){
		try{
			if(fifo != null && fifo.isEmpty()){
				wait();
			}
			else if(fifo != null && !fifo.isEmpty()){
				data = new String(fifo.get(0));
				fifo.remove(0);
				return data;
			}
		}
		catch(Exception e){
			System.out.println("Exception in FIFO.");
			e.printStackTrace();
		}
		return "NULL";
	}
}
