package com.example.TradingBot.tradingbot.config;



public class TradingConfig {
    private boolean isActive = false;

    // SỐ TIỀN CỐ ĐỊNH CHO MỖI LỆNH (VD: 20.0 TƯƠNG ĐƯƠNG 20 USDT)
    private Double orderSizeUSD;


    public Double getOrderSizeUSD() {
        return orderSizeUSD;
    }

    public void setOrderSizeUSD(Double orderSizeUSD) {
        this.orderSizeUSD = orderSizeUSD;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
