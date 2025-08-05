//package Test;
//
//import static org.junit.Assert.*;
//import static org.mockito.Mockito.*;
//
//import Api.ApiReader;
//import Data.DataGetter;
//import Data.DatabaseSingleton;
//import Data.TripDetail;
//import Data.TripCoordinates;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.MockedStatic;
//import org.mockito.Mockito;
//
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.Collections;
//import java.util.List;
//
//public class DataGetterTest {
//
//    private DataGetter dataGetter;
//    private Connection mockConnection;
//    private MockedStatic<DatabaseSingleton> mockedDatabaseSingleton;
//
//    @Before
//    public void setUp() throws SQLException {
//        dataGetter = new DataGetter();
//        mockConnection = mock(Connection.class);
//        mockedDatabaseSingleton = mockStatic(DatabaseSingleton.class);
//        mockedDatabaseSingleton.when(DatabaseSingleton::getConnection).thenReturn(mockConnection);
//    }
//
//    @Test
//    public void testGetLocationFromApiReader() {
//        ApiReader mockApiReader = mock(ApiReader.class);
//        when(mockApiReader.getLatitude()).thenReturn(50.8503);
//        when(mockApiReader.getLongitude()).thenReturn(4.3517);
//
//        try (MockedStatic<ApiReader> mockedApiReader = mockStatic(ApiReader.class)) {
//            mockedApiReader.when(() -> new ApiReader(anyString())).thenReturn(mockApiReader);
//
//            double[] location = dataGetter.getLocationFromApiReader("12345");
//            assertNotNull(location);
//            assertEquals(50.8503, location[0], 0.001);
//            assertEquals(4.3517, location[1], 0.001);
//        }
//    }
//
//    @Test
//    public void testBusStopList() throws SQLException {
//        PreparedStatement mockStmt = mock(PreparedStatement.class);
//        ResultSet mockResultSet = mock(ResultSet.class);
//
//        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
//        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
//        when(mockResultSet.next()).thenReturn(true, false);
//        when(mockResultSet.getString("stop_id")).thenReturn("stoparea:123");
//
//        List<Integer> stops = dataGetter.busStopList(50.8503, 4.3517, 1.0);
//        assertNotNull(stops);
//        assertEquals(1, stops.size());
//        assertEquals(123, (int) stops.get(0));
//    }
//
//    @Test
//    public void testGetTripIDs() throws SQLException {
//        PreparedStatement mockStmt = mock(PreparedStatement.class);
//        ResultSet mockResultSet = mock(ResultSet.class);
//
//        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStmt);
//        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
//        when(mockResultSet.next()).thenReturn(true, false);
//        when(mockResultSet.getInt("trip_id")).thenReturn(1);
//        when(mockResultSet.getInt("time_taken")).thenReturn(30);
//        when(mockResultSet.getString("start_stop_id")).thenReturn("start");
//        when(mockResultSet.getString("end_stop_id")).thenReturn("end");
//        when(mockResultSet.getString("departure_time")).thenReturn("08:00:00");
//        when(mockResultSet.getString("arrival_time")).thenReturn("08:30:00");
//
//        List<TripDetail> trips = dataGetter.getTripIDs(Collections.singletonList(1), Collections.singletonList(2), "08:00:00");
//        assertNotNull(trips);
//        assertEquals(1, trips.size());
//
//        TripDetail trip = trips.get(0);
//        assertEquals(1, trip.getTripId());
//        assertEquals(30, trip.getTimeTaken());
//        assertEquals("start", trip.getStartStopId());
//        assertEquals("end", trip.getEndStopId());
//        assertEquals("08:00:00", trip.getDepartureTime());
//        assertEquals("08:30:00", trip.getArrivalTime());
//    }
//
//    @Test
//    public void testFindFastestRouteInfo() throws SQLException {
//        DataGetter mockDataGetter = Mockito.spy(dataGetter);
//        doReturn(Collections.singletonList(1)).when(mockDataGetter).busStopList(anyDouble(), anyDouble(), anyDouble());
//        doReturn(Collections.singletonList(new TripDetail(1, 30, "start", "end", "08:00:00", "08:30:00", "route1"))).when(mockDataGetter).getTripIDs(anyList(), anyList(), anyString());
//
//        TripCoordinates result = mockDataGetter.findFastestRouteInfo(50.8503, 4.3517, 50.8503, 4.3517, 1.0, "08:00:00");
//        assertNotNull(result);
//        assertEquals(30, result.getBusTripTime());
//    }
//
//    @After
//    public void tearDown() {
//        mockedDatabaseSingleton.close();
//    }
//}
