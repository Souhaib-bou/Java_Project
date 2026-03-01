package Models;

public class TaskRecommendation {

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    public enum ActionType {
        SELECT_TASK,
        OPEN_UPDATE,
        REFRESH
    }

    private final Priority priority;
    private final String type;
    private final String message;
    private final String reason;
    private final Integer taskId;     // nullable
    private final String actionLabel;
    private final ActionType actionType;
    private final int score;

    public TaskRecommendation(Priority priority,
                              String type,
                              String message,
                              String reason,
                              Integer taskId,
                              String actionLabel,
                              ActionType actionType,
                              int score) {
        this.priority = priority;
        this.type = type;
        this.message = message;
        this.reason = reason;
        this.taskId = taskId;
        this.actionLabel = actionLabel;
        this.actionType = actionType;
        this.score = score;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getReason() {
        return reason;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public int getScore() {
        return score;
    }
}