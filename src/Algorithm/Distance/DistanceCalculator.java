package Algorithm.Distance;


public class DistanceCalculator {

    /*
    Calculates the distance between two points on the Earth's surface.
    @param lat1 Latitude of the first point in degrees
    @param lon1 Longitude of the first point in degrees
    @param lat2 Latitude of the second point in degrees
    @param lon2 Longitude of the second point in degrees
    @return Distance between the two points in kilometers
    */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert degrees to radians for latitude and longitude
        double lat1Radians = Math.toRadians(lat1);
        double lon1Radians = Math.toRadians(lon1);
        double lat2Radians = Math.toRadians(lat2);
        double lon2Radians = Math.toRadians(lon2);

        // Difference in coordinates
        double deltaLat = lat2Radians - lat1Radians;
        double deltaLon = lon2Radians - lon1Radians;

        // Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Radians) * Math.cos(lat2Radians) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance calculation
        // Earth's radius in kilometers
        double EARTH_RADIUS_KM = 6371.0;

        return EARTH_RADIUS_KM * c;
    }

    public int calculateWalkingTime(double distanceKm) {
        // Average walking speed in km/h
        int WALKING_SPEED_KM_H = 5;
        return (int) Math.round((distanceKm / WALKING_SPEED_KM_H) * 60); // Convert hours to minutes and round
    }

    public int calculateCyclingTime(double distanceKm) {
        // Average cycling speed in km/h
        int CYCLING_SPEED_KM_H = 15;
        return (int) Math.round((distanceKm / CYCLING_SPEED_KM_H) * 60); // Convert hours to minutes and round
    }
}