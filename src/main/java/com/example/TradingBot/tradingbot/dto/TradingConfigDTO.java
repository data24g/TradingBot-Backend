package com.example.TradingBot.tradingbot.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TradingConfigDTO(

        @NotNull(message = "orderSizeUSD không được để trống")
        @Positive(message = "orderSizeUSD phải là một số dương")
        Double orderSizeUSD
) {}
