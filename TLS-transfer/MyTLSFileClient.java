import java.net.*;
import javax.net.*;
import java.io.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLServerSocketFactory;
import java.security.KeyStore;
import javax.net.ssl.SSLSocketFactory;
import javax.naming.ldap.*;

public class MyTLSFileClient {

	/**
	* Displays the X509 certificate of the server we are connecting to
	*
	* @param cert	The X509 certificate of the server
	*/
	static void displayCert(X509Certificate cert) {
		try {
			String name = cert.getSubjectX500Principal().getName();
			LdapName ln = new LdapName(name);
			
			System.out.println("-----X509 Certificate----");
			
			for(Rdn rdn : ln.getRdns())
				System.out.println(rdn.getValue().toString());
				
			System.out.println("-------------------------");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	* Receives data of requested file from the server and writes to
	* a file output stream. 
	*
	* @param server		The SSL Socket to connect to the server
	* @param filename	The name of the file being requested
	*/
	private static void receiveFile(SSLSocket server, String filename) {
		try {		
			//Declare IO streams
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));	//Output stream to write the filename to the server
			BufferedReader br = new BufferedReader(new InputStreamReader(server.getInputStream()));	//Reader to get the file length
			BufferedInputStream bis = new BufferedInputStream(server.getInputStream());			//Input stream to retrieve the file data from the server
			
			System.out.println("\nrequesting file: " + filename);
			
			//Send filename to the server
			bw.write(filename + "\n");
			bw.flush();
			
			//Get the number of bytes from the server
			int byteCount = Integer.parseInt(br.readLine());

			byte[] buf = new byte[byteCount];
			int rc = bis.read(buf); 			
			
			System.out.println("bytes received from server: " + rc);

			if(rc > 0) {
				String filenameCopy = "_" + filename;
				System.out.println("creating copy file: " + filenameCopy);
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(filenameCopy)));//Create a file to write the received data to
				
				//Write the received data to file output
				while(rc != -1) {
					bos.write(buf, 0, rc);
					bos.flush();
					
					rc = bis.read(buf);	
				}
				bos.close();
				System.out.println("file " + filename + " sent");
			}
			else if (rc == 0) {
				//File does not exist
				System.out.println(filename + " does not exist on the server :(");
			}
			br.close();
			bw.close();
			bis.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		try {
			//Store variables from command-line arguments
			String hostName = args[0];
			int port = Integer.parseInt(args[1]);
			String filename = args[2];
			
			//Print relevant information
			System.out.println("-----CONNECTION MADE-----\n");
			
			SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket ssocket = (SSLSocket)factory.createSocket(hostName, port);
			
			SSLParameters params = new SSLParameters();
			params.setEndpointIdentificationAlgorithm("HTTPS");
			ssocket.setSSLParameters(params);
			
			ssocket.startHandshake();
			
			//Get the X509Certificate for this session
			SSLSession sesh = ssocket.getSession();
			X509Certificate cert = (X509Certificate)sesh.getPeerCertificates()[0];
			
			displayCert(cert);
			receiveFile(ssocket, filename);	
		}
		catch(Exception e) {
			System.err.println("exception : " + e);
			e.printStackTrace();
		}
	}
}

