package com.example.TradingBot.tradingbot.service;


import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;

/**
 * Service chuyên dụng để lấy dữ liệu thị trường (cụ thể là dữ liệu nến) từ Binance.
 * Service này không yêu cầu API key vì nó chỉ truy cập các endpoint công khai.
 */
@Service
public class BinanceDataService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceDataService.class);

    private final UMFuturesClientImpl umFuturesClient;

    public BinanceDataService(@Value("${binance.api.base-url}") String baseUrl) {
        this.umFuturesClient = new UMFuturesClientImpl(baseUrl);
    }

    /**
     * Lấy dữ liệu nến (kline/OHLCV) cho một cặp giao dịch và chuyển đổi nó thành BarSeries.
     * @param symbol Cặp giao dịch (vd: "BTCUSDT").
     * @param interval Khung thời gian (vd: "4h", "15m").
     * @param limit Số lượng nến cần lấy.
     * @return một đối tượng BarSeries chứa dữ liệu nến, hoặc null nếu có lỗi.
     */
    public BarSeries fetchKlineData(String symbol, String interval, int limit) {
        logger.debug("Bắt đầu lấy {} nến {} cho symbol {}", limit, interval, symbol);
        try {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("symbol", symbol);
            params.put("interval", interval);
            params.put("limit", limit);

            String result = this.umFuturesClient.market().klines(params);

            if (result.contains("\"code\":")) {
                logger.error("API Binance trả về lỗi cho Symbol {}: {}", symbol, result);
                return null;
            }

            JSONArray klines = new JSONArray(result);
            if (klines.length() == 0) {
                logger.warn("Không nhận được dữ liệu nến nào cho Symbol {}. Có thể symbol không hợp lệ.", symbol);
                return null;
            }

            BarSeries series = new BaseBarSeries(symbol + "_" + interval);
            for (int i = 0; i < klines.length(); i++) {
                JSONArray kline = klines.getJSONArray(i);
                long closeTimestamp = kline.getLong(6);
                ZonedDateTime endTime = Instant.ofEpochMilli(closeTimestamp).atZone(ZoneId.systemDefault());
                Duration barDuration = intervalToDuration(interval);
                series.addBar(new BaseBar(
                        barDuration, endTime,
                        kline.getBigDecimal(1), kline.getBigDecimal(2),
                        kline.getBigDecimal(3), kline.getBigDecimal(4),
                        kline.getBigDecimal(5)
                ));
            }
            logger.debug("Lấy thành công {} nến cho {}", series.getBarCount(), symbol);
            return series;

        } catch (Exception e) {
            logger.error("LỖI NGHIÊM TRỌNG KHI LẤY DỮ LIỆU KLINE cho Symbol {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }

    private Duration intervalToDuration(String interval) {
        if (interval == null || interval.length() < 2) {
            logger.warn("Chuỗi interval không hợp lệ: {}", interval);
            return Duration.ZERO;
        }

        char unit = interval.charAt(interval.length() - 1);
        int value;
        try {
            value = Integer.parseInt(interval.substring(0, interval.length() - 1));
        } catch (NumberFormatException e) {
            logger.warn("Không thể phân tích giá trị của interval: {}", interval);
            return Duration.ZERO;
        }

        return switch (unit) {
            case 'm' -> Duration.ofMinutes(value);
            case 'h' -> Duration.ofHours(value);
            case 'd' -> Duration.ofDays(value);
            case 'w' -> Duration.ofDays(value * 7L);
            default -> {
                logger.warn("Khung thời gian không xác định: {}", interval);
                yield Duration.ZERO;
            }
        };
    }
}
