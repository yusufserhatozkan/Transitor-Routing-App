package Test;

import static org.junit.Assert.*;
import org.junit.Test;

import Algorithm.Distance.DistanceCalculator;

public class DistanceCalculatorTest {

    @Test
    public void testCalculateDistance() {
        DistanceCalculator dc = new DistanceCalculator();

        double lat1 = 52.5200; 
        double lon1 = 13.4050;
        double lat2 = 48.8566; 
        double lon2 = 2.3522;
        double expectedDistance = 878.4; 
        
        double result = dc.calculateDistance(lat1, lon1, lat2, lon2);
        assertEquals(expectedDistance, result, 10.0); 
    }

    @Test
    public void testCalculateWalkingTime() {
        DistanceCalculator dc = new DistanceCalculator();
        
        double distanceKm = 10.0; 
        int expectedWalkingTime = 120; 
        
        int result = dc.calculateWalkingTime(distanceKm);
        assertEquals(expectedWalkingTime, result);
    }

    @Test
    public void testCalculateCyclingTime() {
        DistanceCalculator dc = new DistanceCalculator();
        
        double distanceKm = 30.0; 
        int expectedCyclingTime = 120; 
        
        int result = dc.calculateCyclingTime(distanceKm);
        assertEquals(expectedCyclingTime, result);
    }
}
