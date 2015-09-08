package maekawa;

import java.util.ArrayList;
import java.util.Random;

public class Process {
	
	/*** PROCESS ***/
	// Process's own id.
	private int pid;
	// Process's clock, starts as zero then increments randomly.
	private int clock;
	// Quorum set of process, to which requests will be sent for Critical Section.
	private ArrayList<Integer> quorum;
	
	/*** PROCESS ***/
	
	/*** QUORUM ***/
	// Process id to whom lock is given
	private int lockP = -1;
	// Timestamp to whom lock is given
	private int lockT = -1;
	// Priority queue, who all sent requests to me
	private ArrayList<String> requestQ;
	
	/*** QUORUM ***/
	
	// Constructor
	public Process(int pid, ArrayList<Integer> quorum){
		this.pid = pid;
		this.quorum = quorum;
		clock = 0;
		requestQ = new ArrayList<String>();		// creates empty queue, 'processID,Time'
	}

	// randomly increment clock
	public void incrementClock(){
		Random r = new Random();
		clock += r.nextInt(1)+1;
	}
	
	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public int getClock() {
		return clock;
	}

	public void setClock(int clock) {
		this.clock = clock;
	}

	public ArrayList<Integer> getQuorum() {
		return quorum;
	}

	public void setQuorum(ArrayList<Integer> quorum) {
		this.quorum = quorum;
	}

	public int getLockP() {
		return lockP;
	}

	public void setLockP(int lockP) {
		this.lockP = lockP;
	}

	public int getLockT() {
		return lockT;
	}

	public void setLockT(int lockT) {
		this.lockT = lockT;
	}

	public ArrayList<String> getRequestQ() {
		return requestQ;
	}

	public void setRequestQ(ArrayList<String> requestQ) {
		this.requestQ = requestQ;
	}
	
}
