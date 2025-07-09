import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.*;
import java.lang.*;

/**
 * JUnit tests for SBAprs
 * CS143 Final Project -- Steve Buer
 */

public class SBAprsTest {

	SBAprs aprs;
		
	/**
	 * Reset data structures
	 */

	@BeforeEach 
	void reset() {

		aprs = new SBAprs();
	}
	
	/**
	 * Test SBAprs
	 */

	@Test
	public void testParseConfigLine() {
		
		assertThrows(NoSuchElementException.class, () -> { aprs.parseConfigLine("MYCALL");});
	}
}
