import util.Secrets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ConnectivityCheck {
    public static void main(String[] args) {
        String[] MODELS = {
                "gemini-2.5-pro",
                "gemini-2.5-flash",
                "gemini-2.0-flash",
                "gemini-1.5-pro",
                "gemini-1.5-flash-8b"
        };

        System.out.println("Verifying API Key: " + Secrets.GEMINI_API_KEY.substring(0, 8) + "...");

        for (String model : MODELS) {
            System.out.println("\nTesting model: " + model);
            try {
                String result = sendTestRequest(model);
                System.out.println("RESULT: SUCCESS!");
                System.out.println("Output Preview: " + result.substring(0, Math.min(100, result.length())));
                break; // Stop at first working model
            } catch (Exception e) {
                System.out.println("RESULT: FAILED - " + e.getMessage());
            }
        }
    }

    private static String sendTestRequest(String model) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key="
                + Secrets.GEMINI_API_KEY;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        String jsonBody = "{\"contents\": [{\"parts\":[{\"text\": \"Hi\"}]}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }
}
