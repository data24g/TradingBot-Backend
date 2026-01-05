package com.example.TradingBot.auth.dto;


import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) để nhận dữ liệu khi tạo một UserAccount mới.
 * Sử dụng Java Record để có mã nguồn ngắn gọn và bất biến.
 *
 * @param username Tên định danh cho người dùng.
 * @param apiKey Khóa API từ Binance.
 * @param apiSecret Khóa bí mật API từ Binance.
 */
public record UserCreateDTO(

        @NotBlank(message = "Username không được để trống")
        String username,

        @NotBlank(message = "API key không được để trống")
        String apiKey,

        @NotBlank(message = "API secret không được để trống")
        String apiSecret
) {
    // Không cần thêm bất kỳ code nào khác!
    // Record tự động tạo constructor, getters, equals(), hashCode(), và toString().
}
