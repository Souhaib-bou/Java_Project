package com.hirely.backend.web;

import com.hirely.backend.dto.PlanStatusUpdateRequest;
import com.hirely.backend.model.OnboardingPlan;
import com.hirely.backend.model.OnboardingTask;
import com.hirely.backend.security.CurrentUser;
import com.hirely.backend.security.CurrentUserProvider;
import com.hirely.backend.service.OnboardingPlanService;
import com.hirely.backend.service.OnboardingTaskService;
import com.hirely.backend.service.OwnershipService;
import com.hirely.backend.service.PlanQrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class OnboardingPlanController {

    private final OnboardingPlanService planService;
    private final OnboardingTaskService taskService;
    private final OwnershipService ownershipService;
    private final CurrentUserProvider currentUserProvider;
    private final PlanQrService planQrService;

    @Value("${app.publicBaseUrl:http://localhost:8081}")
    private String publicBaseUrl;

    public OnboardingPlanController(OnboardingPlanService planService,
                                    OnboardingTaskService taskService,
                                    OwnershipService ownershipService,
                                    CurrentUserProvider currentUserProvider,
                                    PlanQrService planQrService) {
        this.planService = planService;
        this.taskService = taskService;
        this.ownershipService = ownershipService;
        this.currentUserProvider = currentUserProvider;
        this.planQrService = planQrService;
    }

    @GetMapping
    public List<OnboardingPlan> getPlans() {
        CurrentUser cu = currentUserProvider.get();
        if (cu.isCandidate()) return planService.getPlansForCandidate(cu.getUserId());
        return planService.getAllPlans();
    }

    @GetMapping("/{id}")
    public OnboardingPlan getPlan(@PathVariable Integer id) {
        CurrentUser cu = currentUserProvider.get();
        OnboardingPlan plan = planService.getById(id);

        if (cu.isCandidate() && !plan.getUserId().equals(cu.getUserId())) {
            throw new RuntimeException("Forbidden");
        }
        return plan;
    }

    @PostMapping
    public OnboardingPlan createPlan(@RequestBody OnboardingPlan plan) {
        CurrentUser cu = currentUserProvider.get();
        if (!cu.isAdminOrRecruiter()) throw new RuntimeException("Forbidden");
        return planService.savePlan(plan);
    }

    @PutMapping("/{id}")
    public OnboardingPlan updatePlan(@PathVariable Integer id, @RequestBody OnboardingPlan plan) {
        CurrentUser cu = currentUserProvider.get();
        if (!cu.isAdminOrRecruiter()) throw new RuntimeException("Forbidden");

        plan.setPlanId(id);
        return planService.savePlan(plan);
    }

    @PatchMapping("/{id}/status")
    public OnboardingPlan updateStatus(@PathVariable Integer id,
                                       @RequestBody PlanStatusUpdateRequest req) {
        CurrentUser cu = currentUserProvider.get();
        if (req.getStatus() == null) throw new RuntimeException("status is required");

        if (cu.isCandidate() && !ownershipService.candidateOwnsPlan(cu.getUserId(), id)) {
            throw new RuntimeException("Forbidden");
        }

        return planService.updatePlanStatus(id, req.getStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable Integer id) {
        CurrentUser cu = currentUserProvider.get();
        if (!cu.isAdminOrRecruiter()) throw new RuntimeException("Forbidden");

        planService.deletePlan(id);
        return ResponseEntity.ok().build();
    }

    /**
     * NEW: QR PNG for a plan
     * GET /api/plans/{id}/qr.png
     */
    @GetMapping(value = "/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getPlanQrPng(@PathVariable Integer id) {
        CurrentUser cu = currentUserProvider.get();
        OnboardingPlan plan = planService.getById(id);

        // same access rules as getPlan()
        if (cu.isCandidate() && !plan.getUserId().equals(cu.getUserId())) {
            throw new RuntimeException("Forbidden");
        }

        // Create token if missing
        if (plan.getQrToken() == null || plan.getQrToken().isBlank()) {
            plan.setQrToken(planQrService.newToken());
            planService.savePlan(plan);
        }

        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        String url = base + "/plan-access/" + plan.getQrToken();

        return planQrService.qrPngBytes(url, 240);
    }

    /**
     * NEW: Access plan + tasks via QR token
     * GET /api/plans/access/{token}
     *
     * Minimal variant: token alone grants access to that plan's details and tasks.
     */
    @GetMapping("/access/{token}")
    public PlanAccessResponse accessPlanByToken(@PathVariable String token) {
        OnboardingPlan plan = planService.getByQrToken(token);
        List<OnboardingTask> tasks = taskService.getTasksByPlanId(plan.getPlanId());
        return new PlanAccessResponse(plan, tasks);
    }

    public record PlanAccessResponse(OnboardingPlan plan, List<OnboardingTask> tasks) {}
}