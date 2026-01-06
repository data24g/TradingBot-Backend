package com.example.TradingBot.tradingbot.service;


import com.example.TradingBot.tradingbot.dto.MarketStateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import java.util.List; // <-- Import


@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    @Autowired
    private BinanceDataService binanceDataService;

    @Autowired
    private TradingStrategyService tradingStrategyService;

    /**
     * Lấy giá hiện tại của một symbol.
     * @param symbol Tên cặp giao dịch (vd: "BTCUSDT").
     * @return giá hiện tại, hoặc 0.0 nếu không lấy được.
     */
    public double getCurrentPrice(String symbol) {
        try {
            // Lấy 1 nến mới nhất để lấy giá đóng
            BarSeries series = binanceDataService.fetchKlineData(symbol, "1m", 1);
            if (series != null && series.getBarCount() > 0) {
                return series.getLastBar().getClosePrice().doubleValue();
            }
            logger.warn("Không lấy được dữ liệu giá cho symbol: {}", symbol);
            return 0.0;
        } catch (Exception e) {
            logger.error("Lỗi khi lấy giá cho {}: {}", symbol, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Lấy dữ liệu phân tích trạng thái thị trường đầy đủ cho một symbol.
     * @param symbol Tên cặp giao dịch (vd: "BTCUSDT").
     * @return một MarketStateDTO chứa thông tin, hoặc null nếu không thể phân tích.
     */
    public MarketStateDTO getMarketStateData(String symbol) {
        logger.info("Bắt đầu lấy Market State cho symbol: {}", symbol);

        // 1. Lấy dữ liệu H4
        BarSeries seriesH4 = binanceDataService.fetchKlineData(symbol, "4h", 100);
        if (seriesH4 == null) { // Chỉ cần kiểm tra null vì fetchKlineData đã xử lý trường hợp rỗng
            logger.error("Không thể lấy dữ liệu H4 cho symbol {}. Dừng lại.", symbol);
            return null; // Nguyên nhân phổ biến nhất là lỗi API hoặc symbol không hợp lệ
        }
        logger.info("Lấy thành công {} nến H4.", seriesH4.getBarCount());

        // 2. Phân tích xu hướng
        TrendAnalysisResult trendResult = tradingStrategyService.determineTrendFromStructure(seriesH4);
        double currentPrice = seriesH4.getLastBar().getClosePrice().doubleValue();
        logger.info("Kết quả phân tích xu hướng: {}", trendResult.getTrend());

        List<Double> supportLevels;
        List<Double> resistanceLevels;

        // 3. Phân nhánh logic dựa trên kết quả xu hướng để xác định Hỗ trợ/Kháng cự
        if (trendResult.getTrend() == TrendAnalysisResult.Trend.SIDEWAYS) {
            logger.info("Thị trường SIDEWAYS, đang tính toán biên độ Bollinger...");
            RangeBoundaryResult boundaries = tradingStrategyService.identifyRangeBoundaries(seriesH4);
            if (boundaries != null) {
                supportLevels = List.of(boundaries.getLowerBound());
                resistanceLevels = List.of(boundaries.getUpperBound());
            } else {
                logger.warn("Không tính được Bollinger Bands, sử dụng đỉnh/đáy cũ làm dự phòng.");
                supportLevels = trendResult.getIdentifiedTroughs();
                resistanceLevels = trendResult.getIdentifiedPeaks();
            }
        } else {
            logger.info("Thị trường có xu hướng, lấy đỉnh/đáy cũ làm hỗ trợ/kháng cự.");
            supportLevels = trendResult.getIdentifiedTroughs();
            resistanceLevels = trendResult.getIdentifiedPeaks();
        }

        // 4. Tạo DTO để trả về
        MarketStateDTO resultDto = new MarketStateDTO(
                symbol,
                currentPrice,
                trendResult.getTrend(),
                trendResult.getReason(),
                supportLevels,
                resistanceLevels
        );
        logger.info("Tạo DTO thành công cho {}. Trả về kết quả.", symbol);
        return resultDto;
    }
}