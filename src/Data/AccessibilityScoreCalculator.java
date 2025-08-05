package Data;

import java.sql.*;

public class AccessibilityScoreCalculator {

    private final DataGetter dataGetter;

    public AccessibilityScoreCalculator() {
        dataGetter = new DataGetter();
    }
    public double calculateAccessibility(String postalCode, double radius) {
        double[] location = dataGetter.getLocationFromApiReader(postalCode);
        if (location == null) {
            throw new IllegalArgumentException("Invalid postal code");
        }

        double lat = location[0];
        double lon = location[1];

        // Calculate distance to city center
        double distanceToCenter = calculateDistanceToCenter(lat, lon);

        // Calculate number of bus stops within radius
        int busStopsWithinRadius = getBusStopsWithinRadius(lat, lon, radius);

        // Fetch weighted count of amenities, shops, and tourism places
        double weightedScore = calculateWeightedScore(lat, lon, radius);

        // Calculate number of health facilities
        int healthFacilitiesCount = getHealthFacilitiesCount(lat, lon, radius);

        // Calculate number of educational institutions
        int educationalInstitutionsCount = getEducationalInstitutionsCount(lat, lon, radius);

        // Calculate number of supermarkets and convenience stores
        int supermarketConvenienceCount = getSupermarketConvenienceCount(lat, lon, radius);

        // Normalize the scores
        double normalizedDistanceScore = normalizeDistanceScore(distanceToCenter);
        double normalizedWeightedScore = normalizeWeightedScore(weightedScore);
        double normalizedBusStopsScore = normalizeBusStopsScore(busStopsWithinRadius);
        double normalizedHealthFacilitiesScore = normalizeHealthFacilitiesScore(healthFacilitiesCount);
        double normalizedEducationalInstitutionsScore = normalizeEducationalInstitutionsScore(educationalInstitutionsCount);
        double normalizedSupermarketConvenienceScore = normalizeSupermarketConvenienceScore(supermarketConvenienceCount);

        // Calculate the total accessibility score
        double totalAccessibilityScore = calculateTotalAccessibilityScore(
                normalizedDistanceScore,
                normalizedWeightedScore,
                normalizedBusStopsScore,
                normalizedHealthFacilitiesScore,
                normalizedEducationalInstitutionsScore,
                normalizedSupermarketConvenienceScore
        );

        // Convert the total score into a score out of 100
        double accessibilityScore = Math.round(totalAccessibilityScore * 10000) / 100.0;

        // Print results
        System.out.println("Postal Code: " + postalCode);
        System.out.println("Distance to Center (normalized): " + normalizedDistanceScore);
        System.out.println("Weighted Accessibility Score (normalized): " + normalizedWeightedScore);
        System.out.println("Bus Stops within " + radius + " km (normalized): " + normalizedBusStopsScore);
        System.out.println("Health Facilities within " + radius + " km (normalized): " + normalizedHealthFacilitiesScore);
        System.out.println("Educational Institutions within " + radius + " km (normalized): " + normalizedEducationalInstitutionsScore);
        System.out.println("Supermarkets and Convenience Stores within " + radius + " km (normalized): " + normalizedSupermarketConvenienceScore);
        System.out.println("Total Accessibility Score: " + accessibilityScore );
        return accessibilityScore;
    }

    private double calculateDistanceToCenter(double lat, double lon) {
        final double CITY_CENTER_LAT = 50.8516;
        final double CITY_CENTER_LON = 5.6915;
        return 6371 * Math.acos(Math.cos(Math.toRadians(CITY_CENTER_LAT)) * Math.cos(Math.toRadians(lat))
                * Math.cos(Math.toRadians(lon) - Math.toRadians(CITY_CENTER_LON))
                + Math.sin(Math.toRadians(CITY_CENTER_LAT)) * Math.sin(Math.toRadians(lat)));
    }

    private int getBusStopsWithinRadius(double lat, double lon, double radius) {
        int count = 0;
        Connection conn = DatabaseSingleton.getConnection();
        String query = "SELECT COUNT(s.stop_id) AS bus_stops_within_radius " +
                "FROM stops s " +
                "WHERE (6371 * ACOS(COS(RADIANS(?)) * COS(RADIANS(s.stop_lat)) " +
                "* COS(RADIANS(s.stop_lon) - RADIANS(?)) + SIN(RADIANS(?)) * SIN(RADIANS(s.stop_lat)))) <= ?";
        try {
            assert conn != null;
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setDouble(1, lat);
                stmt.setDouble(2, lon);
                stmt.setDouble(3, lat);
                stmt.setDouble(4, radius);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    count = resultSet.getInt("bus_stops_within_radius");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    private double calculateWeightedScore(double lat, double lon, double radius) {
        double totalScore = 0;
        Connection conn = DatabaseSingleton.getConnection();
        String query = "SELECT w.weight " +
                "FROM weights w " +
                "JOIN ( " +
                "    SELECT 'amenity' AS category, type, latitude, longitude " +
                "    FROM amenities " +
                "    UNION ALL " +
                "    SELECT 'shop' AS category, type, latitude, longitude " +
                "    FROM shops " +
                "    UNION ALL " +
                "    SELECT 'tourism' AS category, type, tourism.latitude, tourism.longitude " +
                "    FROM tourism " +
                ") AS combined " +
                "ON w.type = combined.type AND w.category = combined.category " +
                "WHERE (6371 * ACOS(COS(RADIANS(?)) * COS(RADIANS(combined.latitude)) " +
                "* COS(RADIANS(combined.longitude) - RADIANS(?)) + SIN(RADIANS(?)) * SIN(RADIANS(combined.latitude)))) <= ?";
        try {
            assert conn != null;
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setDouble(1, lat);
                stmt.setDouble(2, lon);
                stmt.setDouble(3, lat);
                stmt.setDouble(4, radius);
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    double weight = resultSet.getDouble("weight");
                    totalScore += weight;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totalScore;
    }

    private int getHealthFacilitiesCount(double lat, double lon, double radius) {
        int count = 0;
        Connection conn = DatabaseSingleton.getConnection();
        String query = "SELECT COUNT(*) AS health_facilities_count " +
                "FROM amenities " +
                "WHERE type IN ('hospital', 'clinic', 'pharmacy', 'dentist', 'doctors') " +
                "AND (6371 * ACOS(COS(RADIANS(?)) * COS(RADIANS(latitude)) " +
                "* COS(RADIANS(longitude) - RADIANS(?)) + SIN(RADIANS(?)) * SIN(RADIANS(latitude)))) <= ?";
        try {
            assert conn != null;
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setDouble(1, lat);
                stmt.setDouble(2, lon);
                stmt.setDouble(3, lat);
                stmt.setDouble(4, radius);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    count = resultSet.getInt("health_facilities_count");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    private int getEducationalInstitutionsCount(double lat, double lon, double radius) {
        int count = 0;
        Connection conn = DatabaseSingleton.getConnection();
        String query = "SELECT COUNT(*) AS educational_institutions_count " +
                "FROM amenities " +
                "WHERE type IN ('school', 'university', 'college') " +
                "AND (6371 * ACOS(COS(RADIANS(?)) * COS(RADIANS(latitude)) " +
                "* COS(RADIANS(longitude) - RADIANS(?)) + SIN(RADIANS(?)) * SIN(RADIANS(latitude)))) <= ?";
        try {
            assert conn != null;
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setDouble(1, lat);
                stmt.setDouble(2, lon);
                stmt.setDouble(3, lat);
                stmt.setDouble(4, radius);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    count = resultSet.getInt("educational_institutions_count");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    private int getSupermarketConvenienceCount(double lat, double lon, double radius) {
        int count = 0;
        Connection conn = DatabaseSingleton.getConnection();
        String query = "SELECT COUNT(*) AS supermarket_convenience_count " +
                "FROM shops " +
                "WHERE type IN ('supermarket', 'convenience') " +
                "AND (6371 * ACOS(COS(RADIANS(?)) * COS(RADIANS(latitude)) " +
                "* COS(RADIANS(longitude) - RADIANS(?)) + SIN(RADIANS(?)) * SIN(RADIANS(latitude)))) <= ?";
        try {
            assert conn != null;
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setDouble(1, lat);
                stmt.setDouble(2, lon);
                stmt.setDouble(3, lat);
                stmt.setDouble(4, radius);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    count = resultSet.getInt("supermarket_convenience_count");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    private double normalizeDistanceScore(double distance) {
        final double MAX_DISTANCE = 10.0;
        return Math.min(1, 1 - (distance / MAX_DISTANCE));
    }

    private double normalizeWeightedScore(double weightedScore) {
        final double MAX_WEIGHTED_SCORE = 300.0;
        return Math.min(1, weightedScore / MAX_WEIGHTED_SCORE);
    }

    private double normalizeBusStopsScore(int busStopsCount) {
        final int MAX_BUS_STOPS = 20;
        return Math.min(1, (double) busStopsCount / MAX_BUS_STOPS);
    }

    private double normalizeHealthFacilitiesScore(int healthFacilitiesCount) {
        final int MAX_HEALTH_FACILITIES = 20;
        return Math.min(1, (double) healthFacilitiesCount / MAX_HEALTH_FACILITIES);
    }

    private double normalizeEducationalInstitutionsScore(int educationalInstitutionsCount) {
        final int MAX_EDUCATIONAL_INSTITUTIONS = 10;
        return Math.min(1, (double) educationalInstitutionsCount / MAX_EDUCATIONAL_INSTITUTIONS);
    }

    private double normalizeSupermarketConvenienceScore(int supermarketConvenienceCount) {
        final int MAX_SUPERMARKET_CONVENIENCE = 10;
        return Math.min(1, (double) supermarketConvenienceCount / MAX_SUPERMARKET_CONVENIENCE);
    }

    private double calculateTotalAccessibilityScore(double normalizedDistanceScore, double normalizedWeightedScore, double normalizedBusStopsScore, double normalizedHealthFacilitiesScore, double normalizedEducationalInstitutionsScore, double normalizedSupermarketConvenienceScore) {
        final double WEIGHT_DISTANCE = 0.2;
        final double WEIGHT_WEIGHTED_SCORE = 0.4;
        final double WEIGHT_BUS_STOPS = 0.1;
        final double WEIGHT_HEALTH_FACILITIES = 0.1;
        final double WEIGHT_EDUCATIONAL_INSTITUTIONS = 0.1;
        final double WEIGHT_SUPERMARKET_CONVENIENCE = 0.1;
        return (WEIGHT_DISTANCE * normalizedDistanceScore) +
                (WEIGHT_WEIGHTED_SCORE * normalizedWeightedScore) +
                (WEIGHT_BUS_STOPS * normalizedBusStopsScore) +
                (WEIGHT_HEALTH_FACILITIES * normalizedHealthFacilitiesScore) +
                (WEIGHT_EDUCATIONAL_INSTITUTIONS * normalizedEducationalInstitutionsScore) +
                (WEIGHT_SUPERMARKET_CONVENIENCE * normalizedSupermarketConvenienceScore);
    }

    public static void main(String[] args) {
        AccessibilityScoreCalculator calculator = new AccessibilityScoreCalculator();
        calculator.calculateAccessibility("6221BR", 0.5);
    }
}