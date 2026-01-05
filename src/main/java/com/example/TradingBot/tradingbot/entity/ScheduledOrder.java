package com.example.TradingBot.tradingbot.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "scheduled_orders")
public class ScheduledOrder {
    @Id
    private String id;

    private String userId;
    private String symbol; // e.g. BTCUSDT
    private Double amountUSD; // amount in quote currency to spend per order

    private Recurrence recurrence;
    private LocalDateTime scheduleTime; // first scheduled time
    private String timezone; // IANA timezone id
    private boolean active = true;
    private LocalDateTime nextRun;

    public enum Recurrence {
        NONE, DAILY, WEEKLY, MONTHLY
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Double getAmountUSD() { return amountUSD; }
    public void setAmountUSD(Double amountUSD) { this.amountUSD = amountUSD; }

    public Recurrence getRecurrence() { return recurrence; }
    public void setRecurrence(Recurrence recurrence) { this.recurrence = recurrence; }

    public LocalDateTime getScheduleTime() { return scheduleTime; }
    public void setScheduleTime(LocalDateTime scheduleTime) { this.scheduleTime = scheduleTime; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getNextRun() { return nextRun; }
    public void setNextRun(LocalDateTime nextRun) { this.nextRun = nextRun; }
}
