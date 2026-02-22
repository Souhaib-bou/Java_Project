package util;

public final class ModerationService {
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REJECTED = "REJECTED";

    // Optional local key fallback.
    private static final String API_KEY = "AIzaSyBOoSB89x4BWPwjI4gR7gSKcJl87nIvTHQ";

    private static final double APPROVE_THRESHOLD = 0.20;
    private static final double REJECT_THRESHOLD = 0.80;

    private final PerspectiveClient perspectiveClient;

    public ModerationService() {
        this(new PerspectiveClient());
    }

    public ModerationService(PerspectiveClient perspectiveClient) {
        this.perspectiveClient = perspectiveClient;
    }

    public ModerationResult decideStatus(String text) {
        String apiKey = API_KEY == null ? "" : API_KEY.trim();
        if (apiKey == null || apiKey.isBlank()) {
            DebugLog.info("Moderation", "API key is missing; falling back to PENDING");
            return ModerationResult.fallbackPending();
        }

        try {
            double toxicity = perspectiveClient.analyzeToxicity(text, apiKey);
            String status = statusFromScore(toxicity);
            DebugLog.info("Moderation", "Toxicity=" + toxicity + " => status=" + status);
            return new ModerationResult(status, toxicity, false);
        } catch (Exception ex) {
            DebugLog.error("Moderation", "Perspective call failed; falling back to PENDING", ex);
            return ModerationResult.fallbackPending();
        }
    }

    private String statusFromScore(double toxicity) {
        if (toxicity <= APPROVE_THRESHOLD) {
            return STATUS_APPROVED;
        }
        if (toxicity >= REJECT_THRESHOLD) {
            return STATUS_REJECTED;
        }
        return STATUS_PENDING;
    }

    public static final class ModerationResult {
        private final String status;
        private final double toxicity;
        private final boolean usedFallback;

        public ModerationResult(String status, double toxicity, boolean usedFallback) {
            this.status = status;
            this.toxicity = toxicity;
            this.usedFallback = usedFallback;
        }

        public String getStatus() {
            return status;
        }

        public double getToxicity() {
            return toxicity;
        }

        public boolean isUsedFallback() {
            return usedFallback;
        }

        public static ModerationResult fallbackPending() {
            return new ModerationResult(STATUS_PENDING, -1.0, true);
        }
    }
}
