package Algorithm.Dijkstra;

import org.jgrapht.graph.DefaultWeightedEdge;
import java.util.Map;

public class CustomEdge extends DefaultWeightedEdge {
    private Map<Integer, Double> idWeightMap;

    public CustomEdge(Map<Integer, Double> idWeightMap) {
        this.idWeightMap = idWeightMap;
    }

    public Map<Integer, Double> getIdWeightMap() {
        return idWeightMap;
    }


    @Override
    public String toString() {
        return "CustomEdge{" +
                "idWeightMap=" + idWeightMap +
                '}';
    }
}