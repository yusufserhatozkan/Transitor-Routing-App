package Data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TripCoordinates {
    private final String startStopName;
    private final double startStopLat;
    private final double startStopLon;
    private final String endStopName;
    private final double endStopLat;
    private final double endStopLon;
    private final List<String[]> intermediateStopDetails;
    private int busTripTime;
    private final String departureTime;
    private final String routeID;
    private final int tripId;

    public TripCoordinates(String startStopName, double startStopLat, double startStopLon, String endStopName, double endStopLat, double endStopLon, List<String[]> intermediateStopDetails, String departureTime, String routeID, int tripId) {
        this.startStopName = startStopName;
        this.startStopLat = startStopLat;
        this.startStopLon = startStopLon;
        this.endStopName = endStopName;
        this.endStopLat = endStopLat;
        this.endStopLon = endStopLon;
        this.intermediateStopDetails = intermediateStopDetails;
        this.departureTime = departureTime;
        this.routeID = routeID;
        this.tripId = tripId; 
    }

    public String getStartStopName() {
        return startStopName;
    }

    public double getStartStopLat() {
        return startStopLat;
    }

    public double getStartStopLon() {
        return startStopLon;
    }

    public String getEndStopName() {
        return endStopName;
    }

    public double getEndStopLat() {
        return endStopLat;
    }

    public double getEndStopLon() {
        return endStopLon;
    }

    public List<String[]> getIntermediateStopDetails() {
        return intermediateStopDetails;
    }

    public int getBusTripTime() {
        return busTripTime;
    }

    public void setBusTripTime(int busTripTime) {
        this.busTripTime = busTripTime;
    }

    public String getDepartureTime() {
        return departureTime;
    }
    public String getRouteID(){
        return routeID;
    }

    public int getTripId() { 
        return tripId;
    }

    public static String[] getStopDetailsById(String stopId) {
        Connection conn = DatabaseSingleton.getConnection();
        String[] details = null;
        String getStopDetailsQuery = "SELECT stop_name, stop_lat, stop_lon FROM stops WHERE stop_id = ?";
        try {
            assert conn != null;
            try (PreparedStatement stmt = conn.prepareStatement(getStopDetailsQuery)) {
                stmt.setString(1, stopId);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    String name = resultSet.getString("stop_name");
                    double lat = resultSet.getDouble("stop_lat");
                    double lon = resultSet.getDouble("stop_lon");
                    details = new String[]{name, Double.toString(lat), Double.toString(lon)};
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
        }
        return details;
    }

    public static TripCoordinates getTripCoordinates(TripDetail trip, List<String> intermediateStops) throws SQLException {
        String[] startStopDetails = getStopDetailsById(trip.getStartStopId());
        String[] endStopDetails = getStopDetailsById(trip.getEndStopId());
        List<String[]> intermediateStopDetails = new ArrayList<>();
        for (String stopId : intermediateStops) {
            intermediateStopDetails.add(getStopDetailsById(stopId));
        }

        return new TripCoordinates(
                startStopDetails[0], Double.parseDouble(startStopDetails[1]), Double.parseDouble(startStopDetails[2]),
                endStopDetails[0], Double.parseDouble(endStopDetails[1]), Double.parseDouble(endStopDetails[2]),
                intermediateStopDetails, trip.getDepartureStop(), trip.getRouteID(), trip.getTripId() // Pass tripId to the constructor
        );
    }
}