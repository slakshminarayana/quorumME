package maekawa;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import maekawa.Server_Thread;

public class Project2 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Argument gives the index of this process to take from configuration file.
		int index = Integer.parseInt(args[0]);
		Boolean run = false;
		if(Integer.parseInt(args[1]) == 1){
			run = true;
		}
		Boolean test = false;
		if(Integer.parseInt(args[2]) == 1){
			test = true;
		}
		
		TreeMap<Integer, String> nodeValue = new TreeMap<Integer, String>();

		ArrayList<Integer> quorum = new ArrayList<Integer>();
		
	try{
		BufferedReader reader = new BufferedReader(new FileReader("maekawa/Configuration File.txt"));
		String line = null;
		// get the number of nodes
		boolean nodeCount = true;
		int nodes = 0;

		int hostCount = 0;
		try {
			while((line = reader.readLine()) != null){
				String[] buffer = line.split(" ");
				// Skipping the # lines
				if(buffer[0].equals("#") || line.isEmpty()){
					continue;
				}
				else if(nodeCount){				// Assuming the number of nodes occur first 
					nodeCount = false;
					nodes = Integer.parseInt(buffer[0]); // same as hostcount variable
					System.out.println("Number of Nodes:"+nodes);
				}
				else if(!nodeCount){
					// Getting the hostname, port, quorum details
					hostCount++;
					String host = buffer[0];
					String port = buffer[1];
					nodeValue.put(hostCount,new String(host+"-"+port));
					// my values
					if(index == hostCount){
						for(int i = 2; i < buffer.length; i++){
							if( (!quorum.isEmpty()) && quorum.contains(Integer.parseInt(buffer[i])) ){
								continue;
							}
							quorum.add(Integer.parseInt(buffer[i]));
						}
						if(quorum.isEmpty()){
							System.out.println("No Quorum Given for: "+index+" - Process");
							System.exit(0);
						}
					}
				}
				
			}
			
			System.out.println("NodeValue map");
			for(int i = 1; i <= hostCount; i++){
				System.out.println(i+" : "+nodeValue.get(i));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	catch (FileNotFoundException e) {
			e.printStackTrace();
	}
	
	// Create the common object
	CSCheck csCheck = new CSCheck(new Integer(0), new Integer(0));
	FIFO fifo = new FIFO();
	
	// Starts Client with FIFO
	Thread client = new Thread(new Client_Thread(fifo));
	client.start();
	
	// Starts SERVER bundled with Module_2 - Mutual Exclusion Module
	Thread server = new Thread(new Server_Thread(nodeValue, index, quorum, csCheck, fifo),"Server-"+index);
	server.start();
	
	// Starts Module_1 - Application Module
	Thread csThread = new Thread(new CS_Thread(nodeValue, index, quorum, csCheck, run, test, fifo));
	csThread.start();
	
}	// end of main

}
