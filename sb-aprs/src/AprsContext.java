import java.util.*;

/**
 * All my state in one place
 */

public class AprsContext {

	/* my callsign */

	private String myCall;

	/* current position */

	private double latitude, longitude, altitude;
	
	/* mheard map */

        public TreeMap<String, String> mheardMap; // not threadsafe fixme

	/* distance map */
        
	public TreeMap<String, Double> distanceMap; // not threadsafe fixme

	/* all my ax.25 frames / APRS packets I have collected */

	public ArrayList<Ax25> frameList; // stores Ax25 or Aprs Objects

	/* Aprs node graph */

	AprsGraph aprsGraph;

	/* constructor */

	AprsContext() {

		myCall = "NOCALL";

		mheardMap = new TreeMap<>();
		
		distanceMap = new TreeMap<>();

		frameList = new ArrayList<>();

		aprsGraph = new AprsGraph();
	}

	/**
         * set my callsign
         */

	public void setCall(String s) {

		/* validate */

		this.myCall = s;
	}

	/**
	 * output mheard list in machine readable CSV
	 */

	public String mheard() {

		StringBuilder b = new StringBuilder("MYCALL," + myCall + "\n");

		mheardMap.forEach((k,v) -> b.append(k + "," + v + "," + String.format("%.4f", distanceMap.get(k)) + "\n"));

		return b.toString();
	}

	/**
 	 * return my callsign
	 */

	public String myCall() { return this.myCall; }

	/** 
         * Return String representation of my position   
         */
                                        
        public String myPosition() { return this.latitude + ", " + this.longitude; }                       

	public void setLatitude(double lat) { this.latitude = lat; }
	
	public double getLatitude() { return this.latitude; }

	public void setLongitude(double lon) { this.longitude = lon; }
	
	public double getLongitude() { return this.longitude; }
}
