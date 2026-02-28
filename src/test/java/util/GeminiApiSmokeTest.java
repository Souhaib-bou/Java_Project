package util;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class GeminiApiSmokeTest {
    @Test
    void generateReply_hitsGeminiApi_whenOptedIn() {
        Assumptions.assumeTrue(
                "1".equals(System.getenv("RUN_GEMINI_SMOKE")),
                "Set RUN_GEMINI_SMOKE=1 to run live Gemini API smoke test"
        );

        GeminiClient client = new GeminiClient();
        String reply = client.generateReply(
                "Backend internship interview prep",
                "I have an interview next week and want to prepare effectively.",
                "@gemini Give me a concise prep plan for a 30-minute technical screen.",
                "Internship"
        );

        assertFalse(reply == null || reply.isBlank(), "Expected non-empty Gemini reply");
        assertFalse(reply.contains("Gemini is unavailable right now"), "Got service fallback instead of a reply");
        assertFalse(reply.contains("Gemini quota/rate limit reached"), "Quota/rate limit prevented request");
        assertFalse(reply.contains("Gemini API key is invalid"), "API key/auth prevented request");
    }
}

