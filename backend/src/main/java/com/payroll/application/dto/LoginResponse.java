package com.payroll.application.dto;

public class LoginResponse {

    private final String token;
    private final String tokenType;
    private final String username;
    private final String role;

    public LoginResponse(
            String token,
            String tokenType,
            String username,
            String role) {

        this.token = token;
        this.tokenType = tokenType;
        this.username = username;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}