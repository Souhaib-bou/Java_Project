package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PythonScoreClient {
    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8008";

    private final HttpClient client;
    private final String baseUrl;

    public PythonScoreClient() {
        this(resolveBaseUrl());
    }

    public PythonScoreClient(String baseUrl) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.baseUrl = sanitizeBaseUrl(baseUrl);
    }

    public ScoreResult score(String text, String type) throws Exception {
        return score(text, type, "");
    }

    public ScoreResult score(String text, String type, String contentKey) throws Exception {
        String endpoint = baseUrl + "/score";
        JsonObject payload = new JsonObject();
        payload.addProperty("text", text == null ? "" : text);
        payload.addProperty("type", normalizeType(type));
        payload.addProperty("content_key", contentKey == null ? "" : contentKey);

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json")
                .header("User-Agent", "HirelyForum/1.0 (JavaFX)")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        DebugLog.info("PythonAI", "POST " + endpoint + ", textLen=" + safeLength(text));
        long startNs = System.nanoTime();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long latencyMs = elapsedMs(startNs);
        DebugLog.info("PythonAI", "Response status=" + response.statusCode()
                + ", bodyLen=" + safeLength(response.body()));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Python score request failed (" + response.statusCode() + "): " + preview(response.body()));
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        double relevance = readDouble(root, "relevance", 0.50);
        String predictedCategory = readString(
                root,
                "predictedCategory",
                readString(root, "predicted_category", readString(root, "category", "General")));
        double quality = readDouble(root, "quality", 0.50);
        double duplicateScore = readDouble(root, "duplicate_score",
                readDouble(root, "duplicate_similarity", 0.00));
        Long duplicateOfPostId = readOptionalLong(root, "duplicate_of_post_id");
        if (duplicateOfPostId == null) {
            duplicateOfPostId = readOptionalLong(root, "duplicateOfPostId");
        }

        JsonObject reasonsObj = root.has("reasons") && root.get("reasons").isJsonObject()
                ? root.getAsJsonObject("reasons")
                : new JsonObject();
        List<String> relevanceReasons = readStringList(reasonsObj, "relevance");
        List<String> qualityReasons = readStringList(reasonsObj, "quality");
        List<String> duplicateReasons = readStringList(reasonsObj, "duplicate");

        return new ScoreResult(
                relevance,
                predictedCategory,
                quality,
                duplicateScore,
                duplicateOfPostId,
                relevanceReasons,
                qualityReasons,
                duplicateReasons,
                trimRaw(response.body()),
                latencyMs);
    }

    private static String normalizeType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "post";
        }
        String value = rawType.trim().toLowerCase(Locale.ROOT);
        return "comment".equals(value) ? "comment" : "post";
    }

    private static String resolveBaseUrl() {
        return Secrets.PY_AI_URL;
    }

    private static String sanitizeBaseUrl(String raw) {
        String value = (raw == null || raw.isBlank()) ? DEFAULT_BASE_URL : raw.trim();
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String readString(JsonObject root, String field, String fallback) {
        if (!root.has(field) || root.get(field).isJsonNull()) {
            return fallback;
        }
        return root.get(field).getAsString();
    }

    private static double readDouble(JsonObject root, String field, double fallback) {
        if (!root.has(field) || root.get(field).isJsonNull()) {
            return fallback;
        }
        return root.get(field).getAsDouble();
    }

    private static List<String> readStringList(JsonObject root, String field) {
        List<String> values = new ArrayList<>();
        if (!root.has(field) || !root.get(field).isJsonArray()) {
            return values;
        }
        JsonArray arr = root.getAsJsonArray(field);
        for (JsonElement element : arr) {
            if (element != null && !element.isJsonNull()) {
                values.add(element.getAsString());
            }
        }
        return values;
    }

    private static Long readOptionalLong(JsonObject root, String field) {
        if (!root.has(field) || root.get(field).isJsonNull()) {
            return null;
        }
        return root.get(field).getAsLong();
    }

    private static String trimRaw(String raw) {
        if (raw == null) {
            return "";
        }
        String compact = raw.replace('\n', ' ').replace('\r', ' ');
        if (compact.length() <= 2000) {
            return compact;
        }
        return compact.substring(0, 2000) + "...";
    }

    private static String preview(String raw) {
        if (raw == null) {
            return "<null>";
        }
        String compact = raw.replace('\n', ' ').replace('\r', ' ');
        if (compact.length() <= 220) {
            return compact;
        }
        return compact.substring(0, 220) + "...";
    }

    private static int safeLength(String raw) {
        return raw == null ? 0 : raw.length();
    }

    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    public static final class ScoreResult {
        private final double relevance;
        private final String predictedCategory;
        private final double quality;
        private final double duplicateScore;
        private final Long duplicateOfPostId;
        private final List<String> relevanceReasons;
        private final List<String> qualityReasons;
        private final List<String> duplicateReasons;
        private final String raw;
        private final long latencyMs;

        public ScoreResult(double relevance, String predictedCategory, double quality, double duplicateScore,
                Long duplicateOfPostId,
                List<String> relevanceReasons, List<String> qualityReasons, List<String> duplicateReasons,
                String raw, long latencyMs) {
            this.relevance = relevance;
            this.predictedCategory = predictedCategory;
            this.quality = quality;
            this.duplicateScore = duplicateScore;
            this.duplicateOfPostId = duplicateOfPostId;
            this.relevanceReasons = relevanceReasons == null ? List.of() : List.copyOf(relevanceReasons);
            this.qualityReasons = qualityReasons == null ? List.of() : List.copyOf(qualityReasons);
            this.duplicateReasons = duplicateReasons == null ? List.of() : List.copyOf(duplicateReasons);
            this.raw = raw;
            this.latencyMs = latencyMs;
        }

        public double getRelevance() {
            return relevance;
        }

        public String getPredictedCategory() {
            return predictedCategory;
        }

        public String getCategory() {
            return predictedCategory;
        }

        public double getQuality() {
            return quality;
        }

        public double getDuplicateScore() {
            return duplicateScore;
        }

        public double getDuplicateSimilarity() {
            return duplicateScore;
        }

        public Long getDuplicateOfPostId() {
            return duplicateOfPostId;
        }

        public List<String> getRelevanceReasons() {
            return relevanceReasons;
        }

        public List<String> getQualityReasons() {
            return qualityReasons;
        }

        public List<String> getDuplicateReasons() {
            return duplicateReasons;
        }

        public String getRaw() {
            return raw;
        }

        public long getLatencyMs() {
            return latencyMs;
        }
    }
}
