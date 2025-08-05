package Data;

public class TripDetail {
    private final int tripId;
    private final int timeTaken;
    private final String startStopId;
    private final String endStopId;
    private final String departureStop;
    private final String arrivalStop;
    private String departureTime;
    private String arrivalTime;
    private final String routeID;

    public TripDetail(int tripId, int timeTaken, String startStopId, String endStopId, String departureStop, String arrivalStop, String routeID) {
        this.tripId = tripId;
        this.timeTaken = timeTaken;
        this.startStopId = startStopId;
        this.endStopId = endStopId;
        this.departureStop = departureStop;
        this.arrivalStop = arrivalStop;
        this.routeID = routeID;
    }

    public int getTripId() {
        return tripId;
    }

    public int getTimeTaken() {
        return timeTaken;
    }

    public String getStartStopId() {
        return startStopId;
    }

    public String getEndStopId() {
        return endStopId;
    }

    public String getDepartureStop() {
        return departureStop;
    }

    public String getArrivalStop() {
        return arrivalStop;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public String getRouteID(){return routeID;}

    public String setDepartureTime(String departureTime) {
        return this.departureTime = departureTime;
    }

    public String setArrivalTime(String arrivalTime) {
        return this.arrivalTime = arrivalTime;
    }

    @Override
    public String toString() {
        return "Trip ID: " + tripId +
               ", Time Taken: " + timeTaken +
               ", Start Stop ID: " + startStopId +
               ", End Stop ID: " + endStopId +
               ", Departure Stop: " + departureStop +
               " at " + departureTime +
               ", Arrival Stop: " + arrivalStop +
               " at " + arrivalTime;
    }
}