import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.*;
import java.lang.*;

/**
 * JUnit tests for Aprs Class
 * CS143 Final Project -- Steve Buer
 */

public class AprsTest {

	Aprs ap;

	private final static String TESTPKT = "BALDI>APOT21,WIDE2-1:!4713.13N/12150.61W_PHG7830/W2,WAn,Baldi N7FSP";
		
	/**
	 * Reset data structures
	 */

	@BeforeEach 
	void reset() {

		ap = new Aprs(TESTPKT);
	}
	
	/**
	 * Test src() method
	 */

	@Test 
	public void testHasPosition() {

		assertFalse(ap.hasPosition());
		ap.decodeAprs();
		assertTrue(ap.hasPosition());
	}

	@Test 
	public void testDecodeType() {

		ap.decodeAprs();
		assertEquals(ap.decodeType(), Aprs.AprsReportType.FIXED);
	}

}
