import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class TftpServer {
	private InetAddress serverAddress;
	private  int serverPort;
	private DatagramSocket serverSocket; 
        private static final ExecutorService exec =  Executors.newFixedThreadPool(10); 
    
	 public void startServer()
	    {
		try {
	     serverSocket = new DatagramSocket(serverPort, serverAddress);
		 System.out.println("TftpServer on port " + serverSocket.getLocalPort());
	
		   while(true) {
			byte[] buf = new byte[1472];
			DatagramPacket p = new DatagramPacket(buf, 1472);
			System.out.println("waiting for connection ....");
			serverSocket.receive(p);			
			TftpServerWorker worker = new TftpServerWorker(p);
		        exec.execute(worker);
		    }
		}
		catch(Exception e) {
		    System.err.println("Exception: " + e);
		}
		serverSocket.close();
		return;
	  }
	
	
	public static void main(String[] args) {
		TftpServer tftpServer = new TftpServer();
		tftpServer.checkArgs(args);
		try{
			tftpServer.parseArgsAndInit(args);
			tftpServer.startServer();
		}
		catch(Exception e){
			e.printStackTrace();
			
		}
	}

	private void checkArgs(String[] args){
		 if(args.length <2) {		 
			 System.out.println("Usage: TftpServer server_ip server_port");
			 System.exit(0);
		 }
	}
	
	private void parseArgsAndInit(String[] args) throws Exception{
	    serverAddress = InetAddress.getByName(args[0]);		    
	    serverPort = Integer.parseInt(args[1]);		    	    
   }	
}

class TftpServerWorker implements Runnable
{
    //use to store RRQ request
    private DatagramPacket req;
    //client address who sent the RRQ request 
    private SocketAddress clientAddress;
    //socket used to send the file data packets to client
    private DatagramSocket sendfileSocket;
    //byte buffer to store the file data
    private byte[] dataBuffer = new byte[TftpUtil.DATA_BUFFER_SIZE];
    //the first block sequence
    private byte currentBlockSeq = 1;
    //use to retrieve the ack packet, only two bytes long
    private DatagramPacket ackDP = new DatagramPacket(new byte[2], 2);
    private int TIME_OUT = 1000; //1 second 
    
 	
    public void run()
    {
     try{
    		sendfileSocket = new DatagramSocket();
    		System.out.println("TftpServer sending a file on port " + sendfileSocket.getLocalPort());
		byte pType = TftpUtil.checkPacketType(req);
		clientAddress = req.getSocketAddress();
		//checking if the first packet from client is a RRQ packet 
	    		if(pType == TftpUtil.RRQ){
	    			String filename = getFileName(req);
	    			System.out.println("Requested file name:" + filename);
	    			//if the file doesn't exist, send ERROR packet and close socket
	    			if(!(new File(filename)).exists()) {
	    				DatagramPacket errorDP = TftpUtil.packErrorPacket(filename);
	    				errorDP.setSocketAddress(clientAddress);
	    				sendfileSocket.send(errorDP);
	    			}
	    			else{
	    				//the file does exist, send file
	    				sendfile(filename);
	    			}
	    		}// end if
	
        }
		catch(Exception e){		
			e.printStackTrace();			
		}
    	    sendfileSocket.close();	
  	    return;
    }

     
	 private void sendfile(String filename) throws Exception
	    {
	    	    FileInputStream fileInput = new FileInputStream(filename);			    	   
	    	    while(true){
			      int rec = fileInput.read(dataBuffer);
			      //the file size is a multiple of 512, send empty packet 
			      if(rec == -1){
			             sendDataPacket(new byte[0],0);
			             System.out.println("The last packet [0 byte in size]:#"+currentBlockSeq);
			             break;
			        }	
			        //send a file data packet
			        boolean successed = sendDataPacket(dataBuffer,rec);
			       //tried five times
			       if (!successed) {
			    	       System.out.println("Tried five times, give up");
			    	        break;
			       }
			       // the last packet (the file size if not a multiple of 512)
			       if (rec < 512 && rec > 0 ) {
			           System.out.println("The last packet ["+rec+" bytes in size]:#"+currentBlockSeq);
			    	        break; 
			       }
			       currentBlockSeq++;       
	        	}//while
	    	    fileInput.close();
	    	 }
	
	 //
	 private  boolean sendDataPacket(byte[] databuffer,int length) throws Exception{
		 int resendCount = 0;
	
	       DatagramPacket dataPacket = packFileDataPacket(databuffer,length);
	       //try five times
     	    while(resendCount < TftpUtil.MAX_RESEND){
     	   	  try{
		    	      sendfileSocket.send(dataPacket); 
		        	  sendfileSocket.setSoTimeout(TIME_OUT);
   		          System.out.println("sent data block #"+currentBlockSeq+", waiting for ack #" + currentBlockSeq);
   		          //ack arrives
   		          sendfileSocket.receive(ackDP);
   		          byte ackedBlockseq = TftpUtil.extractACKNumber(ackDP);
   		          System.out.println("received ack #" + ackedBlockseq);
   		         if(ackedBlockseq != currentBlockSeq) {
   		             //the acked block seq is not the seq of block sent
      		          //ignore this ack and resend  		        	       
      	        	       resendCount++; 
   		        	       continue;
   		        	   } 
   		            //this data packet has been acked, return
   		            return true; 
		    
     	         }//end of try
     	         catch(SocketTimeoutException ste){
     	    	        resendCount++;
     	          	System.out.println("timeout #" + resendCount );
     	         }
     	     }//end of while
   	   	
   	        return false;  	
   }
	 
	 
	 private DatagramPacket packFileDataPacket(byte[] dataBuffer, int length){
		 int packetLength = 2 + length;//type (1) + block seq (1) + data length
	   	  ByteBuffer byteBuffer = ByteBuffer.allocate(packetLength); 
	 	  byteBuffer.put(TftpUtil.DATA);//type
	 	  byteBuffer.put(currentBlockSeq);//block seq
		  byteBuffer.put(dataBuffer,0,length);//data	 
		  DatagramPacket dataPacket = new DatagramPacket(byteBuffer.array(), packetLength);
		  dataPacket.setSocketAddress(clientAddress);
	      return  dataPacket;
		 
	 }
	 
   private  String getFileName(DatagramPacket dataDP){  
   	  byte[] data = dataDP.getData();
   	  int dataLength = dataDP.getLength();
      ByteBuffer byteBuffer = ByteBuffer.allocate(dataLength-1); 
      //remove the packet type (RRQ)
	  byteBuffer.put(data,1,dataLength-1);
      return new String(byteBuffer.array());
  }

    public TftpServerWorker(DatagramPacket req)
    {
	this.req = req;
    }
}

