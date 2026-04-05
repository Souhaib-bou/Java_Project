package com.hirely.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "onboardingplan")
@Access(AccessType.FIELD)
public class OnboardingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "planId")
    private Integer planId;

    @Column(name = "user_id")
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PlanStatus status;

    @Column(name = "deadline")
    private LocalDate deadline;
    @Column(name = "qr_token")
    private String qrToken;

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }

    public Integer getPlanId() { return planId; }
    public void setPlanId(Integer planId) { this.planId = planId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public PlanStatus getStatus() { return status; }
    public void setStatus(PlanStatus status) { this.status = status; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
}