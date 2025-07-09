import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.*;
import java.lang.*;

/**
 * JUnit tests for AprsGraph
 * CS143 Final Project -- Steve Buer
 */

public class AprsGraphTest {

	AprsGraph g;
		
	/**
	 * Reset data structures
	 */

	@BeforeEach 
	void reset() {

		g = new AprsGraph();
	}
	
	/**
	 * Test SBAprs
	 */

	@Test
	public void testHaversineDistance() {

		assertEquals(AprsGraph.haversineDistance(34.0594, 118.2426, 40.7128, 74.0060), 2447.982251179628);
	}
}
