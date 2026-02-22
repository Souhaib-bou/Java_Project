package util;

public final class ModerationService {
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REJECTED = "REJECTED";

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
        String apiKey = System.getenv("PERSPECTIVE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return ModerationResult.fallbackPending();
        }

        try {
            double toxicity = perspectiveClient.analyzeToxicity(text, apiKey);
            return new ModerationResult(statusFromScore(toxicity), toxicity, false);
        } catch (Exception ignored) {
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
