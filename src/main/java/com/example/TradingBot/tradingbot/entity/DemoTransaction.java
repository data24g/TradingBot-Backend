package com.example.TradingBot.tradingbot.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "demo_transactions")
public class DemoTransaction {
    @Id
    private String id;
    
    private String planId;
    private String userId;
    private String symbol;
    private String side; // BUY or SELL
    
    private Double price;
    private Double amountUSDT;
    private Double amountCoin;
    
    private String date; // Stored as ISO string for frontend compatibility
    private String status; // SUCCESS, FAILED, PENDING
    
    public DemoTransaction() {
        this.date = java.time.LocalDateTime.now().toString();
        this.status = "SUCCESS";
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public Double getAmountUSDT() { return amountUSDT; }
    public void setAmountUSDT(Double amountUSDT) { this.amountUSDT = amountUSDT; }
    
    public Double getAmountCoin() { return amountCoin; }
    public void setAmountCoin(Double amountCoin) { this.amountCoin = amountCoin; }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

