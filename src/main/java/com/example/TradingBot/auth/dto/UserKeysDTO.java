package com.example.TradingBot.auth.dto;


/**
 * DTO chứa thông tin nhạy cảm (API Key & Secret) đã được giải mã.
 * CẢNH BÁO: Chỉ sử dụng nội bộ hoặc qua kênh bảo mật (HTTPS).
 */
public record UserKeysDTO(
        String apiKey,
        String apiSecret
) {}
