package Data;

import Api.ApiReader;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The DataGetter class handles data retrieval from the API and database for operations
 * such as fetching coordinates, finding bus stops, and calculating routes.
 */
public class DataGetter {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    public DataGetter() {
    }

    /**
     * Retrieves the location coordinates (latitude and longitude) for a given postal code using the ApiReader.
     *
     * @param postalCode the postal code to retrieve coordinates for.
     * @return an array containing the latitude and longitude, or null if not found.
     */
    public double[] getLocationFromApiReader(String postalCode) {
        ApiReader apiReader = new ApiReader(postalCode);
        if (apiReader.getLatitude() != 0 && apiReader.getLongitude() != 0) {
            return new double[]{apiReader.getLatitude(), apiReader.getLongitude()};
        }
        return null;
    }

    /**
     * Retrieves a list of bus stop IDs within a specified radius from the given latitude and longitude.
     *
     * @param latitude  the latitude of the center point.
     * @param longitude the longitude of the center point.
     * @param radius    the search radius in kilometers.
     * @return a list of bus stop IDs within the radius.
     */
    public List<Integer> busStopList(double latitude, double longitude, double radius) {
        List<Integer> stops = new ArrayList<>();
        Connection conn = DatabaseSingleton.getConnection();
        String getDestinationStops = "SELECT stop_id FROM stops WHERE (6371000 * acos(cos(radians(?)) * cos(radians(stop_lat)) * cos(radians(stop_lon) - radians(?)) + sin(radians(?)) * sin(radians(stop_lat)))) <= ? AND stop_lat BETWEEN ? AND ? AND stop_lon BETWEEN ? AND ?";

        try {
            assert conn != null;
            try (PreparedStatement stmt = conn.prepareStatement(getDestinationStops)) {
                stmt.setDouble(1, latitude);
                stmt.setDouble(2, longitude);
                stmt.setDouble(3, latitude);
                stmt.setDouble(4, radius * 1000);
                double SOUTH_BOUND = 50.800000;
                stmt.setDouble(5, SOUTH_BOUND);
                double NORTH_BOUND = 50.870000;
                stmt.setDouble(6, NORTH_BOUND);
                double WEST_BOUND = 5.650000;
                stmt.setDouble(7, WEST_BOUND);
                double EAST_BOUND = 5.750000;
                stmt.setDouble(8, EAST_BOUND);
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    String stopId = resultSet.getString("stop_id").replace("stoparea:", "");
                    stops.add(Integer.parseInt(stopId));
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage() + "line 94 busStopList");
        }
        System.out.println("Number of stops found: " + stops.size());
        return stops;
    }

    /**
     * Retrieves a list of trip details that match the given departure stops, arrival stops, and departure time.
     *
     * @param departureStops the list of departure stop IDs.
     * @param arrivalStops   the list of arrival stop IDs.
     * @param time           the departure time.
     * @return a list of TripDetail objects matching the criteria.
     */
    public TripDetail getTripIDs(List<Integer> departureStops, List<Integer> arrivalStops, String time) {
        if (departureStops.isEmpty() || arrivalStops.isEmpty()) {
            return null;
        }

        String getMatchingTripIDs =
                "SELECT s1.trip_id, s1.stop_id as start_stop_id, s2.stop_id as end_stop_id, " +
                        "s1.departure_time, s2.arrival_time, " +
                        "MIN(time_to_sec(timediff(s2.arrival_time, s1.departure_time))) AS time_taken, " +
                        "ABS(time_to_sec(timediff(s1.departure_time, ?))) AS time_diff " +
                        "FROM stop_times s1 " +
                        "JOIN stop_times s2 ON s1.trip_id = s2.trip_id " +
                        "WHERE s1.stop_id IN (" + joinIds(departureStops) + ") " +
                        "AND s2.stop_id IN (" + joinIds(arrivalStops) + ") " +
                        "AND timediff(s2.arrival_time, s1.departure_time) > '00:00:00' " +
                        "AND s1.departure_time >= ? " +
                        "GROUP BY s1.trip_id, s1.stop_id, s2.stop_id, s1.departure_time, s2.arrival_time " +
                        "ORDER BY time_diff , time_taken " +
                        "LIMIT 1;";


        Connection conn = DatabaseSingleton.getConnection();

        try {
            assert conn != null;
            PreparedStatement preparedStatement = conn.prepareStatement(getMatchingTripIDs);
            preparedStatement.setString(1, time);
            preparedStatement.setString(2, time);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return new TripDetail(
                        resultSet.getInt("trip_id"),
                        resultSet.getInt("time_taken"),
                        resultSet.getString("start_stop_id"),
                        resultSet.getString("end_stop_id"),
                        resultSet.getString("departure_time"),
                        resultSet.getString("arrival_time"),
                        getRouteID(resultSet.getString("trip_id"))
                );
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage() + "line 155 getTripIDs");
        }
        return null;
    }


    public TransferTripCoordinates getTransferTripIDs(double departureLat, double departureLong, double arrivalLat, double arrivalLong, double radius, String time) {
        List<Integer> departureStops = busStopList(departureLat, departureLong, radius);
        List<Integer> arrivalStops = busStopList(arrivalLat, arrivalLong, radius);
        if (departureStops.isEmpty() || arrivalStops.isEmpty()) {
            return null;
        }

        Connection connection = DatabaseSingleton.getConnection();

        try {
            assert connection != null;
            try (Statement statement = connection.createStatement()) {
                System.out.println(joinIds(departureStops));
                System.out.println(joinIds(arrivalStops));

                String query = String.format(
                        "WITH origin_trips AS (" +
                                "    SELECT DISTINCT trip_id " +
                                "    FROM stop_times " +
                                "    WHERE stop_id IN (%s)" +
                                "), " +
                                "destination_trips AS (" +
                                "    SELECT DISTINCT trip_id " +
                                "    FROM stop_times " +
                                "    WHERE stop_id IN (%s)" +
                                "), " +
                                "transfer_stops AS (" +
                                "    SELECT " +
                                "        st1.trip_id AS origin_trip_id, " +
                                "        st2.trip_id AS destination_trip_id, " +
                                "        st1.stop_id AS transfer_stop, " +
                                "        st1.arrival_time AS transfer_arrival, " +
                                "        st2.departure_time AS transfer_departure " +
                                "    FROM stop_times st1 " +
                                "    JOIN stop_times st2 ON st1.stop_id = st2.stop_id " +
                                "    WHERE st1.trip_id IN (SELECT trip_id FROM origin_trips) " +
                                "      AND st2.trip_id IN (SELECT trip_id FROM destination_trips) " +
                                "      AND st1.arrival_time < st2.departure_time " +
                                "      AND st1.trip_id <> st2.trip_id" +
                                ") " +
                                "SELECT " +
                                "    ts.origin_trip_id AS origin_trip, " +
                                "    ts.destination_trip_id AS destination_trip, " +
                                "    ts.transfer_stop AS transfer_stop, " +
                                "    s1.stop_id AS origin_stop_name, " +
                                "    s2.stop_id AS destination_stop_name, " +
                                "    s3.stop_id AS transfer_stop_name, " +
                                "    st1.departure_time AS origin_departure_time, " +
                                "    st2.arrival_time AS destination_arrival_time, " +
                                "    ts.transfer_arrival AS origin_arrival_time, " +
                                "    ts.transfer_departure AS destination_departure_time, " +
                                "    TIME_TO_SEC(TIMEDIFF(ts.transfer_arrival, st1.departure_time)) AS origin_trip_duration, " +
                                "    TIME_TO_SEC(TIMEDIFF(st2.arrival_time, ts.transfer_departure)) AS destination_trip_duration, " +
                                "    TIME_TO_SEC(TIMEDIFF(st2.arrival_time, st1.departure_time)) AS total_travel " +
                                "FROM transfer_stops ts " +
                                "JOIN stop_times st1 ON ts.origin_trip_id = st1.trip_id " +
                                "                     AND st1.stop_id IN (%s) " +
                                "JOIN stop_times st2 ON ts.destination_trip_id = st2.trip_id " +
                                "                     AND st2.stop_id IN (%s) " +
                                "JOIN stops s1 ON st1.stop_id = s1.stop_id " +
                                "JOIN stops s2 ON st2.stop_id = s2.stop_id " +
                                "JOIN stops s3 ON ts.transfer_stop = s3.stop_id " +
                                "WHERE st2.arrival_time > ts.transfer_departure " +
                                "  AND st1.departure_time < ts.transfer_arrival " +
                                "ORDER BY total_travel;",
                        joinIds(departureStops),
                        joinIds(arrivalStops),
                        joinIds(departureStops),
                        joinIds(arrivalStops)
                );

                ResultSet rs = statement.executeQuery(query);

                if (rs.next()) {
                    System.out.println("found a resultset");

                    int originTripId = rs.getInt("origin_trip");
                    int originTripDuration = rs.getInt("origin_trip_duration");
                    String originStopName = rs.getString("origin_stop_name");
                    String transferStopName = rs.getString("transfer_stop_name");
                    String originDepartureTime = rs.getString("origin_departure_time");
                    String originArrivalTime = rs.getString("origin_arrival_time");

                    int destinationTripId = rs.getInt("destination_trip");
                    int destinationTripDuration = rs.getInt("destination_trip_duration");
                    String destinationStopName = rs.getString("destination_stop_name");
                    String destinationDepartureTime = rs.getString("destination_departure_time");
                    String destinationArrivalTime = rs.getString("destination_arrival_time");

                    int transferStopId = rs.getInt("transfer_stop");
                    int totalTravelTime = rs.getInt("total_travel");

                    System.out.println("Origin Trip ID: " + originTripId);
                    System.out.println("Origin Trip Duration: " + originTripDuration);
                    System.out.println("Origin Stop Name: " + originStopName);
                    System.out.println("Transfer Stop Name: " + transferStopName);
                    System.out.println("Origin Departure Time: " + originDepartureTime);
                    System.out.println("Origin Arrival Time: " + originArrivalTime);

                    System.out.println("Destination Trip ID: " + destinationTripId);
                    System.out.println("Destination Trip Duration: " + destinationTripDuration);
                    System.out.println("Destination Stop Name: " + destinationStopName);
                    System.out.println("Destination Departure Time: " + destinationDepartureTime);
                    System.out.println("Destination Arrival Time: " + destinationArrivalTime);

                    System.out.println("Transfer Stop ID: " + transferStopId);
                    System.out.println("Total Travel Time: " + totalTravelTime);
                    ArrayList<TripCoordinates> legs = new ArrayList<>();
                    ArrayList<Integer> transferStopIDs = new ArrayList<>();
                    transferStopIDs.add(transferStopId);
                    TripDetail trip1 = new TripDetail(
                            originTripId,
                            originTripDuration,
                            originStopName,
                            transferStopName,
                            originDepartureTime,
                            originArrivalTime,
                            getRouteID(rs.getString("origin_trip"))
                    );
                    legs.add(TripCoordinates.getTripCoordinates(trip1, getIntermediateStopIds(trip1.getStartStopId(), trip1.getEndStopId(), trip1.getTripId())));
                    TripDetail trip2 = new TripDetail(
                            destinationTripId,
                            destinationTripDuration,
                            transferStopName,
                            destinationStopName,
                            destinationDepartureTime,
                            destinationArrivalTime,
                            getRouteID(rs.getString("destination_trip"))
                    );
                    legs.add(TripCoordinates.getTripCoordinates(trip2, getIntermediateStopIds(trip2.getStartStopId(), trip2.getEndStopId(), trip2.getTripId())));
                    return new TransferTripCoordinates(
                            legs,
                            transferStopIDs,
                            transferStopName,
                            totalTravelTime
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching transfer trips: " + e.getMessage());
        } finally {
            try {
                Statement cleanupStatement = connection.createStatement();
                cleanupStatement.execute("DROP TEMPORARY TABLE IF EXISTS origin_trips, destination_trips, transfer_stops");
                cleanupStatement.close();
            } catch (SQLException e) {
                System.out.println("Error cleaning up temporary tables: " + e.getMessage());
            }
        }
        return null;
    }

    public double[] getStopCoordinates(int stopID) {
        String getCoordinates = "select stop_lat, stop_lon from stops where stop_id = " + stopID + ";";
        Connection connection = DatabaseSingleton.getConnection();
        try {
            assert connection != null;
            try (PreparedStatement preparedStatement = connection.prepareStatement(getCoordinates)) {
                ResultSet rs = preparedStatement.executeQuery();
                if (rs.next()) {
                    double[] coordinates = new double[2];
                    double lat = rs.getDouble("stop_lat");
                    double lon = rs.getDouble("stop_lon");
                    coordinates[0] = lat;
                    coordinates[1] = lon;
                    return coordinates;
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public String getStopName(String stopId) {
        String getCoordinates = "select stop_name from stops where stop_id = " + stopId + ";";
        Connection connection = DatabaseSingleton.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(getCoordinates)) {
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                String name = rs.getString("stop_name");
                return name;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return stopId;
    }

    /**
     * Retrieves the route ID for a given trip ID.
     *
     * @param trip_id the trip ID to retrieve the route ID for.
     * @return the route ID as a string.
     */
    public String getRouteID(String trip_id) {
        Connection connection = DatabaseSingleton.getConnection();
        String getName = "select route_id from trips where trip_id = ?";
        try {
            assert connection != null;
            try (PreparedStatement preparedStatement = connection.prepareStatement(getName)) {
                preparedStatement.setString(1, trip_id);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getString("route_id");
                }
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    public String getTripName(int trip_id) {
        Connection connection = DatabaseSingleton.getConnection();
        String getName = "SELECT route_short_name " + 
                         "FROM routes " + 
                         "WHERE route_id IN (SELECT route_id FROM trips WHERE trip_id = ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(getName)) {
            preparedStatement.setInt(1, trip_id);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return rs.getString("route_short_name");
            }
        } catch (Exception e) {
            System.out.println("Error getting trip name");
            System.out.println(e.getMessage());
        }
        return "WALK FAT FUCKER";
    }
    

    /**
     * Retrieves the bus number (route short name) for a given route ID.
     *
     * @param routeID the route ID to retrieve the bus number for.
     * @return the bus number as a string.
     */
    public String getBusNumber(String routeID) {
        Connection connection = DatabaseSingleton.getConnection();
        String getName = "select route_short_name from routes where route_id = ?";
        try {
            assert connection != null;
            try (PreparedStatement preparedStatement = connection.prepareStatement(getName)) {
                preparedStatement.setString(1, routeID);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getString("route_short_name");
                }
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    /**
     * Joins a list of integers into a comma-separated string.
     *
     * @param ids the list of integers to join.
     * @return a comma-separated string of the integers.
     */
    private String joinIds(List<Integer> ids) {
        StringJoiner joiner = new StringJoiner(",");
        for (Integer id : ids) {
            joiner.add(id.toString());
        }
        return joiner.toString();
    }

    /**
     * Retrieves a list of intermediate stop IDs for a given trip between the start and end stops.
     *
     * @param startStopId the ID of the start stop.
     * @param endStopId   the ID of the end stop.
     * @param tripId      the trip ID.
     * @return a list of intermediate stop IDs.
     * @throws SQLException if a database access error occurs.
     */
    private List<String> getIntermediateStopIds(String startStopId, String endStopId, int tripId) throws SQLException {
        System.out.println("getting intermediate stops");
        List<String> intermediateStops = new ArrayList<>();
        Connection conn = DatabaseSingleton.getConnection();

        String getIntermediateStopsQuery = "SELECT stop_times.stop_id " +
                "FROM stop_times " +
                "WHERE trip_id = ? AND stop_sequence >= (SELECT stop_sequence FROM stop_times WHERE trip_id = ? AND stop_id = ?) " +
                "AND stop_sequence <= (SELECT stop_sequence FROM stop_times WHERE trip_id = ? AND stop_id = ?) " +
                "ORDER BY stop_sequence";

        try {
            assert conn != null;
            try (PreparedStatement stmt = conn.prepareStatement(getIntermediateStopsQuery)) {
                stmt.setInt(1, tripId);
                stmt.setInt(2, tripId);
                stmt.setString(3, startStopId);
                stmt.setInt(4, tripId);
                stmt.setString(5, endStopId);

                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    intermediateStops.add(resultSet.getString("stop_id"));
                    System.out.println(resultSet.getString("stop_id"));
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage() + "line 292 getIntermediateStopIds");
        }

        return intermediateStops;
    }

    /**
     * Finds the fastest route information between two locations using bus stops within a specified radius and departure time.
     *
     * @param departureLat  the latitude of the departure location.
     * @param departureLong the longitude of the departure location.
     * @param arrivalLat    the latitude of the arrival location.
     * @param arrivalLong   the longitude of the arrival location.
     * @param radius        the search radius in kilometers.
     * @param time          the departure time.
     * @return a TripCoordinates object containing the route information, or null if no route is found.
     * @throws SQLException if a database access error occurs.
     */


    public TripCoordinates findFastestDirectRouteInfo(double departureLat, double departureLong, double arrivalLat, double arrivalLong, double radius, String time) throws SQLException {
        long start = System.currentTimeMillis();
        List<Integer> departureStops = busStopList(departureLat, departureLong, radius);
        List<Integer> arrivalStops = busStopList(arrivalLat, arrivalLong, radius);
        TripDetail trip = getTripIDs(departureStops, arrivalStops, time);
        long end = System.currentTimeMillis();

        TripCoordinates tripCoordinates = null;
        if (trip != null) {
            List<String> intermediateStopIds = getIntermediateStopIds(trip.getStartStopId(), trip.getEndStopId(), trip.getTripId());

            tripCoordinates = TripCoordinates.getTripCoordinates(trip, intermediateStopIds);

            tripCoordinates.setBusTripTime(trip.getTimeTaken());

            System.out.println("Finding fastest route info took: " + (end - start) + " ms");
            return tripCoordinates;

        } else {
            System.out.println("No direct trips found.");
        }
        System.out.println("Finding fastest route info took: " + (end - start) + " ms");
        return null;
    }

    public List<double[]> getShapePoints(int tripId) throws SQLException {
        List<double[]> shapePoints = new ArrayList<>();
        Connection conn = DatabaseSingleton.getConnection();
        String query = "SELECT shape_pt_lat, shape_pt_lon, shape_pt_sequence " +
                "FROM shapes " +
                "WHERE shape_id = (SELECT shape_id FROM trips WHERE trip_id = ?) " +
                "ORDER BY shape_pt_sequence";
        try {
            assert conn != null;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, tripId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                double lat = rs.getDouble("shape_pt_lat");
                double lon = rs.getDouble("shape_pt_lon");
                shapePoints.add(new double[]{lat, lon});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shapePoints;
    }

    /**
     * Cleans the database by removing irrelevant data from the stops, stop_times, trips, and routes tables.
     */
    public void cleanData() {
        Connection connection = DatabaseSingleton.getConnection();
        String removeStops = "DELETE FROM stops WHERE NOT (stop_lat BETWEEN 50.8150 AND 50.8700 AND stop_lon BETWEEN 5.6500 AND 5.7500)";
        String removeStopTimes = "DELETE FROM stop_times WHERE stop_id NOT IN (SELECT stop_id FROM stops)";
        String removeTrips = "DELETE FROM trips WHERE trip_id NOT IN (SELECT trip_id FROM stop_times)";
        String removeRoutes = "DELETE FROM routes WHERE route_id NOT IN (SELECT route_id FROM trips)";

        // Index creation statements
        String createIndexOnStops = "CREATE INDEX idx_stop_id ON stop_times (stop_id)";
        String createIndexOnTripId = "CREATE INDEX idx_trip_id ON stop_times (trip_id)";
        String createIndexOnDepartureTime = "CREATE INDEX idx_departure_time ON stop_times (departure_time)";
        String createIndexOnArrivalTime = "CREATE INDEX idx_arrival_time ON stop_times (arrival_time)";
        String createIndexOnStopSequence = "CREATE INDEX idx_stop_sequence ON stop_times (stop_sequence)";

        // View creation statement
        String createViewRouteWeights = "CREATE OR REPLACE VIEW view_route_weights AS " +
                "SELECT " +
                "t1.stop_id AS start_stop_id, " +
                "t1.trip_id, " +
                "t1.departure_time AS start_departure_time, " +
                "t2.stop_id AS end_stop_id, " +
                "t2.arrival_time AS end_arrival_time, " +
                "TIMESTAMPDIFF(MINUTE, t1.departure_time, t2.arrival_time) AS travel_time " +
                "FROM stop_times t1 " +
                "JOIN stop_times t2 ON t1.trip_id = t2.trip_id AND t2.stop_sequence = t1.stop_sequence + 1";

        try {
            assert connection != null;
            try (PreparedStatement preparedStatement = connection.prepareStatement(removeStops)) {
                preparedStatement.executeUpdate();
                try (PreparedStatement preparedStatement1 = connection.prepareStatement(removeStopTimes)) {
                    preparedStatement1.executeUpdate();
                    try (PreparedStatement preparedStatement2 = connection.prepareStatement(removeTrips)) {
                        preparedStatement2.executeUpdate();
                        try (PreparedStatement preparedStatement3 = connection.prepareStatement(removeRoutes)) {
                            preparedStatement3.executeUpdate();
                            try (Statement statement = connection.createStatement()) {
                                try {
                                    statement.executeUpdate(createIndexOnStops);
                                } catch (SQLException e) {
                                    if (!e.getMessage().contains("Duplicate key name")) throw e;
                                }
                                try {
                                    statement.executeUpdate(createIndexOnTripId);
                                } catch (SQLException e) {
                                    if (!e.getMessage().contains("Duplicate key name")) throw e;
                                }
                                try {
                                    statement.executeUpdate(createIndexOnDepartureTime);
                                } catch (SQLException e) {
                                    if (!e.getMessage().contains("Duplicate key name")) throw e;
                                }
                                try {
                                    statement.executeUpdate(createIndexOnArrivalTime);
                                } catch (SQLException e) {
                                    if (!e.getMessage().contains("Duplicate key name")) throw e;
                                }
                                try {
                                    statement.executeUpdate(createIndexOnStopSequence);
                                } catch (SQLException e) {
                                    if (!e.getMessage().contains("Duplicate key name")) throw e;
                                }
                                statement.executeUpdate(createViewRouteWeights);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Data cleaned and views/indexes created successfully.");
    }


    public Map<String, String[]> getAllBusStops() {
        Map<String, String[]> busStops = new HashMap<>();
        Connection connection = DatabaseSingleton.getConnection();
        String query = "SELECT stop_id, stop_lat, stop_lon FROM stops";

        try {
            assert connection != null;
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    String stopId = resultSet.getString("stop_id").replace("stoparea:", "");
                    double lat = resultSet.getDouble("stop_lat");
                    double lon = resultSet.getDouble("stop_lon");
                    String latStr = String.format("%.2f", lat);
                    String lonStr = String.format("%.2f", lon);
                    busStops.put(stopId, new String[]{latStr, lonStr});

                    // System.out.print(stopId + " ");
                    // System.out.print(lat + " " + lon);
                    // System.out.println();
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage() + "line 389 getAllBustops");
        }
        return busStops;
    }

 
        public Map<String, String[]> getBusStopCoordinates() {
            Map<String, String[]> busStopCoordinates = new HashMap<>();
            Connection connection = DatabaseSingleton.getConnection();
            String query = "SELECT stop_id, stop_lat, stop_lon FROM stops";
    
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String stopId = rs.getString("stop_id");
                    String latitude = rs.getString("stop_lat");
                    String longitude = rs.getString("stop_lon");
                    busStopCoordinates.put(stopId, new String[]{latitude, longitude});
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return busStopCoordinates;
        }
    

        public Map<String, Double> getNextDepartureTimesBatch(List<String> stopIds, double arrivalTime) {
        Map<String, Double> nextDepartureTimes = new ConcurrentHashMap<>();
        List<Callable<Void>> tasks = new ArrayList<>();

        for (String stopId : stopIds) {
            tasks.add(() -> {
                Connection connection = DatabaseSingleton.getConnection();
                String query = "SELECT departure_time FROM stop_times WHERE stop_id = ? AND departure_time > ? ORDER BY departure_time LIMIT 1";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, stopId);
                    stmt.setDouble(2, arrivalTime);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        nextDepartureTimes.put(stopId, rs.getDouble("departure_time"));
                    } else {
                        nextDepartureTimes.put(stopId, Double.MAX_VALUE);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            });
        }

        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return nextDepartureTimes;
    }
}
    