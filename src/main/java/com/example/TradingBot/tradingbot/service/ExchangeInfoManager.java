package com.example.TradingBot.tradingbot.service;


import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import jakarta.annotation.PostConstruct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



@Component
public class ExchangeInfoManager {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeInfoManager.class);
    private final UMFuturesClientImpl client;
    private final Map<String, SymbolFilter> symbolFilters = new ConcurrentHashMap<>();

    public ExchangeInfoManager(@Value("${binance.api.base-url}") String baseUrl) {
        this.client = new UMFuturesClientImpl(baseUrl);
    }

    @PostConstruct
    public void init() {
        try {
            logger.info("Đang tải thông tin Exchange (Exchange Information)...");
            String response = client.market().exchangeInfo();
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray symbols = jsonResponse.getJSONArray("symbols");

            for (int i = 0; i < symbols.length(); i++) {
                JSONObject symbolInfo = symbols.getJSONObject(i);
                if (!"TRADING".equals(symbolInfo.getString("status"))) continue;

                String symbolName = symbolInfo.getString("symbol");
                // (MỚI) Lấy pricePrecision
                int pricePrecision = symbolInfo.getInt("pricePrecision");
                String stepSize = "1"; // Mặc định

                JSONArray filters = symbolInfo.getJSONArray("filters");
                for (int j = 0; j < filters.length(); j++) {
                    JSONObject filter = filters.getJSONObject(j);
                    if ("LOT_SIZE".equals(filter.getString("filterType"))) {
                        stepSize = filter.getString("stepSize");
                    }
                }
                symbolFilters.put(symbolName, new SymbolFilter(stepSize, pricePrecision));
            }
            logger.info("Đã tải thành công thông tin cho {} symbols.", symbolFilters.size());
        } catch (Exception e) {
            logger.error("LỖI NGHIÊM TRỌNG: Không thể tải Exchange Information.", e);
        }
    }

    public String formatQuantity(String symbol, double quantity) {
        SymbolFilter filter = symbolFilters.get(symbol);
        if (filter == null) return String.format("%.3f", quantity);

        BigDecimal qtyDecimal = BigDecimal.valueOf(quantity);
        BigDecimal stepSizeDecimal = new BigDecimal(filter.stepSize);
        BigDecimal formattedQty = qtyDecimal.divide(stepSizeDecimal, 0, RoundingMode.FLOOR).multiply(stepSizeDecimal);
        return formattedQty.toPlainString();
    }

    /**
     * (MỚI) Hàm định dạng giá theo đúng pricePrecision của symbol.
     */
    public String formatPrice(String symbol, double price) {
        SymbolFilter filter = symbolFilters.get(symbol);
        if (filter == null) {
            // Nếu không tìm thấy, trả về một định dạng an toàn
            return String.format("%.4f", price);
        }

        BigDecimal priceDecimal = BigDecimal.valueOf(price);
        // Làm tròn đến số chữ số thập phân cho phép
        return priceDecimal.setScale(filter.pricePrecision, RoundingMode.HALF_UP).toPlainString();
    }

    private static class SymbolFilter {
        final String stepSize;
        final int pricePrecision; // <-- Thêm trường mới

        SymbolFilter(String stepSize, int pricePrecision) {
            this.stepSize = stepSize;
            this.pricePrecision = pricePrecision;
        }
    }
}