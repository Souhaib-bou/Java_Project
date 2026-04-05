package com.hirely.backend.service;

import com.hirely.backend.model.OnboardingTask;
import com.hirely.backend.model.TaskStatus;
import com.hirely.backend.repo.OnboardingTaskRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OnboardingTaskService {

    private final OnboardingTaskRepo taskRepo;

    public OnboardingTaskService(OnboardingTaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }

    // Admin/Recruiter/Candidate (candidate will be restricted by controller using ownership)
    public List<OnboardingTask> getTasksByPlanId(Integer planId) {
        return taskRepo.findByPlanId(planId);
    }

    // Admin/Recruiter: create or full update
    public OnboardingTask saveTask(OnboardingTask task) {
        return taskRepo.save(task);
    }

    public void deleteTask(Integer taskId) {
        taskRepo.deleteById(taskId);
    }

    public OnboardingTask getById(Integer taskId) {
        return taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
    }

    // Candidate: update only status + filePath (ownership check happens in controller/security step)
    public OnboardingTask candidateUpdate(Integer taskId, TaskStatus status, String filePath) {
        OnboardingTask task = getById(taskId);
        if (status != null) task.setStatus(status);
        if (filePath != null) task.setFilePath(filePath);
        return taskRepo.save(task);
    }
}