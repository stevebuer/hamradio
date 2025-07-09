import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.*;
import java.lang.*;

/**
 * JUnit tests for Ax25 Class
 * CS143 Final Project -- Steve Buer
 */

public class Ax25Test {

	Ax25 ax;

	private final static String TESTPKT = "W7OMR>TW5PPS,CRYSTL,ERINB,JUPITR*,WIDE2:`2?vm\"Ak/\"4u}13.2V";	
		
	/**
	 * Reset data structures
	 */

	@BeforeEach 
	void reset() {

		ax = new Ax25(TESTPKT);
	}
	
	/**
	 * Test src() method
	 */

	@Test 
	public void testSrc() {

		ax.decodeAx25();
		assertEquals(ax.src(), "W7OMR");
	}
	
	/**
	 * Test dst() method
	 */

	@Test 
	public void testDst() {
		
		ax.decodeAx25();
		assertEquals(ax.dst(), "TW5PPS");
	}

	/**
	 * Test digitAt(int) method
	 */

	@Test 
	public void testDigiAt() {
		
		ax.decodeAx25();
		assertEquals(ax.digiAt(0), "CRYSTL");
	}

	/**
	 * Test lastDigi() method
	 */

	@Test 
	public void testLastDigi() {

		ax.decodeAx25();
		assertEquals(ax.lastDigi(), "JUPITR");
	}

	@Test 
	public void testDigiCount() {

		ax.decodeAx25();
		assertEquals(ax.digiCount(), 3);
	}
}
