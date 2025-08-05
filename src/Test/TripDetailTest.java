package Test;

import static org.junit.Assert.*;
import org.junit.Test;
import Data.TripDetail;

public class TripDetailTest {

    @Test
    public void testTripDetail() {
        TripDetail tripDetail = new TripDetail(1, 100, "start", "end", "08:00:00", "09:00:00", "route1");
                
        assertEquals(1, tripDetail.getTripId());
        assertEquals(100, tripDetail.getTimeTaken());
        assertEquals("start", tripDetail.getStartStopId());
        assertEquals("end", tripDetail.getEndStopId());
        assertEquals("08:00:00", tripDetail.getDepartureStop());
        assertEquals("09:00:00", tripDetail.getArrivalStop());
        assertEquals("route1", tripDetail.getRouteID());
    }
    
    @Test
    public void testToString() {
        TripDetail tripDetail = new TripDetail(1, 100, "start", "end", "08:00:00", "09:00:00", "route1");
        tripDetail.setDepartureTime("08:00");
        tripDetail.setArrivalTime("09:00");
        
        String expectedString = "Trip ID: 1, Time Taken: 100, Start Stop ID: start, End Stop ID: end, Departure Stop: 08:00:00 at 08:00, Arrival Stop: 09:00:00 at 09:00";
        assertEquals(expectedString, tripDetail.toString());
    }
}
