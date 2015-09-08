package maekawa;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.UnresolvedAddressException;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class Maekawa_Client {
	public static final int MESSAGE_SIZE = 1000;
	public static final String domain = ".utdallas.edu";
	
	public void live(String next, String controlMsg){
		SctpChannel sctpChannel = null;
		
		try{
		System.out.println("Message:"+controlMsg+". , "+"TO = "+next);
		String[] tempArray = next.split("-");
		String hostname = tempArray[0];
		hostname = hostname.concat(domain);
		int port = Integer.parseInt(tempArray[1]);
		
		//Buffer to hold messages in byte format
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		//Create a socket address for server at hostname at port
		SocketAddress socketAddress = new InetSocketAddress(hostname,port);
		//Open a channel. NOT SERVER CHANNEL
		sctpChannel = SctpChannel.open();
		
		// Bind the channel's socket to a local port. Again this is not a server bind
		// Using destination's port as my local port, avoids duplicates.
		
		// The bind method takes a SocketAddress as its argument which typically contains a port number as well as an address.
		// Establish a relationship between the socket and the local addresses.
		// Once a relationship is established then the socket remains bound until the channel is closed.
		// Can use unbindAddress, but there will always be atleast one local address bound to channel's socket once invocation is successful
		sctpChannel.bind(new InetSocketAddress(0));
		// SCTP port number remains the same for the lifetime of the channel.
		
		//Connect the channel's socket to the remote server, this throws exception of Connection Refused
		sctpChannel.connect(socketAddress);
		//Before sending messages add additional information about the message
		MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
		
		//convert the string message into bytes and put it in the byte buffer, giving BufferOverFlowException
		//byteBuffer.put(message.getBytes());
		byteBuffer.put(controlMsg.getBytes(),0,controlMsg.getBytes().length);
		
		//Reset a pointer to point to the start of buffer 
		byteBuffer.flip();
		//Send a message in the channel (byte format)
		sctpChannel.send(byteBuffer,messageInfo);
		}
		catch(UnsupportedOperationException e){
			System.out.println("UnsupportedOperationException in Client : "+e.getMessage());
			e.printStackTrace();
		}
		catch(UnresolvedAddressException e){
			System.out.println("UnresolvedAddessException in Client : "+e.getMessage());
			e.printStackTrace();
		}
		catch(ClosedByInterruptException e){
			System.out.println("ClosedByInterruptException in Client : "+e.getMessage());
			e.printStackTrace();
		}
		catch(AsynchronousCloseException e){
			System.out.println("AsyncCloseException in Client : "+e.getMessage());
			e.printStackTrace();
		}
		catch(IOException e){
			System.out.println("IOException in Client : "+e.getMessage());
			e.printStackTrace();
		}
		finally{
			if(sctpChannel != null){
				try {
					sctpChannel.close();
				} catch (IOException e) {
					System.out.println("In Client: Error while Closing Channel");
					e.printStackTrace();
				}
			}
		}
	}

}
