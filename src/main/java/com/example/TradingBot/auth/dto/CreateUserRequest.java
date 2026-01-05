package com.example.TradingBot.auth.dto;

// package com.xosotv.live.xosotv_backend.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    private String username;

    private String phone; // <-- THÊM TRƯỜNG PHONE

    private String referrerCode; // Mã giới thiệu (nếu có)

    // Constructors, Getters, Setters
    // ...
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPhone() { return phone; } // <-- GETTER/SETTER CHO PHONE
    public void setPhone(String phone) { this.phone = phone; } // <-- GETTER/SETTER CHO PHONE
    public String getReferrerCode() { return referrerCode; }
    public void setReferrerCode(String referrerCode) { this.referrerCode = referrerCode; }



}