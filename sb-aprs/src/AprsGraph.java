import java.util.concurrent.*;
import java.util.*;

/**
 * Graph of APRS Network based on packets heard 
 */

public class AprsGraph {

	private static final double EARTH_METERS = 6378137.0;
	private static final double EARTH_MILES = 3963.1;

	private static boolean useMeters = false;

	/* graph edge set (thread safe) */

	// issue: when this was node1:node1 was unique via set memebership, now this is just a list?
	// needs equals/compare method for uniqueness ?

	// public CopyOnWriteArraySet<GraphEdge> edgeSet;

	public static CopyOnWriteArraySet<String> edgeSet; // simplest thing that works for now...
	
	/* graph vertext set (thread safe) */

	public CopyOnWriteArraySet<String> vertexSet;

	AprsGraph() {

		edgeSet = new CopyOnWriteArraySet<>();
		vertexSet = new CopyOnWriteArraySet<>();
	}

	/**
	 * calculate the distance between two points on the earth using haversine formula 
	 *
	 * https://en.wikipedia.org/wiki/Haversine_formula
	 *
	 * draws heavily from: https://www.movable-type.co.uk/scripts/latlong.html
	 *
	 * @param lat1 latitude of point1
	 * @param lon1 latitude of point1
	 * @param lat2 latitude of point2
	 * @param lon2 latitude of point2
	 *
	 * @return distance in kilometers or miles
	 */

	public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {

		/* delta of lat and lon */

		double delta_lat = Math.toRadians(Math.abs(lat2 - lat1));
		double delta_lon = Math.toRadians(Math.abs(lon2 - lon1));

		/* haversine (theta) in radians */

		double hav_theta = hav(delta_lat) + (Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * hav(delta_lon));

		/* return distance in meters or miles */

		if (useMeters)
			return (EARTH_METERS/1000) * (2 * Math.atan2(Math.sqrt(hav_theta), Math.sqrt(1 - hav_theta)));
		else
			return (EARTH_MILES) * (2 * Math.atan2(Math.sqrt(hav_theta), Math.sqrt(1 - hav_theta)));
	}

	/**
         * haversine trig function
         */

	private static double hav(double theta) {

		/* sin^2 (theta / 2) = (1 - cos(theta)) /2 */

		return ((1.0 - Math.cos(theta))/2.0);
	}

	/**
	 * Graph edge descriptor
	 */

	private class GraphEdge {

		/* the edge vertexes */

		String node1, node2;

		/* distance (length) of the edge */

		double distance;

		/* slope of edge for rendering ? */

		int heading;

		/* construct */

		GraphEdge(String a, String b) {

			node1 = a;
			node2 = b;
		}

		public String toString() {

			return "" + node1 + ":" + node2;
		}

		public String toDot() {

			return "\t\"" + node1 + "\" -- \"" + node2 + "\";";
		}
	}

	/**
	 * add an edge between two nodes
	 */

	public static void addEdge(String n1, String n2) {

		String e = "";

		/* not an valid edge */

		if (n1.equals(n2))
			return;

		/* edges are unique ordered pairs with n1 < n2 */

		if (n1.compareToIgnoreCase(n2) < 0)
			e += n1 + ":" + n2;
		else
			e += n2 + ":" + n1;

		edgeSet.add(e);
	}

	/**
	 * Output our graph in GraphViz DOT format
	 */

	public static String outputGraph() {

		String s = "graph G {\n\n";

		Iterator<String> iter = edgeSet.iterator();

		while (iter.hasNext()) {

			String e = iter.next();

			s += toDotFormat(e);
		}

		s += "}";

		return s;
	}

	public static String toDotFormat(String edge) {

			String a[] = edge.split(":");

			return "\t\"" + a[0] + "\" -- \"" + a[1] + "\";\n";
	}

	public static void main(String[] args) {
		
		/* LA to NYC */

		System.out.println("Test LA --> NY: " + haversineDistance(34.0594, 118.2426, 40.7128, 74.0060) + " miles");

		/* dc to eiffel tower per wikipedia example: 6161.6 km */

		useMeters = true;

		System.out.println("Test DC --> Paris: " + haversineDistance(38.898, 77.037, 48.858, -2.294) + " kilometers");

		AprsGraph g = new AprsGraph();

		g.addEdge("N7MKO", "JUPITR"); g.addEdge("JUPITR", "VCAPK"); g.addEdge("JUPITR", "ERINB"); g.addEdge("ERINB", "VCAPK");

		System.out.println(g.outputGraph());
	}
}
