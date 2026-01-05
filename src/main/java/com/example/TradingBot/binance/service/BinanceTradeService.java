package com.example.TradingBot.binance.service;


import com.binance.connector.client.SpotClient;
import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.impl.SpotClientImpl;
//import com.xosotv.live.xosotv_backend.binance_service.model.UserApiKeys;
//import com.xosotv.live.xosotv_backend.binance_service.repository.UserApiKeysRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.TradingBot.auth.model.UserAccount;
import com.example.TradingBot.auth.repository.UserAccountRepository;
import com.example.TradingBot.auth.service.EncryptionService;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

@Service
public class BinanceTradeService {

    private static final Logger log = LoggerFactory.getLogger(BinanceTradeService.class);

    private final UserAccountRepository userApiKeysRepository;
    private final EncryptionService encryptionService;

    public BinanceTradeService(UserAccountRepository userApiKeysRepository, EncryptionService encryptionService) {
        this.userApiKeysRepository = userApiKeysRepository;
        this.encryptionService = encryptionService;
    }

    /**
     * Thực hiện một lệnh MUA MARKET trên Binance bằng cách chỉ định số lượng quote asset (ví dụ: USDT) muốn chi tiêu.
     * @param userId ID của người dùng để lấy API keys.
     * @param symbol Cặp giao dịch theo định dạng của Binance (ví dụ: "BTCUSDT").
     * @param quoteOrderQty Số lượng tài sản định giá (USDT) muốn chi tiêu.
     * @return Chuỗi JSON kết quả từ Binance nếu thành công.
     * @throws BinanceClientException nếu API trả về lỗi.
     */
    public String executeMarketBuy(String userId, String symbol, BigDecimal quoteOrderQty) {
        log.info("Executing MARKET BUY on Binance for user: {}, symbol: {}, quoteQty: {}", userId, symbol, quoteOrderQty);
        SpotClient client = getAuthenticatedClient(userId);

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);
        parameters.put("side", "BUY");
        parameters.put("type", "MARKET");
        // quoteOrderQty: Chỉ định số tiền (USDT) bạn muốn chi tiêu. API sẽ tự tính số lượng token mua được.
        parameters.put("quoteOrderQty", quoteOrderQty.toPlainString());

        try {
            String result = client.createTrade().newOrder(parameters);
            log.info("Binance MARKET BUY successful. Result: {}", result);
            return result;
        } catch (BinanceClientException e) {
            log.error("Binance MARKET BUY failed for user {}. Error - Code: {}, Message: {}", userId, e.getErrorCode(), e.getErrMsg());
            throw e; // Ném lại lỗi để tầng trên có thể xử lý
        }
    }

    /**
     * Thực hiện một lệnh BÁN MARKET trên Binance bằng cách chỉ định số lượng base asset (ví dụ: BTC) muốn bán.
     * @param userId ID của người dùng để lấy API keys.
     * @param symbol Cặp giao dịch theo định dạng của Binance (ví dụ: "BTCUSDT").
     * @param quantity Số lượng token muốn bán.
     * @return Chuỗi JSON kết quả từ Binance nếu thành công.
     * @throws BinanceClientException nếu API trả về lỗi.
     */
    public String executeMarketSell(String userId, String symbol, BigDecimal quantity) {
        log.info("Executing MARKET SELL on Binance for user: {}, symbol: {}, quantity: {}", userId, symbol, quantity);
        SpotClient client = getAuthenticatedClient(userId);

        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);
        parameters.put("side", "SELL");
        parameters.put("type", "MARKET");
        // quantity: Chỉ định số lượng token (BTC, ETH...) bạn muốn bán.
        parameters.put("quantity", quantity.toPlainString());

        try {
            String result = client.createTrade().newOrder(parameters);
            log.info("Binance MARKET SELL successful. Result: {}", result);
            return result;
        } catch (BinanceClientException e) {
            log.error("Binance MARKET SELL failed for user {}. Error - Code: {}, Message: {}", userId, e.getErrorCode(), e.getErrMsg());
            throw e;
        }
    }

    /**
     * Lấy API keys của người dùng và tạo một SpotClient đã được xác thực.
     */
    private SpotClient getAuthenticatedClient(String userId) {
        UserAccount apiKeys = userApiKeysRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy API keys cho người dùng: " + userId));

        String apiKey = apiKeys.getEncryptedApiKey();
        String secretKey = apiKeys.getEncryptedApiSecret();

        if (apiKey == null || secretKey == null) {
            throw new IllegalStateException("API key/secret trống cho người dùng: " + userId);
        }

        String decryptedApiKey = encryptionService.decrypt(apiKey);
        String decryptedSecret = encryptionService.decrypt(secretKey);

        return new SpotClientImpl(decryptedApiKey, decryptedSecret);
    }
}
