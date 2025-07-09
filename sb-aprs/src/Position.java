/**
 * class to store a position (gps or fixed)
 */

public class Position {

	boolean fixed;
	boolean gps;

	public double latitude;
	public double longitude;
	public double altitude;

	Position() {

		fixed = false;
		gps = false;
	}

	Position(double a, double b, double c, boolean d) {

		this();

		this.latitude = a;
		this.longitude = b;
		this.altitude = c;

		this.gps = d;
		this.fixed = !d;
	}

	/**
         * override, give me a simple parseable string
         */

	public String toString() {

		return "" + latitude + "," + longitude + "," + altitude;
	}
}
