package com.hirely.backend.service;

import com.hirely.backend.model.OnboardingPlan;
import com.hirely.backend.model.OnboardingTask;
import com.hirely.backend.repo.OnboardingPlanRepo;
import com.hirely.backend.repo.OnboardingTaskRepo;
import org.springframework.stereotype.Service;

@Service
public class OwnershipService {

    private final OnboardingPlanRepo planRepo;
    private final OnboardingTaskRepo taskRepo;

    public OwnershipService(OnboardingPlanRepo planRepo, OnboardingTaskRepo taskRepo) {
        this.planRepo = planRepo;
        this.taskRepo = taskRepo;
    }

    public boolean candidateOwnsPlan(Integer candidateUserId, Integer planId) {
        return planRepo.findById(planId)
                .map(p -> p.getUserId().equals(candidateUserId))
                .orElse(false);
    }

    public boolean candidateOwnsTask(Integer candidateUserId, Integer taskId) {
        return taskRepo.findById(taskId)
                .map(task -> candidateOwnsPlan(candidateUserId, task.getPlanId()))
                .orElse(false);
    }
}