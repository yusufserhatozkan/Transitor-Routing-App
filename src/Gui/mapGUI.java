package Gui;

import Algorithm.Dijkstra.CustomDijkstra;
import Algorithm.Dijkstra.CustomEdge;
import Algorithm.Dijkstra.DijkstraResult;
import Data.DataGetter;
import Data.TripCoordinates;
import Data.TransferTripCoordinates;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import Algorithm.Distance.DistanceCalculator;
import Data.AccessibilityScoreCalculator;
import org.jgrapht.Graph;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class mapGUI extends Application implements PostcodeInputListener {
    private static final Logger logger = Logger.getLogger(mapGUI.class.getName());
    private final PostcodeInputPane locationInput = new PostcodeInputPane();
    private final MapLoader mapLoader = new MapLoader();
    private final DistanceCalculator distanceCalculator = new DistanceCalculator();
    private final AccessibilityScoreCalculator accessibilityScoreCalculator = new AccessibilityScoreCalculator();
    private double radius = 0.6;
    private static final DataGetter dataGetter = new DataGetter();
    private String methodChosen = "";

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(mapLoader);
        VBox leftContainer = new VBox(10);
        leftContainer.setPadding(new Insets(10));
        leftContainer.getChildren().add(locationInput);
        leftContainer.setAlignment(Pos.TOP_CENTER);
        locationInput.setAlignment(Pos.CENTER);

        Label radiusLabel = new Label("Radius: 600 m");
        Slider radiusSlider = new Slider(0.2, 2.0, 0.6);
        radiusSlider.setShowTickMarks(true);
        radiusSlider.setShowTickLabels(true);
        radiusSlider.setMajorTickUnit(0.2);
        radiusSlider.setMinorTickCount(4);
        radiusSlider.setBlockIncrement(0.1);
        radiusSlider.setSnapToTicks(true);

        ChoiceBox<String> choiceBox = new ChoiceBox<>();
        choiceBox.getItems().add("Greedy Algorithm");
        choiceBox.getItems().add("Dijkstras Algorithm");
        leftContainer.getChildren().add(choiceBox);

        choiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals("Greedy Algorithm")) {
                methodChosen = "Greedy Algorithm";
            } else if (newValue.equals("Dijkstras Algorithm")) {
                methodChosen = "Dijkstras Algorithm";
            }
        });

        radiusSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            radius = newValue.doubleValue();
            radiusLabel.setText(String.format("Radius: %.0f m", radius * 1000));
        });

        VBox sliderContainer = new VBox(5, radiusLabel, radiusSlider);
        sliderContainer.setAlignment(Pos.CENTER);

        leftContainer.getChildren().add(sliderContainer);

        Button calculateButton = getCalculateButton();
        leftContainer.getChildren().add(calculateButton);

        borderPane.setLeft(leftContainer);

        Scene scene = new Scene(borderPane);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private @NotNull Button getCalculateButton() {
        Button calculateButton = new Button("Calculate Distance");
        calculateButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 20px;");
        calculateButton.setOnAction(event -> {
            String originPostcode = locationInput.getOriginPostcodes();
            String destinationPostcode = locationInput.getDestinationPostcodes();
            String selectedTime = locationInput.getSelectedTime();
            if (methodChosen.equals("Greedy Algorithm")) {
                onPostcodesEntered(originPostcode, destinationPostcode, radius, selectedTime);
            } else if (methodChosen.equals("Dijkstras Algorithm")) {
               DijkstraResult dijkstraResult = CustomDijkstra.mainCustomDijkstra(originPostcode, destinationPostcode, radius, selectedTime);
               drawDijkstraRouteBasic(dijkstraResult, originPostcode, destinationPostcode);
            }
            double originAccessibilityScore = accessibilityScoreCalculator.calculateAccessibility(originPostcode, radius);
            double destinationAccessibilityScore = accessibilityScoreCalculator.calculateAccessibility(destinationPostcode, radius);
            locationInput.setOriginAccessibilityLabel(String.valueOf(originAccessibilityScore));
            locationInput.setDestinationAccessibilityLabel(String.valueOf(destinationAccessibilityScore));
        });
        return calculateButton;
    }

    @Override
    public void onPostcodesEntered(String originPostcode, String destinationPostcode, double radius, String time) {
        this.radius = radius;
        new Thread(() -> {
            try {
                double[] originCoordinates = dataGetter.getLocationFromApiReader(originPostcode);
                double[] destinationCoordinates = dataGetter.getLocationFromApiReader(destinationPostcode);

                if (originCoordinates != null && destinationCoordinates != null) {
                    Platform.runLater(() -> {
                        locationInput.clearError();
                        logger.info(String.format("Origin Coordinates: (%.6f, %.6f)", originCoordinates[0], originCoordinates[1]));
                        logger.info(String.format("Destination Coordinates: (%.6f, %.6f)", destinationCoordinates[0], destinationCoordinates[1]));
                        logger.info("Radius used in onPostcodesEntered: " + radius + " km");
                        addMarkersAndPathToMap(originCoordinates[0], originCoordinates[1], destinationCoordinates[0], destinationCoordinates[1], radius, time);
                    });


                } else {
                    Platform.runLater(() -> locationInput.displayError("Invalid postcodes. Please try again"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.severe("Error retrieving coordinates: " + e.getMessage());
                Platform.runLater(() -> locationInput.displayError("Error retrieving coordinates. Please try again"));
            }
        }).start();
    }

    private double[] findClosestShapePoint(double lat, double lon, List<double[]> shapePoints) {
        double minDistance = Double.MAX_VALUE;
        double[] closestPoint = null;

        for (double[] point : shapePoints) {
            double distance = distanceCalculator.calculateDistance(lat, lon, point[0], point[1]);
            if (distance < minDistance) {
                minDistance = distance;
                closestPoint = point;
            }
        }

        if (closestPoint == null) {
            logger.severe("No closest point found for lat: " + lat + ", lon: " + lon);
        } else {
            logger.info("Closest point found: " + closestPoint[0] + ", " + closestPoint[1]);
        }

        return closestPoint;
    }

    // Method to draw the relevant shape points
    private void drawRelevantShapePoints(double startLat, double startLon, double endLat, double endLon, List<double[]> shapePoints, String color) {
        double[] closestStartPoint = findClosestShapePoint(startLat, startLon, shapePoints);
        double[] closestEndPoint = findClosestShapePoint(endLat, endLon, shapePoints);

        if (closestStartPoint == null || closestEndPoint == null) {
            logger.severe("Closest start or end point is null. Aborting draw operation.");
            return; // Early exit if any point is null
        }

        boolean startDrawing = false;
        double previousLat = closestStartPoint[0];
        double previousLon = closestStartPoint[1];

        for (double[] point : shapePoints) {
            if (point[0] == closestStartPoint[0] && point[1] == closestStartPoint[1]) {
                startDrawing = true;
            }
            if (startDrawing) {
                mapLoader.drawLine(previousLat, previousLon, point[0], point[1], color, false);
                previousLat = point[0];
                previousLon = point[1];
            }
            if (point[0] == closestEndPoint[0] && point[1] == closestEndPoint[1]) {
                break;
            }
        }
    }

    private void addMarkersAndPathToMap(double originLat, double originLong, double destinationLat, double destinationLong, double radius, String time) {
        double distance = distanceCalculator.calculateDistance(originLat, originLong, destinationLat, destinationLong);
        int walkingTime = distanceCalculator.calculateWalkingTime(distance);
        int cyclingTime = distanceCalculator.calculateCyclingTime(distance);

        new Thread(() -> {
            try {
                TripCoordinates tripCoordinates = dataGetter.findFastestDirectRouteInfo(originLat, originLong, destinationLat, destinationLong, radius, time);

                if (tripCoordinates != null) {
                    drawDirectRoute(tripCoordinates, originLat, originLong, destinationLat, destinationLong);
                    return;
                }
                TransferTripCoordinates transferTripCoordinates = dataGetter.getTransferTripIDs(originLat, originLong, destinationLat, destinationLong, radius, time);
                if (transferTripCoordinates != null){
                    drawTransferRoute(transferTripCoordinates, originLat, originLong, destinationLat, destinationLong);
                }
                else{
                    Platform.runLater(() -> {
                        mapLoader.clearMap();
                        locationInput.displayError("No trip found for the given coordinates.");
                        mapLoader.addMapMarker(originLat, originLong, "Origin Postcode", "green");
                        mapLoader.addMapMarker(destinationLat, destinationLong, "Destination Postcode", "orange");
                        mapLoader.drawLine(originLat, originLong, destinationLat, destinationLong, "black", false);
                        locationInput.setDistanceLabel(String.valueOf(Math.round(distance * 100.0) / 100.0));
                        locationInput.setWalkingTimeLabel("Walking Time: " + walkingTime + " minutes");
                        locationInput.setCyclingTimeLabel("Cycling Time: " + cyclingTime + " minutes");
                        locationInput.setBusTripTimeLabel("No bus trip found");
                        mapLoader.setCenter((originLat + destinationLat) / 2, (originLong + destinationLong) / 2, 13);
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> locationInput.displayError("Database error occurred. Please try again."));
            }
        }).start();
    }

    private void drawDirectRoute(TripCoordinates tripCoordinates, double originLat, double originLong, double destinationLat, double destinationLong) {
        int busTripTime = tripCoordinates.getBusTripTime() / 60;
        double distance = distanceCalculator.calculateDistance(originLat, originLong, destinationLat, destinationLong);
        int walkingTime = distanceCalculator.calculateWalkingTime(distance);
        int cyclingTime = distanceCalculator.calculateCyclingTime(distance);

        Platform.runLater(() -> {
            mapLoader.clearMap();
            locationInput.displayInfo("Your bus leaves the start stop at: " + tripCoordinates.getDepartureTime());
            List<double[]> routeCoordinates = new ArrayList<>();

            mapLoader.addMapMarker(originLat, originLong, "Origin Postcode", "green");
            routeCoordinates.add(new double[]{originLat, originLong});

            mapLoader.addMapMarker(destinationLat, destinationLong, "Destination Postcode", "orange");
            routeCoordinates.add(new double[]{destinationLat, destinationLong});

            mapLoader.addMapMarker(tripCoordinates.getStartStopLat(), tripCoordinates.getStartStopLon(), tripCoordinates.getStartStopName(), "red");
            routeCoordinates.add(new double[]{tripCoordinates.getStartStopLat(), tripCoordinates.getStartStopLon()});

            mapLoader.drawLine(originLat, originLong, tripCoordinates.getStartStopLat(), tripCoordinates.getStartStopLon(), "green", true);

            mapLoader.addMapMarker(tripCoordinates.getEndStopLat(), tripCoordinates.getEndStopLon(), tripCoordinates.getEndStopName(), "red");
            routeCoordinates.add(new double[]{tripCoordinates.getEndStopLat(), tripCoordinates.getEndStopLon()});

            mapLoader.drawLine(destinationLat, destinationLong, tripCoordinates.getEndStopLat(), tripCoordinates.getEndStopLon(), "orange", true);

            StringBuilder intermediateStops = new StringBuilder("Stops:\n");
            try {
                List<double[]> shapePoints = dataGetter.getShapePoints(tripCoordinates.getTripId());
                drawRelevantShapePoints(tripCoordinates.getStartStopLat(), tripCoordinates.getStartStopLon(), tripCoordinates.getEndStopLat(), tripCoordinates.getEndStopLon(), shapePoints, "grey");

                for (String[] stopDetails : tripCoordinates.getIntermediateStopDetails()) {
                    double lat = Double.parseDouble(stopDetails[1]);
                    double lon = Double.parseDouble(stopDetails[2]);
                    String stopName = stopDetails[0];
                    mapLoader.addMapMarker(lat, lon, stopName, "black");
                    intermediateStops.append(stopName).append("\n");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                locationInput.displayError("Error fetching shape points.");
            }

            setMapCenter(routeCoordinates);

            locationInput.setWalkingTimeLabel("Walking Time: " + walkingTime + " minutes");
            locationInput.setCyclingTimeLabel("Cycling Time: " + cyclingTime + " minutes");
            locationInput.setBusTripTimeLabel("Bus Trip Time: " + busTripTime + " minutes");
            locationInput.updateRouteDetails("Bus number: " + dataGetter.getBusNumber(tripCoordinates.getRouteID()) + "\n" + intermediateStops);
        });
    }

    public void drawDijkstraRouteBasic(DijkstraResult dijkstraResult, String originPostcode, String destinationPostcode) {
        List<String> path = dijkstraResult.getPath();
        Graph<String, CustomEdge> graph = dijkstraResult.getGraph();
        double[] originCoordinates = dataGetter.getLocationFromApiReader(originPostcode);
        double[] destinationCoordinates = dataGetter.getLocationFromApiReader(destinationPostcode);
        double distance = distanceCalculator.calculateDistance(originCoordinates[0], originCoordinates[1], destinationCoordinates[0], destinationCoordinates[1]);
        int walkingTime = distanceCalculator.calculateWalkingTime(distance);
        int cyclingTime = distanceCalculator.calculateCyclingTime(distance);

        Platform.runLater(() -> {
            mapLoader.clearMap();
            StringBuilder routeDetailsBuilder = new StringBuilder();

            for (int i = 0; i < path.size() - 1; i++) {
                String currentStop = path.get(i);
                double[] pointCoords;
                boolean isWalkingSegment = false;

                if (isPostalCode(currentStop)) {
                    pointCoords = dataGetter.getLocationFromApiReader(currentStop);
                    isWalkingSegment = true;
                } else {
                    pointCoords = dataGetter.getStopCoordinates(Integer.parseInt(currentStop));
                }

                if (pointCoords == null) {
                    locationInput.displayError("Something has gone wrong. Please try different postal codes.");
                    return;
                }

                mapLoader.addMapMarker(pointCoords[0], pointCoords[1], getStopName(currentStop), "black");
                double previousLat = pointCoords[0];
                double previousLon = pointCoords[1];

                String nextStop = path.get(i + 1);
                if (isPostalCode(nextStop)) {
                    pointCoords = dataGetter.getLocationFromApiReader(nextStop);
                    isWalkingSegment = true;
                } else {
                    pointCoords = dataGetter.getStopCoordinates(Integer.parseInt(nextStop));
                }

                if (pointCoords == null) {
                    locationInput.displayError("Something has gone wrong. Please try different postal codes.");
                    return;
                }

                int currentTripID = graph.getEdge(currentStop, nextStop).getIdWeightMap().keySet().iterator().next();

                mapLoader.addMapMarker(pointCoords[0], pointCoords[1], getStopName(nextStop), "black");
                mapLoader.drawLine(previousLat, previousLon, pointCoords[0], pointCoords[1], "black", isWalkingSegment);

                try {
                    List<double[]> shapePoints = dataGetter.getShapePoints(currentTripID);
                    drawRelevantShapePoints(pointCoords[0], pointCoords[1], pointCoords[0], pointCoords[1], shapePoints, "grey"); // Bus route color
                } catch (SQLException e) {
                    e.printStackTrace();
                    locationInput.displayError("Error fetching shape points.");
                }

                if (!isWalkingSegment) {
                    routeDetailsBuilder.append("Take bus ")
                            .append(dataGetter.getTripName(currentTripID))
                            .append(" from ")
                            .append(dataGetter.getStopName(currentStop))
                            .append(" to ")
                            .append(dataGetter.getStopName(nextStop))
                            .append("\n");
                } else {
                    routeDetailsBuilder.append("Walk from ")
                            .append(dataGetter.getStopName(currentStop))
                            .append(" to ")
                            .append(dataGetter.getStopName(nextStop))
                            .append("\n");
                }
            }
            int totalTravelTime = (int) dijkstraResult.getTotalTravelTime();
            locationInput.setDistanceLabel("Distance: " + distance + " km");
            locationInput.setWalkingTimeLabel("Walking Time: " + walkingTime + " minutes");
            locationInput.setCyclingTimeLabel("Cycling Time: " + cyclingTime + " minutes");
            locationInput.setBusTripTimeLabel("Bus Trip Time: " + totalTravelTime + " minutes");

            locationInput.updateRouteDetails(routeDetailsBuilder.toString());
        });
    }

    private boolean isPostalCode(String input) {
        // Assuming postal codes follow a specific pattern like having letters and digits
        return input.matches(".*[a-zA-Z].*"); // Checks if the input contains any letters
    }

    // Helper method to get stop name considering if it's a postal code or bus stop ID
    private String getStopName(String stop) {
        if (isPostalCode(stop)) {
            return stop; // Or return a default name for postal codes
        } else {
            return dataGetter.getStopName(stop);
        }
    }

    private void drawTransferRoute(TransferTripCoordinates transferTripCoordinates, double originLat, double originLong, double destinationLat, double destinationLong) {
        int totalBusTripTime = transferTripCoordinates.getTimeTaken() / 60;
        double distance = distanceCalculator.calculateDistance(originLat, originLong, destinationLat, destinationLong);
        int walkingTime = distanceCalculator.calculateWalkingTime(distance);
        int cyclingTime = distanceCalculator.calculateCyclingTime(distance);

        Platform.runLater(() -> {
            mapLoader.clearMap();
            List<double[]> routeCoordinates = new ArrayList<>();

            mapLoader.addMapMarker(originLat, originLong, "Origin Postcode", "green");
            routeCoordinates.add(new double[]{originLat, originLong});

            mapLoader.addMapMarker(destinationLat, destinationLong, "Destination Postcode", "orange");
            routeCoordinates.add(new double[]{destinationLat, destinationLong});

            StringBuilder intermediateStops = new StringBuilder();
            for (TripCoordinates leg : transferTripCoordinates.getLegs()) {
                mapLoader.addMapMarker(leg.getStartStopLat(), leg.getStartStopLon(), leg.getStartStopName(), "red");
                routeCoordinates.add(new double[]{leg.getStartStopLat(), leg.getStartStopLon()});

                intermediateStops.append("Stops for Trip:\n");

                try {
                    List<double[]> shapePoints = dataGetter.getShapePoints(leg.getTripId());
                    drawRelevantShapePoints(leg.getStartStopLat(), leg.getStartStopLon(), leg.getEndStopLat(), leg.getEndStopLon(), shapePoints, "grey");

                    for (String[] stopDetails : leg.getIntermediateStopDetails()) {
                        double lat = Double.parseDouble(stopDetails[1]);
                        double lon = Double.parseDouble(stopDetails[2]);
                        String stopName = stopDetails[0];
                        mapLoader.addMapMarker(lat, lon, stopName, "black");
                        intermediateStops.append(stopName).append("\n");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    locationInput.displayError("Error fetching shape points.");
                }

                mapLoader.addMapMarker(leg.getEndStopLat(), leg.getEndStopLon(), leg.getEndStopName(), "red");
                routeCoordinates.add(new double[]{leg.getEndStopLat(), leg.getEndStopLon()});

                mapLoader.drawLine(originLat, originLong, leg.getStartStopLat(), leg.getStartStopLon(), "blue", true);
                mapLoader.drawLine(leg.getEndStopLat(), leg.getEndStopLon(), destinationLat, destinationLong, "blue", true);
            }

            setMapCenter(routeCoordinates);

            locationInput.setWalkingTimeLabel("Walking Time: " + walkingTime + " minutes");
            locationInput.setCyclingTimeLabel("Cycling Time: " + cyclingTime + " minutes");
            locationInput.setBusTripTimeLabel("Bus Trip Time: " + totalBusTripTime + " minutes");
            locationInput.updateRouteDetails(intermediateStops.toString() + "Total bus time: " + totalBusTripTime + " minutes");
        });
    }

    private void setMapCenter(List<double[]> coordinates) {
        if (coordinates.isEmpty()) {
            return;
        }

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (double[] coordinate : coordinates) {
            minLat = Math.min(minLat, coordinate[0]);
            maxLat = Math.max(maxLat, coordinate[0]);
            minLon = Math.min(minLon, coordinate[1]);
            maxLon = Math.max(maxLon, coordinate[1]);
        }

        double centerLat = (minLat + maxLat) / 2;
        double centerLon = (minLon + maxLon) / 2;

        double latDiff = maxLat - minLat;
        int finalZoomLevel = getFinalZoomLevel(maxLon, minLon, latDiff);
        Platform.runLater(() -> mapLoader.setCenter(centerLat, centerLon, finalZoomLevel));
    }

    private static int getFinalZoomLevel(double maxLon, double minLon, double latDiff) {
        double lonDiff = maxLon - minLon;

        // Calculate the zoom level based on the maximum extent
        double maxDiff = Math.max(latDiff, lonDiff);
        int zoomLevel = 14;  // Default zoom level
        if (maxDiff > 0.05) zoomLevel = 13;
        if (maxDiff > 0.1) zoomLevel = 12;
        if (maxDiff > 0.2) zoomLevel = 11;
        if (maxDiff > 0.5) zoomLevel = 10;
        if (maxDiff > 1.0) zoomLevel = 9;
        if (maxDiff > 2.0) zoomLevel = 8;

        int finalZoomLevel = zoomLevel;
        return finalZoomLevel;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
