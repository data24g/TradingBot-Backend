package com.example.TradingBot.tradingbot.service;
//
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.example.TradingBot.auth.model.UserAccount;
import com.example.TradingBot.auth.repository.UserAccountRepository;
import com.example.TradingBot.auth.service.EncryptionService;
import com.example.TradingBot.tradingbot.entity.TradeHistory;
import com.example.TradingBot.tradingbot.repository.TradeHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.HashMap;
import java.util.Map;



/**
 * Service "Nhà điều hành Giao dịch", thực thi toàn bộ quy trình từ kiểm tra
 * điều kiện, phân tích tín hiệu đa khung thời gian, cho đến đặt lệnh.
 * Có khả năng chuyển đổi giữa chiến lược Giao dịch theo Xu hướng và Giao dịch trong Biên độ.
 */
@Service
public class TradingExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(TradingExecutionService.class);

    @Autowired  private TradeHistoryRepository tradeHistoryRepository;

    // --- Dependencies ---
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private BinanceApiService binanceApiService;
    @Autowired private EncryptionService encryptionService;
    @Autowired private TradingStrategyService tradingStrategyService;
    @Autowired private BinanceDataService binanceDataService;

    // --- Configurations from application.properties ---
    @Value("${bot.system.max-trades-per-day}")
    private int maxTradesPerDay;
    @Value("${bot.system.risk.default-sl-percentage}")
    private double stopLossPercentage;
    @Value("${bot.system.risk.default-tp-percentage}")
    private double takeProfitPercentage;
    @Value("${bot.system.leverage.default}")
    private int defaultLeverage;

    /**
     * Phương thức chính, thực thi toàn bộ logic giao dịch cho một user và một coin cụ thể.
     */
    @Async
    public void executeForUserAndCoin(UserAccount user, String coin) {
        try {
            // --- GIAI ĐOẠN 1: KIỂM TRA CÁC ĐIỀU KIỆN TIÊN QUYẾT ---
            if (!checkPrerequisites(user)) {
                return; // Dừng lại nếu không đủ điều kiện cơ bản
            }
            logger.info("User [{}], Coin [{}]: Đủ điều kiện. Bắt đầu phân tích trạng thái thị trường...", user.getUsername(), coin);

            // --- GIAI ĐOẠN 2: XÁC ĐỊNH TRẠNG THÁI THỊ TRƯỜNG TỪ H4 ---
            BarSeries seriesH4 = binanceDataService.fetchKlineData(coin, "4h", 100);
            if (seriesH4.isEmpty()) {
                logger.warn("User [{}], Coin [{}]: Không lấy được dữ liệu H4. Bỏ qua.", user.getUsername(), coin);
                return;
            }
            TrendAnalysisResult h4Result = tradingStrategyService.determineTrendFromStructure(seriesH4);
            logger.info("Phân tích H4: Trạng thái thị trường là {}", h4Result.getTrend());

            String signal = null;
            BarSeries entrySeries = null; // Dùng để lấy giá vào lệnh từ đúng khung thời gian

            // --- GIAI ĐOẠN 3: CHỌN CHIẾN LƯỢC PHÙ HỢP VÀ PHÂN TÍCH ---
            if (h4Result.getTrend() == TrendAnalysisResult.Trend.UPTREND || h4Result.getTrend() == TrendAnalysisResult.Trend.DOWNTREND) {
                // --- KÍCH HOẠT CHẾ ĐỘ GIAO DỊCH THEO XU HƯỚNG ---
                logger.info(">> Kích hoạt [Chế độ Giao dịch theo Xu hướng]");
                Map<String, BarSeries> allSeries = fetchAllTimeframeData(coin, seriesH4);
                if (!allSeries.isEmpty()) {
                    signal = tradingStrategyService.analyzeTrendSignal(allSeries);
                    if (signal != null) {
                        entrySeries = allSeries.get("5m");
                    }
                }
            } else if (h4Result.getTrend() == TrendAnalysisResult.Trend.SIDEWAYS) {
                // --- KÍCH HOẠT CHẾ ĐỘ GIAO DỊCH TRONG BIÊN ĐỘ ---
                logger.info(">> Kích hoạt [Chế độ Giao dịch trong Biên độ]");
                RangeBoundaryResult boundaries = tradingStrategyService.identifyRangeBoundaries(seriesH4);
                if (boundaries != null) {
                    BarSeries seriesM15 = binanceDataService.fetchKlineData(coin, "15m", 30);
                    if (!seriesM15.isEmpty()) {
                        signal = tradingStrategyService.findRangeEntrySignal(seriesM15, boundaries);
                        if (signal != null) {
                            entrySeries = seriesM15;
                        }
                    }else {
                        logger.info(">> Không lấy được dữ liệu của M15");

                    }
                }
            }

            // --- GIAI ĐOẠN 4: THỰC THI NẾU CÓ TÍN HIỆU TỪ BẤT KỲ CHẾ ĐỘ NÀO ---
            if (signal != null && entrySeries != null) {
                double entryPrice = entrySeries.getLastBar().getClosePrice().doubleValue();
                double configuredOrderSize = user.getTradingConfig().getOrderSizeUSD();
                placeTrade(user, coin, signal, entryPrice, configuredOrderSize);
            }

        } catch (Exception e) {
            logger.error("Lỗi nghiêm trọng khi thực thi cho User [{}], Coin [{}]: {}", user.getUsername(), coin, e.getMessage());
        }
    }

    /**
     * Hàm tổng hợp kiểm tra các điều kiện ban đầu: giới hạn lệnh và cấu hình.
     */
    private boolean checkPrerequisites(UserAccount user) {
        if (!checkAndUpdateTradeLimit(user)) {
            userAccountRepository.save(user);
            return false;
        }
        if (user.getTradingConfig() == null || user.getTradingConfig().getOrderSizeUSD() == null || user.getTradingConfig().getOrderSizeUSD() <= 0) {
            logger.warn("User [{}] chưa cấu hình hoặc cấu hình orderSizeUSD không hợp lệ. Bỏ qua.", user.getUsername());
            return false;
        }
        return true;
    }

    /**
     * Thu thập dữ liệu nến cho tất cả các khung thời gian cần thiết cho chế độ giao dịch theo xu hướng.
     */
    private Map<String, BarSeries> fetchAllTimeframeData(String coin, BarSeries seriesH4) {
        Map<String, BarSeries> allSeries = new HashMap<>();
        allSeries.put("4h", seriesH4); // Tái sử dụng dữ liệu H4 đã lấy trước đó
        allSeries.put("1h", binanceDataService.fetchKlineData(coin, "1h", 100));
        allSeries.put("15m", binanceDataService.fetchKlineData(coin, "15m", 50));
        allSeries.put("5m", binanceDataService.fetchKlineData(coin, "5m", 50));

        for (BarSeries series : allSeries.values()) {
            if (series.isEmpty()) {
                return new HashMap<>();
            }
        }
        return allSeries;
    }

    /**
     * Hàm chuyên trách cho việc đặt lệnh sau khi đã có tín hiệu.
     */
    private void placeTrade(UserAccount user, String coin, String signal, double entryPrice, double configuredOrderSize) {
        UMFuturesClientImpl client = createClientForUser(user);

        double requiredMargin = configuredOrderSize / defaultLeverage;
        BigDecimal availableBalance = binanceApiService.getAccountBalance(client, "USDT");

        if (availableBalance.doubleValue() < requiredMargin) {
            logger.warn("User [{}] KHÔNG ĐỦ SỐ DƯ ĐỂ KÝ QUỸ. Yêu cầu: {} USDT, Hiện có: {} USDT. Dừng lại.",
                    user.getUsername(), String.format("%.2f", requiredMargin), availableBalance);
            return;
        }
        logger.info("User [{}] đủ số dư để ký quỹ. Yêu cầu: {}, Hiện có: {}. Tiến hành đặt lệnh.",
                user.getUsername(), String.format("%.2f", requiredMargin), availableBalance);

        binanceApiService.setLeverage(client, coin, defaultLeverage);

        double quantity = calculatePositionSize(configuredOrderSize, entryPrice);
        if (quantity <= 0) {
            logger.warn("User [{}] - Không thể tính được khối lượng lệnh hợp lệ. Bỏ qua.", user.getUsername());
            return;
        }

        if ("LONG".equals(signal)) {
            double stopLossPrice = entryPrice * (1 - stopLossPercentage / 100.0);
            double takeProfitPrice = entryPrice * (1 + takeProfitPercentage / 100.0);
            binanceApiService.placeNewOrder(client, coin, "BUY", quantity, entryPrice, stopLossPrice, takeProfitPrice);
        } else if ("SHORT".equals(signal)) {
            double stopLossPrice = entryPrice * (1 + stopLossPercentage / 100.0);
            double takeProfitPrice = entryPrice * (1 - takeProfitPercentage / 100.0);
            binanceApiService.placeNewOrder(client, coin, "SELL", quantity, entryPrice, stopLossPrice, takeProfitPrice);
        }

        user.setTradesToday(user.getTradesToday() + 1);
        user.setLastTradeDate(LocalDate.now(ZoneId.of("UTC")));
        userAccountRepository.save(user);
        logger.info("User [{}] đã vào lệnh thành công. Số lệnh hôm nay: {}/{}", user.getUsername(), user.getTradesToday(), maxTradesPerDay);
    }

    private boolean checkAndUpdateTradeLimit(UserAccount user) {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        if (user.getLastTradeDate() == null || user.getLastTradeDate().isBefore(today)) {
            user.setTradesToday(0);
            user.setLastTradeDate(today);
            logger.info("User [{}]: Đã sang ngày mới. Reset bộ đếm lệnh.", user.getUsername());
        }
        return user.getTradesToday() < maxTradesPerDay;
    }

    private double calculatePositionSize(double orderSizeUSD, double entryPrice) {
        if (orderSizeUSD <= 0 || entryPrice <= 0) return 0.0;
        return orderSizeUSD / entryPrice;
    }

    private UMFuturesClientImpl createClientForUser(UserAccount user) {
        String apiKey = encryptionService.decrypt(user.getEncryptedApiKey());
        String secretKey = encryptionService.decrypt(user.getEncryptedApiSecret());
        return binanceApiService.createClient(apiKey, secretKey);
    }

    private void saveTradeHistory(UserAccount user, String symbol, String side, double entryPrice, double quantity,
                                  double orderSizeUSD, double stopLossPrice, double takeProfitPrice) {
        TradeHistory trade = new TradeHistory();
        trade.setUserId(user.getId());
        trade.setUsername(user.getUsername());
        trade.setSymbol(symbol);
        trade.setSide(side);
        trade.setEntryPrice(entryPrice);
        trade.setQuantity(quantity);
        trade.setOrderSizeUSD(orderSizeUSD);
        trade.setStopLossPrice(stopLossPrice);
        trade.setTakeProfitPrice(takeProfitPrice);
        trade.setEntryTime(LocalDateTime.now());
        // (CẬP NHẬT) Đặt trạng thái ban đầu là OPEN
        trade.setStatus("OPEN");

        tradeHistoryRepository.save(trade);
        logger.info("Đã lưu lịch sử giao dịch MỚI cho User [{}], Symbol {}", user.getUsername(), symbol);
    }
}

