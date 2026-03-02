package com.hirely.backend.service;

import com.hirely.backend.model.OnboardingPlan;
import com.hirely.backend.model.PlanStatus;
import com.hirely.backend.repo.OnboardingPlanRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OnboardingPlanService {

    private final OnboardingPlanRepo planRepo;

    public OnboardingPlanService(OnboardingPlanRepo planRepo) {
        this.planRepo = planRepo;
    }

    // Admin/Recruiter: all plans
    public List<OnboardingPlan> getAllPlans() {
        return planRepo.findAll();
    }

    // Candidate: only own plans
    public List<OnboardingPlan> getPlansForCandidate(Integer candidateUserId) {
        return planRepo.findByUserId(candidateUserId);
    }

    // Admin/Recruiter: create or full update
    public OnboardingPlan savePlan(OnboardingPlan plan) {
        return planRepo.save(plan);
    }

    public void deletePlan(Integer planId) {
        planRepo.deleteById(planId);
    }

    public OnboardingPlan getById(Integer planId) {
        return planRepo.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));
    }

    // Candidate: update only status (ownership check happens in controller/security step)
    public OnboardingPlan updatePlanStatus(Integer planId, PlanStatus newStatus) {
        OnboardingPlan plan = getById(planId);
        plan.setStatus(newStatus);
        return planRepo.save(plan);
    }
    public OnboardingPlan getByQrToken(String token) {
        return planRepo.findByQrToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));
    }
}