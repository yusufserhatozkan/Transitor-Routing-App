package Algorithm.OSM;

import java.util.List;
import java.util.Map;

public class Graph {
    private Map<Integer, Coordinate> coordinates;
    private Map<Integer, List<Edge>> adjacencyList;

    public Graph(Map<Integer, Coordinate> coordinates, Map<Integer, List<Edge>> adjacencyList) {
        this.coordinates = coordinates;
        this.adjacencyList = adjacencyList;
    }

    public Map<Integer, Coordinate> getCoordinates() {
        return coordinates;
    }

    public Map<Integer, List<Edge>> getAdjacencyList() {
        return adjacencyList;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "coordinates=" + coordinates +
                ", adjacencyList=" + adjacencyList +
                '}';
    }
}
