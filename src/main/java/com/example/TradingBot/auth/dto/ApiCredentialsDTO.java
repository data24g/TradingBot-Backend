package com.example.TradingBot.auth.dto;


import jakarta.validation.constraints.NotBlank;

/**
 * DTO để nhận dữ liệu khi người dùng cập nhật API Key và Secret Key.
 */
public record ApiCredentialsDTO(

        @NotBlank(message = "API key không được để trống")
        String newApiKey,

        @NotBlank(message = "API secret không được để trống")
        String newApiSecret
) {}