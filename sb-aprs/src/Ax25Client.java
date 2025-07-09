import java.util.*;
import java.io.*;

/**
 * connect to Ax25 sources: direwolf, AGW, KISS
 */

public class Ax25Client { 

	boolean agw = false;
	boolean kiss = false;
	boolean tnc2 = false;

	boolean configured = false;

	String filePath = null;

	FileInputStream istream = null;
	BufferedReader br;

	/* store packets read from file */

	Vector<String> packets = null;

	/**
         * construct object to read from file
         */ 

	Ax25Client(String file) {

		/* only kissutil/TNC2 supported for now */

		this.tnc2 = true; 
		
		this.filePath = file;

		configured = true;
	}

	public void openKissutilFifo() { // throws IOException {

		try {
			/* https://docs.oracle.com/javase/8/docs/api/java/io/InputStreamReader.html */

			istream = new FileInputStream(filePath);

			br = new BufferedReader(new InputStreamReader(istream));

		} catch (Exception e) {

			System.err.println("ERROR: " + e.getMessage());
		} 
	}

	public String nextPacket() throws IOException {

		while (true) {

			if (br.ready())
				return br.readLine();
			else
				try {
					Thread.sleep(1000);

				} catch (Exception e) {

					return null;
				}
		}
	}

	/**
         * Read packets stored in a Direwolf kissutil file
         *
         * @param filename the kissutil file to read from
         */

	public void loadKissFile(String filename) throws FileNotFoundException {

		SBLog.log("Reading TNC2 (kissutil) file: " + filePath);

		this.packets = new Vector<String>();

		Scanner s = new Scanner(new File(filePath));

		while (s.hasNextLine()) {

			String l = s.nextLine();

			packets.add(l.substring(4, l.length()));
		}
		
		SBLog.log("Read " + packets.size() + " AX.25 packets");

		parseAll();
	}

	private void parseAll() {

		Iterator<String> iter = packets.iterator();

		while (iter.hasNext()) {

			String p = iter.next();

			SBLog.log("PACKET: " + p);

			Aprs aprsFrame = new Aprs(p);

			aprsFrame.decodeAprs();
			
			SBAprs.storeMetaData(aprsFrame);
		}
	}
		
	/**
	 * dump packets for debug 
         */

	public String dumpPackets() { return this.packets.toString(); } 

	/**
         * To be implemented future release
         */

	private void connectAGW() throws IOException { }
	
	private void connectKiss() throws IOException { } 
}
