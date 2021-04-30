import java.io.*;
import java.net.*;
import java.util.*;

class ChatClientListener implements Runnable {
	private MulticastSocket ms;
	private InetAddress group;
	
	/**
	* Constructor
	*
	* @param socket	The multicast socket the client connects to
	* @param address	The address the socket connects to
	*/
	public ChatClientListener(MulticastSocket socket, InetAddress address) {
		ms = socket;
		group = address;
	}

	@Override
	public void run() {
		try {
			while(true) {
				//Create packet to receive response
				byte[] buf = new byte[1024];
				DatagramPacket responsePacket = new DatagramPacket(buf, 0, buf.length);
				ms.receive(responsePacket);
				
				//Decode received bytes to a string message and print it
				String responseMessage = new String(responsePacket.getData(), 0, responsePacket.getLength());
				System.out.println(responsePacket.getAddress() + ": " + responseMessage);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

public class ChatClient {
	
	public static void main(String[] args) {
		try {
			int port = 40202;
			System.out.println("**Communication Begun**" + "\nConnecting on port: " + port + "\nWhen you're finished, type 'exit'");
			MulticastSocket socket = new MulticastSocket(port);
			InetAddress multicastGroup = InetAddress.getByName("239.0.202.1");
			socket.joinGroup(multicastGroup);
			
			//Buffered reader to allow users to write message
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));	

			//Create thread to listen to responses
			Thread t = new Thread(new ChatClientListener(socket,  multicastGroup));			
			t.start();

			while(true) {
				//Retrieve message that user types in
				String helloMessage = br.readLine();	

				//close the multicast group if the user sends exit
				if("exit".equals(helloMessage)) 
					break;	
				
				//Create a datagram packet to send the user's message
				DatagramPacket helloPacket = new DatagramPacket(helloMessage.getBytes(), helloMessage.length(), multicastGroup, port);		
				socket.send(helloPacket);
			}
			
			//Finish communicating with the group
			System.out.println("**Communication Ended**");
			br.close();
			socket.leaveGroup(multicastGroup);
			socket.close();
			System.exit(0);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
