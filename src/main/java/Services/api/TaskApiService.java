package Services.api;

import Models.OnboardingTask;
import Utils.api.ApiClient;

import java.io.File;
import java.util.Objects;

public class TaskApiService {

    // GET /api/plans/{planId}/tasks
    public String getTasksJson(int planId) throws Exception {
        return ApiClient.get("/api/plans/" + planId + "/tasks");
    }

    // POST /api/plans/{planId}/tasks (admin/recruiter)
    public String createTaskJson(int planId, OnboardingTask task) throws Exception {
        String body = "{"
                + "\"title\":\"" + esc(task.getTitle()) + "\","
                + "\"description\":\"" + esc(nz(task.getDescription())) + "\","
                + "\"status\":\"" + toApiStatus(task.getStatus()) + "\","
                + "\"filePath\":" + jsonNullableString(task.getFilepath())
                + "}";

        return ApiClient.post("/api/plans/" + planId + "/tasks", body);
    }

    // PUT /api/tasks/{taskId} (admin/recruiter)
    public String updateTaskJson(int taskId, OnboardingTask task) throws Exception {
        String body = "{"
                + "\"planId\":" + task.getPlanId() + ","
                + "\"title\":\"" + esc(task.getTitle()) + "\","
                + "\"description\":\"" + esc(nz(task.getDescription())) + "\","
                + "\"status\":\"" + toApiStatus(task.getStatus()) + "\","
                + "\"filePath\":" + jsonNullableString(task.getFilepath())
                + "}";

        return ApiClient.put("/api/tasks/" + taskId, body);
    }

    // DELETE /api/tasks/{taskId} (admin/recruiter)
    public void deleteTask(int taskId) throws Exception {
        ApiClient.delete("/api/tasks/" + taskId);
    }

    // PATCH /api/tasks/{taskId}/candidate  (candidate)
    public void candidateUpdate(int taskId, String statusOrUiStatus, String filepath) throws Exception {
        String body = "{"
                + "\"status\":\"" + toApiStatus(statusOrUiStatus) + "\","
                + "\"filePath\":" + jsonNullableString(filepath)
                + "}";

        ApiClient.patch("/api/tasks/" + taskId + "/candidate", body);
    }

    // NEW: POST /api/tasks/{taskId}/file (multipart)
    public String uploadTaskFile(int taskId, File file) throws Exception {
        return ApiClient.multipartPostFile("/api/tasks/" + taskId + "/file", "file", file);
    }

    // UI -> API enum (snake_case)
    private String toApiStatus(String uiStatus) {
        if (uiStatus == null) return "not_started";
        String s = uiStatus.trim().toLowerCase();

        return switch (s) {
            case "not started", "not_started" -> "not_started";
            case "in progress", "in_progress" -> "in_progress";
            case "completed" -> "completed";
            case "blocked" -> "blocked";
            case "on hold", "on_hold" -> "on_hold";
            default -> "not_started";
        };
    }

    private String nz(String v) { return v == null ? "" : v; }

    private String jsonNullableString(String v) {
        if (v == null) return "null";
        String t = v.trim();
        if (t.isEmpty()) return "null";
        return "\"" + esc(t) + "\"";
    }

    private String esc(String s) {
        return Objects.toString(s, "")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
    public void removeTaskFile(int taskId) throws Exception {
        ApiClient.delete("/api/tasks/" + taskId + "/file");
    }
}