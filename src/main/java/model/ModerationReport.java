package model;

import java.util.ArrayList;
import java.util.List;

public class ModerationReport {
    private double toxicity;
    private double relevance;
    private String predictedCategory;
    private double duplicateSimilarity;
    private double duplicateScore;
    private Long duplicateOfPostId;
    private double qualityScore;
    private String decision;
    private List<String> reasons = new ArrayList<>();
    private String perspectiveRaw;
    private String pythonRaw;
    private long perspectiveLatencyMs;
    private long pythonLatencyMs;
    private long totalLatencyMs;
    private boolean fallbackUsed;

    public double getToxicity() {
        return toxicity;
    }

    public void setToxicity(double toxicity) {
        this.toxicity = toxicity;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }

    public String getPredictedCategory() {
        return predictedCategory;
    }

    public void setPredictedCategory(String predictedCategory) {
        this.predictedCategory = predictedCategory;
    }

    public String getCategory() {
        return getPredictedCategory();
    }

    public void setCategory(String category) {
        setPredictedCategory(category);
    }

    public double getDuplicateSimilarity() {
        return duplicateScore;
    }

    public void setDuplicateSimilarity(double duplicateSimilarity) {
        this.duplicateSimilarity = duplicateSimilarity;
        this.duplicateScore = duplicateSimilarity;
    }

    public double getDuplicateScore() {
        return duplicateScore;
    }

    public void setDuplicateScore(double duplicateScore) {
        this.duplicateScore = duplicateScore;
        this.duplicateSimilarity = duplicateScore;
    }

    public Long getDuplicateOfPostId() {
        return duplicateOfPostId;
    }

    public void setDuplicateOfPostId(Long duplicateOfPostId) {
        this.duplicateOfPostId = duplicateOfPostId;
    }

    public double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons == null ? new ArrayList<>() : reasons;
    }

    public String getPerspectiveRaw() {
        return perspectiveRaw;
    }

    public void setPerspectiveRaw(String perspectiveRaw) {
        this.perspectiveRaw = perspectiveRaw;
    }

    public String getPythonRaw() {
        return pythonRaw;
    }

    public void setPythonRaw(String pythonRaw) {
        this.pythonRaw = pythonRaw;
    }

    public long getPerspectiveLatencyMs() {
        return perspectiveLatencyMs;
    }

    public void setPerspectiveLatencyMs(long perspectiveLatencyMs) {
        this.perspectiveLatencyMs = perspectiveLatencyMs;
    }

    public long getPythonLatencyMs() {
        return pythonLatencyMs;
    }

    public void setPythonLatencyMs(long pythonLatencyMs) {
        this.pythonLatencyMs = pythonLatencyMs;
    }

    public long getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public void setTotalLatencyMs(long totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }
}
