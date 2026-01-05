package com.example.TradingBot.tradingbot.schedule;
import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.example.TradingBot.auth.model.UserAccount;
import com.example.TradingBot.auth.repository.UserAccountRepository;
import com.example.TradingBot.auth.service.EncryptionService;
import com.example.TradingBot.tradingbot.service.BinanceApiService;
import com.example.TradingBot.tradingbot.entity.TradeHistory;
import com.example.TradingBot.tradingbot.repository.TradeHistoryRepository;
import com.example.TradingBot.tradingbot.service.TradingExecutionService;
import com.example.TradingBot.tradingbot.service.ScheduledOrderService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * "Nhạc trưởng" của hệ thống, điều phối các tác vụ theo lịch trình.
 * Mỗi chu kỳ, nó thực hiện hai nhiệm vụ chính: Giám sát các lệnh đang mở,
 * và Quét tìm các cơ hội giao dịch mới.
 */
@Component
public class MasterScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MasterScheduler.class);
    // --- Dependencies ---
    @Autowired private TradingExecutionService tradingExecutionService;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private TradeHistoryRepository tradeHistoryRepository;
    @Autowired private BinanceApiService binanceApiService;
    @Autowired private EncryptionService encryptionService;
    @Autowired private ScheduledOrderService scheduledOrderService;

    // --- Configurations from application.properties ---
    @Value("#{'${bot.system.coins.list}'.split(',')}")
    private List<String> coinsToScan;
    @Value("${bot.system.trading-window.start-hour}")
    private int startHour;
    @Value("${bot.system.trading-window.end-hour}")
    private int endHour;
    @Value("${bot.system.timezone}")
    private String timezone;

    /**
     * Phương thức chính được kích hoạt định kỳ, điều phối cả hai nhiệm vụ.
     */
    @Scheduled(fixedRate = 30000) // Chạy mỗi 30 giây
    public void masterScanCycle() {
        // Execute scheduled DCA orders (run independently of trading window)
        try {
            scheduledOrderService.executeDueOrders();
        } catch (Exception e) {
            logger.error("Error executing scheduled orders: {}", e.getMessage());
        }

        // Kiểm tra khung giờ giao dịch của toàn hệ thống trước khi làm bất cứ điều gì
        ZonedDateTime zonedNow = ZonedDateTime.now(ZoneId.of(timezone));
        int currentHour = zonedNow.getHour();
        if (currentHour < startHour || currentHour >= endHour) {
            return; // Ngoài khung giờ, không làm gì cả
        }

        // --- NHIỆM VỤ 1: Giám sát và cập nhật các lệnh đã đóng ---
        monitorAndCloseTrades();

        // --- NHIỆM VỤ 2: Quét và tìm các lệnh mới để mở ---
        scanForNewTrades();
    }

    /**
     * Giám sát các giao dịch có trạng thái "OPEN" trong database.
     * Nếu vị thế tương ứng trên Binance đã đóng, cập nhật lại bản ghi.
     */
    private void monitorAndCloseTrades() {
        logger.info("--- MONITOR: Bắt đầu giám sát các lệnh đang mở ---");
        List<TradeHistory> openTrades = tradeHistoryRepository.findByStatus("OPEN");
        if (openTrades.isEmpty()) {
            logger.info("--- MONITOR: Không có lệnh nào đang mở để giám sát. ---");
            return;
        }

        for (TradeHistory trade : openTrades) {
            try {
                UserAccount user = userAccountRepository.findById(trade.getUserId()).orElse(null);
                if (user == null) {
                    logger.warn("--- MONITOR: Không tìm thấy user ID {} cho trade ID {}. Bỏ qua.", trade.getUserId(), trade.getId());
                    continue;
                }

                UMFuturesClientImpl client = createClientForUser(user);

                // Nếu vị thế trên Binance không còn tồn tại -> nó đã bị đóng
                if (!binanceApiService.hasOpenPosition(client, trade.getSymbol())) {
                    logger.warn("--- MONITOR: Vị thế User [{}], Symbol {} đã đóng. Đang cập nhật lịch sử...", user.getUsername(), trade.getSymbol());

                    JSONObject latestTradeInfo = binanceApiService.getLatestTradeForSymbol(client, trade.getSymbol());
                    if (latestTradeInfo != null) {
                        trade.setStatus("CLOSED");
                        trade.setExitPrice(latestTradeInfo.getDouble("price"));
                        trade.setPnl(latestTradeInfo.getDouble("realizedPnl"));
                        long tradeTime = latestTradeInfo.getLong("time");
                        trade.setExitTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(tradeTime), ZoneId.systemDefault()));

                        tradeHistoryRepository.save(trade);
                        logger.info("--- MONITOR: Đã cập nhật lịch sử đóng lệnh thành công cho trade ID {}.", trade.getId());
                    } else {
                        logger.error("--- MONITOR: Không thể lấy thông tin giao dịch cuối cùng để cập nhật lịch sử cho User [{}]", user.getUsername());
                    }
                }
            } catch (Exception e) {
                logger.error("--- MONITOR: Lỗi khi giám sát lệnh ID {}: {}", trade.getId(), e.getMessage());
            }
        }
    }

    /**
     * Quét tìm các cơ hội giao dịch mới cho các user đang hoạt động.
     */
    private void scanForNewTrades() {
        logger.info("--- SCANNER: Bắt đầu quét các cơ hội giao dịch mới ---");
        List<UserAccount> activeUsers = userAccountRepository.findByTradingConfig_IsActiveTrue();

        if (activeUsers.isEmpty()) {
            logger.info("--- SCANNER: Không có tài khoản nào đang hoạt động. ---");
            return;
        }

        for (String coin : coinsToScan) {
            for (UserAccount user : activeUsers) {
                // Giao phó toàn bộ logic phân tích và đặt lệnh cho TradingExecutionService
                tradingExecutionService.executeForUserAndCoin(user, coin.trim());
            }
        }
        logger.info("--- SCANNER: Hoàn tất chu kỳ quét ---");
    }

    /**
     * Hàm phụ trợ để tạo client Binance cho một người dùng cụ thể.
     */
    private UMFuturesClientImpl createClientForUser(UserAccount user) {
        String apiKey = encryptionService.decrypt(user.getEncryptedApiKey());
        String secretKey = encryptionService.decrypt(user.getEncryptedApiSecret());
        return binanceApiService.createClient(apiKey, secretKey);
    }
}