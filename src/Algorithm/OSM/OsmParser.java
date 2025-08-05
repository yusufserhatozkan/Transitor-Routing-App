package Algorithm.OSM;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.NodeAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OsmParser {

    private static final String PATH_TO_OSM_FILE = "src/resources/Maastricht.osm.pbf";
    private static final String PATH_TO_GRAPHHOPPER_WORKINGDIR = "src/graphhopper_working_directory";

    private GraphHopper hopper;
    private Graph graph;

    public OsmParser() {
        hopper = getHopper();
        graph = createGraph();
    }

    private GraphHopper getHopper() {
        if (hopper == null) {
            hopper = new GraphHopperOSM().forServer();
            hopper.setDataReaderFile(PATH_TO_OSM_FILE);
            hopper.setGraphHopperLocation(PATH_TO_GRAPHHOPPER_WORKINGDIR);
            hopper.setEncodingManager(EncodingManager.create(null,"car,bike,foot"));
            //hopper.setEncodingManager(new EncodingManager("car, bike"));
            hopper.importOrLoad();
            
        }
        return hopper;
    }

    public Graph createGraph() {
        if (graph != null) {
            return graph;
        }

        hopper = getHopper();

        Map<Integer, Coordinate> coordinates = new HashMap<>();
        Map<Integer, List<Edge>> adjacencyList = new HashMap<>();
        try {
            GraphHopperStorage graphHopperStorage = hopper.getGraphHopperStorage();
            NodeAccess nodeAccess = graphHopperStorage.getNodeAccess();

            AllEdgesIterator edgeIterator = graphHopperStorage.getAllEdges();
            while (edgeIterator.next()) {
                int sourceNodeId = edgeIterator.getBaseNode();
                int targetNodeId = edgeIterator.getAdjNode();

                double sourceLat = nodeAccess.getLat(sourceNodeId);
                double sourceLon = nodeAccess.getLon(sourceNodeId);
                Coordinate source = new Coordinate(sourceNodeId, sourceLat, sourceLon);

                double targetLat = nodeAccess.getLat(targetNodeId);
                double targetLon = nodeAccess.getLon(targetNodeId);
                Coordinate target = new Coordinate(targetNodeId, targetLat, targetLon);

                Edge edge = new Edge(targetNodeId, target, edgeIterator.getDistance());
                adjacencyList.computeIfAbsent(sourceNodeId, k -> new ArrayList<>()).add(edge);

                Edge reverseEdge = new Edge(sourceNodeId, source, edgeIterator.getDistance());
                adjacencyList.computeIfAbsent(targetNodeId, k -> new ArrayList<>()).add(reverseEdge);

                coordinates.putIfAbsent(sourceNodeId, source);
                coordinates.putIfAbsent(targetNodeId, target);
            }
        } finally {
            hopper.close();
        }

        return new Graph(coordinates, adjacencyList);
    }

}
