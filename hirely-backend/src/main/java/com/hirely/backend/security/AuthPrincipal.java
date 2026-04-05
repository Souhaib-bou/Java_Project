package com.hirely.backend.security;

public class AuthPrincipal {
    private final Integer userId;
    private final Integer roleId;

    public AuthPrincipal(Integer userId, Integer roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public Integer getUserId() { return userId; }
    public Integer getRoleId() { return roleId; }
}