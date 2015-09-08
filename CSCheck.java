package maekawa;

public class CSCheck{

	Integer csClock;
	Integer pClock;
	Boolean critical;
	
	public Boolean getCritical() {
		return critical;
	}

	public void setCritical(Boolean critical) {
		this.critical = critical;
	}

	public CSCheck(Integer c, Integer cl){
		csClock = c;
		pClock = cl;
		critical = false;
	}
	
	public synchronized void get(){
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	public synchronized void set(Integer csClock, Integer clock){
		this.csClock = csClock;
		this.pClock = clock;
		notify();
	}

	public synchronized void Critical(Boolean critical){
		this.critical = critical;
		notify();
	}
	
	public Integer getCsClock() {
		return csClock;
	}

	public void setCsClock(Integer csClock) {
		this.csClock = csClock;
	}

	public Integer getClock() {
		return pClock;
	}

	public void setClock(Integer clock) {
		this.pClock = clock;
	}
	
}
