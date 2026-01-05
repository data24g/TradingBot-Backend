package com.example.TradingBot.auth.model;


import com.example.TradingBot.tradingbot.config.TradingConfig;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Document(collection = "user_accounts") // Tên collection trong MongoDB
public class UserAccount {
    @Id
    private String id; // ID trong MongoDB là String

    private String username;
    private String encryptedApiKey;
    private String encryptedApiSecret;
    private int tradesToday = 0;
    private LocalDate lastTradeDate;

    // Nhúng đối tượng TradingConfig trực tiếp vào đây
    private TradingConfig tradingConfig;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEncryptedApiKey() {
        return encryptedApiKey;
    }

    public void setEncryptedApiKey(String encryptedApiKey) {
        this.encryptedApiKey = encryptedApiKey;
    }

    public String getEncryptedApiSecret() {
        return encryptedApiSecret;
    }

    public void setEncryptedApiSecret(String encryptedApiSecret) {
        this.encryptedApiSecret = encryptedApiSecret;
    }

    public int getTradesToday() {
        return tradesToday;
    }

    public void setTradesToday(int tradesToday) {
        this.tradesToday = tradesToday;
    }

    public LocalDate getLastTradeDate() {
        return lastTradeDate;
    }

    public void setLastTradeDate(LocalDate lastTradeDate) {
        this.lastTradeDate = lastTradeDate;
    }

    public TradingConfig getTradingConfig() {
        return tradingConfig;
    }

    public void setTradingConfig(TradingConfig tradingConfig) {
        this.tradingConfig = tradingConfig;
    }
}