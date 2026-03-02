package Controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class MapPickerController {

    @FXML private WebView mapView;

    private String selectedLocation = null;
    private Runnable onLocationSelected;

    // Bridge object exposed to JavaScript
    public class JavaBridge {
        public void onLocationPicked(String placeName) {
            selectedLocation = placeName;
            javafx.application.Platform.runLater(() -> {
                if (onLocationSelected != null) onLocationSelected.run();
            });
        }
    }

    private final JavaBridge bridge = new JavaBridge();

    @FXML
    public void initialize() {
        WebEngine engine = mapView.getEngine();

        // Inject the Java bridge once the page loads
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
            }
        });

        engine.loadContent(buildMapHtml());
    }

    public String getSelectedLocation() {
        return selectedLocation;
    }

    public void setOnLocationSelected(Runnable callback) {
        this.onLocationSelected = callback;
    }

    private String buildMapHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8"/>
                <style>
                    * { margin: 0; padding: 0; }
                    #map { width: 100vw; height: 100vh; }
                    #info {
                        position: absolute;
                        top: 10px; left: 50%;
                        transform: translateX(-50%);
                        z-index: 1000;
                        background: white;
                        padding: 8px 18px;
                        border-radius: 20px;
                        font-family: Arial, sans-serif;
                        font-size: 13px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.2);
                        color: #0f172a;
                        pointer-events: none;
                    }
                </style>
                <link rel="stylesheet"
                      href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            </head>
            <body>
                <div id="info">🗺️ Click anywhere on the map to select a location</div>
                <div id="map"></div>

                <script>
                    var map = L.map('map').setView([33.8869, 9.5375], 6); // centered on Tunisia

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        attribution: '© OpenStreetMap contributors'
                    }).addTo(map);

                    var marker = null;

                    map.on('click', function(e) {
                        var lat = e.latlng.lat;
                        var lon = e.latlng.lng;

                        // Drop/move marker
                        if (marker) {
                            marker.setLatLng(e.latlng);
                        } else {
                            marker = L.marker(e.latlng).addTo(map);
                        }

                        document.getElementById('info').innerHTML = '⏳ Getting location name...';

                        // Reverse geocode using Nominatim
                        fetch('https://nominatim.openstreetmap.org/reverse?lat=' + lat
                              + '&lon=' + lon + '&format=json', {
                            headers: { 'User-Agent': 'HirelyHRApp/1.0' }
                        })
                        .then(r => r.json())
                        .then(data => {
                            var place = data.display_name || (lat.toFixed(4) + ', ' + lon.toFixed(4));
                            // Shorten — use city/town/state level
                            var addr = data.address || {};
                            var short = addr.city || addr.town || addr.village
                                      || addr.county || addr.state || place;
                            if (addr.country) short += ', ' + addr.country;

                            document.getElementById('info').innerHTML = '📍 ' + short;
                            marker.bindPopup(short).openPopup();

                            // Send to Java
                            if (window.javaBridge) {
                                window.javaBridge.onLocationPicked(short);
                            }
                        })
                        .catch(err => {
                            document.getElementById('info').innerHTML = '❌ Could not get location name';
                        });
                    });
                </script>
            </body>
            </html>
        """;
    }
}