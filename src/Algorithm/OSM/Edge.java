package Algorithm.OSM;

public class Edge {
    private final int targetNodeId;
    private final Coordinate target;
    private final double distance;

    public Edge(int targetNodeId, Coordinate target, double distance) {
        this.targetNodeId = targetNodeId;
        this.target = target;
        this.distance = distance;
    }

    public int getTargetNodeId() {
        return targetNodeId;
    }

    public Coordinate getTarget() {
        return target;
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "targetNodeId=" + targetNodeId +
                ", target=" + target +
                ", distance=" + distance +
                '}';
    }
}

