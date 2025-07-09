import java.util.*;
import java.io.*;
import java.time.*;

// Client to read from APRS server or file dump in APRS-IS format

public class AprsIsClient {

	String server;
	int port, count;

	HashSet<String> heardSet;

	TreeMap<String, String> heardTime;

	public AprsIsClient() {

		heardSet = new HashSet<String>();

		heardTime = new TreeMap<String, String>();
	}

	public void setFilter() {


	}

	// connect to server

	public void connect() {

	}

	// read line from server

	public void readLine() {

	}

	// read aprs-is format messages stored in a file

	public void readFile(String filename) throws FileNotFoundException {

		SBLog.log("Reading APRS-IS data file: " + filename);

		Scanner s = new Scanner(new File(filename));

		while (s.hasNextLine()) {

			String l = s.nextLine();

			if (l.isBlank() || l.charAt(0) == '#') /* skip blank or comment lines */
				continue;

			parseLine(l);
		}
		
		SBLog.log("Read " + this.count + " APRS reports");

	}

	private void parseLine(String line) {

		this.count++;
		
		/* split address and APRS data fields */

		String[] fields = line.split(":");

		if (true) { // debug

			SBLog.log("ADDRESS: " + fields[0]);
			SBLog.log("MESSAGE: " + fields[1]);
		}

		/* parse out source address */

		String[] addr = fields[0].split(">");

		if (true) { // debug

			SBLog.log("SRC: " + addr[0]);
		}

		/* get RF digi path */

		// https://blog.aprs.fi/2020/02/how-aprs-paths-work.html

		String[] digis = addr[1].split(",");

		String dest = digis[0];
		
		SBLog.log("DEST: " + dest);

		String[] digi_list = Arrays.copyOfRange(digis, 1, digis.length - 1);
		
		if (true) { // debug

			String s = "";

			for (String d : digi_list) {
			
				s += d + " ";
			}

			SBLog.log("DIGIS: " + s);
		}
			
		SBLog.log("IGATE: " + digis[digis.length - 1]);

		/* store to mheard list */

		heardSet.add(addr[0]);

		heardTime.put(addr[0], LocalDate.now() + " " + LocalTime.now().toString().substring(0, 8));

		/* parse APRS Message */

		parseAprsReport(fields[1]);
	}

	private void parseAprsReport(String r) {

		if (false) {

			SBLog.log("REPORT: " + r);
		}

		/* GPS position report begins with ! or @ character */

		if (r.charAt(0) == '!' || r.charAt(0) == '@') {

			SBLog.log("POSITION: " + r);

		}
	}

	private void parseAprsLocation(String l) {

		/* fixed position report */

		if (l.charAt(0) == '!') {

		}
		
		/* mobile position report */
		
		if (l.charAt(0) == '@') {

		}
	}

	// classic mHeard command modeled after TNC2 / Linux

	public void printHeard() {

		for (String e : heardSet) {
		
			SBLog.log(e + " last heard: " + heardTime.get(e));
		}
	}

	public int getCount() {

		return this.count;
	}
}
