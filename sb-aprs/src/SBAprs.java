import java.util.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import com.sun.net.httpserver.*;

/**
 * Server daemon program for gathering and analyzing APRS reports, serving data via web API
 */

public class SBAprs {

	/* default file paths */

	private static String configFile = "SBAprs.cfg";
	private static String fifoFile = "/tmp/sbaprs.fifo";
	private static String dataFile = null;

	/* use gpsd vs fixed position */

	private static boolean useGpsd = false;

	/* enable debug messages */

	private static boolean debug = false;

	/* start web server */

	private static boolean webServer = false;

	/* APRS-IS source */

	private static boolean aprsIsClient = false;

	/* shared APRS context */

	private static AprsContext aprs;

	/**
	* Parse a line of the config file
	*
	* @param String configuration line
	*/
	
	static void parseConfigLine(String line) {

		Scanner s = new Scanner(line);

		while (s.hasNext()) {

			String t = s.next();

			switch (t.toUpperCase()) {

				case "MYCALL":
					
					aprs.setCall(s.next());
					break;

				case "LAT":

					aprs.setLatitude(s.nextDouble());
					break;

				case "LON":
					
					aprs.setLongitude(s.nextDouble());
					break;

				case "GPSD":

					useGpsd = s.nextBoolean();
					break;

				case "DATAFILE":
					
					dataFile = s.next();
					break;

				default:
					SBLog.log("Unknown config paramter: " + t);
			}
		}
	}

	/**
	* Load a configuration file and read line by line
	*
	* @param String file pathname
	*/

	static void loadConfig(String filename) throws FileNotFoundException {

		SBLog.log("Config file: " + filename);

		Scanner s = new Scanner(new File(filename));

		while (s.hasNextLine()) {

			String l = s.nextLine();

			if (l.isBlank() || l.charAt(0) == '#') /* skip blank or comment lines */
				continue;

			parseConfigLine(l);
		}
	}

	/** 
         * parse simple command line switches
         */

	private static void parseArgs(String[] a) {

		for (String s : a) {
					
			switch (s) {

				case "-d":
					debug = true;
					break;

				case "-g":
					useGpsd = true;
					break;

				case "-w":
					webServer = true;
					break;

				case "-h":
				case "-?":
				default:
					SBLog.log("usage: SBAprs -d -h -w");
					System.exit(1);
			}
		}
	}

	/**
	 * add frame to the metadata collections
	 */

	public static void storeMetaData(Ax25 frame) {

		// java.time: https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html

		aprs.mheardMap.put(frame.src, LocalDateTime.now().toString().split("\\.")[0]);	

		/* we do mheard on AX.25 non-APRS, but only Aprs has position possibly */

		if (frame instanceof Aprs report) {

			/* store distance of we can have a position report */

			if (report.hasPosition()) {

				double d = AprsGraph.haversineDistance(aprs.getLatitude(), aprs.getLongitude(), report.lat, report.lon);

				aprs.distanceMap.put(frame.src, d);
				
				System.out.println("STATION: " + frame.src + " DISTANCE: " + String.format("%.2f", d) + " miles");
			}
		}

		/* store graph edges */

		if (frame.digiCount() > 3) {

			System.out.println("DIGI COUNT > 3: NOT IMPLEMENTED");

		} else if (frame.digiCount() == 3) {

			AprsGraph.addEdge(frame.digiAt(0), frame.src); /* heard via 3 digis */
			AprsGraph.addEdge(frame.digiAt(1), frame.digiAt(0));
			AprsGraph.addEdge(frame.lastDigi(), frame.digiAt(1));
			AprsGraph.addEdge(aprs.myCall(), frame.lastDigi());

		} else if (frame.digiCount() == 2) {

			AprsGraph.addEdge(frame.digiAt(0), frame.src); /* heard via 2 digis */
			AprsGraph.addEdge(frame.lastDigi(), frame.digiAt(0));
			AprsGraph.addEdge(aprs.myCall(), frame.lastDigi());


	  	} else if (frame.hasDigiPath()) {

			AprsGraph.addEdge(frame.lastDigi(), frame.src); /* heard via digi */
			AprsGraph.addEdge(aprs.myCall(), frame.lastDigi());

		} else 
			AprsGraph.addEdge(aprs.myCall(), frame.src); /* heard direct */
	}

	public static void main(String[] args) {

		Ax25Client ax25;
		AprsIsClient aisc;
		GpsdClient gpsd;

		if (args.length > 0)
			parseArgs(args);
		
		SBLog.log("Starting SBAprs...");
		
		aprs = new AprsContext();

		try {
			loadConfig(configFile);

		} catch (Exception e) {

			SBLog.log("Error loading config: " + configFile + " : " + e.getMessage());
			return;
		}

		if (useGpsd) {

			gpsd = new GpsdClient();

			gpsd.connect();

			gpsd.getPosition();
		}
		
		SBLog.log("My callsign: " + aprs.myCall());
		SBLog.log("My position: " + aprs.myPosition());

		/* start API server */

		if (webServer) {

			AprsApiServer server = new AprsApiServer(aprs);
        
			Thread t = new Thread(server);
        
			t.start();
		}

		if (aprsIsClient) {

			aisc = new AprsIsClient();

			try {
				aisc.readFile(dataFile);
		
			} catch (Exception e) {
			
				SBLog.log("Error loading data: " + dataFile);
				return;
			}
		}

		if (dataFile != null) {
			
			/* read from capture file */

			ax25 = new Ax25Client(dataFile);

			try {
				ax25.loadKissFile(dataFile);			
		
			} catch (Exception e) {
			
				SBLog.log("Error loading: " + dataFile + " : "+ e.getMessage());
				return;
			}

		} else {

			/* open fifo to kissutil */

			ax25 = new Ax25Client(fifoFile);
		
			ax25.openKissutilFifo();

			while (true) {

				String p = null;

				try { 
        				p = ax25.nextPacket();

		        		Aprs aprsFrame = new Aprs(p.substring(4, p.length()));

                        		aprsFrame.decodeAprs();
                        
                        		SBAprs.storeMetaData(aprsFrame);

				} catch (Exception e) {

					System.err.println("ERROR: " + e.getMessage() + "'" + p + "'");
				}
			}
		}
	}
}
