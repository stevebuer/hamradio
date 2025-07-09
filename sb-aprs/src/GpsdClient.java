import java.net.*;
import java.io.*;
import java.util.Arrays;

/**
 * Read from GPSd socket
 *
 * https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
 */

public class GpsdClient {

	/* gpsd server (this machine) */

	static private final String HOST = "localhost";
	static private final int PORT = 2947;

	/* I am connected */

	Socket socket = null;
	BufferedReader in = null;
	BufferedWriter out = null;
	boolean connected = false;

	/* connect method */

	public void connect() {

		try {
			socket = new Socket(HOST, PORT);

			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

		} catch (Exception e) {

			System.err.println("error: " + e.getMessage());
		}

		this.connected = true;
	}
		
	/**
         * talk to gpsd per:
         *
	 *   https://gpsd.gitlab.io/gpsd/client-howto.html
	 */

	public Position getPosition() {

		String s = null;
		int packet_count = 0;
		boolean sentHello = false;

		while (true) {

			packet_count++;

			try {
				s = in.readLine();

				/* get banner & request gps messages */

				if (sentHello == false)
					out.write("?WATCH={\"enable\":true,\"json\":true}"); out.flush(); sentHello = true;

				/* this is a TPV message (position with GPS locked) */

				if (s.contains("TPV") && s.contains("mode\":3"))
					return parseGpsdTpv(s);

			} catch (Exception e) {

				System.err.println("error: " + e.getMessage());
			}
		}
	}

	public void close() {

		try {
			socket.close();	

		} catch (Exception e) {

			System.err.println("error: " + e.getMessage());
		}
	}

	/**
	  * hacky json parse
	  */

	private static Position parseGpsdTpv(String msg) {

		String[] json = msg.substring(1, msg.length() - 1).split(",");

		String lat = json[5];
		String lon = json[6];
		String alt = json[9];

		double myLat = Double.parseDouble(lat.split(":")[1]);
		double myLon = Double.parseDouble(lon.split(":")[1]);
		double myAlt = Double.parseDouble(alt.split(":")[1]);

		/*
		System.out.println("ARR: " + Arrays.toString(json));
		System.out.println("LAT: " + lat + " LON: " + lon + " ALT: " + alt);
		System.out.println("myLat: " + myLat + " myLon: " + myLon + " myAlt: " + myAlt);
		*/

		return new Position(myLat, myLon, myAlt, true);
	}

	public static void main(String[] args) {

		System.out.println("Gpsd Test...");

		GpsdClient client = new GpsdClient();

		client.connect();

		Position myPosition = client.getPosition();

		client.close();

		System.out.println("My Position: " + myPosition);

	}
}
