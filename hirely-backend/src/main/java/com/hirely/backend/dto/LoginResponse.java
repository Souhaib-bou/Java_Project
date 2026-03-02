package com.hirely.backend.dto;

public class LoginResponse {
    private String token;
    private Integer userId;
    private Integer roleId;

    public LoginResponse(String token, Integer userId, Integer roleId) {
        this.token = token;
        this.userId = userId;
        this.roleId = roleId;
    }

    public String getToken() { return token; }
    public Integer getUserId() { return userId; }
    public Integer getRoleId() { return roleId; }
}