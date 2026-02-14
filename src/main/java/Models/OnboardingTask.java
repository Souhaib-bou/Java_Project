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
