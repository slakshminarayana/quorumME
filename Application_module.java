package maekawa;

import java.util.concurrent.TimeUnit;

public class Application_module {

	public void runCS(){
		for(int i=1; i<=10; i++)
			System.out.println("I am in Critical Section");
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			System.out.println("Interrupted while executing Critical Section");
		}
	}	
}
