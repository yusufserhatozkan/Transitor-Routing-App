package Data;

import com.graphhopper.Trip;
//import org.locationtech.jts.triangulate.tri.Tri;

import java.util.ArrayList;

public class TransferTripCoordinates {
    ArrayList<TripCoordinates> legs;
    ArrayList<Integer> transferStopID;
    String transferStopName;
    int timeTaken;

    public TransferTripCoordinates(ArrayList<TripCoordinates> legs, ArrayList<Integer> transferStopID, String transferStopName, int timeTaken) {
        this.legs = legs;
        this.transferStopID = transferStopID;
        this.transferStopName = transferStopName;
        this.timeTaken = timeTaken;
    }

    public ArrayList<TripCoordinates> getLegs() {
        return legs;
    }

    public ArrayList<Integer> getTransferStopID() {
        return transferStopID;
    }

    public String getTransferStopName() {
        return transferStopName;
    }

    public int getTimeTaken() {
        return timeTaken;
    }
}
