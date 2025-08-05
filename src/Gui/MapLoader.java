package Gui;

import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.File;

/**
 * The MapLoader class is responsible for loading and interacting with the map displayed in the GUI.
 * It uses a WebView to load an HTML map and provides methods to add markers, draw lines, and clear the map.
 */
public class MapLoader extends Pane {
    private final WebView webView = new WebView();
    private final WebEngine webEngine = webView.getEngine();
    private static final String MAP_HTML_PATH = "src/resources/map.html";

    /**
     * Constructor for the MapLoader class. Initializes the WebView and loads the map.
     */
    public MapLoader() {
        loadMap();
        getChildren().add(webView);
    }

    /**
     * Loads the map HTML file into the WebView.
     */
    private void loadMap() {
        File file = new File(MAP_HTML_PATH);
        if (file.exists()) {
            System.out.println("Loading map HTML from: " + file.toURI());
            webEngine.load(file.toURI().toString());
        } else {
            System.err.println("Map HTML file not found: " + MAP_HTML_PATH);
            return;
        }

        // Make sure the map is loaded before doing anything else
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == javafx.concurrent.Worker.State.SUCCEEDED) {
                System.out.println("Map content loaded");
            } else {
                System.err.println("Map content not loaded: " + newValue);
            }
        });
    }

    /**
     * Adds a marker to the map at the specified latitude and longitude with the given name and color.
     * @param latitude the latitude of the marker.
     * @param longitude the longitude of the marker.
     * @param name the name of the marker.
     * @param color the color of the marker.
     */
    public void addMapMarker(double latitude, double longitude, String name, String color) {
        String formattedLat = String.format("%.6f", latitude).replace(',', '.');
        String formattedLon = String.format("%.6f", longitude).replace(',', '.');
        String script = String.format(
                "if (typeof window.addMarker === 'function') { window.addMarker(%s, %s, '%s', '%s'); } else { console.log('addMarker function not defined yet.'); }",
                formattedLat, formattedLon, name, color);
        webEngine.executeScript(script);
    }

    /**
     * Draws a line on the map from the origin coordinates to the destination coordinates with the specified color and style.
     * @param originLat the latitude of the origin point.
     * @param originLng the longitude of the origin point.
     * @param destinationLat the latitude of the destination point.
     * @param destinationLng the longitude of the destination point.
     * @param color the color of the line.
     * @param dashed whether the line should be dashed.
     */
    public void drawLine(double originLat, double originLng, double destinationLat, double destinationLng, String color, boolean dashed) {
        String formattedOriginLat = String.format("%.6f", originLat).replace(',', '.');
        String formattedOriginLng = String.format("%.6f", originLng).replace(',', '.');
        String formattedDestinationLat = String.format("%.6f", destinationLat).replace(',', '.');
        String formattedDestinationLng = String.format("%.6f", destinationLng).replace(',', '.');
        String script = String.format(
                "if (window.drawLine) { window.drawLine(%s, %s, %s, %s, '%s', %s); } else { console.log('drawLine function not defined yet.'); }",
                formattedOriginLat, formattedOriginLng, formattedDestinationLat, formattedDestinationLng, color, dashed);
        webEngine.executeScript(script);
    }

    /**
     * Clears all markers and lines from the map.
     */
    public void clearMap() {
        webEngine.executeScript("if (window.clearMap) { window.clearMap(); } else { console.log('clearMap function not defined yet.'); }");
    }

    /**
     * Sets the center of the map to the specified latitude and longitude with the given zoom level.
     * @param lat the latitude to center the map on.
     * @param lon the longitude to center the map on.
     * @param zoomLevel the zoom level to set on the map.
     */
    public void setCenter(double lat, double lon, int zoomLevel) {
        String formattedLat = String.format("%.6f", lat).replace(',', '.');
        String formattedLon = String.format("%.6f", lon).replace(',', '.');
        String script = String.format(
                "map.setView([%s, %s], %d);",
                formattedLat, formattedLon, zoomLevel);
        webEngine.executeScript(script);
    }
}
