package Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true) // ✅ ignore extra fields like "deadline"
public class OnboardingTask {

    private int taskId;
    private int planId;
    private String title;
    private String description;
    private String status;

    @JsonProperty("filePath")
    private String filepath;

    @JsonProperty("originalFileName")
    private String originalFileName;

    @JsonProperty("contentType")
    private String contentType;

    @JsonProperty("cloudinaryPublicId")
    private String cloudinaryPublicId;
    public OnboardingTask() {}

    public OnboardingTask(int taskId, int planId, String title,
                          String description, String status, String filepath) {
        this.taskId = taskId;
        this.planId = planId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.filepath = filepath;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getCloudinaryPublicId() { return cloudinaryPublicId; }
    public void setCloudinaryPublicId(String cloudinaryPublicId) { this.cloudinaryPublicId = cloudinaryPublicId; }

    @Override
    public String toString() {
        return "OnboardingTask{" +
                "taskId=" + taskId +
                ", planId=" + planId +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}