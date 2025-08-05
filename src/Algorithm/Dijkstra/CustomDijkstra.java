package Algorithm.Dijkstra;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Data.DataGetter;
import Algorithm.Distance.DistanceCalculator;

public class CustomDijkstra {
    private final Graph<String, CustomEdge> graph;
    private final Map<String, Double> distances;
    private final Map<String, String> previousNodes;
    private final PriorityQueue<Node> priorityQueue;
    private static String lastTimeParameter = "";

    // Constructor
    public CustomDijkstra(Graph<String, CustomEdge> graph) {
        this.graph = graph;
        this.distances = new HashMap<>();
        this.previousNodes = new HashMap<>();
        this.priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(Node::getDistance));
    }

    // Main Dijkstra method
    public static DijkstraResult mainCustomDijkstra(String originPostcode, String destinationPostcode, double radius, String time) {
        Instant startTime = Instant.now();

        boolean rebuildGraph = shouldRebuildGraph(time);
        DataGetter dataGetter = new DataGetter();
        DistanceCalculator distanceCalculator = new DistanceCalculator();        

        Graph<String, CustomEdge> graph;

        if (!rebuildGraph && GraphCache.isGraphCached()) {
            graph = GraphCache.getCachedGraph();
            removeSuperNodes(graph);
        } else {
            graph = new DefaultDirectedWeightedGraph<>(CustomEdge.class);
            initializeGraph(graph, dataGetter, time);
        }

        addSuperNodes(graph, originPostcode, destinationPostcode, dataGetter, distanceCalculator);

        CustomDijkstra dijkstra = new CustomDijkstra(graph);
        dijkstra.execute(originPostcode, time);
        List<String> path = dijkstra.getPath(destinationPostcode);
        double totalTravelTime = dijkstra.getTotalTravelTime(destinationPostcode);

        Instant endTime = Instant.now();
        Duration timeElapsed = Duration.between(startTime, endTime);

        printResults(originPostcode, destinationPostcode, graph, path, totalTravelTime, timeElapsed);

        lastTimeParameter = time;

        return new DijkstraResult(path, graph, totalTravelTime);
    }

    // Initialize the graph with bus stops and routes
    private static void initializeGraph(Graph<String, CustomEdge> graph, DataGetter dataGetter, String time) {
        Map<String, String[]> busStops = dataGetter.getBusStopCoordinates();
        busStops.keySet().forEach(graph::addVertex);
    
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Callable<Void>> tasks = new ArrayList<>();
        List<String> stopIds = new ArrayList<>(busStops.keySet());
    
        for (String fromStop : stopIds) {
            tasks.add(() -> {
                List<Map<String, Object>> routeWeights = RouteWeights.getRouteWeights(Collections.singletonList(fromStop), time);
                for (Map<String, Object> weightData : routeWeights) {
                    String toStop = (String) weightData.get("end_stop_id");
                    int tripId = (int) weightData.get("trip_id");
                    double totalTime = (double) weightData.get("total_time");
    
                    if (graph.containsVertex(fromStop) && graph.containsVertex(toStop)) {
                        graph.addEdge(fromStop, toStop, new CustomEdge(Collections.singletonMap(tripId, totalTime)));
                    }
                }
                return null;
            });
        }
    
        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    
        addWalkingPaths(graph, busStops);
        GraphCache.cacheGraph((DefaultDirectedWeightedGraph<String, CustomEdge>) graph);
    }

    // Add super nodes to the graph
    private static void addSuperNodes(Graph<String, CustomEdge> graph, String startNode, String endNode, DataGetter dataGetter, DistanceCalculator distanceCalculator) {
        double[] originCoordinates = dataGetter.getLocationFromApiReader(startNode);
        double[] destinationCoordinates = dataGetter.getLocationFromApiReader(endNode);
    
        if (originCoordinates == null || destinationCoordinates == null) {
            throw new IllegalArgumentException("Invalid postal code: " + (originCoordinates == null ? startNode : endNode));
        }
    
        double startLat = Double.parseDouble(String.valueOf(originCoordinates[0]).replace(',', '.'));
        double startLon = Double.parseDouble(String.valueOf(originCoordinates[1]).replace(',', '.'));
        double endLat = Double.parseDouble(String.valueOf(destinationCoordinates[0]).replace(',', '.'));
        double endLon = Double.parseDouble(String.valueOf(destinationCoordinates[1]).replace(',', '.'));
    
        graph.addVertex(startNode);
        graph.addVertex(endNode);
    
        connectSuperNodes(graph, startNode, endNode, startLat, startLon, endLat, endLon, dataGetter, distanceCalculator);
    }
    
    // Connect super nodes to the graph
    private static void connectSuperNodes(Graph<String, CustomEdge> graph, String startNode, String endNode, double startLat, double startLon, double endLat, double endLon, DataGetter dataGetter, DistanceCalculator distanceCalculator) {
        Map<String, String[]> busStops = dataGetter.getBusStopCoordinates();
        int counter = 0;
    
        for (String busStop : busStops.keySet()) {
            if (!busStop.equals(startNode) && !busStop.equals(endNode)) {
                String[] latLon = busStops.get(busStop);
                if (latLon == null) {
                    System.err.println("Missing data for bus stop: " + busStop);
                    continue;
                }
    
                double lat = Double.parseDouble(latLon[0].replace(',', '.'));
                double lon = Double.parseDouble(latLon[1].replace(',', '.'));
    
                double startDistance = distanceCalculator.calculateWalkingTime(distanceCalculator.calculateDistance(startLat, startLon, lat, lon));
                double endDistance = distanceCalculator.calculateWalkingTime(distanceCalculator.calculateDistance(endLat, endLon, lat, lon));
    
                graph.addEdge(startNode, busStop, new CustomEdge(Collections.singletonMap(counter++, startDistance)));
                graph.addEdge(busStop, endNode, new CustomEdge(Collections.singletonMap(counter++, endDistance)));
            }
        }
    }
    
    // Method to add walking paths between bus stops
    private static void addWalkingPaths(Graph<String, CustomEdge> graph, Map<String, String[]> busStops) {
        DistanceCalculator distanceCalculator = new DistanceCalculator();
        int counter = 0;
    
        for (String fromStop : busStops.keySet()) {
            for (String toStop : busStops.keySet()) {
                if (!fromStop.equals(toStop)) {
                    String[] fromCoords = busStops.get(fromStop);
                    String[] toCoords = busStops.get(toStop);
    
                    if (fromCoords != null && toCoords != null) {
                        double fromLat = Double.parseDouble(fromCoords[0].replace(',', '.'));
                        double fromLon = Double.parseDouble(fromCoords[1].replace(',', '.'));
                        double toLat = Double.parseDouble(toCoords[0].replace(',', '.'));
                        double toLon = Double.parseDouble(toCoords[1].replace(',', '.'));
    
                        double walkingDistance = distanceCalculator.calculateDistance(fromLat, fromLon, toLat, toLon);
                        double walkingTime = distanceCalculator.calculateWalkingTime(walkingDistance);
    
                        // Only add a walking path if it's within a reasonable walking distance, e.g., 1 km
                        if (walkingDistance <= 1000) {
                            graph.addEdge(fromStop, toStop, new CustomEdge(Collections.singletonMap(counter++, walkingTime)));
                        }
                    }
                }
            }
        }
    }

    // Remove old super nodes from the graph
    private static void removeSuperNodes(Graph<String, CustomEdge> graph) {
        List<String> superNodes = new ArrayList<>();
        for (String vertex : graph.vertexSet()) {
            if (vertex.startsWith("superNode")) {
                superNodes.add(vertex);
            }
        }
        for (String superNode : superNodes) {
            graph.removeVertex(superNode);
        }
    }

    // Determine whether to rebuild the graph
    private static boolean shouldRebuildGraph(String currentTime) {
        return !GraphCache.isGraphCached() || !currentTime.equals(lastTimeParameter);
    }

    // Execute Dijkstra's algorithm
    public void execute(String startNode, String startTime) {
        initializeDistancesAndQueue(startNode, startTime);

        while (!priorityQueue.isEmpty()) {
            Node currentNode = priorityQueue.poll();
            String currentLabel = currentNode.getLabel();
            double currentArrivalTime = currentNode.getArrivalTime();

            for (CustomEdge edge : graph.outgoingEdgesOf(currentLabel)) {
                String adjacentNode = graph.getEdgeTarget(edge);
                double edgeWeight = edge.getIdWeightMap().values().stream().mapToDouble(Double::doubleValue).sum();

                double newArrivalTime = currentArrivalTime + edgeWeight;
                double newDist = distances.get(currentLabel) + edgeWeight;

                if (newDist < distances.get(adjacentNode)) {
                    distances.put(adjacentNode, newDist);
                    previousNodes.put(adjacentNode, currentLabel);
                    priorityQueue.add(new Node(adjacentNode, newDist, newArrivalTime));
                }
            }
        }
    }

    // Initialize distances and priority queue
    private void initializeDistancesAndQueue(String startNode, String startTime) {
        double startArrivalTime = convertTimeToMinutes(startTime);

        for (String node : graph.vertexSet()) {
            if (node.equals(startNode)) {
                distances.put(node, 0.0);
                priorityQueue.add(new Node(node, 0.0, startArrivalTime));
            } else {
                distances.put(node, Double.MAX_VALUE);
                priorityQueue.add(new Node(node, Double.MAX_VALUE, Double.MAX_VALUE));
            }
            previousNodes.put(node, null);
        }
    }

    // Helper method to convert time string to minutes since midnight
    private int convertTimeToMinutes(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return hours * 60 + minutes;
    }

    // Get the path from the start node to the end node
    public List<String> getPath(String endNode) {
        List<String> path = new LinkedList<>();
        for (String at = endNode; at != null; at = previousNodes.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }

    // Get the total travel time to the end node
    public double getTotalTravelTime(String endNode) {
        return distances.getOrDefault(endNode, Double.MAX_VALUE);
    }

    // Print the results
    private static void printResults(String startNode, String endNode, Graph<String, CustomEdge> graph, List<String> path, double totalTravelTime, Duration timeElapsed) {
        System.out.println("Shortest path from " + startNode + " to " + endNode + ":");
        for (int i = 0; i < path.size() - 1; i++) {
            String node = path.get(i);
            String nextNode = path.get(i + 1);
            CustomEdge edge = graph.getEdge(node, nextNode);
            System.out.println(node + " via edge " + edge + " to " + nextNode);
            System.out.println(edge.getIdWeightMap().keySet());
        }
        System.out.println(endNode);
        System.out.println("Total travel time: " + totalTravelTime + " minutes");
        System.out.println("Time taken for execution: " + timeElapsed.toMillis() + " milliseconds");
    }

    // Node class representing a node in the graph
    private static class Node {
        private final String label;
        private final double distance;
        private final double arrivalTime;

        public Node(String label, double distance, double arrivalTime) {
            this.label = label;
            this.distance = distance;
            this.arrivalTime = arrivalTime;
        }

        public String getLabel() {
            return label;
        }

        public double getDistance() {
            return distance;
        }

        public double getArrivalTime() {
            return arrivalTime;
        }
    }
}




//------------------------------------------------OSM Trying--------------------------------------------------------------------------------






// package Algorithm.Dijkstra;

// import org.jgrapht.Graph;
// import org.jgrapht.graph.DefaultDirectedWeightedGraph;

// import java.time.Duration;
// import java.time.Instant;
// import java.util.*;
// import java.util.concurrent.Callable;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;

// import Data.DataGetter;
// import Algorithm.Distance.*;
// import Algorithm.OSM.OsmParser;

// public class CustomDijkstra {
//     private final Graph<String, CustomEdge> graph;
//     private final Map<String, Double> distances;
//     private final Map<String, String> previousNodes;
//     private final PriorityQueue<Node> priorityQueue;

//     private static String lastTimeParameter = "";
//     private static int counterDataCleaning = 0;

//     // Constructor
//     public CustomDijkstra(Graph<String, CustomEdge> graph) {
//         this.graph = graph;
//         this.distances = new HashMap<>();
//         this.previousNodes = new HashMap<>();
//         this.priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(Node::getDistance));
//     }

//     // Main Dijkstra method
//     public static DijkstraResult mainCustomDijkstra(String originPostcode, String destinationPostcode, double radius, String time) {
//         Instant startTime = Instant.now();

//         boolean rebuildGraph = shouldRebuildGraph(time);
//         DataGetter dataGetter = new DataGetter();
//         DistanceCalculatorOSM distanceCalculatorOSM = new DistanceCalculatorOSM(new OsmParser());        

//         Graph<String, CustomEdge> graph;

//         if (!rebuildGraph && GraphCache.isGraphCached()) {
//             graph = GraphCache.getCachedGraph();
//             removeSuperNodes(graph);
//         } else {
//             graph = new DefaultDirectedWeightedGraph<>(CustomEdge.class);
//             initializeGraph(graph, dataGetter, time);
//         }

//         addSuperNodes(graph, originPostcode, destinationPostcode, dataGetter, distanceCalculatorOSM);

//         CustomDijkstra dijkstra = new CustomDijkstra(graph);
//         dijkstra.execute(originPostcode);
//         List<String> path = dijkstra.getPath(destinationPostcode);
//         double totalTravelTime = dijkstra.getTotalTravelTime(destinationPostcode);

//         Instant endTime = Instant.now();
//         Duration timeElapsed = Duration.between(startTime, endTime);

//         printResults(originPostcode, destinationPostcode, graph, path, totalTravelTime, timeElapsed);

//         lastTimeParameter = time;

//         return new DijkstraResult(path, graph, totalTravelTime);
//     }

//     // Initialize the graph with bus stops and routes
//     private static void initializeGraph(Graph<String, CustomEdge> graph, DataGetter dataGetter, String time) {
//         Map<String, String[]> busStops = dataGetter.getAllBusStops();
//         busStops.keySet().forEach(graph::addVertex);

//         ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//         List<Callable<Void>> tasks = new ArrayList<>();
//         List<String> stopIds = new ArrayList<>(busStops.keySet());

//         for (String fromStop : stopIds) {
//             tasks.add(() -> {
//                 List<Map<String, Object>> routeWeights = RouteWeights.getRouteWeights(Collections.singletonList(fromStop), time);
//                 for (Map<String, Object> weightData : routeWeights) {
//                     String toStop = (String) weightData.get("end_stop_id");
//                     int tripId = (int) weightData.get("trip_id");
//                     double travelTime = (double) weightData.get("travel_time");

//                     if (graph.containsVertex(fromStop) && graph.containsVertex(toStop)) {
//                         graph.addEdge(fromStop, toStop, new CustomEdge(Collections.singletonMap(tripId, travelTime)));
//                     }
//                 }
//                 return null;
//             });
//         }

//         try {
//             executorService.invokeAll(tasks);
//         } catch (InterruptedException e) {
//             e.printStackTrace();
//         } finally {
//             executorService.shutdown();
//         }

//         GraphCache.cacheGraph((DefaultDirectedWeightedGraph<String, CustomEdge>) graph);
//     }

//     // Add super nodes to the graph
//     private static void addSuperNodes(Graph<String, CustomEdge> graph, String startNode, String endNode, DataGetter dataGetter, DistanceCalculatorOSM distanceCalculatorOSM) {
//         double[] originCoordinates = dataGetter.getLocationFromApiReader(startNode);
//         double[] destinationCoordinates = dataGetter.getLocationFromApiReader(endNode);

//         if (originCoordinates == null || destinationCoordinates == null) {
//             throw new IllegalArgumentException("Invalid postal code: " + (originCoordinates == null ? startNode : endNode));
//         }

//         double startLat = Double.parseDouble(String.valueOf(originCoordinates[0]).replace(',', '.'));
//         double startLon = Double.parseDouble(String.valueOf(originCoordinates[1]).replace(',', '.'));
//         double endLat = Double.parseDouble(String.valueOf(destinationCoordinates[0]).replace(',', '.'));
//         double endLon = Double.parseDouble(String.valueOf(destinationCoordinates[1]).replace(',', '.'));

//         graph.addVertex(startNode);
//         graph.addVertex(endNode);

//         connectSuperNodes(graph, startNode, endNode, startLat, startLon, endLat, endLon, dataGetter, distanceCalculatorOSM);
//     }

//     // Connect super nodes to the graph
//     private static void connectSuperNodes(Graph<String, CustomEdge> graph, String startNode, String endNode, double startLat, double startLon, double endLat, double endLon, DataGetter dataGetter, DistanceCalculatorOSM distanceCalculatorOSM) {
//         Map<String, String[]> busStops = dataGetter.getAllBusStops();
//         int counter = 0;

//         for (String busStop : graph.vertexSet()) {
//             if (!busStop.equals(startNode) && !busStop.equals(endNode)) {
//                 String[] latLon = busStops.get(busStop);
//                 if (latLon == null) {
//                     System.err.println("Missing data for bus stop: " + busStop);
//                     continue;
//                 }

//                 double lat = Double.parseDouble(latLon[0].replace(',', '.'));
//                 double lon = Double.parseDouble(latLon[1].replace(',', '.'));

//                 double startDistance = distanceCalculatorOSM.calculateWalkingTime(distanceCalculatorOSM.calculateDistance(startLat, startLon, lat, lon));
//                 double endDistance = distanceCalculatorOSM.calculateWalkingTime(distanceCalculatorOSM.calculateDistance(endLat, endLon, lat, lon));

//                 graph.addEdge(startNode, busStop, new CustomEdge(Collections.singletonMap(counter++, startDistance)));
//                 graph.addEdge(busStop, endNode, new CustomEdge(Collections.singletonMap(counter++, endDistance)));
//             }
//         }
//     }

//     // Remove old super nodes from the graph
//     private static void removeSuperNodes(Graph<String, CustomEdge> graph) {
//         List<String> superNodes = new ArrayList<>();
//         for (String vertex : graph.vertexSet()) {
//             if (vertex.startsWith("superNode")) {
//                 superNodes.add(vertex);
//             }
//         }
//         for (String superNode : superNodes) {
//             graph.removeVertex(superNode);
//         }
//     }

//     // Determine whether to rebuild the graph
//     private static boolean shouldRebuildGraph(String currentTime) {
//         return !GraphCache.isGraphCached() || !currentTime.equals(lastTimeParameter);
//     }

//     // Execute Dijkstra's algorithm
//     public void execute(String startNode) {
//         initializeDistancesAndQueue(startNode);

//         while (!priorityQueue.isEmpty()) {
//             Node currentNode = priorityQueue.poll();
//             String currentLabel = currentNode.getLabel();

//             for (CustomEdge edge : graph.outgoingEdgesOf(currentLabel)) {
//                 String adjacentNode = graph.getEdgeTarget(edge);
//                 double edgeWeight = edge.getIdWeightMap().values().stream().mapToDouble(Double::doubleValue).sum();

//                 double newDist = distances.get(currentLabel) + edgeWeight;
//                 if (newDist < distances.get(adjacentNode)) {
//                     distances.put(adjacentNode, newDist);
//                     previousNodes.put(adjacentNode, currentLabel);
//                     priorityQueue.add(new Node(adjacentNode, newDist));
//                 }
//             }
//         }
//     }

//     // Initialize distances and priority queue
//     private void initializeDistancesAndQueue(String startNode) {
//         for (String node : graph.vertexSet()) {
//             if (node.equals(startNode)) {
//                 distances.put(node, 0.0);
//                 priorityQueue.add(new Node(node, 0.0));
//             } else {
//                 distances.put(node, Double.MAX_VALUE);
//                 priorityQueue.add(new Node(node, Double.MAX_VALUE));
//             }
//             previousNodes.put(node, null);
//         }
//     }

//     // Get the path from the start node to the end node
//     public List<String> getPath(String endNode) {
//         List<String> path = new LinkedList<>();
//         for (String at = endNode; at != null; at = previousNodes.get(at)) {
//             path.add(at);
//         }
//         Collections.reverse(path);
//         return path;
//     }

//     // Get the total travel time to the end node
//     public double getTotalTravelTime(String endNode) {
//         return distances.getOrDefault(endNode, Double.MAX_VALUE);
//     }

//     // Print the results
//     private static void printResults(String startNode, String endNode, Graph<String, CustomEdge> graph, List<String> path, double totalTravelTime, Duration timeElapsed) {
//         System.out.println("Shortest path from " + startNode + " to " + endNode + ":");
//         for (int i = 0; i < path.size() - 1; i++) {
//             String node = path.get(i);
//             String nextNode = path.get(i + 1);
//             CustomEdge edge = graph.getEdge(node, nextNode);
//             System.out.println(node + " via edge " + edge + " to " + nextNode);
//             System.out.println(edge.getIdWeightMap().keySet());
//         }
//         System.out.println(endNode);
//         System.out.println("Total travel time: " + totalTravelTime + " minutes");
//         System.out.println("Time taken for execution: " + timeElapsed.toMillis() + " milliseconds");
//     }

//     // Node class representing a node in the graph
//     private static class Node {
//         private final String label;
//         private final double distance;

//         public Node(String label, double distance) {
//             this.label = label;
//             this.distance = distance;
//         }

//         public String getLabel() {
//             return label;
//         }

//         public double getDistance() {
//             return distance;
//         }
//     }
// }
