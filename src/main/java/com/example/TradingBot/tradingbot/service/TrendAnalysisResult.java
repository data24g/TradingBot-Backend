package com.example.TradingBot.tradingbot.service;

import java.util.List;

public class TrendAnalysisResult {

    public enum Trend { UPTREND, DOWNTREND, SIDEWAYS, UNDETERMINED }

    private final Trend trend;
    private final String reason;
    private final List<Double> identifiedPeaks; // Các đỉnh đã tìm thấy
    private final List<Double> identifiedTroughs; // Các đáy đã tìm thấy

    public TrendAnalysisResult(Trend trend, String reason, List<Double> peaks, List<Double> troughs) {
        this.trend = trend;
        this.reason = reason;
        this.identifiedPeaks = peaks;
        this.identifiedTroughs = troughs;
    }

    // Constructor đơn giản hơn cho các trường hợp không có đỉnh/đáy
    public TrendAnalysisResult(Trend trend, String reason) {
        this(trend, reason, List.of(), List.of());
    }

    public Trend getTrend() {
        return trend;
    }

    public String getReason() {
        return reason;
    }

    public List<Double> getIdentifiedPeaks() {
        return identifiedPeaks;
    }

    public List<Double> getIdentifiedTroughs() {
        return identifiedTroughs;
    }
}