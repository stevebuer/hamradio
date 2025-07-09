/**
 * Aprs application protocol layer
 */

public class Aprs extends Ax25 {

	enum AprsReportType { WEATHER, FIXED, MOBILE, MIC_E, TELEMETRY, MESSAGE, DF, UNKNOWN }

	/* aprs message */

	String report;

	/* type of report */

	AprsReportType aprsType;

	/* aprs symbol */

	char symbol;

	/* position & distance */

	public double alt, lat, lon;

	/* methods */

	public Aprs(String packet) {

		super(packet);
	}

	public boolean hasPosition() { if (this.lat > 0.0) return true; else return false; }

	public void decodeAprs() {

		this.decodeAx25();

		report = this.data;

		// System.out.println("REPORT: " + report);

		switch (decodeType()) {

			case FIXED:
			case MOBILE:
			case MIC_E:
				decodePositionReport();
				break;

			default:
		}
	}

	/**
	 * parse aprs report type
         */

	public AprsReportType decodeType() {

		if (report.charAt(0) == '!' || report.charAt(0) == '=')
			aprsType = AprsReportType.FIXED;

		else if (report.charAt(0) == '@')
			aprsType = AprsReportType.MOBILE;

		/* todo: mic-e first char is one of 0x1c, 0x1d, 0x60, 0x27 */

		else
			aprsType = AprsReportType.UNKNOWN;


		return aprsType;
	}

	public void decodePositionReport() {

		if (aprsType == AprsReportType.FIXED) {

			this.lat = Double.parseDouble(report.substring(1, 8));
			this.lon = Double.parseDouble(report.substring(10, 18));
			
			// System.out.println("DEBUG FIXED DECODE: LAT LON " + lat + " " + lon);
		
		} else if (aprsType == AprsReportType.MOBILE) {

			int n = 0;

			/* skip zulu time report if present */

			if (report.charAt(7) == 'z')
				n = 7;

			this.lat = Double.parseDouble(report.substring(1 + n, 8 + n));
			this.lon = Double.parseDouble(report.substring(10 + n, 18 + n));
			
			// System.out.println("DEBUG MOBILE DECODE: LAT LON " + lat + " " + lon);
		} else
			System.out.println("STATION: " + this.src);

		/* bugfix: shift decimal place left */

		this.lat /= 100.0;
		this.lon /= 100.0;
	}
}
