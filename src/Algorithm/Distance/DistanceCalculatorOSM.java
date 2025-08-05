package Algorithm.Distance;

import Algorithm.OSM.Coordinate;
import Algorithm.OSM.Dijkstra;
import Algorithm.OSM.Graph;
import Algorithm.OSM.OsmParser;
public class DistanceCalculatorOSM {
    private Graph graph;

    public DistanceCalculatorOSM(OsmParser osmParser) {
        this.graph = osmParser.createGraph();
    }

    /*
    Calculates the distance between two points using the OSM data.
    @param lat1 Latitude of the first point in degrees
    @param lon1 Longitude of the first point in degrees
    @param lat2 Latitude of the second point in degrees
    @param lon2 Longitude of the second point in degrees
    @return Distance between the two points in kilometers
    */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        Coordinate source = new Coordinate(lat1, lon1);
        Coordinate target = new Coordinate(lat2, lon2);
        
        // Use the graph to find the shortest path distance
        return findShortestPathDistance(source, target);
    }

    private double findShortestPathDistance(Coordinate source, Coordinate target) {
        // Implement Dijkstra's algorithm or any shortest path algorithm here
        Dijkstra dijkstra = new Dijkstra(graph);
        return dijkstra.findShortestDistance(source, target);
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
