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
        DebugLog.info("Wikipedia", "Sending summary request, query=\"" + preview(query)
                + "\", endpoint=" + endpoint);

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "HirelyForum/1.0 (JavaFX)")
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        DebugLog.info("Wikipedia", "Response status=" + response.statusCode()
                + ", bodyLen=" + safeLength(response.body()));
        if (response.statusCode() == 404) {
            DebugLog.info("Wikipedia", "No page found for query=\"" + preview(query) + "\"");
            throw new NotFoundException("No Wikipedia summary found.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            DebugLog.info("Wikipedia", "Non-success response body preview: " + preview(response.body()));
            throw new IOException("Wikipedia request failed (" + response.statusCode() + ")");
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String title = getString(json, "title");
        String extract = getString(json, "extract");
        String url = getNestedString(json, "content_urls", "desktop", "page");

        if (extract == null || extract.isBlank()) {
            throw new NotFoundException("No Wikipedia summary found.");
        }

        DebugLog.info("Wikipedia", "Parsed summary title=\"" + preview(title)
                + "\", extractLen=" + safeLength(extract)
                + ", hasUrl=" + (url != null && !url.isBlank()));

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

    private static int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private static String preview(String value) {
        if (value == null) {
            return "<null>";
        }
        String compact = value.replace('\n', ' ').replace('\r', ' ');
        if (compact.length() <= 160) {
            return compact;
        }
        return compact.substring(0, 160) + "...";
    }
}
