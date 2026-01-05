package com.example.TradingBot.auth.dto;


import java.time.LocalDate;

/**
 * DTO "an toàn" để trả về thông tin CHI TIẾT của một người dùng.
 * Bao gồm cả cấu hình giao dịch của họ.
 */
public record UserDetailDTO(
        String id,
        String username,
        boolean isActive,
        Double orderSizeUSD, // <-- TRƯỜNG MÀ BẠN CẦN
        int tradesToday,
        LocalDate lastTradeDate
) {}
