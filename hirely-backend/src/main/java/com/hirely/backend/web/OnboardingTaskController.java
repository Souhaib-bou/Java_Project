package com.hirely.backend.web;

import com.hirely.backend.dto.CandidateTaskUpdateRequest;
import com.hirely.backend.model.OnboardingTask;
import com.hirely.backend.security.CurrentUser;
import com.hirely.backend.security.CurrentUserProvider;
import com.hirely.backend.service.CloudinaryService;
import com.hirely.backend.service.OnboardingTaskService;
import com.hirely.backend.service.OwnershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OnboardingTaskController {

    private final OnboardingTaskService taskService;
    private final OwnershipService ownershipService;
    private final CurrentUserProvider currentUserProvider;
    private final CloudinaryService cloudinaryService;

    public OnboardingTaskController(OnboardingTaskService taskService,
                                    OwnershipService ownershipService,
                                    CurrentUserProvider currentUserProvider,
                                    CloudinaryService cloudinaryService) {
        this.taskService = taskService;
        this.ownershipService = ownershipService;
        this.currentUserProvider = currentUserProvider;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/plans/{planId}/tasks")
    public List<OnboardingTask> getTasks(@PathVariable Integer planId) {
        CurrentUser cu = currentUserProvider.get();

        if (cu.isCandidate() && !ownershipService.candidateOwnsPlan(cu.getUserId(), planId)) {
            throw new RuntimeException("Forbidden");
        }

        return taskService.getTasksByPlanId(planId);
    }

    @PostMapping("/plans/{planId}/tasks")
    public OnboardingTask createTask(@PathVariable Integer planId, @RequestBody OnboardingTask task) {
        CurrentUser cu = currentUserProvider.get();
        if (!cu.isAdminOrRecruiter()) throw new RuntimeException("Forbidden");

        task.setPlanId(planId);
        return taskService.saveTask(task);
    }

    @PutMapping("/tasks/{taskId}")
    public OnboardingTask updateTask(@PathVariable Integer taskId, @RequestBody OnboardingTask task) {
        CurrentUser cu = currentUserProvider.get();
        if (!cu.isAdminOrRecruiter()) throw new RuntimeException("Forbidden");

        task.setTaskId(taskId);
        return taskService.saveTask(task);
    }

    @PatchMapping("/tasks/{taskId}/candidate")
    public OnboardingTask candidateUpdate(@PathVariable Integer taskId,
                                          @RequestBody CandidateTaskUpdateRequest req) {
        CurrentUser cu = currentUserProvider.get();

        if (!cu.isCandidate()) throw new RuntimeException("Forbidden");
        if (!ownershipService.candidateOwnsTask(cu.getUserId(), taskId)) throw new RuntimeException("Forbidden");

        return taskService.candidateUpdate(taskId, req.getStatus(), req.getFilePath());
    }

    /**
     * NEW: Upload file to Cloudinary then save URL + Cloudinary metadata in DB.
     * POST /api/tasks/{taskId}/file (multipart/form-data, field "file")
     */
    @PostMapping(value = "/tasks/{taskId}/file", consumes = "multipart/form-data")
    public ResponseEntity<OnboardingTask> uploadTaskFile(@PathVariable Integer taskId,
                                                         @RequestParam("file") MultipartFile file) {
        CurrentUser cu = currentUserProvider.get();

        // Candidate: must own task. Admin/Recruiter: allowed.
        if (cu.isCandidate() && !ownershipService.candidateOwnsTask(cu.getUserId(), taskId)) {
            throw new RuntimeException("Forbidden");
        }
        if (!cu.isCandidate() && !cu.isAdminOrRecruiter()) {
            throw new RuntimeException("Forbidden");
        }

        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            // 1) Load task (your service method)
            OnboardingTask task = taskService.getById(taskId);

            // 2) Upload to Cloudinary
            CloudinaryService.UploadResult up = cloudinaryService.uploadTaskFile(file, taskId);

            // 3) Save in DB
            task.setFilePath(up.secureUrl());
            task.setCloudinaryPublicId(up.publicId());
            task.setOriginalFileName(up.originalFileName());
            task.setContentType(up.contentType());

            OnboardingTask saved = taskService.saveTask(task);

            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/tasks/{taskId}/file")
    public ResponseEntity<?> removeTaskFile(@PathVariable Integer taskId) {
        CurrentUser cu = currentUserProvider.get();

        if (cu.isCandidate() && !ownershipService.candidateOwnsTask(cu.getUserId(), taskId)) {
            throw new RuntimeException("Forbidden");
        }
        if (!cu.isCandidate() && !cu.isAdminOrRecruiter()) {
            throw new RuntimeException("Forbidden");
        }

        try {
            OnboardingTask task = taskService.getById(taskId);

            String publicId = task.getCloudinaryPublicId();
            if (publicId != null && !publicId.isBlank()) {
                cloudinaryService.deleteByPublicId(publicId);
            }

            task.setFilePath(null);
            task.setCloudinaryPublicId(null);
            task.setOriginalFileName(null);
            task.setContentType(null);

            taskService.saveTask(task);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Remove attachment failed: " + e.getMessage());
        }
    }
}