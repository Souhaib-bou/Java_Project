import util.Secrets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GeminiPro: A smart Java client that prioritizes the "Most Powerful" free
 * models
 * and automatically cascades down to lighter models when quotas are hit.
 */
public class GeminiPro {

    // --- Model Hierarchy (Highest Power to Most Lightweight) ---
    // Note: Pro models are most powerful, Flash models are fastest.
    private static final String[] MODELS = {
            "gemini-2.5-pro", // Ultra-flagship (Scan confirmed available)
            "gemini-2.5-flash", // Fast flagship (Scan confirmed available)
            "gemini-2.0-flash", // 2.0 series
            "gemini-1.5-pro", // 1.5 Pro
            "gemini-1.5-flash-8b" // Lightweight fallback
    };

    private static int currentModelIndex = 0;

    // --- Aesthetics ---
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String PURPLE = "\u001B[35m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";

    public static void main(String[] args) {
        displayHeader();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\n" + CYAN + BOLD + "You: " + RESET);
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println(PURPLE + "Goodbye! ✨" + RESET);
                break;
            }

            if (input.trim().isEmpty())
                continue;

            try {
                processRequest(input);
            } catch (Exception e) {
                System.out.println("\n" + RED + "Critical Failure: " + e.getMessage() + RESET);
            }
        }
        scanner.close();
    }

    private static void processRequest(String prompt) throws Exception {
        boolean success = false;

        while (!success && currentModelIndex < MODELS.length) {
            String activeModel = MODELS[currentModelIndex];
            System.out.print(YELLOW + "Gemini (" + activeModel + ") is thinking..." + RESET);

            try {
                String responseBody = sendRequest(prompt, activeModel);
                String text = extractText(responseBody);

                // Clear the "thinking" line
                System.out.print("\r" + " ".repeat(60) + "\r");
                System.out.println(GREEN + BOLD + "Gemini [" + activeModel + "]: " + RESET);
                displayFormattedText(text);
                success = true;

            } catch (QuotaExceededException e) {
                currentModelIndex++;
                if (currentModelIndex < MODELS.length) {
                    System.out.print("\r" + RED + "Quota/Limit reached for " + activeModel + ". Switching to "
                            + MODELS[currentModelIndex] + "..." + RESET + "\n");
                } else {
                    System.out.println(
                            "\r" + RED + "All models have exhausted their quota. Please try again later." + RESET);
                    throw new RuntimeException("No available models.");
                }
            } catch (Exception e) {
                System.out.println("\n" + RED + "API Error: " + e.getMessage() + RESET);
                break;
            }
        }
    }

    private static String sendRequest(String prompt, String model) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key="
                + Secrets.GEMINI_API_KEY;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15)) // Increased timeout for Pro models
                .build();

        // Simple JSON escaping for safety
        String escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String jsonBody = "{\"contents\": [{\"parts\":[{\"text\": \"" + escapedPrompt + "\"}]}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 429 is the standard code for Rate Limit / Quota Exceeded
        if (response.statusCode() == 429) {
            throw new QuotaExceededException();
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("API error (" + response.statusCode() + "): " + response.body());
        }

        return response.body();
    }

    private static String extractText(String json) {
        // More robust pattern to handle potential variations in JSON response
        Pattern pattern = Pattern.compile("\"text\":\\s*\"(.*?)\"([\\s,}]|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String result = matcher.group(1);
            // Basic unescaping
            return result.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return "No text found in response.";
    }

    private static void displayFormattedText(String text) {
        System.out.println("--------------------------------------------------");
        System.out.println(text);
        System.out.println("--------------------------------------------------");
    }

    private static void displayHeader() {
        System.out.println(PURPLE + BOLD + "========================================" + RESET);
        System.out.println(PURPLE + BOLD + "     GEMINI AI: MOST POWERFUL FREE      " + RESET);
        System.out.println(PURPLE + BOLD + "========================================" + RESET);
        System.out.println(YELLOW + "Starting with: " + MODELS[0] + " (Most Powerful)" + RESET);
        System.out.println(YELLOW + "Type 'exit' to quit." + RESET);
    }

    private static class QuotaExceededException extends Exception {
    }
}
