package com.example.TradingBot.tradingbot.service;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

/**
 * Service chuẩn hóa để tương tác với API Binance Futures,
 * sử dụng tên phương thức đúng của thư viện binance-connector-java v3.x.
 */

/**
 * Service chuẩn hóa để tương tác với API Binance Futures.
 * Phiên bản này sử dụng các tên phương thức tương thích ngược (fallback)
 * để giải quyết các vấn đề về phiên bản thư viện.
 */
@Service
public class BinanceApiService {

    private static final Logger logger = LoggerFactory.getLogger(BinanceApiService.class);

    @Value("${binance.api.base-url}")
    private String baseUrl;

    @Autowired
    private ExchangeInfoManager exchangeInfoManager;

    public UMFuturesClientImpl createClient(String apiKey, String secretKey) {
        return new UMFuturesClientImpl(apiKey, secretKey, baseUrl);
    }

    public void setLeverage(UMFuturesClientImpl client, String symbol, int leverage) {
        try {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("symbol", symbol);
            params.put("leverage", leverage);
            client.account().changeInitialLeverage(params);
            logger.info("Đã đặt đòn bẩy thành công: Symbol={}, Leverage={}x", symbol, leverage);
        } catch (Exception e) {
            logger.error("LỖI KHI ĐẶT ĐÒN BẨY cho Symbol {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Không thể đặt đòn bẩy.", e);
        }
    }

    public BigDecimal getAccountBalance(UMFuturesClientImpl client, String asset) {
        try {
            String response = client.account().accountInformation(new LinkedHashMap<>());
            JSONObject accountInfo = new JSONObject(response);
            JSONArray assets = accountInfo.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject balance = assets.getJSONObject(i);
                if (asset.equals(balance.getString("asset"))) {
                    return balance.getBigDecimal("availableBalance");
                }
            }
            logger.warn("Không tìm thấy số dư cho tài sản: {}", asset);
            return BigDecimal.ZERO;
        } catch (Exception e) {
            logger.error("LỖI KHI LẤY SỐ DƯ TÀI KHOẢN: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * (FALLBACK) Sử dụng tên phương thức cũ hơn 'positionInformation'.
     */
    public boolean hasOpenPosition(UMFuturesClientImpl client, String symbol) {
        try {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("symbol", symbol);
            String response = client.account().positionInformation(params);
            JSONArray positions = new JSONArray(response);

            if (positions.length() > 0) {
                for(int i = 0; i < positions.length(); i++) {
                    JSONObject positionInfo = positions.getJSONObject(i);
                    if(symbol.equals(positionInfo.getString("symbol"))) {
                        BigDecimal positionAmount = positionInfo.getBigDecimal("positionAmt");
                        if (positionAmount.compareTo(BigDecimal.ZERO) != 0) {
                            logger.debug("Phát hiện vị thế đang mở cho Symbol {}: Số lượng = {}", symbol, positionAmount);
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Lỗi khi kiểm tra vị thế mở cho Symbol {}: {}", symbol, e.getMessage());
            return false;
        }
    }

    public void placeNewOrder(
            UMFuturesClientImpl client,
            String symbol,
            String side,
            double quantity,
            double entryPrice,
            double stopLossPrice,
            double takeProfitPrice
    ) {
        try {
            // ... (Logic định dạng và đặt lệnh giữ nguyên) ...
            String formattedQuantity = exchangeInfoManager.formatQuantity(symbol, quantity);
            if (new BigDecimal(formattedQuantity).compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Hủy đặt lệnh cho {}: Số lượng sau khi làm tròn quá nhỏ ({}).", symbol, formattedQuantity);
                return;
            }
            String formattedStopLoss = exchangeInfoManager.formatPrice(symbol, stopLossPrice);
            String formattedTakeProfit = exchangeInfoManager.formatPrice(symbol, takeProfitPrice);
            logger.info("Chuẩn bị đặt lệnh cho {}: quantity={}, sl={}, tp={}",
                    symbol, formattedQuantity, formattedStopLoss, formattedTakeProfit);
            LinkedHashMap<String, Object> orderParams = new LinkedHashMap<>();
            orderParams.put("symbol", symbol);
            orderParams.put("side", side);
            orderParams.put("type", "MARKET");
            orderParams.put("quantity", formattedQuantity);
            client.account().newOrder(orderParams);
            logger.info("Đã vào lệnh thành công {} {}", symbol, side);
            String oppositeSide = "BUY".equals(side) ? "SELL" : "BUY";
            LinkedHashMap<String, Object> slParams = new LinkedHashMap<>();
            slParams.put("symbol", symbol);
            slParams.put("side", oppositeSide);
            slParams.put("type", "STOP_MARKET");
            slParams.put("stopPrice", formattedStopLoss);
            slParams.put("closePosition", true);
            client.account().newOrder(slParams);
            logger.info("STOP LOSS đặt OK: {}", formattedStopLoss);
            LinkedHashMap<String, Object> tpParams = new LinkedHashMap<>();
            tpParams.put("symbol", symbol);
            tpParams.put("side", oppositeSide);
            tpParams.put("type", "TAKE_PROFIT_MARKET");
            tpParams.put("stopPrice", formattedTakeProfit);
            tpParams.put("closePosition", true);
            client.account().newOrder(tpParams);
            logger.info("TAKE PROFIT đặt OK: {}", formattedTakeProfit);
        } catch (Exception e) {
            logger.error("LỖI khi đặt bộ lệnh: {}", e.getMessage());
            throw new RuntimeException("Không thể đặt bộ lệnh", e);
        }
    }

    /**
     * (FALLBACK) Sử dụng tên phương thức cũ nhất 'accountTradeList'.
     */
    public JSONObject getLatestTradeForSymbol(UMFuturesClientImpl client, String symbol) {
        try {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("symbol", symbol);
            params.put("limit", 1);
            String response = client.account().accountTradeList(params); // <-- SỬ DỤNG TÊN CŨ NHẤT
            JSONArray trades = new JSONArray(response);

            if (trades.length() > 0) {
                return trades.getJSONObject(0);
            }
            return null;
        } catch (Exception e) {
            logger.error("Lỗi khi lấy giao dịch cuối cùng cho Symbol {}: {}", symbol, e.getMessage());
            return null;
        }
    }
}