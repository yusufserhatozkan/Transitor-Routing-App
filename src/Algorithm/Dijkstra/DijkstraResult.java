package Algorithm.Dijkstra;

import org.jgrapht.Graph;
import java.util.List;

public class DijkstraResult {
    private final List<String> path;
    private final Graph<String, CustomEdge> graph;
    private final double totalTravelTime;

    public DijkstraResult(List<String> path, Graph<String, CustomEdge> graph, double totalTravelTime) {
        this.path = path;
        this.graph = graph;
        this.totalTravelTime = totalTravelTime;
    }

    public List<String> getPath() {
        return path;
    }

    public Graph<String, CustomEdge> getGraph() {
        return graph;
    }

    public double getTotalTravelTime() {
        return totalTravelTime;
    }
}
