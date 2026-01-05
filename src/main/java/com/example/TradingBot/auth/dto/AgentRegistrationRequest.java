package com.example.TradingBot.auth.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

// Sử dụng validation để đảm bảo dữ liệu đầu vào là hợp lệ
public class AgentRegistrationRequest {

    // --- Thông tin tài khoản cơ bản ---
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 20, message = "Tên đăng nhập phải từ 3 đến 20 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 40, message = "Mật khẩu phải từ 6 đến 40 ký tự")
    private String password;

    private String phone;

    // --- Thông tin chi tiết Đại lý ---
    @NotBlank(message = "Mã đại lý không được để trống")
    private String agentCode;

    @NotNull(message = "Tỷ lệ hoa hồng không được để trống")
    @DecimalMin(value = "0.0", message = "Tỷ lệ hoa hồng phải lớn hơn hoặc bằng 0")
    private BigDecimal commissionRate;

    @NotNull(message = "Tỷ lệ hoa hồng tối đa không được để trống")
    @DecimalMin(value = "0.0", message = "Tỷ lệ hoa hồng tối đa phải lớn hơn hoặc bằng 0")
    private BigDecimal maxCommissionRate;

    @NotBlank(message = "Khu vực hoạt động không được để trống")
    private String operatingArea;

    // ID của đại lý cấp trên, có thể là null nếu là đại lý cấp cao nhất
    private String uplineAgentId;

    private boolean allowSubAgents = false; // Mặc định là false

    @NotBlank(message = "Địa chỉ liên hệ không được để trống")
    private String contactAddress;

    // Các trường không bắt buộc
    private String businessLicenseNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String notes;

    // Getters and Setters cho tất cả các trường...

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public BigDecimal getMaxCommissionRate() { return maxCommissionRate; }
    public void setMaxCommissionRate(BigDecimal maxCommissionRate) { this.maxCommissionRate = maxCommissionRate; }
    public String getOperatingArea() { return operatingArea; }
    public void setOperatingArea(String operatingArea) { this.operatingArea = operatingArea; }
    public String getUplineAgentId() { return uplineAgentId; }
    public void setUplineAgentId(String uplineAgentId) { this.uplineAgentId = uplineAgentId; }
    public boolean isAllowSubAgents() { return allowSubAgents; }
    public void setAllowSubAgents(boolean allowSubAgents) { this.allowSubAgents = allowSubAgents; }
    public String getContactAddress() { return contactAddress; }
    public void setContactAddress(String contactAddress) { this.contactAddress = contactAddress; }
    public String getBusinessLicenseNumber() { return businessLicenseNumber; }
    public void setBusinessLicenseNumber(String businessLicenseNumber) { this.businessLicenseNumber = businessLicenseNumber; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
