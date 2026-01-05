package com.example.TradingBot.tradingbot.dto;


import com.example.TradingBot.tradingbot.service.TrendAnalysisResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MarketStateDTO(
        String symbol,
        double currentPrice,
        TrendAnalysisResult.Trend trend,
        String trendReason,

        // (MỚI) Các mức hỗ trợ và kháng cự
        List<Double> supportLevels,
        List<Double> resistanceLevels
) {}
