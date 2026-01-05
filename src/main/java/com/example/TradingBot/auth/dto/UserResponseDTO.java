package com.example.TradingBot.auth.dto;


import java.time.LocalDate;

/**
 * DTO "an toàn" để trả về thông tin người dùng cho client.
 * Nó không chứa bất kỳ thông tin nhạy cảm nào như API keys.
 */
public record UserResponseDTO(
        String id,
        String username,
        boolean isActive,
        int tradesToday,
        LocalDate lastTradeDate
) {}
