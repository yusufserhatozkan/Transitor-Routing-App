package Algorithm.Dijkstra;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;

public class GraphCache {
    private static DefaultDirectedWeightedGraph<String, CustomEdge> cachedGraph;

    public static DefaultDirectedWeightedGraph<String, CustomEdge> getCachedGraph() {
        return cachedGraph;
    }

    public static void cacheGraph(DefaultDirectedWeightedGraph<String, CustomEdge> graph) {
        cachedGraph = graph;
    }

    public static boolean isGraphCached() {
        return cachedGraph != null;
    }

    public static void clearCache() {
        cachedGraph = null;
    }
}
