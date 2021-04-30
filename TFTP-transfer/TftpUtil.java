import java.net.DatagramPacket;
import java.nio.ByteBuffer;

public class TftpUtil {
	
	//read request packet 
	public static final byte RRQ = 1;
	//data packet
	public static final byte DATA = 2;
	//ack packet
	public static final byte ACK = 3;
	//error packet
	public static final byte ERROR = 4;
	//the maximum number of resent
	public static final int MAX_RESEND = 5; 	
	//data buffer size: 512
	public static int DATA_BUFFER_SIZE = 512;
	//packet buffer size: 2 bytes header + 512 bytes file data;
	public static int PACKET_BUFFER_SIZE = DATA_BUFFER_SIZE + 2;  
	
	//return the type (RRQ, DATA, ACK or ERROR) of a packet  
	public static byte checkPacketType(DatagramPacket dataDP){
		   	byte[] payload = dataDP.getData();   	
		   	return payload[0];
      }
	 
	 //return a RRQ packet 
	public static DatagramPacket packRRQDatagramPacket(byte[] filename) throws Exception{
		return packDatagramPacket(RRQ, filename); 	
    }	
	
	//return a "file not found" error packet
	public static DatagramPacket packErrorPacket(String filename) throws Exception{
		String errorMessage = filename + " not found";
		return packDatagramPacket(ERROR, errorMessage.getBytes());
	}
	
	/*
	 * utility method that wrap a packet type, data into a DatagramPacket
	 */
   	private static DatagramPacket packDatagramPacket(byte type, byte[] payload) throws Exception{
   		int dataLength = 1 + payload.length;
   	    ByteBuffer byteBuffer = ByteBuffer.allocate(dataLength); 
 		byteBuffer.put(type);
		byteBuffer.put(payload);   	
        return new DatagramPacket(byteBuffer.array(), dataLength);  	
     }	
	
    //return the ack number of a ACK packet
   	public static  byte extractACKNumber(DatagramPacket ackDP){
	    	byte[] payload = ackDP.getData();
	    	return payload[1];
	}
	
   	//print the string content of a ERROR packet 
    public static void printErrorString(DatagramPacket p){
		   byte[] data = p.getData();
		   int dataLength = p.getLength();
		   ByteBuffer byteBuffer = ByteBuffer.allocate(dataLength-1);
		   //ignore the packet type
	 	   byteBuffer.put(data,1, dataLength-1);
	 	  System.out.print(new String(byteBuffer.array()));
		}
  
    //return the block sequence of a data packet
    public static byte extractBlockSeq(DatagramPacket dataDP){
		byte[] payload = dataDP.getData();
		if(payload.length <=1) return -1; //-1: no block sequence in data
	    int type = payload[0];
        if(type == DATA){
         	return payload[1];
        }
        return -1; //-1: not a data packet
	   
   }
    
}
