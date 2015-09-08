package maekawa;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

// Assumption: once process send REQ for CS, it can only send another REQ, when it has completed its current REQ for CS.
// So priority queue will not have duplicate requests from same process
// format, "process_id,timestamp"
// Issues:
// 1. Need to check for proper quorums, property of intersection between every quorum - No longer required
// 2. Need to increment CLOCK, randomly - 
// only if I am the process, so the inter-critical section time and duration of critical section has to be done.
// Process - clock runs for each process, 

public class Maekawa_Server {

	private static final int MESSAGE_SIZE = 1000;
	
	Process pro;
	FIFO fifo;
	
	int timeCounter = 0;			// (as Process)  --  Verification
	ArrayList<String> timeBasket = new ArrayList<String>(); 	// (as Process)  --  Verification
	ArrayList<Integer> pid = new ArrayList<Integer>();			// (as Process)  --  Verification
	TreeMap<Integer, String> nodeValue;
	ArrayList<Integer> lockQueue = new ArrayList<Integer>();
	LinkedList<String> csTime = new LinkedList<String>();		// (as Process)  --  Verification
	// Critical Section Logical Clock
	CSCheck csCheck;					// (as Process), keeps the latest my clock time, CS clock time
	int done = 0;
	boolean inquire = false;		// (as Quorum)
	ArrayList<Integer> fails = new ArrayList<Integer>();		// (as Process)
	
	/**
	 * Message Format:
	 * REQ,P(i),T(i)	sent as Process
	 * REL,P(i)			sent as Process
	 * YIELD,P(i)		sent as Process
	 * LOCK,P(i)		sent as Quorum
	 * INQ,P(i)			sent as Quorum
	 * FAIL,P(i)		sent as Quorum
	 * 
	 * GO,s,t - sent from my CS thread to myself, 
	 * GO - tells to send REL messages to Quorum, 
	 * s - tells logical clock time (Start Time).
	 * t - tells logical clock time (End Time). 
	 */
	
	public Maekawa_Server(int index, ArrayList<Integer> quorum, CSCheck csCheck, FIFO fifo){
		this.fifo = fifo;
		this.csCheck = csCheck;
		pro = new Process(index, quorum);
	}
	
	@SuppressWarnings("static-access")
	public void live(TreeMap<Integer, String> nodes) {
		// get the list of machineName-portNumber
		this.nodeValue = nodes;
		// update the common object
		pro.incrementClock();
		csCheck.setClock(pro.getClock());
		
		SctpServerChannel serverChannel = null;
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		
		// My port
		String[] mc = nodeValue.get(pro.getPid()).split("-");
		int port = Integer.parseInt(mc[1]);
		System.out.println("Machine:"+mc[0]+", Port:"+mc[1]);
	try{
		
		// Open the server channel
		serverChannel = SctpServerChannel.open();
		
		// socket address at given port
		InetSocketAddress serverAddr = new InetSocketAddress(port);
		// Binding the socket address with channel
		serverChannel.bind(serverAddr);
		System.out.println("Server Up");
		// No Termination	
		while(true){	
		// Blocks till the receive event occurs
		SctpChannel sctpCh = serverChannel.accept();
		MessageInfo messageInfo = sctpCh.receive(byteBuffer, null, null);
		
		String messagePart = byteToString(byteBuffer, messageInfo.bytes());
		
		// Clearing to receive message
		byteBuffer.clear();
		
		String controlMsg = null;
		String[] msg = messagePart.split(",");
		
		System.out.println("Queue in "+pro.getPid()+" : "+pro.getRequestQ());
		
		if(msg[0].equals("REQ")){
			// message values
			StringBuffer buf = new StringBuffer().append(msg[1]).append(",").append(msg[2]);
			System.out.println("GOT REQUEST in "+pro.getPid()+" : "+messagePart);
			// priority queue is empty
			if(pro.getRequestQ().isEmpty()){
				pro.setLockP(Integer.parseInt(msg[1]));
				pro.setLockT(Integer.parseInt(msg[2]));
				pro.getRequestQ().add(buf.toString());
				controlMsg = new StringBuffer().append("LOCK").append(",").append(pro.getPid()).toString();
				fifo.putFIFO(new StringBuffer().append(nodeValue.get(Integer.parseInt(msg[1]))).append("=").append(controlMsg).toString());
			}
			else{
					if(pro.getLockT() < Integer.parseInt(msg[2])){
						add(buf.toString(), pro.getRequestQ());
						controlMsg = new StringBuffer().append("FAIL").append(",").append(pro.getPid()).toString();
						fifo.putFIFO(new StringBuffer().append(nodeValue.get(Integer.parseInt(msg[1]))).append("=").append(controlMsg).toString());
					}
					else if(pro.getLockT() > Integer.parseInt(msg[2])){
						// lock holder has higher timestamp
						add(buf.toString(), pro.getRequestQ());
						if(!inquire){
							inquire = true;
							controlMsg = new StringBuffer().append("INQ").append(",").append(pro.getPid()).toString();
							fifo.putFIFO(new StringBuffer().append(nodeValue.get(pro.getLockP())).append("=").append(controlMsg).toString());
						}
					}
					else if(pro.getLockT() == Integer.parseInt(msg[2])){
						// need to compare process ids
						if(pro.getLockP() < Integer.parseInt(msg[1])){
							add(buf.toString(), pro.getRequestQ());
							controlMsg = new StringBuffer().append("FAIL").append(",").append(pro.getPid()).toString();
							fifo.putFIFO(new StringBuffer().append( nodeValue.get(Integer.parseInt(msg[1])) ).append("=").append(controlMsg).toString());
						}
						else if(pro.getLockP() > Integer.parseInt(msg[1])){
							// lock holder has same timestamp but higher process id
							add(buf.toString(), pro.getRequestQ());
							if(!inquire){
								inquire = true;
								controlMsg = new StringBuffer().append("INQ").append(",").append(pro.getPid()).toString();
								fifo.putFIFO(new StringBuffer().append( nodeValue.get(pro.getLockP()) ).append("=").append(controlMsg).toString());
							}
						}
						else if(pro.getLockP() == Integer.parseInt(msg[1])){
							throw new Exception("ERROR: Process ids cannot be same");
						}
					}
			} // else part of non-empty requestQ
			
		}
		
		else if(msg[0].equals("REL")){
			System.out.println("GOT RELEASE in "+pro.getPid()+" : "+messagePart);
			
			inquire = false;
			
			// need to find(compare pid) my index in priority queue, then remove it
			for(int i=0; i < pro.getRequestQ().size(); i++){
				String value = (String)pro.getRequestQ().get(i);
				String[] temp = value.split(",");
				// Compares ProcessIDs
				if(temp[0].equals(msg[1])){
					pro.getRequestQ().remove(i);
					break;
				}
			}
			if(pro.getRequestQ().isEmpty()){
				// No one has request for my lock
				System.out.println("No Pending Request in "+pro.getPid());
				pro.setRequestQ(new ArrayList<String>());
				pro.setLockP(-1);
				pro.setLockT(-1);
			}
			else{
				String value = (String) pro.getRequestQ().get(0);
				String[] temp = value.split(",");
				pro.setLockP(Integer.parseInt(temp[0]));
				pro.setLockT(Integer.parseInt(temp[1]));
				// need to send the lock message
				controlMsg = new StringBuffer().append("LOCK").append(",").append(pro.getPid()).toString();
				fifo.putFIFO(new StringBuffer().append( nodeValue.get(pro.getLockP()) ).append("=").append(controlMsg).toString());
			}
		}
		
		else if(msg[0].equals("YIELD")){
			System.out.println("GOT YIELD in "+pro.getPid()+" : "+messagePart);
			inquire = false;
			String value = (String) pro.getRequestQ().get(0);
			String[] temp = value.split(",");
			// overriding the Lock values
			pro.setLockP(Integer.parseInt(temp[0]));
			pro.setLockT(Integer.parseInt(temp[1]));
			controlMsg = new StringBuffer().append("LOCK").append(",").append(pro.getPid()).toString();
			fifo.putFIFO(new StringBuffer().append( nodeValue.get(pro.getLockP()) ).append("=").append(controlMsg).toString());
		}
		
		else if(msg[0].equals("LOCK")){
			System.out.println("GOT LOCK in "+pro.getPid()+" : "+messagePart);
			// Process Clock incremented
			pro.incrementClock();
			csCheck.setClock(pro.getClock());
			if(fails.contains(Integer.parseInt(msg[1]))){
				fails.remove(fails.indexOf(Integer.parseInt(msg[1])));
			}
			lockQueue.add(Integer.parseInt(msg[1]));
			// if I get the locks from all my quorum members
			if(lockQueue.size() == pro.getQuorum().size()){
				csCheck.setCritical(true);
				//critical = true;
				if(fails.size() == 0){
					System.out.println("No Fail Message and Critical Set");
					// Resetting the fails queue
					fails = new ArrayList<Integer>();
					System.out.println("Going for CS : "+pro.getPid());
					// Notifies the cs_thread
					csCheck.set(csCheck.getCsClock(), csCheck.getClock());
				}
				else{
				}
			}
			else{
			}
		}
		
		else if(msg[0].equals("GO")){
			System.out.println("GOT GO in "+pro.getPid()+" : "+messagePart);
			// Check if myself
			if(pro.getPid() == Integer.parseInt(msg[3])){
				
				// PHASE ONE
				csTime.add(new StringBuffer().append(msg[1]).append(":").append(msg[2]).toString());
				// Sending REL to my QUORUM
				Thread.currentThread().sleep(2000);
				for(int i = 0; i < pro.getQuorum().size(); i++){
					controlMsg = new StringBuffer().append("REL").append(",").append(pro.getPid()).toString();
					fifo.putFIFO(new StringBuffer().append( nodeValue.get(pro.getQuorum().get(i)) ).append("=").append(controlMsg).toString());
				}
				lockQueue = new ArrayList<Integer>();
				csCheck.Critical(false);
			}
			else{
				// Keeping the latest csClock
				csCheck.setCsClock(max( csCheck.getCsClock(),Integer.parseInt(msg[2]) ) + 1);
			}
		}
		
		else if(msg[0].equals("INQ")){
			System.out.println("GOT INQUIRE in "+pro.getPid()+" : "+messagePart);
			// Process Clock incremented
			pro.incrementClock();
			csCheck.setClock(pro.getClock());
			if(csCheck.getCritical()){
				// When I am in critical section, discard INQ message, do not respond
			}
			else{
				if( !fails.isEmpty() ){
					System.out.println("Lock Queue in "+pro.getPid()+" : "+lockQueue);
					// Cannot have same process in both fails and lockQueue, I can only YIELD for lock that I have.
					if( lockQueue.contains(Integer.parseInt(msg[1])) && !fails.contains(Integer.parseInt(msg[1]))){
						// decrement counter, as held Lock is being given away
						//lockCount--;
						lockQueue.remove(lockQueue.indexOf(Integer.parseInt(msg[1])));
						// need to send the yield message
						controlMsg = new StringBuffer().append("YIELD").append(",").append(pro.getPid()).toString();
						fifo.putFIFO(new StringBuffer().append( nodeValue.get(Integer.parseInt(msg[1])) ).append("=").append(controlMsg).toString());
					}
				}
				else{
					// Haven't received any FAIL messages
					// Do not yield then
				}
			}
		}
		
		// When I get FAIL message, behave as PROCESS
		else if(msg[0].equals("FAIL")){
			System.out.println("GOT FAILED in "+pro.getPid()+" : "+messagePart);
			// Process Clock incremented
			pro.incrementClock();
			csCheck.setClock(pro.getClock());
			// Add the processID who sent me FAIL message, no duplicate ProcessIDS - ASSUMPTION
			// Keeps the check for corresponding processes who sent me FAIL messages
			// On receiving LOCK from them I should remove them from Fail List
			fails.add(Integer.parseInt(msg[1]));
			if(lockQueue.contains(Integer.parseInt(msg[1]))){
				System.out.println("Error : *************** Cannot be contained both in Lock and Fail queues ********************");
			}
		}
		
		else if(msg[0].equals("INFO")){
			System.out.println("GOT CS INFO in "+pro.getPid()+" : "+messagePart);
			StringBuffer buffer = null;
			if(!csTime.isEmpty()){
				buffer = new StringBuffer().append("TIME").append(",").append(pro.getPid());
				for(String i : csTime){
					buffer.append(",").append(i);
				}
			}
			else{
				buffer = new StringBuffer().append("TIME").append(",").append(pro.getPid()).append(",").append("-1");
			}
			fifo.putFIFO(new StringBuffer().append( nodeValue.get( Integer.parseInt(msg[1]) ) ).append("=").append(buffer.toString()).toString());
		}
		
		else if(msg[0].equals("TIME")){
			System.out.println("GOT CS TIME in "+pro.getPid()+" : "+messagePart);
			timeCounter++;
			if(msg[2].equals("-1"))
			{
				if(timeCounter==nodeValue.size()){
					System.out.println("***** No Critical Section Executed *****");
					timeCounter = 0;
				}
				continue;
			}
			for(int i=2; i<msg.length ;i++)
			{
				timeBasket.add(msg[i]);
				pid.add(Integer.parseInt(msg[1]));
			}
			if(timeCounter==nodeValue.size())
			{
				System.out.println("TimeBasket and Pid");
				for(int i=0; i< timeBasket.size(); i++)
				{
					System.out.print(timeBasket.get(i)+",");
					System.out.print(pid.get(i)+",");
				}
				timeCounter = 0;
				// Verification Can be given to different Thread for computation
				verify(timeBasket, pid);
			}
			
		}
		
		else if(msg[0].equals("DONE")){
			done++;
			if(done == nodeValue.size()){
				System.out.println("Terminating : "+pro.getPid());
				System.exit(1);
			}
		}
		
	} // end of While
	}
	catch(Exception e){
		e.printStackTrace();
	}
}

	// Verify the Overlapping of Critical Section Times.
	public void verify(ArrayList<String> timeBasket, ArrayList<Integer> pid){
		ArrayList<Integer> start = new ArrayList<Integer>();
		ArrayList<Integer> end = new ArrayList<Integer>();
		ArrayList<Integer> processID = new ArrayList<Integer>();
		
		for(int i=0; i < timeBasket.size(); i++){
			String[] words = timeBasket.get(i).split(":");
			int s = Integer.parseInt(words[0]);
			int t = Integer.parseInt(words[1]);
			insert(s,t,start,end,processID,pid.get(i));
		}
		Boolean mutual = true;
		// Checking Overlaps
		for(int i=0; i < end.size()-1; i++){
			if(end.get(i) <= start.get(i+1)){
				continue;
			}
			else{
				System.out.println("*****\\ Mutual Exclusion Violated /*****");
				// Add who violated the mutual exclusion
				System.out.println("*****\\ Violating Processes : "+processID.get(i)+" and "+processID.get(i+1)+" /*****");
				mutual = false;
			}
		}
		if(mutual)
		{
			System.out.println("***** Mutual Exclusion Successful *****");
		}
	}
	
	// Assumes start and end are already sorted
	public void insert(int s, int t, ArrayList<Integer> start, ArrayList<Integer> end, ArrayList<Integer> process, int pid){
		if(start.isEmpty() && end.isEmpty()){
			start.add(s);
			end.add(t);
			process.add(pid);
		}
		else{
			for(int i = 0; i < start.size(); i++){
				if(start.get(i) < s){
					if(i == (start.size()-1)){
						start.add(s);
						end.add(t);
						process.add(pid);
						break;
					}
					else{
						continue;
					}
				}
				else if(start.get(i) == s){
					if(end.get(i) < t){
						if(i == (start.size()-1)){
							start.add(s);
							end.add(t);
							process.add(pid);
							break;
						}
						else{
							continue;
						}
					}
					else if(end.get(i) >= t){
						start.add(i,s);
						end.add(i,t);
						process.add(i,pid);
						break;
					}
				}
				else{
					start.add(i,s);
					end.add(i,t);
					process.add(i,pid);
					break;
				}
			}
		}
	}
	
	// Adds the element at correct index 
	public void add(String request, ArrayList<String> requestQ){
		for(int i=0; i < requestQ.size(); i++){
			String value = (String) requestQ.get(i);
			String[] temp = value.split(",");
			String[] req = request.split(",");
			if(Integer.parseInt(req[1]) < Integer.parseInt(temp[1])){
				requestQ.add(i, request);
				break;
			}
			else if(Integer.parseInt(req[1]) == Integer.parseInt(temp[1])){
				if(Integer.parseInt(req[0]) < Integer.parseInt(temp[0])){
					requestQ.add(i, request);
					break;
				}
				else if(i == requestQ.size()-1){
					requestQ.add(request);
					break;
				}
			}
			else if(i == requestQ.size()-1){
				requestQ.add(request);
				break;
			}
		}
	}
	
	// sorts the request queue as priority
	// sort by ascending timestamps, if timestamps equal then sort by ascending process ids
	// format, "process_id,timestamp"
	public void sortQ(ArrayList<String> requestQ){
		for(int i=0; i< (requestQ.size()-1); i++){
			int small = i;
			for(int j = i+1; j < requestQ.size(); j++){
				String value1 = (String) requestQ.get(small);
				String[] temp1 = value1.split(",");
				String value2 = (String) requestQ.get(j);
				String[] temp2 = value2.split(",");
				// if timestamp is lower
				if(Integer.parseInt(temp1[1]) > Integer.parseInt(temp2[1])){
					small = j;
				}
				// if timestamp is equal
				else if(Integer.parseInt(temp1[1]) == Integer.parseInt(temp2[1])){
					// check for process ids
					if(Integer.parseInt(temp1[0]) > Integer.parseInt(temp2[0])){
						small = j;
					}
				}
			}// inner for loop
		String swap = (String)requestQ.get(i);
		requestQ.set(i, requestQ.get(small));
		requestQ.set(small, swap);
		}
		
		
	}

	public int max(int x, int y){
		if(x < y){
			return y;
		}
		else{
			return x;
		}
	}
	
	public String byteToString(ByteBuffer byteBuffer, int length)
	{
		byteBuffer.position(0);
		byteBuffer.limit(MESSAGE_SIZE);
		byte[] bufArr = new byte[length];
		byteBuffer.get(bufArr);
		return new String(bufArr);
	}
	
}
