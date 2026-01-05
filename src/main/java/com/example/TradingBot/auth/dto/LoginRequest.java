package com.example.TradingBot.auth.dto;


import com.example.TradingBot.auth.model.Role;

public class LoginRequest {
    private String identifier; // email hoặc username
    private String password;
    private Role role; // <-- VAI TRÒ MONG MUỐN

    public LoginRequest() {}

    public LoginRequest(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    public LoginRequest(String identifier, String password, Role role) {
        this.identifier = identifier;
        this.password = password;
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
