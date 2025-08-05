//package Test;
//
//import static org.junit.Assert.*;
//import static org.mockito.Mockito.*;
//
//import Data.DatabaseSingleton;
//import Data.TripCoordinates;
//import Data.TripDetail;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.MockedStatic;
//
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//public class TripCoordinatesTest {
//
//    private Connection mockConnection;
//    private MockedStatic<DatabaseSingleton> mockedDatabaseSingleton;
//
//    @Before
//    public void setUp() throws SQLException {
//        mockConnection = mock(Connection.class);
//        mockedDatabaseSingleton = mockStatic(DatabaseSingleton.class);
//        mockedDatabaseSingleton.when(DatabaseSingleton::getConnection).thenReturn(mockConnection);
//    }
//
//    @Test
//    public void testCoordinates() {
//        List<String[]> intermediateStops = new ArrayList<>();
//        intermediateStops.add(new String[]{"Stop 1", "51.0", "3.0"});
//
//        TripCoordinates tripCoordinates = new TripCoordinates("Start Stop", 1.0, 2.0, "End Stop", 3.0, 4.0, intermediateStops, "08:00:00", "route1");
//
//        assertEquals("Start Stop", tripCoordinates.getStartStopName());
//        assertEquals(1.0, tripCoordinates.getStartStopLat(), 0.001);
//        assertEquals(2.0, tripCoordinates.getStartStopLon(), 0.001);
//        assertEquals("End Stop", tripCoordinates.getEndStopName());
//        assertEquals(3.0, tripCoordinates.getEndStopLat(), 0.001);
//        assertEquals(4.0, tripCoordinates.getEndStopLon(), 0.001);
//        assertEquals("08:00:00", tripCoordinates.getDepartureTime());
//        assertEquals("route1", tripCoordinates.getRouteID());
//        assertNotNull(tripCoordinates.getIntermediateStopDetails());
//        assertEquals(1, tripCoordinates.getIntermediateStopDetails().size());
//    }
//
//    @Test
//    public void testGetStopDetailsById() throws SQLException {
//        PreparedStatement mockStmt = mock(PreparedStatement.class);
//        ResultSet mockResultSet = mock(ResultSet.class);
//
//        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
//        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
//        when(mockResultSet.next()).thenReturn(true);
//        when(mockResultSet.getString("stop_name")).thenReturn("Stop Name");
//        when(mockResultSet.getDouble("stop_lat")).thenReturn(51.0);
//        when(mockResultSet.getDouble("stop_lon")).thenReturn(3.0);
//
//        String[] stopDetails = TripCoordinates.getStopDetailsById("123");
//        assertNotNull(stopDetails);
//        assertEquals("Stop Name", stopDetails[0]);
//        assertEquals("51.0", stopDetails[1]);
//        assertEquals("3.0", stopDetails[2]);
//    }
//
//    @Test
//    public void testGetTripCoordinates() throws SQLException {
//        PreparedStatement mockStmt = mock(PreparedStatement.class);
//        ResultSet mockResultSet = mock(ResultSet.class);
//
//        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
//        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
//        when(mockResultSet.next()).thenReturn(true);
//        when(mockResultSet.getString("stop_name")).thenReturn("Stop Name");
//        when(mockResultSet.getDouble("stop_lat")).thenReturn(51.0);
//        when(mockResultSet.getDouble("stop_lon")).thenReturn(3.0);
//
//        TripDetail mockTripDetail = mock(TripDetail.class);
//        when(mockTripDetail.getStartStopId()).thenReturn("start");
//        when(mockTripDetail.getEndStopId()).thenReturn("end");
//        when(mockTripDetail.getDepartureStop()).thenReturn("08:00:00");
//        when(mockTripDetail.getRouteID()).thenReturn("route1");
//
//        List<String> intermediateStops = Collections.singletonList("intermediate");
//
//        TripCoordinates tripCoordinates = TripCoordinates.getTripCoordinates(mockTripDetail, intermediateStops);
//        assertNotNull(tripCoordinates);
//        assertEquals("Stop Name", tripCoordinates.getStartStopName());
//        assertEquals(51.0, tripCoordinates.getStartStopLat(), 0.001);
//        assertEquals(3.0, tripCoordinates.getStartStopLon(), 0.001);
//        assertEquals("Stop Name", tripCoordinates.getEndStopName());
//        assertEquals(51.0, tripCoordinates.getEndStopLat(), 0.001);
//        assertEquals(3.0, tripCoordinates.getEndStopLon(), 0.001);
//    }
//
//    @After
//    public void tearDown() {
//        mockedDatabaseSingleton.close();
//    }
//}
