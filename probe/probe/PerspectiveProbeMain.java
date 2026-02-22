package probe;

import util.ModerationService;
import util.PerspectiveClient;

import java.util.List;

public final class PerspectiveProbeMain {
    // Set your key here for this probe runner.
    private static final String API_KEY = "AIzaSyBOoSB89x4BWPwjI4gR7gSKcJl87nIvTHQ";

    private static final double APPROVE_THRESHOLD = 0.20;
    private static final double REJECT_THRESHOLD = 0.80;

    private record Sample(String group, String text) {
    }

    private static final List<Sample> SAMPLES = List.of(
            new Sample("NEGATIVE", "I hate you"),
            new Sample("NEGATIVE", "You are disgusting and worthless"),
            new Sample("NEGATIVE", "Go die"),
            new Sample("NEGATIVE", "Nobody likes you"),
            new Sample("NEGATIVE", "You are a complete idiot"),
            new Sample("NEGATIVE", "I will kill you"),
            new Sample("NEGATIVE", "You are trash"),
            new Sample("POSITIVE", "I like this post"),
            new Sample("POSITIVE", "Great job, thanks for sharing"),
            new Sample("POSITIVE", "This explanation helped me a lot"),
            new Sample("POSITIVE", "I appreciate your effort"),
            new Sample("POSITIVE", "Wonderful work, keep going"),
            new Sample("NEUTRAL", "Can you explain this step?"),
            new Sample("NEUTRAL", "I disagree with your point"),
            new Sample("NEUTRAL", "This is not correct in my opinion"));

    private PerspectiveProbeMain() {
    }

    public static void main(String[] args) {
        String key = API_KEY == null ? "" : API_KEY.trim();
        if (key.isBlank()) {
            System.err.println("PerspectiveProbeMain: set API_KEY in this file before running.");
            return;
        }

        PerspectiveClient perspectiveClient = new PerspectiveClient();
        ModerationService moderationService = new ModerationService();

        System.out.println("=== Perspective Phrase Probe ===");
        System.out.println("Samples: " + SAMPLES.size());
        System.out.println();

        int idx = 1;
        for (Sample sample : SAMPLES) {
            String linePrefix = String.format("%02d", idx);
            try {
                double toxicity = perspectiveClient.analyzeToxicity(sample.text(), key);
                String apiDecision = statusFromScore(toxicity);
                ModerationService.ModerationResult appDecision = moderationService.decideStatus(sample.text());

                System.out.println(linePrefix + " [" + sample.group() + "] " + quote(sample.text()));
                System.out.printf("   toxicity=%.4f | apiDecision=%s | appDecision=%s%n",
                        toxicity,
                        apiDecision,
                        appDecision.getStatus());
            } catch (Exception ex) {
                System.out.println(linePrefix + " [" + sample.group() + "] " + quote(sample.text()));
                System.out.println("   ERROR: " + ex.getMessage());
            }
            idx++;
        }
    }

    private static String statusFromScore(double toxicity) {
        if (toxicity <= APPROVE_THRESHOLD) {
            return ModerationService.STATUS_APPROVED;
        }
        if (toxicity >= REJECT_THRESHOLD) {
            return ModerationService.STATUS_REJECTED;
        }
        return ModerationService.STATUS_PENDING;
    }

    private static String quote(String text) {
        return "\"" + text + "\"";
    }
}
