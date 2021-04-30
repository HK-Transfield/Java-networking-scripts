import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;

class HttpServerSession implements Runnable {
	
	
	Socket client;
	
	public HttpServerSession(Socket s) {
		
		client = s;//Create socket for client to connect to
		
	}	
	
	/**
	*
	* writes a HTTP response header to a buffered output stream
	*
	* @param bos	buffered ouput stream to write HTTP response to client 
	* @param s	HTTP response header
	* @return 	the HTTP response written into the buffered output stream
	*
	*/
	private void writeln(BufferedOutputStream bos, String s) throws IOException {
			
			String news = s + "\r\n";
			byte[] array = news.getBytes();
			
			System.out.println("\nresponse header : " + s);//print response to console

			//For each byte in the string, write it to the BufferedOutputStream
			for(int i = 0; i < array.length; i++) 
				bos.write(array[i]);
			
			return;
			
		}
	
	@Override	
	public void run() {
		
		String response;
			
		try {
			//declare varibles
			BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));	//reads the HTTP request from the client
			String request = reader.readLine();								//stores the HTTP request 
			String parts[] = request.split(" ");								//splits the request in order to check correct format
			String filename = parts[1].substring(1);							//obtains the filename from the request and stores it
			File file = new File(filename);								//represents file from the filename
			SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");			//formats the date to use in HTTP response
			
			//print file name and request to console
			System.out.println("\nfile requested : " + filename);
			System.out.println("request header : " + request);
					

			//Checks if the request has 3 parts and begins with "GET"
			if(parts.length == 3 && parts[0].compareTo("GET") == 0) {
			
				BufferedOutputStream stream = new BufferedOutputStream(client.getOutputStream());	//create new buffered output stream to write to client
			
				if(file.isFile() == true) {
					//If the filename is a file, create a  new file input stream
					FileInputStream fis = new FileInputStream(file);

					System.out.println("file length : " + (int)file.length());

					byte[] buf = new byte[(int)file.length()];	//set byte array to size of the file length
					int rc = fis.read(buf);	

					//write HTTP response to the buffered output stream (This is my extenstion for 3.9)
					response = "HTTP/1.1 200 OK\r\nLast-Modified: " + sdf.format(file.lastModified()) + "\r\nContent-Length: " + rc + "\r\n";
					writeln(stream, response);				
									
					while (rc != -1) {
						//While the binary file has not reached the end of the array
						Thread.currentThread().sleep(1000); //Test to make file load over a slow connection				
						stream.write(buf, 0, rc);	
						stream.flush();
						
						rc = fis.read(buf);
					}
				fis.close();
				}
				else {
					//If the file does not exist, send back 404 not found header response
					response = "HTTP/1.1 404 Not Found\r\n";
					writeln(stream, response);
					stream.write("404 Not found\nThe file you have requested cannot be found :(".getBytes());
					System.out.println(response);
				}				
				//Close all I/O streams and client						
				stream.close();	
				reader.close();			
				client.close();
				System.out.println("status : closed");	
			}
			else {
				System.out.println("abort process : incorrect request format");
				client.close();
			}			
		}
		catch(Exception e) {
			System.err.println("Exception : " + e);
		}
	}	
}

public class HttpServer {
	private static final ExecutorService exec =  Executors.newFixedThreadPool(10);	
	
	public static void main(String args[]) {
		try {
			//Print out message, indicating that the web server has started
			System.out.println("status : started" + "\nport : 8008");
		
			ServerSocket ss = new ServerSocket(8008); //Declare server socket for web server
			
			while(true) {	
				Socket client = ss.accept();
				System.out.println("connection received: " + client.getInetAddress());
				
				//Execute a new HTTP Server session when client connects
				HttpServerSession session = new HttpServerSession(client);
				exec.execute(session);	
			}
		}
		catch(Exception e) {
			System.err.println("Exception: " + e);
		}
	}
}
