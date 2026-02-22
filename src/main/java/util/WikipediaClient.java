package util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class WikipediaClient {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static final class WikiSummary {
        public final String title;
        public final String extract;
        public final String url;

        public WikiSummary(String title, String extract, String url) {
            this.title = title;
            this.extract = extract;
            this.url = url;
        }
    }

    public static final class NotFoundException extends Exception {
        public NotFoundException(String message) {
            super(message);
        }
    }

    private WikipediaClient() {
    }

    public static WikiSummary fetchSummary(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String endpoint = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encoded;

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "HirelyForum/1.0 (JavaFX)")
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            throw new NotFoundException("No Wikipedia summary found.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Wikipedia request failed (" + response.statusCode() + ")");
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String title = getString(json, "title");
        String extract = getString(json, "extract");
        String url = getNestedString(json, "content_urls", "desktop", "page");

        if (extract == null || extract.isBlank()) {
            throw new NotFoundException("No Wikipedia summary found.");
        }

        return new WikiSummary(title, extract, url);
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsString();
    }

    private static String getNestedString(JsonObject obj, String... keys) {
        JsonObject current = obj;
        for (int i = 0; i < keys.length - 1; i++) {
            if (current == null || !current.has(keys[i]) || current.get(keys[i]).isJsonNull()) {
                return null;
            }
            current = current.getAsJsonObject(keys[i]);
        }
        if (current == null || !current.has(keys[keys.length - 1]) || current.get(keys[keys.length - 1]).isJsonNull()) {
            return null;
        }
        return current.get(keys[keys.length - 1]).getAsString();
    }
}
