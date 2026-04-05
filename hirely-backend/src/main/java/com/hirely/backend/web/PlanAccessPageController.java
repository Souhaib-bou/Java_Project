package com.hirely.backend.web;

import com.hirely.backend.model.OnboardingPlan;
import com.hirely.backend.model.OnboardingTask;
import com.hirely.backend.service.OnboardingPlanService;
import com.hirely.backend.service.OnboardingTaskService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PlanAccessPageController {

    private final OnboardingPlanService planService;
    private final OnboardingTaskService taskService;

    public PlanAccessPageController(OnboardingPlanService planService, OnboardingTaskService taskService) {
        this.planService = planService;
        this.taskService = taskService;
    }

    @GetMapping(value = "/plan-access/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public String planAccessHtml(@PathVariable String token) {
        OnboardingPlan plan = planService.getByQrToken(token);
        List<OnboardingTask> tasks = taskService.getTasksByPlanId(plan.getPlanId());

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='utf-8'/>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1'/>")
                .append("<title>Plan Access</title>")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;margin:16px;background:#f6f7fb;}")
                .append(".card{background:#fff;border:1px solid #e8eaf2;border-radius:12px;padding:14px;margin-bottom:12px;}")
                .append("h2{margin:0 0 8px 0;font-size:18px;}")
                .append(".muted{color:#667085;font-size:13px;}")
                .append("table{width:100%;border-collapse:collapse;}")
                .append("td,th{padding:10px;border-bottom:1px solid #eef0f7;text-align:left;font-size:14px;}")
                .append("</style></head><body>");

        sb.append("<div class='card'>")
                .append("<h2>Onboarding Plan #").append(plan.getPlanId()).append("</h2>")
                .append("<div class='muted'>User ID: ").append(plan.getUserId()).append("</div>")
                .append("<div class='muted'>Status: ").append(plan.getStatus()).append("</div>")
                .append("<div class='muted'>Deadline: ").append(plan.getDeadline() == null ? "—" : plan.getDeadline()).append("</div>")
                .append("</div>");

        sb.append("<div class='card'>")
                .append("<h2>Tasks</h2>");

        if (tasks.isEmpty()) {
            sb.append("<div class='muted'>No tasks for this plan.</div>");
        } else {
            sb.append("<table><thead><tr>")
                    .append("<th>ID</th><th>Title</th><th>Status</th><th>Attachment</th>")
                    .append("</tr></thead><tbody>");

            for (OnboardingTask t : tasks) {
                String link = t.getFilePath();
                String attach = (link == null || link.isBlank())
                        ? "—"
                        : "<a href='" + escapeHtml(link) + "' target='_blank'>Open</a>";

                sb.append("<tr>")
                        .append("<td>").append(t.getTaskId()).append("</td>")
                        .append("<td>").append(escapeHtml(nullToEmpty(t.getTitle()))).append("</td>")
                        .append("<td>").append(escapeHtml(String.valueOf(t.getStatus()))).append("</td>")
                        .append("<td>").append(attach).append("</td>")
                        .append("</tr>");
            }

            sb.append("</tbody></table>");
        }

        sb.append("</div></body></html>");
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}