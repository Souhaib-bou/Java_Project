package com.hirely.backend.dto;

import com.hirely.backend.model.PlanStatus;

public class PlanStatusUpdateRequest {
    private PlanStatus status;

    public PlanStatus getStatus() { return status; }
    public void setStatus(PlanStatus status) { this.status = status; }
}