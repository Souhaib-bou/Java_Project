package com.hirely.backend.dto;

import com.hirely.backend.model.TaskStatus;

public class CandidateTaskUpdateRequest {
    private TaskStatus status;
    private String filePath;

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}