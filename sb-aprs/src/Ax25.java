import java.util.*;

/*
 * AX.25 layer 2 protocol information
 */

public class Ax25 {

	/* original frame in TNC2 text format */

	protected String frame;
	protected String header;
	protected String data;

	/* ax25 src and dst address */

	protected String src;
	protected String dst;

	/* digipeater list (optional) */

	protected Vector<String> digis;

	/* frame type */

	protected int type;

	public String src() { return src; }
	public String dst() { return dst; }

	/* constructor */

	Ax25(String f) {

		this.frame = f;
	}

	/* decode a frame */

	public void decodeAx25() {

		this.header = frame.split(":")[0];
		this.data = frame.split(":")[1];

		this.src = header.split(">")[0];

		String[] digiArray = (header.split(">")[1]).split(",");

		this.dst = digiArray[0];

		digis = new Vector<>(Arrays.asList(Arrays.copyOfRange(digiArray, 1, digiArray.length)));
	}

	/* packet received directly or via digi */

	public String digiAt(int n) {

		return digis.elementAt(n);
	}

	public boolean hasDigiPath() {

		for (String d : digis)
			if (d.contains("*"))
				return true;

		return false;
	}

	public String lastDigi() {

		for (String d : digis)
			if (d.contains("*"))
				return d.substring(0, d.length() - 1);
	
		return "ERROR";
	}

	public int digiCount() {
		
		int n = 0;

		if (!this.hasDigiPath())
			return n;

		/* size is total digi entries, but we want total digis marked used */

		for (String d : digis) {

			n++;

			if (d.contains("*"))
				return n;
		}

		return n;
	}
}
