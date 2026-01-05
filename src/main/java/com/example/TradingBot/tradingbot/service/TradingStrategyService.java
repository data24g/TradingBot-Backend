package com.example.TradingBot.tradingbot.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service chứa logic phân tích kỹ thuật cốt lõi.
 * Có khả năng phân tích cả thị trường có xu hướng và thị trường đi ngang,
 * và tự động điều chỉnh độ nhạy phân tích khi không đủ dữ liệu.
 */
@Service
public class TradingStrategyService {

    private static final Logger logger = LoggerFactory.getLogger(TradingStrategyService.class);

    private static final int TREND_ANALYSIS_WINDOW = 20;

    @Value("${bot.system.range.boundary-tolerance-percentage}")
    private double boundaryTolerancePercentage;

    // =================================================================================
    // LOGIC GIAO DỊCH THEO XU HƯỚNG (TREND-FOLLOWING)
    // =================================================================================

    public String analyzeTrendSignal(Map<String, BarSeries> allSeries) {
        if (!allSeries.containsKey("4h") || !allSeries.containsKey("1h") || !allSeries.containsKey("15m") || !allSeries.containsKey("5m")) {
            logger.warn("Thiếu dữ liệu nến cho chiến lược theo xu hướng.");
            return null;
        }

        TrendAnalysisResult h4Result = determineTrendFromStructure(allSeries.get("4h"));
        if (h4Result.getTrend() != TrendAnalysisResult.Trend.UPTREND && h4Result.getTrend() != TrendAnalysisResult.Trend.DOWNTREND) {
            return null;
        }

        TrendAnalysisResult h1Result = determineTrendFromStructure(allSeries.get("1h"));
        if (h1Result.getTrend() != h4Result.getTrend()) {
            logger.info("Hủy tín hiệu: Xu hướng H1 ({}) không đồng thuận với H4 ({}).", h1Result.getTrend(), h4Result.getTrend());
            return null;
        }

        TrendAnalysisResult.Trend trendDirection = h4Result.getTrend();
        logger.info("XU HƯỚNG CHÍNH ĐƯỢC XÁC NHẬN: {}", trendDirection);

        if (!isPriceInPullbackZone(allSeries.get("15m"), trendDirection)) {
            logger.info("Chờ đợi: Giá trên M15 chưa đi vào vùng pullback.");
            return null;
        }
        logger.info("Thiết lập OK: Giá trên M15 đang trong vùng pullback.");

        String signal = findEntryTrigger(allSeries.get("5m"), trendDirection);
        if (signal != null) {
            logger.warn("!!! TÍN HIỆU TREND HỘI TỤ: {} !!!", signal);
            return signal;
        }

        return null;
    }

    // =================================================================================
    // LOGIC GIAO DỊCH PHÁ VỠ BIÊN ĐỘ (BREAKOUT-TRADING)
    // =================================================================================

    public RangeBoundaryResult identifyRangeBoundaries(BarSeries seriesH4) {
        if (seriesH4 == null || seriesH4.getBarCount() < 20) return null;

        ClosePriceIndicator closePrice = new ClosePriceIndicator(seriesH4);
        StandardDeviationIndicator sd20 = new StandardDeviationIndicator(closePrice, 20);
        BollingerBandsMiddleIndicator middleBand = new BollingerBandsMiddleIndicator(new EMAIndicator(closePrice, 20));
        BollingerBandsLowerIndicator lowerBand = new BollingerBandsLowerIndicator(middleBand, sd20);
        BollingerBandsUpperIndicator upperBand = new BollingerBandsUpperIndicator(middleBand, sd20);

        int endIndex = seriesH4.getEndIndex();
        double lower = lowerBand.getValue(endIndex).doubleValue();
        double upper = upperBand.getValue(endIndex).doubleValue();
        double currentPrice = seriesH4.getLastBar().getClosePrice().doubleValue();

        logger.info("Phân tích H4 Sideways: Biên dưới ~{:.5f} | Giá hiện tại ~{:.5f} | Biên trên ~{:.5f}",
                lower, currentPrice, upper);

        return new RangeBoundaryResult(upper, lower);
    }

    public String findBreakoutSignal(BarSeries seriesM15, RangeBoundaryResult boundaries) {
        if (seriesM15 == null || seriesM15.getBarCount() < 2) return null;

        Num upperBound = seriesM15.numOf(boundaries.getUpperBound());
        Num lowerBound = seriesM15.numOf(boundaries.getLowerBound());

        int endIndex = seriesM15.getEndIndex();
        Bar prevBar = seriesM15.getBar(endIndex - 1);
        Bar currentBar = seriesM15.getBar(endIndex);

        if (prevBar.getClosePrice().isLessThanOrEqual(upperBound) && currentBar.getClosePrice().isGreaterThan(upperBound)) {
            logger.warn("!!! TÍN HIỆU BREAKOUT LONG M15: Giá phá vỡ biên trên {:.5f}", boundaries.getUpperBound());
            return "LONG";
        }

        if (prevBar.getClosePrice().isGreaterThanOrEqual(lowerBound) && currentBar.getClosePrice().isLessThan(lowerBound)) {
            logger.warn("!!! TÍN HIỆU BREAKOUT SHORT M15: Giá phá vỡ biên dưới {:.5f}", boundaries.getLowerBound());
            return "SHORT";
        }

        return null;
    }

    // =================================================================================
    // CÁC HÀM PHỤ TRỢ CHUNG
    // =================================================================================

    /**
     * (CẬP NHẬT) Xác định xu hướng, có khả năng tự điều chỉnh window nếu không đủ dữ liệu.
     */
    public TrendAnalysisResult determineTrendFromStructure(BarSeries series) {
        if (series == null || series.getBarCount() < 20) { // Ngưỡng tối thiểu tuyệt đối
            return new TrendAnalysisResult(TrendAnalysisResult.Trend.UNDETERMINED, "Không đủ dữ liệu nến (ít hơn 20).");
        }

        // --- LOGIC TỰ ĐỘNG ĐIỀU CHỈNH WINDOW ---
        int currentWindow = TREND_ANALYSIS_WINDOW;
        while (series.getBarCount() < (currentWindow * 2 + 1) && currentWindow > 5) {
            currentWindow--;
        }

        if (series.getBarCount() < (currentWindow * 2 + 1)) {
            return new TrendAnalysisResult(TrendAnalysisResult.Trend.UNDETERMINED, "Không đủ dữ liệu nến ngay cả với window tối thiểu (5).");
        }

        if (currentWindow != TREND_ANALYSIS_WINDOW) {
            logger.warn("Không đủ nến cho window lý tưởng ({}). Tự động điều chỉnh window xuống còn {}.", TREND_ANALYSIS_WINDOW, currentWindow);
        }

        List<Double> recentHighs = findLocalExtrema(series, currentWindow, true);
        List<Double> recentLows = findLocalExtrema(series, currentWindow, false);

        if (recentHighs.size() < 2 || recentLows.size() < 2) {
            return new TrendAnalysisResult(TrendAnalysisResult.Trend.SIDEWAYS, "Không xác định được đủ đỉnh/đáy quan trọng.", recentHighs, recentLows);
        }

        double lastHigh = recentHighs.get(recentHighs.size() - 1);
        double prevHigh = recentHighs.get(recentHighs.size() - 2);
        double lastLow = recentLows.get(recentLows.size() - 1);
        double prevLow = recentLows.get(recentLows.size() - 2);

        if (lastLow > prevLow && lastHigh > prevHigh) {
            String reason = String.format("Đáy/Đỉnh sau cao hơn (L:%.2f > P:%.2f)", lastLow, prevLow);
            return new TrendAnalysisResult(TrendAnalysisResult.Trend.UPTREND, reason, recentHighs, recentLows);
        }
        if (lastHigh < prevHigh && lastLow < prevLow) {
            String reason = String.format("Đáy/Đỉnh sau thấp hơn (H:%.2f < P:%.2f)", lastHigh, prevHigh);
            return new TrendAnalysisResult(TrendAnalysisResult.Trend.DOWNTREND, reason, recentHighs, recentLows);
        }

        return new TrendAnalysisResult(TrendAnalysisResult.Trend.SIDEWAYS, "Đỉnh/đáy không tạo xu hướng.", recentHighs, recentLows);
    }

    private String findEntryTrigger(BarSeries series, TrendAnalysisResult.Trend mainTrend) {
        if (series == null || series.getBarCount() < 21) return null;

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator ema20 = new EMAIndicator(closePrice, 20);
        int endIndex = series.getEndIndex();

        Num currentPrice = series.getBar(endIndex).getClosePrice();
        Num prevPrice = series.getBar(endIndex - 1).getClosePrice();
        Num emaValue = ema20.getValue(endIndex - 1);

        if (mainTrend == TrendAnalysisResult.Trend.UPTREND && prevPrice.isLessThanOrEqual(emaValue) && currentPrice.isGreaterThan(emaValue)) {
            return "LONG";
        }
        if (mainTrend == TrendAnalysisResult.Trend.DOWNTREND && prevPrice.isGreaterThanOrEqual(emaValue) && currentPrice.isLessThan(emaValue)) {
            return "SHORT";
        }
        return null;
    }

    private boolean isPriceInPullbackZone(BarSeries series, TrendAnalysisResult.Trend mainTrend) {
        if (series == null || series.getBarCount() < 21) return false;

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator ema20 = new EMAIndicator(closePrice, 20);
        Num currentPrice = series.getLastBar().getClosePrice();
        Num emaValue = ema20.getValue(series.getEndIndex());

        if (mainTrend == TrendAnalysisResult.Trend.UPTREND) {
            return currentPrice.isLessThanOrEqual(emaValue);
        }
        if (mainTrend == TrendAnalysisResult.Trend.DOWNTREND) {
            return currentPrice.isGreaterThanOrEqual(emaValue);
        }
        return false;
    }

    private List<Double> findLocalExtrema(BarSeries series, int window, boolean findPeaks) {
        List<Double> extrema = new ArrayList<>();
        if (series.getBarCount() <= window * 2) {
            logger.warn("Không đủ nến ({}) để tìm đỉnh/đáy với window={}", series.getBarCount(), window);
            return extrema;
        }

        for (int i = window; i < series.getBarCount() - window; i++) {
            boolean isExtremum = true;
            Num centerValue = findPeaks ? series.getBar(i).getHighPrice() : series.getBar(i).getLowPrice();
            for (int j = i - window; j <= i + window; j++) {
                if (i == j) continue;
                Num compareValue = findPeaks ? series.getBar(j).getHighPrice() : series.getBar(j).getLowPrice();
                if ((findPeaks && centerValue.isLessThan(compareValue)) || (!findPeaks && centerValue.isGreaterThan(compareValue))) {
                    isExtremum = false;
                    break;
                }
            }
            if (isExtremum) {
                if (extrema.isEmpty() || !extrema.get(extrema.size() - 1).equals(centerValue.doubleValue())) {
                    extrema.add(centerValue.doubleValue());
                }
            }
        }
        return extrema;
    }

    /**
     * Tìm tín hiệu đảo chiều (Mean Reversion) tại biên trên khung M15, có tính đến sai số.
     * @param seriesM15 Dữ liệu nến của khung M15.
     * @param boundaries Các biên đã được xác định từ H4.
     * @return "LONG" nếu đảo chiều tăng tại biên dưới, "SHORT" nếu đảo chiều giảm tại biên trên, ngược lại null.
     */
    public String findRangeEntrySignal(BarSeries seriesM15, RangeBoundaryResult boundaries) {
        if (seriesM15 == null || seriesM15.getBarCount() < 2) return null;

        // --- TÍNH TOÁN VÙNG ĐỆM (BUFFER ZONE) ---
        double toleranceFactor = boundaryTolerancePercentage / 100.0;
        // Biên trên "mờ": Vùng gần biên trên để bắt đầu tìm tín hiệu Short
        double fuzzyUpperBound = boundaries.getUpperBound() * (1 - toleranceFactor);
        // Biên dưới "mờ": Vùng gần biên dưới để bắt đầu tìm tín hiệu Long
        double fuzzyLowerBound = boundaries.getLowerBound() * (1 + toleranceFactor);

        logger.debug("Phân tích Range M15 (Đảo chiều): Vùng Short [>= {:.5f}], Vùng Long [<= {:.5f}]", fuzzyUpperBound, fuzzyLowerBound);

        int endIndex = seriesM15.getEndIndex();
        Bar prevBar = seriesM15.getBar(endIndex - 1);
        Bar currentBar = seriesM15.getBar(endIndex);

        // --- Kiểm tra Tín hiệu LONG (Đảo chiều tăng tại vùng đáy) ---
        // Điều kiện: Nến trước đó đã đi VÀO VÙNG ĐỆM dưới, và nến hiện tại là một nến tăng xác nhận sự đảo chiều.
        if (prevBar.getLowPrice().isLessThanOrEqual(seriesM15.numOf(fuzzyLowerBound))) {
            if (currentBar.isBullish()) { // isBullish() = close > open
                logger.warn("!!! TÍN HIỆU RANGE LONG (Đảo chiều) M15: Giá bật lại từ vùng đệm dưới (biên cứng: {:.5f})", boundaries.getLowerBound());
                return "LONG";
            }
        }

        // --- Kiểm tra Tín hiệu SHORT (Đảo chiều giảm tại vùng đỉnh) ---
        // Điều kiện: Nến trước đó đã đi VÀO VÙNG ĐỆM trên, và nến hiện tại là một nến giảm xác nhận sự đảo chiều.
        if (prevBar.getHighPrice().isGreaterThanOrEqual(seriesM15.numOf(fuzzyUpperBound))) {
            if (currentBar.isBearish()) { // isBearish() = close < open
                logger.warn("!!! TÍN HIỆU RANGE SHORT (Đảo chiều) M15: Giá bật lại từ vùng đệm trên (biên cứng: {:.5f})", boundaries.getUpperBound());
                return "SHORT";
            }
        }

        return null;
    }

}