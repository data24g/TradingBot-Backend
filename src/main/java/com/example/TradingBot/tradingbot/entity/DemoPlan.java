package com.example.TradingBot.tradingbot.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "demo_plans")
public class DemoPlan {
    @Id
    private String id;
    
    private String userId;
    private String username;
    private String symbol; // e.g. BTCUSDT
    private Double amountUSD; // amount in USDT per order
    private String side; // BUY or SELL
    private String recurrence; // NONE, DAILY, WEEKLY, MONTHLY
    private Integer targetHour; // Hour of day to execute
    private Integer targetMinute; // Minute of hour to execute
    private Integer targetWeekday; // For WEEKLY (0-6, 0=Sunday)
    private Integer targetDate; // For MONTHLY (1-31)
    private LocalDateTime scheduleTime; // First scheduled time
    private String timezone;
    private boolean active = true;
    private LocalDateTime nextRun;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt; // Time when the plan was completed (for non-recurring plans)

    // Demo tracking fields
    private Double totalInvested = 0.0;
    private Double currentValue = 0.0;
    private Double totalCoins = 0.0;
    
    public DemoPlan() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public Double getAmountUSD() { return amountUSD; }
    public void setAmountUSD(Double amountUSD) { this.amountUSD = amountUSD; }
    
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    
    public String getRecurrence() { return recurrence; }
    public void setRecurrence(String recurrence) { this.recurrence = recurrence; }
    
    public Integer getTargetHour() { return targetHour; }
    public void setTargetHour(Integer targetHour) { this.targetHour = targetHour; }
    
    public Integer getTargetMinute() { return targetMinute; }
    public void setTargetMinute(Integer targetMinute) { this.targetMinute = targetMinute; }
    
    public Integer getTargetWeekday() { return targetWeekday; }
    public void setTargetWeekday(Integer targetWeekday) { this.targetWeekday = targetWeekday; }
    
    public Integer getTargetDate() { return targetDate; }
    public void setTargetDate(Integer targetDate) { this.targetDate = targetDate; }
    
    public LocalDateTime getScheduleTime() { return scheduleTime; }
    public void setScheduleTime(LocalDateTime scheduleTime) { this.scheduleTime = scheduleTime; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getNextRun() { return nextRun; }
    public void setNextRun(LocalDateTime nextRun) { this.nextRun = nextRun; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Double getTotalInvested() { return totalInvested; }
    public void setTotalInvested(Double totalInvested) { this.totalInvested = totalInvested; }
    
    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
    
    public Double getTotalCoins() { return totalCoins; }
    public void setTotalCoins(Double totalCoins) { this.totalCoins = totalCoins; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}

