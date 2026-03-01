package Services.api;

import Models.OnboardingPlan;
import Utils.api.ApiClient;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PlanApiService {

    // GET /api/plans
    public String getPlansJson() throws Exception {
        return ApiClient.get("/api/plans");
    }

    // POST /api/plans  (admin/recruiter only)
    public String createPlanJson(OnboardingPlan plan) throws Exception {
        LocalDate deadline = new Date(plan.getDeadline().getTime()).toLocalDate();

        String body = "{"
                + "\"userId\":" + plan.getUserId() + ","
                + "\"status\":\"" + toApiStatus(plan.getStatus()) + "\","
                + "\"deadline\":\"" + deadline + "\""
                + "}";

        return ApiClient.post("/api/plans", body);
    }

    // PATCH /api/plans/{id}/status  (candidate allowed only for own plan)
    public void updateStatus(int planId, String status) throws Exception {
        String body = "{ \"status\":\"" + toApiStatus(status) + "\" }";
        ApiClient.patch("/api/plans/" + planId + "/status", body);
    }

    // important: backend enums are usually snake_case
    private String toApiStatus(String uiStatus) {
        if (uiStatus == null) return "pending";
        String s = uiStatus.trim().toLowerCase();
        return switch (s) {
            case "in progress" -> "in_progress";
            case "completed" -> "completed";
            case "on hold" -> "on_hold";
            default -> "pending";
        };
    }
    public byte[] getPlanQrPng(int planId) throws Exception {
        return ApiClient.getBytes("/api/plans/" + planId + "/qr.png");
    }
}