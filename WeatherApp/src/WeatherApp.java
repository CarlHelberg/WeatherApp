import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherApp extends JFrame {
    private JTextField cityField, countryField, zipField;
    private JLabel tempLabel, humidityLabel, windSpeedLabel, conditionsLabel;
    private JTextArea historyArea;
    private JButton fetchButton, refreshButton;
    private JComboBox<String> unitSelector;
    private String unit = "metric"; // Default to Celsius

    public WeatherApp() {
        setTitle("BudgeIt Weather App");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top input panel for location and units
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.add(new JLabel("City Name:"));
        cityField = new JTextField();
        inputPanel.add(cityField);

        inputPanel.add(new JLabel("Country Code (optional):"));
        countryField = new JTextField();
        inputPanel.add(countryField);

        inputPanel.add(new JLabel("ZIP Code (optional):"));
        zipField = new JTextField();
        inputPanel.add(zipField);

        inputPanel.add(new JLabel("Temperature Unit:"));
        unitSelector = new JComboBox<>(new String[]{"Celsius", "Fahrenheit"});
        unitSelector.addActionListener(e -> {
            unit = unitSelector.getSelectedItem().equals("Celsius") ? "metric" : "imperial";
        });
        inputPanel.add(unitSelector);

        fetchButton = new JButton("Fetch Weather");
        refreshButton = new JButton("Refresh Data");
        inputPanel.add(fetchButton);
        inputPanel.add(refreshButton);
        add(inputPanel, BorderLayout.NORTH);

        // Weather details panel
        JPanel weatherPanel = new JPanel(new GridLayout(4, 1));
        tempLabel = new JLabel("Temperature: N/A");
        humidityLabel = new JLabel("Humidity: N/A");
        windSpeedLabel = new JLabel("Wind Speed: N/A");
        conditionsLabel = new JLabel("Conditions: N/A");
        weatherPanel.add(tempLabel);
        weatherPanel.add(humidityLabel);
        weatherPanel.add(windSpeedLabel);
        weatherPanel.add(conditionsLabel);
        add(weatherPanel, BorderLayout.CENTER);

        // History panel
        historyArea = new JTextArea();
        historyArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(historyArea);
        add(scrollPane, BorderLayout.SOUTH);

        // Button listeners
        fetchButton.addActionListener(e -> fetchLocationAndWeatherData());
        refreshButton.addActionListener(e -> fetchLocationAndWeatherData());

        setVisible(true);
    }

    // Fetch latitude and longitude from the Geocoding API based on user inputs
    private void fetchLocationAndWeatherData() {
        String city = cityField.getText();
        String country = countryField.getText();
        String zip = zipField.getText();

        if (city.isEmpty() && zip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter either a city name or zip code.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String apiKey = "4406488cfb21257a3c8b3b9dc259c961";  // Replace with your API key
            String geoUrl = buildGeocodingUrl(city, country, zip, apiKey);
            System.out.println("geoURL: " + geoUrl );
            HttpURLConnection conn = (HttpURLConnection) new URL(geoUrl).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONArray geoData = new JSONArray(response.toString());
            if (geoData.length() > 0) {
                JSONObject location = geoData.getJSONObject(0);
                double lat = location.getDouble("lat");
                double lon = location.getDouble("lon");

                fetchWeatherData(lat, lon);
            } else {
                JOptionPane.showMessageDialog(this, "Location not found. Please check inputs.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to retrieve location data. Check input or API.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper method to build the Geocoding API URL
    private String buildGeocodingUrl(String city, String country, String zip, String apiKey) {
        StringBuilder url = new StringBuilder("https://api.openweathermap.org/geo/1.0/");
        if (!zip.isEmpty()) {
            url.append("zip?zip=").append(zip);
            if (!country.isEmpty()) url.append(",").append(country);
        } else {
            url.append("direct?q=").append(city);
            if (!country.isEmpty()) url.append(",").append(country);
        }
        url.append("&appid=").append(apiKey);
        return url.toString();
    }

    // Fetch weather data for given latitude and longitude coordinates
    private void fetchWeatherData(double lat, double lon) {
        String apiKey = "4406488cfb21257a3c8b3b9dc259c961";  // Replace with your API key
        String apiUrl = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%.2f&lon=%.2f&units=%s&appid=%s",
                lat, lon, unit, apiKey);

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            parseWeatherData(response.toString());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to retrieve weather data. Check API connectivity.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Parse and display the fetched weather data
    private void parseWeatherData(String jsonResponse) {
        try {
            JSONObject weatherData = new JSONObject(jsonResponse);

            double temperature = weatherData.getJSONObject("main").getDouble("temp");
            int humidity = weatherData.getJSONObject("main").getInt("humidity");
            double windSpeed = weatherData.getJSONObject("wind").getDouble("speed");
            String conditions = weatherData.getJSONArray("weather").getJSONObject(0).getString("description");

            tempLabel.setText("Temperature: " + temperature + (unit.equals("metric") ? " °C" : " °F"));
            humidityLabel.setText("Humidity: " + humidity + " %");
            windSpeedLabel.setText("Wind Speed: " + windSpeed + " m/s");
            conditionsLabel.setText("Conditions: " + conditions);

            String history = String.format("Location: %s, Temp: %.1f%s, Humidity: %d%%, Wind: %.1f m/s, Cond: %s\n",
                    cityField.getText(), temperature, (unit.equals("metric") ? "°C" : "°F"),
                    humidity, windSpeed, conditions);
            historyArea.append(history);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error parsing weather data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherApp::new);
    }
}
