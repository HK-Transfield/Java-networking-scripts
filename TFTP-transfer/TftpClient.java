import java.io.File;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class TftpClient {
	private static final ExecutorService exec =  Executors.newFixedThreadPool(10); 
	private DatagramSocket clientDS;	//Client datagram socket to send and receive packets from server
	private InetAddress serverAddress;	//Server Address to send RRQ packet to
	private int serverPort;		//Server port to send RRQ packet to
	private String filename;		//File name of file requested	
	
	/**
	* Starts the Client and sends a RRQ packet to the server
	*/
	public void startClient() {
		try {
			clientDS = new DatagramSocket();
			
			//Create RRQ packet, set address and port, send to the server
			DatagramPacket RRQdp = TftpUtil.packRRQDatagramPacket(filename.getBytes());
			RRQdp.setPort(serverPort);
			RRQdp.setAddress(serverAddress);
			clientDS.send(RRQdp);
			
			int filenameSize = RRQdp.getLength() - TftpUtil.RRQ;
			System.out.println("file requested : " + filename);
			System.out.println("\nfilename size : " + filenameSize);
			
			TftpClientWorker tcw = new TftpClientWorker(clientDS, filename);	
			exec.execute(tcw);
		}
		catch(Exception e) {
			e.printStackTrace();
		}	
	}
	
	/**
	* Main method that starts the program
	*/
	public static void main(String[] args) {
		TftpClient tc = new TftpClient();
		tc.checkArgs(args);
		try {	
			tc.parseArgsAndInit(args);
			tc.startClient();	
		}
		catch(Exception e) {
			e.printStackTrace();
		}	
	}
	
	/**
	* Checks how many arguements entered into the command-line
	*/
	private void checkArgs(String[] args){
		 if(args.length < 3) {		 
			 System.out.println("Usage: TftpServer <server_ip> <server_port> <file_name>");
			 System.exit(0);
		 }
	}
	
	/**
	* parses the command-line arguments into the appropriate variables
	*/
	private void parseArgsAndInit(String[] args) throws Exception{
	    serverAddress = InetAddress.getByName(args[0]);		    
	    serverPort = Integer.parseInt(args[1]);	
	    filename = args[2];	    	    
   }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////

class TftpClientWorker implements Runnable {
	private DatagramSocket receivedDS;	//Datagram socket to receive packets from
	private DatagramPacket receivedDP;	//Datagram packet received from server
	private String filenameCopyOf;	//The file name of the file requested
	private DatagramPacket ackDP;		//ACK packet to send to server
	private byte blockSeq;			//Stores the block sequence from the received DATA packet
	
	/***
	* Initiates the TftpClientWorker class, which receives DatagramPackets from the 
	* TFTP server and processes each packet
	*/
	public TftpClientWorker(DatagramSocket receivedDS, String filename) throws IOException {
		this.receivedDS = receivedDS;
		filenameCopyOf = "Copy_of_" + filename;	//Create a copy of the file to compare hash value of original file
	}
	
	public void run() {
		try {	
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			do {
				//Receive datagram packet from the server and print information in the commandline
				receivedDP = new DatagramPacket(new byte[TftpUtil.PACKET_BUFFER_SIZE], TftpUtil.PACKET_BUFFER_SIZE);
				receivedDS.receive(receivedDP);
				receivedDS.setSoTimeout(1000);
				
				//Check what type of packet we received and print it
				byte pType = TftpUtil.checkPacketType(receivedDP);
				
				//Packet is an ERROR packet
				if(pType == TftpUtil.ERROR) {
					TftpUtil.printErrorString(receivedDP); //Prints error message
					receivedDS.close();
					System.exit(0);
					break;
				}
				//Packet is a DATA packet
				else if (pType == TftpUtil.DATA) {
					blockSeq = TftpUtil.extractBlockSeq(receivedDP); //Extract block sequence from received DATA packet
					
					//Write data bytes to the ByteArrayOutputStream
					DataOutputStream output = new DataOutputStream(baos);
					output.write(receivedDP.getData(), 2, receivedDP.getLength() - 2);
					System.out.println("stream size : " + baos.size());
					
					sendAcknowledgement(blockSeq);//Send ACK packet				
				}
			}
			while(!isLastPacket(receivedDP));
			
			writeFile(baos);//Write the bytes to the file
			
			System.exit(0);
			return;		
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	* Writes the  data contained in the ByteArrayOutputStream to a file output
	* 
	* @param stream	ByteArrayOutputStream that contains data from packets
	*/
	private void writeFile(ByteArrayOutputStream stream) {
		try {
			FileOutputStream os = new FileOutputStream(filenameCopyOf);
			stream.writeTo(os);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	* Boolean that checks whether or not the received packet is the 
	* last packet in the trasmission
	*
	* @param dp	DatagramPacket that has been received
	* @return	boolean value of packet
	*/
	private boolean isLastPacket(DatagramPacket dp) {
		//Check if the length of data is less than the buffer size
		int dataLength = dp.getLength() - 2;
		
		if (dp.getLength() < TftpUtil.DATA_BUFFER_SIZE) {
			System.out.println("Final data length : " + dataLength);
			return true;
		}
		else {
			System.out.println("Current data length : " + dataLength);
			System.out.println("Received packet block# : " + blockSeq);
			return false;
		}
	}
	
	/**
	* Packs and sends an ACK packet to the server
	*
	* @param block	block sequence number of the packet
	*/
	private void sendAcknowledgement(byte block) {
		try {	
			byte[] ack = new byte[2];
			//Create an ACK packet
			ack[0] = TftpUtil.ACK;
			ack[1] = block;
			ackDP = new DatagramPacket(ack, ack.length, receivedDP.getSocketAddress());
			
			System.out.println("Sent ACK # " + block + "\n");
			
			//Send ACK packet to server
			receivedDS.send(ackDP);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
