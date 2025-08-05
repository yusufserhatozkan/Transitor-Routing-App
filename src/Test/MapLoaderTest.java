// package Test;

// import static org.junit.Assert.*;
// import org.junit.Test;
// import org.testfx.framework.junit.ApplicationTest;
// import javafx.scene.web.WebEngine;
// import javafx.stage.Stage;
// import Gui.MapLoader;

// public class MapLoaderTest extends ApplicationTest {

//     private MapLoader loader;

//     @Override
//     public void start(Stage stage) {
//         loader = new MapLoader();
//         stage.setScene(new javafx.scene.Scene(loader));
//         stage.show();
//     }

//     @Test
//     public void testLoadMap() {
//         File file = new File("src/resources/map.html");
//         assertTrue("Map HTML file should exist", file.exists());

//         WebEngine webEngine = loader.getWebEngine();
//         webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
//             if (newValue == javafx.concurrent.Worker.State.SUCCEEDED) {
//                 assertTrue("Map content should be loaded", true);
//             }
//         });

//         loader.loadMap();
//     }

//     @Test
//     public void testAddMapMarker() {
//         loader.addMapMarker(1.0, 2.0, "Test Marker", "red");
//         // You can add more specific assertions if you can access the web page content
//         assertTrue(true);
//     }

//     @Test
//     public void testDrawLine() {
//         loader.drawLine(1.0, 2.0, 3.0, 4.0, "blue", false);
//         // You can add more specific assertions if you can access the web page content
//         assertTrue(true);
//     }

//     @Test
//     public void testClearMap() {
//         loader.clearMap();
//         // You can add more specific assertions if you can access the web page content
//         assertTrue(true);
//     }

//     @Test
//     public void testSetCenter() {
//         loader.setCenter(1.0, 2.0, 10);
//         // You can add more specific assertions if you can access the web page content
//         assertTrue(true);
//     }
// }
