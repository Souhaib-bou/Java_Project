package Models;

public class OnboardingTask {

    private int taskId;
    private int planId;
    private String title;
    private String description;
    private String status;
    private String filepath;

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

    /**
     * Returns the taskid value.
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * Sets the taskid value.
     */
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    /**
     * Returns the planid value.
     */
    public int getPlanId() {
        return planId;
    }

    /**
     * Sets the planid value.
     */
    public void setPlanId(int planId) {
        this.planId = planId;
    }

    /**
     * Returns the title value.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title value.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the description value.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description value.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the status value.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status value.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the filepath value.
     */
    public String getFilepath() {
        return filepath;
    }

    /**
     * Sets the filepath value.
     */
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    @Override
    /**
     * Executes this operation.
     */
    public String toString() {
        return "OnboardingTask{" +
                "taskId=" + taskId +
                ", planId=" + planId +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
