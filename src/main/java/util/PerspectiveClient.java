package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class PerspectiveClient {
    private static final String ENDPOINT =
            "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=";

    private final HttpClient client;

    public PerspectiveClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public double analyzeToxicity(String text, String apiKey) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Missing Perspective API key");
        }

        String payload = buildPayload(text);
        DebugLog.info("Perspective", "Sending analyze request, textLen=" + safeLength(text)
                + ", key=" + maskKey(apiKey));

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT + apiKey))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("User-Agent", "HirelyForum/1.0 (JavaFX)")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        DebugLog.info("Perspective", "Response status=" + response.statusCode()
                + ", bodyLen=" + safeLength(response.body()));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            DebugLog.info("Perspective", "Non-success response body preview: " + preview(response.body()));
            throw new IOException("Perspective request failed (" + response.statusCode() + ")");
        }

        double toxicity = parseToxicity(response.body());
        DebugLog.info("Perspective", "Parsed TOXICITY summaryScore.value=" + toxicity);
        return toxicity;
    }

    private String buildPayload(String text) {
        JsonObject root = new JsonObject();
        JsonObject comment = new JsonObject();
        comment.addProperty("text", text == null ? "" : text);
        root.add("comment", comment);

        JsonArray languages = new JsonArray();
        languages.add("en");
        root.add("languages", languages);

        JsonObject requested = new JsonObject();
        requested.add("TOXICITY", new JsonObject());
        root.add("requestedAttributes", requested);

        return root.toString();
    }

    private double parseToxicity(String body) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonObject attributeScores = root.getAsJsonObject("attributeScores");
            JsonObject toxicity = attributeScores == null ? null : attributeScores.getAsJsonObject("TOXICITY");
            JsonObject summary = toxicity == null ? null : toxicity.getAsJsonObject("summaryScore");
            if (summary == null || !summary.has("value")) {
                throw new IOException("Perspective response missing TOXICITY summaryScore");
            }

            double value = summary.get("value").getAsDouble();
            if (value < 0) {
                return 0;
            }
            if (value > 1) {
                return 1;
            }
            return value;
        } catch (RuntimeException ex) {
            DebugLog.error("Perspective", "Failed to parse response body: " + preview(body), ex);
            throw new IOException("Failed to parse Perspective response", ex);
        }
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "<empty>";
        }
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private String preview(String value) {
        if (value == null) {
            return "<null>";
        }
        String compact = value.replace('\n', ' ').replace('\r', ' ');
        if (compact.length() <= 220) {
            return compact;
        }
        return compact.substring(0, 220) + "...";
    }
}
