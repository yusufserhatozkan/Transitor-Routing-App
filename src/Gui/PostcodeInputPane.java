package Gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * The PostcodeInputPane class is a custom VBox that contains input fields for origin and destination postcodes,
 * a time selection box, and labels for displaying distance and travel times.
 */
public class PostcodeInputPane extends VBox {
    private final Label distanceLabel;
    private final Label walkingTimeLabel;
    private final Label cyclingTimeLabel;
    private final Label busTripTimeLabel;
    private final Label msgLabel;
    private final TextArea routeDetails;
    private final ComboBox<String> timeBox;
    private final Label originAccessibilityLabel;
    private final Label destinationAccessibilityLabel;

    /**
     * Constructor for the PostcodeInputPane class. Sets up the layout and initializes the UI components.
     */
    public PostcodeInputPane() {
        setSpacing(5);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(10));

        Label originLabel = new Label("Origin");
        HBox originBox = createTextFieldBox();
        originBox.setAlignment(Pos.CENTER);

        Label destinationLabel = new Label("Destination");
        HBox destinationBox = createTextFieldBox();
        destinationBox.setAlignment(Pos.CENTER);

        Label timeLabel = new Label("Departure Time:");
        timeBox = new ComboBox<>();
        timeBox.setEditable(true);
        timeBox.setPrefWidth(100); // Set the preferred width of the timeBox
        addTimesToBox();
        setCurrentTime();
        Button nowButton = new Button("Now");
        nowButton.setOnAction(e -> setCurrentTime());

        HBox timeBoxContainer = new HBox(10, timeBox, nowButton); // Rename to avoid conflict
        timeBoxContainer.setAlignment(Pos.CENTER);

        this.distanceLabel = new Label("Distance:");
        this.walkingTimeLabel = new Label("Walking:");
        this.cyclingTimeLabel = new Label("Cycling:");
        this.busTripTimeLabel = new Label("Bus Trip Time:");
        this.msgLabel = new Label();
        this.msgLabel.setTextFill(Color.RED);

        this.routeDetails = new TextArea();
        this.routeDetails.setEditable(false);
        this.routeDetails.setWrapText(true);
        this.routeDetails.setPrefRowCount(3);
        this.routeDetails.setPrefColumnCount(20);

        // Initialize accessibility labels
        this.originAccessibilityLabel = new Label("Origin Accessibility Score:");
        this.destinationAccessibilityLabel = new Label("Destination Accessibility Score:");

        getChildren().addAll(originLabel, originBox, destinationLabel, destinationBox, timeLabel, timeBoxContainer, distanceLabel, walkingTimeLabel, cyclingTimeLabel, busTripTimeLabel, routeDetails, originAccessibilityLabel, destinationAccessibilityLabel, msgLabel);
        setMargin(walkingTimeLabel, new Insets(20, 0, 0, 10));
        setMargin(distanceLabel, new Insets(40, 0, 0, 10));
        setMargin(cyclingTimeLabel, new Insets(20, 0, 0, 10));
        setMargin(busTripTimeLabel, new Insets(20, 0, 0, 10));
    }


    /**
     * Creates a box of text fields for postcode input, handling key events for navigation between fields.
     * @return the HBox containing the text fields.
     */
    private HBox createTextFieldBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);

        for (int i = 0; i < 6; i++) {
            boolean acceptsDigitsOnly = i < 4;
            PostcodeTextField textField = new PostcodeTextField(!acceptsDigitsOnly);
            int finalI = i;
            textField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.BACK_SPACE && textField.getText().isEmpty() && finalI > 0) {
                    PostcodeTextField previousField = (PostcodeTextField) box.getChildren().get(finalI - 1);
                    previousField.requestFocus();
                    previousField.selectAll();
                    event.consume();
                }
            });
            textField.setOnKeyReleased(event -> {
                if (event.getCode() != KeyCode.BACK_SPACE && textField.getText().length() == 1) {
                    if (finalI < 5) {
                        PostcodeTextField nextField = (PostcodeTextField) box.getChildren().get(finalI + 1);
                        nextField.requestFocus();
                        nextField.selectAll();
                    } else {
                        textField.getParent().requestFocus();
                    }
                }
            });
            box.getChildren().add(textField);
        }

        return box;
    }

    /**
     * Adds times to the time selection box in 15-minute intervals.
     */
    private void addTimesToBox() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 15) {
                LocalTime time = LocalTime.of(hour, minute);
                timeBox.getItems().add(time.format(formatter));
            }
        }
    }

    /**
     * Sets the current time in the time selection box.
     */
    private void setCurrentTime() {
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        timeBox.setValue(now.format(formatter));
    }

    /**
     * Sets the text of the distance label.
     * @param distance the distance to display.
     */
    public void setDistanceLabel(String distance){
        distanceLabel.setText("Distance: " + distance + "km");
    }

    /**
     * Sets the text of the walking time label.
     * @param time the walking time to display.
     */
    public void setWalkingTimeLabel(String time){
        walkingTimeLabel.setText(time);
    }

    /**
     * Sets the text of the cycling time label.
     * @param time the cycling time to display.
     */
    public void setCyclingTimeLabel(String time){
        cyclingTimeLabel.setText(time);
    }

    /**
     * Sets the text of the bus trip time label.
     * @param time the bus trip time to display.
     */
    public void setBusTripTimeLabel(String time){
        busTripTimeLabel.setText(time);
    }

    /**
     * Gets the selected departure time from the time box.
     * @return the selected departure time.
     */
    public String getSelectedTime() {
        return timeBox.getValue();
    }

    /**
     * Gets the origin postcode from the text fields.
     * @return the origin postcode as a string.
     */
    public String getOriginPostcodes() {
        return getPostcodeAsString(1);
    }

    /**
     * Gets the destination postcode from the text fields.
     * @return the destination postcode as a string.
     */
    public String getDestinationPostcodes() {
        return getPostcodeAsString(3);
    }

    /**
     * Constructs the postcode string from the text fields starting at the given index.
     * @param startIndex the index to start from.
     * @return the postcode as a string.
     */
    private String getPostcodeAsString(int startIndex) {
        StringBuilder postcode = new StringBuilder();
        for (javafx.scene.Node node : ((HBox) getChildren().get(startIndex)).getChildren()) {
            if (node instanceof PostcodeTextField textField) {
                postcode.append(textField.getText());
            }
        }
        return postcode.toString().toUpperCase();
    }

    /**
     * Updates the logistics labels with the calculated distances and times.
     * @param distance the distance between the postcodes.
     * @param walkingTime the walking time.
     * @param cyclingTime the cycling time.
     * @param busTripTime the bus trip time.
     */
    public void updateLogistics(double distance, int walkingTime, int cyclingTime, int busTripTime) {
        Platform.runLater(() -> {
            setDistanceLabel(String.valueOf(Math.round(distance * 100.0) / 100.0));
            setWalkingTimeLabel("Walking Time: " + walkingTime + " minutes");
            setCyclingTimeLabel("Cycling Time: " + cyclingTime + " minutes");
            setBusTripTimeLabel("Bus Time: " + busTripTime + " minutes");
        });
    }

    /**
     * Updates the route details text area with the provided details.
     * @param details the route details to display.
     */
    public void updateRouteDetails(String details) {
        Platform.runLater(() -> routeDetails.setText(details));
    }

    /**
     * Displays an error message in the message label.
     * @param message the error message to display.
     */
    public void displayError(String message) {
        Platform.runLater(() -> {
            msgLabel.setText(message);
            msgLabel.setTextFill(Color.RED);
            msgLabel.setVisible(true);
        });
    }

    /**
     * Clears any error messages from the message label.
     */
    public void clearError() {
        Platform.runLater(() -> {
            msgLabel.setText("");
            msgLabel.setVisible(false);
        });
    }

    /**
     * Displays an informational message in the message label.
     * @param message the informational message to display.
     */
    public void displayInfo(String message) {
        Platform.runLater(() -> {
            msgLabel.setText(message);
            msgLabel.setTextFill(Color.BLUE);
            msgLabel.setVisible(true);
        });
    }
    public void setOriginAccessibilityLabel(String score) {
        Platform.runLater(() -> originAccessibilityLabel.setText("Origin Accessibility Score: " + score));
    }

    public void setDestinationAccessibilityLabel(String score) {
        Platform.runLater(() -> destinationAccessibilityLabel.setText("Destination Accessibility Score: " + score));
    }
    public String getRouteDetails(){
        return routeDetails.getText();
    }
}
