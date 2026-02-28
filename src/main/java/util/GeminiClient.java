package util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Career-assistant wrapper for Gemini generateContent API.
 * Returns safe fallback text when key/service is unavailable.
 */
public final class GeminiClient {
    private static final String[] MODEL_CANDIDATES = {
            "gemini-2.5-pro",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-pro",
            "gemini-1.5-flash-8b"
    };

    private static final String API_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String FALLBACK_TEXT =
            "Gemini is unavailable right now (missing key or service error). Please try again later.";
    private static final String EMPTY_REPLY_FALLBACK =
            "I\u2019m here\u2014could you share a bit more detail about the role/company/location?";
    private static final String RATE_LIMIT_FALLBACK =
            "Gemini quota/rate limit reached right now. Please try again in a moment.";
    private static final String AUTH_FALLBACK =
            "Gemini API key is invalid or not authorized for this project.";
    private static final String MODEL_FALLBACK =
            "Gemini model endpoint is unavailable. Please update model config.";
    private static final int OUTPUT_MAX = 1200;
    private static final Pattern GEMINI_TRIGGER_PATTERN = Pattern.compile("(?i)@gemini");
    private static final Pattern MULTISPACE_PATTERN = Pattern.compile("\\s+");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(12);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(35);
    private static final String CONTEXT_HEADER = """
            You are \u201CGemini\u201D, a bot that replies as a COMMENT in a comment thread inside Hirely, a professional, career-focused hiring forum.
            Your reply will be stored as a normal comment under the post, visible to recruiters, students, and admins.

            Write as a concise, professional forum commenter:
            - 3\u20138 short bullet points max, OR 1 short paragraph + bullets.
            - Be job/internship/career focused.
            - If the request is off-topic, politely steer back to career context.
            - Don\u2019t mention system prompts, APIs, models, moderation, or internal tooling.
            - Don\u2019t claim you took actions; only give guidance.
            """;

    private final HttpClient http;

    public GeminiClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public String generateReply(String postTitle, String postContent, String userComment, String tag) {
        String key = resolveApiKey();
        if (key.isBlank()) {
            DebugLog.info("Gemini", "GEMINI_API_KEY is not set in util.Secrets.");
            return FALLBACK_TEXT;
        }

        try {
            String prompt = buildPrompt(postTitle, postContent, userComment, tag);
            GeminiAttempt attempt = generateWithFallbackModels(prompt, key);
            String text = attempt == null ? "" : attempt.text;
            if (text == null || text.isBlank()) {
                DebugLog.info("Gemini", "Gemini returned empty response body");
                return EMPTY_REPLY_FALLBACK;
            }
            return shapeOutput(text);
        } catch (Exception ex) {
            DebugLog.error("Gemini", "Gemini generation failed", ex);
            return fallbackForException(ex);
        }
    }

    public static String cleanTriggerToken(String userComment) {
        if (userComment == null || userComment.isBlank()) {
            return "";
        }
        String stripped = GEMINI_TRIGGER_PATTERN.matcher(userComment).replaceAll("");
        return MULTISPACE_PATTERN.matcher(stripped).replaceAll(" ").trim();
    }

    private GeminiAttempt generateWithFallbackModels(String prompt, String apiKey) throws Exception {
        Exception last = null;
        boolean sawQuota = false;
        boolean sawMissingModel = false;

        for (String model : MODEL_CANDIDATES) {
            ApiResponse response;
            try {
                response = callGenerateContent(model, prompt, apiKey);
            } catch (Exception ex) {
                last = ex;
                DebugLog.error("Gemini", "HTTP call failed for model " + model, ex);
                continue;
            }

            int status = response.statusCode;
            if (status == 200) {
                String text = extractText(response.body);
                if (text == null || text.isBlank()) {
                    last = new RuntimeException("Empty Gemini text for model: " + model);
                    continue;
                }
                return new GeminiAttempt(model, text);
            }

            if (status == 429) {
                sawQuota = true;
                DebugLog.info("Gemini", "Quota/rate limit for model " + model + " (HTTP 429). Trying next model.");
                continue;
            }
            if (status == 404) {
                sawMissingModel = true;
                DebugLog.info("Gemini", "Model not found " + model + " (HTTP 404). Trying next model.");
                continue;
            }
            if (status == 401 || status == 403) {
                throw new RuntimeException("AUTH " + status + ": " + response.body);
            }

            last = new RuntimeException("Gemini API error (" + status + "): " + response.body);
            DebugLog.info("Gemini", "Non-retryable status " + status + " for model " + model);
            break;
        }

        if (sawQuota) {
            throw new RuntimeException("QUOTA_EXHAUSTED");
        }
        if (sawMissingModel) {
            throw new RuntimeException("MODEL_NOT_FOUND");
        }
        if (last != null) {
            throw last;
        }
        throw new RuntimeException("No models attempted");
    }

    private ApiResponse callGenerateContent(String model, String prompt, String apiKey) throws Exception {
        String url = API_BASE + model + ":generateContent?key=" + apiKey;
        String body = buildRequestJson(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body() == null ? "" : response.body();
        return new ApiResponse(response.statusCode(), responseBody);
    }

    private String buildRequestJson(String prompt) {
        JsonObject root = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt == null ? "" : prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        root.add("contents", contents);
        return root.toString();
    }

    private String extractText(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return "";
            }
            JsonObject obj = parsed.getAsJsonObject();
            JsonArray candidates = safeArray(obj.get("candidates"));
            if (candidates == null) {
                return "";
            }
            for (JsonElement candEl : candidates) {
                if (!candEl.isJsonObject()) {
                    continue;
                }
                JsonObject cand = candEl.getAsJsonObject();
                JsonObject content = safeObject(cand.get("content"));
                if (content == null) {
                    continue;
                }
                JsonArray parts = safeArray(content.get("parts"));
                if (parts == null) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                for (JsonElement partEl : parts) {
                    JsonObject part = safeObject(partEl);
                    if (part == null) {
                        continue;
                    }
                    String text = safeString(part.get("text"));
                    if (!text.isBlank()) {
                        if (sb.length() > 0) {
                            sb.append('\n');
                        }
                        sb.append(text.trim());
                    }
                }
                String joined = sb.toString().trim();
                if (!joined.isBlank()) {
                    return joined;
                }
            }
            return "";
        } catch (Exception ex) {
            DebugLog.error("Gemini", "Failed parsing Gemini JSON response", ex);
            return "";
        }
    }

    private JsonArray safeArray(JsonElement el) {
        if (el == null || !el.isJsonArray()) {
            return null;
        }
        return el.getAsJsonArray();
    }

    private JsonObject safeObject(JsonElement el) {
        if (el == null || !el.isJsonObject()) {
            return null;
        }
        return el.getAsJsonObject();
    }

    private String safeString(JsonElement el) {
        if (el == null || !el.isJsonPrimitive()) {
            return "";
        }
        try {
            String s = el.getAsString();
            return s == null ? "" : s;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String buildPrompt(String postTitle, String postContent, String userComment, String tag) {
        String title = limit(clean(postTitle), 180);
        String content = limit(clean(postContent), 1200);
        String comment = limit(cleanTriggerToken(userComment), 700);
        String normalizedTag = limit(clean(tag), 120);

        return CONTEXT_HEADER + """

                Post title: %s
                Post tag: %s
                Post content: %s
                User comment: %s

                Task: Reply as a helpful career assistant comment.
                """.formatted(title, normalizedTag, content, comment);
    }

    private String resolveApiKey() {
        return normalize(Secrets.GEMINI_API_KEY);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("YOUR_KEY_HERE")) {
            return "";
        }
        return trimmed;
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').trim();
    }

    private String limit(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        if (max <= 3) {
            return value.substring(0, max);
        }
        int end = max;
        while (end > 0 && !Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        if (end < max / 2) {
            end = max;
        }
        return value.substring(0, end).trim() + "...";
    }

    private String shapeOutput(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return EMPTY_REPLY_FALLBACK;
        }
        String structured = maybeFormatWallText(trimmed);
        String capped = limit(structured, OUTPUT_MAX);
        return capped.isBlank() ? EMPTY_REPLY_FALLBACK : capped;
    }

    private String maybeFormatWallText(String text) {
        if (text.length() <= 600 || text.contains("\n")) {
            return text;
        }
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length < 3) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (String sentence : sentences) {
            String s = sentence == null ? "" : sentence.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("- ").append(s);
        }
        return sb.length() == 0 ? text : sb.toString();
    }

    private String fallbackForException(Exception ex) {
        String message = ex == null ? "" : normalize(ex.getMessage()).toUpperCase();
        if (message.contains("401") || message.contains("403")
                || message.contains("UNAUTHENTICATED") || message.contains("PERMISSION_DENIED")) {
            return AUTH_FALLBACK;
        }
        if (message.contains("404") || message.contains("NOT_FOUND")) {
            return MODEL_FALLBACK;
        }
        if (message.contains("429") || message.contains("RESOURCE_EXHAUSTED")
                || message.contains("QUOTA") || message.contains("RATE")) {
            return RATE_LIMIT_FALLBACK;
        }
        return FALLBACK_TEXT;
    }

    private record ApiResponse(int statusCode, String body) {
    }

    private record GeminiAttempt(String model, String text) {
    }
}

