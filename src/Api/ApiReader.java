package Api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiReader {  
    private final String JSON_INPUT_STRING;
    private static final String API_URL = "https://computerscience.dacs.unimaas.nl/get_coordinates";
    private double latitude;
    private double longitude;

    public ApiReader(String zipCode) {
        JSON_INPUT_STRING = String.format("{\"postcode\": \"%s\"}", zipCode);
        try {
            JsonNode foundObject = searchInLocalJson(zipCode);
            if (foundObject != null) {
                System.out.println("Postcode exists in the JSON file.");
                latitude = foundObject.get("Lat").asDouble();
                longitude = foundObject.get("Lon").asDouble();
            } else {
                String response = getCoordinatesFromAPI();

                // Remove the curly braces and split the string into parts
                String[] parts = response.replace("{", "").replace("}", "").split(",");

                // Extract the values
                latitude = Double.parseDouble(parts[0].split(":")[1].trim().replace("\"", ""));
                longitude = Double.parseDouble(parts[1].split(":")[1].trim().replace("\"", ""));
                String postcode = parts[2].split(":")[1].trim().replace("\"", "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonNode searchInLocalJson(String zipCode) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile = new File("src/resources/localization.json");
        JsonNode root = mapper.readTree(jsonFile);
        JsonNode array = root.get("export_dataframe.csv");
        for (JsonNode object : array) {
            if (object.has("Zip") && object.get("Zip").asText().equals(zipCode)) {
                return object;
            }
        }
        return null;
    }

    private String getCoordinatesFromAPI() throws IOException {
        HttpURLConnection connection = getHttpURLConnection();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        return response.toString();
    }

    private @NotNull HttpURLConnection getHttpURLConnection() throws IOException {     
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = JSON_INPUT_STRING.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}
}
