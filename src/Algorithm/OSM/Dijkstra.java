package Algorithm.OSM;

import java.util.*;

public class Dijkstra {
    private final Graph graph;
    private final Map<Integer, Double> distances;
    private final Map<Integer, Integer> previousNodes;
    private final PriorityQueue<Node> priorityQueue;

    public Dijkstra(Graph graph) {
        this.graph = graph;
        this.distances = new HashMap<>();
        this.previousNodes = new HashMap<>();
        this.priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(Node::getDistance));
    }

    public double findShortestDistance(Coordinate source, Coordinate target) {
        Integer startNode = findClosestNode(source);
        Integer endNode = findClosestNode(target);
        if (startNode == null || endNode == null) {
            return Double.MAX_VALUE;
        }

        execute(startNode);

        return distances.getOrDefault(endNode, Double.MAX_VALUE);
    }

    private void execute(int startNode) {
        for (Integer node : graph.getCoordinates().keySet()) {
            if (node == startNode) {
                distances.put(node, 0.0);
                priorityQueue.add(new Node(node, 0.0));
            } else {
                distances.put(node, Double.MAX_VALUE);
                priorityQueue.add(new Node(node, Double.MAX_VALUE));
            }
            previousNodes.put(node, null);
        }

        while (!priorityQueue.isEmpty()) {
            Node currentNode = priorityQueue.poll();
            int currentLabel = currentNode.getLabel();

            for (Edge edge : graph.getAdjacencyList().get(currentLabel)) {
                int adjacentNode = edge.getTargetNodeId();
                double edgeWeight = edge.getDistance();

                double newDist = distances.get(currentLabel) + edgeWeight;
                if (newDist < distances.get(adjacentNode)) {
                    distances.put(adjacentNode, newDist);
                    previousNodes.put(adjacentNode, currentLabel);
                    priorityQueue.add(new Node(adjacentNode, newDist));
                }
            }
        }
    }

    private Integer findClosestNode(Coordinate coordinate) {
        double minDistance = Double.MAX_VALUE;
        Integer closestNode = null;

        for (Map.Entry<Integer, Coordinate> entry : graph.getCoordinates().entrySet()) {
            double distance = haversineDistance(coordinate, entry.getValue());
            if (distance < minDistance) {
                minDistance = distance;
                closestNode = entry.getKey();
            }
        }

        return closestNode;
    }

    private double haversineDistance(Coordinate c1, Coordinate c2) {
        double lat1Radians = Math.toRadians(c1.getLatitude());
        double lon1Radians = Math.toRadians(c1.getLongitude());
        double lat2Radians = Math.toRadians(c2.getLatitude());
        double lon2Radians = Math.toRadians(c2.getLongitude());

        double deltaLat = lat2Radians - lat1Radians;
        double deltaLon = lon2Radians - lon1Radians;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Radians) * Math.cos(lat2Radians) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double EARTH_RADIUS_KM = 6371.0;
        return EARTH_RADIUS_KM * c;
    }

    private static class Node {
        private final int label;
        private final double distance;

        public Node(int label, double distance) {
            this.label = label;
            this.distance = distance;
        }

        public int getLabel() {
            return label;
        }

        public double getDistance() {
            return distance;
        }
    }
}
