package com.example.TradingBot.auth.model;


import java.math.BigDecimal;

public class AgentDetails {

    private String agentCode;           // Mã Đại Lý (ví dụ: AG696952)
    private BigDecimal commissionRate;      // Tỷ Lệ Hoa Hồng (%)
    private BigDecimal maxCommissionRate;   // Tỷ Lệ Hoa Hồng Tối Đa (%)
    private String operatingArea;       // Khu Vực Hoạt Động
    private String uplineAgentId;       // ID của Đại Lý Cấp Trên
    private boolean allowSubAgents;     // Cho phép tạo đại lý con
    private String contactAddress;      // Địa Chỉ Liên Hệ
    private String businessLicenseNumber; // Số Giấy Phép Kinh Doanh
    private String emergencyContactName;  // Tên Người Liên Hệ Khẩn Cấp
    private String emergencyContactPhone; // SĐT Khẩn Cấp
    private String notes;               // Ghi Chú

    // Constructors
    public AgentDetails() {
    }

    // Getters and Setters
    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public BigDecimal getMaxCommissionRate() {
        return maxCommissionRate;
    }

    public void setMaxCommissionRate(BigDecimal maxCommissionRate) {
        this.maxCommissionRate = maxCommissionRate;
    }

    public String getOperatingArea() {
        return operatingArea;
    }

    public void setOperatingArea(String operatingArea) {
        this.operatingArea = operatingArea;
    }

    public String getUplineAgentId() {
        return uplineAgentId;
    }

    public void setUplineAgentId(String uplineAgentId) {
        this.uplineAgentId = uplineAgentId;
    }

    public boolean isAllowSubAgents() {
        return allowSubAgents;
    }

    public void setAllowSubAgents(boolean allowSubAgents) {
        this.allowSubAgents = allowSubAgents;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    public String getBusinessLicenseNumber() {
        return businessLicenseNumber;
    }

    public void setBusinessLicenseNumber(String businessLicenseNumber) {
        this.businessLicenseNumber = businessLicenseNumber;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
