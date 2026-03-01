package Utils.api;

import Utils.UserSession;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;

public class ApiClient {

    private static final String BASE_URL = "http://localhost:8081";

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // JSON builder (unchanged behavior)
    private static HttpRequest.Builder baseJson(String endpoint) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8");

        String token = UserSession.getInstance().getToken();
        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }
        return b;
    }

    // Non-JSON builder (for multipart etc.)
    private static HttpRequest.Builder baseNoContentType(String endpoint) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .timeout(Duration.ofSeconds(60)); // uploads can take longer

        String token = UserSession.getInstance().getToken();
        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }
        return b;
    }

    public static String get(String endpoint) throws Exception {
        return send(baseJson(endpoint).GET().build());
    }

    public static String post(String endpoint, String json) throws Exception {
        return send(baseJson(endpoint)
                .POST(HttpRequest.BodyPublishers.ofString(json == null ? "" : json, StandardCharsets.UTF_8))
                .build());
    }

    public static String patch(String endpoint, String json) throws Exception {
        return send(baseJson(endpoint)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json == null ? "" : json, StandardCharsets.UTF_8))
                .build());
    }

    public static String put(String endpoint, String json) throws Exception {
        return send(baseJson(endpoint)
                .PUT(HttpRequest.BodyPublishers.ofString(json == null ? "" : json, StandardCharsets.UTF_8))
                .build());
    }

    public static String delete(String endpoint) throws Exception {
        return send(baseJson(endpoint).DELETE().build());
    }

    /**
     * NEW: Multipart upload (single file field).
     * Example usage:
     * ApiClient.multipartPostFile("/api/tasks/27/file", "file", selectedFile)
     */
    public static String multipartPostFile(String endpoint, String fieldName, File file) throws Exception {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File invalid: " + (file == null ? "null" : file.getAbsolutePath()));
        }

        String boundary = "----HirelyBoundary" + UUID.randomUUID().toString().replace("-", "");
        String CRLF = "\r\n";

        String filename = file.getName().replace("\"", "_");
        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";

        byte[] fileBytes = Files.readAllBytes(file.toPath());

        // Build multipart body as bytes
        // --boundary
        // Content-Disposition: form-data; name="file"; filename="x.pdf"
        // Content-Type: application/pdf
        //
        // <bytes>
        // --boundary--
        byte[] partHeader = (
                "--" + boundary + CRLF +
                        "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"" + CRLF +
                        "Content-Type: " + contentType + CRLF +
                        CRLF
        ).getBytes(StandardCharsets.UTF_8);

        byte[] partFooter = (CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[partHeader.length + fileBytes.length + partFooter.length];
        System.arraycopy(partHeader, 0, body, 0, partHeader.length);
        System.arraycopy(fileBytes, 0, body, partHeader.length, fileBytes.length);
        System.arraycopy(partFooter, 0, body, partHeader.length + fileBytes.length, partFooter.length);

        HttpRequest req = baseNoContentType(endpoint)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        return send(req);
    }

    private static String send(HttpRequest req) throws Exception {
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int code = res.statusCode();
        String body = res.body();

        if (code >= 200 && code < 300) return body;

        if (code == 401) {
            throw new RuntimeException("HTTP 401 (Unauthorized) -> You must login again.");
        }
        if (code == 403) {
            throw new RuntimeException("HTTP 403 (Forbidden) -> You are not allowed to do this action.");
        }

        throw new RuntimeException("HTTP " + code + " -> " + (body == null ? "" : body));
    }
    public static byte[] getBytes(String endpoint) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .timeout(Duration.ofSeconds(30))
                // do NOT force Content-Type application/json here
                .header("Accept", "image/png")
                .build();

        // attach Authorization like your baseJson does:
        String token = UserSession.getInstance().getToken();
        HttpRequest.Builder b = HttpRequest.newBuilder(req.uri())
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "image/png");

        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }

        HttpResponse<byte[]> res = client.send(b.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        int code = res.statusCode();

        if (code >= 200 && code < 300) return res.body();

        if (code == 401) throw new RuntimeException("HTTP 401 (Unauthorized) -> You must login again.");
        if (code == 403) throw new RuntimeException("HTTP 403 (Forbidden) -> You are not allowed to do this action.");

        throw new RuntimeException("HTTP " + code + " -> Failed to load bytes");
    }
}