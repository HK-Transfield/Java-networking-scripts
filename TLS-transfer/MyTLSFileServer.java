import java.net.*;
import javax.net.*;
import java.io.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLServerSocketFactory;
import java.security.KeyStore;
import javax.net.ssl.SSLSocketFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import javax.naming.ldap.*;
import java.io.Console;   

class TLSFileWorker implements Runnable {

	SSLSocket client;

	public TLSFileWorker(SSLSocket s) {
		client = s;
		System.out.println("status: connected");
	}

	@Override
	public void run() {
		try {
			//Declare IO streams
			BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));	//BufferedReader used to get the filename from the client
			BufferedOutputStream bos = new BufferedOutputStream(client.getOutputStream());
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
			
			//Retrieve filename from client
			String filename;			
			filename = br.readLine();
			
			//Create a file from filename
			File file = new File(filename);//Store the requested file		
			int fileLength = (int)file.length();
			
			System.out.println("requested file: " + filename);
			System.out.println("length of file: " + file.length());

			//Check if the file exists
			if(file.isFile() == true) {
				//Send file length to the client
				bw.write(Integer.toString(fileLength) + "\n");
				bw.flush();
				BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
				System.out.println("File " + filename + " : EXISTS");
				
				byte[] buf = new byte[(int)file.length()];	//set byte array to size of the file length
				int rc = fis.read(buf);

				//Write binary file to the client
				while (rc != -1) {
					bos.write(buf, 0, rc);
					bos.flush();

					rc = fis.read(buf);
				}
				fis.close();
			}
			else {
				//Close the socket if the file does not exists
				bw.write(Integer.toString(0) + "\n");
				System.out.println("File " + filename + " : DOES NOT EXIST :(\n");
			}
			bw.close();
			br.close();
			bos.close();
			client.close();
			System.out.println("status: terminated");
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}
}

/******************************************************************************************/

public class MyTLSFileServer {
	private static final ExecutorService exec =  Executors.newFixedThreadPool(10);
	public static void main(String args[]) {
		try {
			int port = Integer.parseInt(args[0]);
			KeyStore ks = KeyStore.getInstance("JKS");
			Console cnsl = System.console(); 

			System.out.println("-----STARTING TLS FILE SERVER-----\n");
			
			if(cnsl != null) {
				/**store the passphrase to unlock the JKS file. **/
				char[] passphrase = cnsl.readPassword("Password: ");
				//char[] passphrase = "3Nephi111011".toCharArray();
				System.out.println("connecting on port: " + port);

				/**load the keystorefile **/
				ks.load(new FileInputStream("server.jks"), passphrase);

				/**use the KeyManager Class to manage the key **/
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(ks, passphrase);

				/**Get an SSL Context that speaks some version of TLS */
				SSLContext ctx = SSLContext.getInstance("TLS");

				/** initialise the SSL context with the keys. **/
				ctx.init(kmf.getKeyManagers(), null, null);

				ServerSocketFactory ssf = ctx.getServerSocketFactory();

				//Create SSLServerSocket
				SSLServerSocket sserverSocket = (SSLServerSocket)
				ssf.createServerSocket(port);

				String EnabledProtocols[] = {"TLSv1.2", "TLSv1.1"};

				sserverSocket.setEnabledProtocols(EnabledProtocols);

				while(true) {
					SSLSocket sclient = (SSLSocket)sserverSocket.accept();//blocking

					//Initialize a new client worker
					TLSFileWorker worker = new TLSFileWorker(sclient);
					exec.execute(worker);
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}

