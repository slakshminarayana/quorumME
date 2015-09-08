package maekawa;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class CS_Thread implements Runnable{

	CSCheck csCheck;
	FIFO fifo;
	int index;
	TreeMap<Integer, String> nodeValue;
	Application_module module1;
	ArrayList<Integer> quorum;
	Boolean run;
	Boolean test;
	public CS_Thread(TreeMap<Integer, String> nodeValue, int index, ArrayList<Integer> quorum, CSCheck csCheck, Boolean run, Boolean test, FIFO fifo){
		this.nodeValue = nodeValue;
		this.index = index;
		this.csCheck = csCheck;
		this.run = run;
		this.test = test;
		this.quorum = quorum;
		this.fifo = fifo;
		module1 = new Application_module();
	}
	
	public void run() {
		try {
			TimeUnit.SECONDS.sleep(40);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	if(run)
		{				
		for(int i = 1; i <= 5; i++){	
			cs_enter();
			// Start Time for CS
			int s = csCheck.getCsClock();
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			module1.runCS();
			// Increment the Critical Section Logical Clock
			csClock();
			// End Time for CS
			int t = csCheck.getCsClock();
		
			cs_leave(s,t);
		}
		System.out.println("Done Requesting Critical Section - "+index);
	}
	if(test){
		if(!run){
			try {
				TimeUnit.SECONDS.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		StringBuffer req=new StringBuffer().append("INFO").append(",").append(index);
		for(int i = 1; i <= nodeValue.size(); i++){
			fifo.putFIFO(new StringBuffer().append(nodeValue.get(i)).append("=").append(req.toString()).toString() );
		}
	}
}

	// randomly increment clock
	public void csClock(){
		csCheck.setCsClock(csCheck.getCsClock()+new Random().nextInt(100)+10);
	}
	
	// Generate Request
	public void cs_enter(){
		try {
			String controlMsg = new StringBuffer().append("REQ").append(",").append(index).append(",").append(csCheck.getClock()).toString();
			for(Integer i : quorum){
				fifo.putFIFO(new StringBuffer().append(nodeValue.get(i)).append("=").append(controlMsg).toString());
			}
			csCheck.get();
			csCheck.setCritical(true);
		} catch (Exception e) {
			System.out.println("CS_THREAD: exception - REQUEST NOT GENERATED"+e.getMessage());
		}
	}
	
	public void cs_leave(int s, int t){
		String controlMsg = new StringBuffer().append("GO").append(",").append(s).append(",").append(t).append(",").append(index).toString();
		System.out.println("CS TIMES: "+s+" , "+t);
		for(int i = 1; i <= nodeValue.size(); i++){
			fifo.putFIFO(new StringBuffer().append(nodeValue.get(i)).append("=").append(controlMsg).toString());
		}
		// Blocking itself, till I send my own REL message, before sending out another request
		csCheck.get();
	}
	
	// For termination
	public void done(){
		// Send out DONE,P(i) message to everyone
		String controlMsg = new StringBuffer().append("DONE").append(",").append(index).toString();
		System.out.println("Sending out Done message");
		for(int i = 1; i <= nodeValue.size(); i++){
			fifo.putFIFO(new StringBuffer().append(nodeValue.get(i)).append("=").append(controlMsg).toString());
		}
	}
}
