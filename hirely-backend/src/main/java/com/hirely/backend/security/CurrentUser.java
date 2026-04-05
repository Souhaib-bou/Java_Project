package com.hirely.backend.security;

public class CurrentUser {
    private final Integer userId;
    private final AppRole role;

    public CurrentUser(Integer userId, AppRole role) {
        this.userId = userId;
        this.role = role;
    }

    public Integer getUserId() { return userId; }
    public AppRole getRole() { return role; }

    public boolean isAdminOrRecruiter() {
        return role == AppRole.ADMIN || role == AppRole.RECRUITER;
    }

    public boolean isCandidate() {
        return role == AppRole.CANDIDATE;
    }
}