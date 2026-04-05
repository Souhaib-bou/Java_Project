package Models;

<<<<<<< HEAD
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true) // ✅ ignore extra fields like "deadline"
=======
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
public class OnboardingTask {

    private int taskId;
    private int planId;
    private String title;
    private String description;
    private String status;
<<<<<<< HEAD

    @JsonProperty("filePath")
    private String filepath;

    @JsonProperty("originalFileName")
    private String originalFileName;

    @JsonProperty("contentType")
    private String contentType;

    @JsonProperty("cloudinaryPublicId")
    private String cloudinaryPublicId;
=======
    private String filepath;

>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
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

<<<<<<< HEAD
=======
    /**
     * Returns the taskid value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public int getTaskId() {
        return taskId;
    }

<<<<<<< HEAD
=======
    /**
     * Sets the taskid value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

<<<<<<< HEAD
=======
    /**
     * Returns the planid value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public int getPlanId() {
        return planId;
    }

<<<<<<< HEAD
=======
    /**
     * Sets the planid value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public void setPlanId(int planId) {
        this.planId = planId;
    }

<<<<<<< HEAD
=======
    /**
     * Returns the title value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public String getTitle() {
        return title;
    }

<<<<<<< HEAD
=======
    /**
     * Sets the title value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public void setTitle(String title) {
        this.title = title;
    }

<<<<<<< HEAD
=======
    /**
     * Returns the description value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public String getDescription() {
        return description;
    }

<<<<<<< HEAD
=======
    /**
     * Sets the description value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public void setDescription(String description) {
        this.description = description;
    }

<<<<<<< HEAD
=======
    /**
     * Returns the status value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public String getStatus() {
        return status;
    }

<<<<<<< HEAD
=======
    /**
     * Sets the status value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public void setStatus(String status) {
        this.status = status;
    }

<<<<<<< HEAD
=======
    /**
     * Returns the filepath value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public String getFilepath() {
        return filepath;
    }

<<<<<<< HEAD
=======
    /**
     * Sets the filepath value.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

<<<<<<< HEAD
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getCloudinaryPublicId() { return cloudinaryPublicId; }
    public void setCloudinaryPublicId(String cloudinaryPublicId) { this.cloudinaryPublicId = cloudinaryPublicId; }

    @Override
=======
    @Override
    /**
     * Executes this operation.
     */
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
    public String toString() {
        return "OnboardingTask{" +
                "taskId=" + taskId +
                ", planId=" + planId +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 6583a07f403729f05366fbaae91babf1e4568b67
