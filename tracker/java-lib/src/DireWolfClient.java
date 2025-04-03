//
// Java DireWolf Client
//

import java.net.*;
import java.io.*;

enum ServerType { TCPKISS, AGWPE }

public class DireWolfClient {

	String server;
	Socket sock;
	int port = 8000;

	int AX25_MAXLEN = 256;

	DireWolfClient(String server) {

		this.server = server;
	}

	void connect() {

		try {

			sock = new Socket(server, port);

		} catch (Exception e) {

			System.err.println("error: " + e.getMessage());
		}
	}

	void readPacket() {
			
		// BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        
		try {

			// read in a TCPKISS or AGWPE Packet
		
			byte[] buffer = new byte[16];
			int bytesRead;

			InputStream input = sock.getInputStream();

			while ((bytesRead = input.read(buffer)) != -1) {

				String data = new String(buffer, 0, bytesRead);
				System.out.print(data);
            		}

		} catch (IOException e) {
			
			e.printStackTrace();
		}

		// decode AGWPE header vs TCPKISS




	}

	public static void main(String args[]) {

		System.out.println("Test DireWolfClient...");

		DireWolfClient c = new DireWolfClient("localhost");

		c.connect();

		try {

			Thread.sleep(5*1000);

		} catch (Exception e) {

			System.out.println("sleep interrupt");
		}
		
		c.readPacket();
	}
}
