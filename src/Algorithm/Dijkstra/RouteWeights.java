package Algorithm.Dijkstra;

import java.sql.*;
import java.util.*;
import Data.DatabaseSingleton;

public class RouteWeights {
    public static List<Map<String, Object>> getRouteWeights(List<String> stopIds, String userTime) {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection connection = DatabaseSingleton.getConnection();
        String placeholders = String.join(",", Collections.nCopies(stopIds.size(), "?"));
        String query = "SELECT " +
                       "start_stop_id, trip_id, start_departure_time, end_stop_id, end_arrival_time, travel_time " +
                       "FROM view_route_weights " +
                       "WHERE start_stop_id IN (" + placeholders + ") AND start_departure_time > ? " +
                       "ORDER BY start_departure_time";
    
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            int index = 1;
            for (String stopId : stopIds) {
                preparedStatement.setString(index++, stopId);
            }
            preparedStatement.setString(index, userTime);
    
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("start_stop_id", rs.getString("start_stop_id"));
                row.put("trip_id", rs.getInt("trip_id"));
                row.put("start_departure_time", rs.getString("start_departure_time"));
                row.put("end_stop_id", rs.getString("end_stop_id"));
                row.put("end_arrival_time", rs.getString("end_arrival_time"));
                row.put("travel_time", rs.getInt("travel_time") * 1.0); 
                
                // if there is a previous stop, calculate waiting time
                if (!results.isEmpty()) {
                    Map<String, Object> previousRow = results.get(results.size() - 1);
                    String previousArrivalTime = (String) previousRow.get("end_arrival_time");
                    String nextDepartureTime = (String) row.get("start_departure_time");
                    double waitingTime = calculateWaitingTime(previousArrivalTime, nextDepartureTime);
                    row.put("waiting_time", waitingTime);
                    row.put("total_time", waitingTime + (double) row.get("travel_time"));
                } else {
                    row.put("waiting_time", 0.0);
                    row.put("total_time", row.get("travel_time"));
                }
                
                results.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static double calculateWaitingTime(String previousArrivalTime, String nextDepartureTime) {
        int previousArrivalMinutes = convertTimeToMinutes(previousArrivalTime);
        int nextDepartureMinutes = convertTimeToMinutes(nextDepartureTime);
        return nextDepartureMinutes - previousArrivalMinutes;
    }

    private static int convertTimeToMinutes(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return hours * 60 + minutes;
    }
}

