package com.example.TradingBot.binance.service;


import com.example.TradingBot.binance.dto.BinanceProxyDTO;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class BinanceProxyService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String SPOT_BASE_URL = "https://api.binance.com";
    private static final String FUTURES_BASE_URL = "https://fapi.binance.com";

    // 1. Lấy thông tin tài khoản Spot (Số dư)
    public Object getSpotAccount(BinanceProxyDTO dto) {
        String queryString = "timestamp=" + System.currentTimeMillis();
        String signature = createSignature(queryString, dto.apiSecret());
        String url = SPOT_BASE_URL + "/api/v3/account?" + queryString + "&signature=" + signature;

        return callBinanceApi(url, dto.apiKey());
    }

    // 2. Lấy số dư Futures
    public Object getFuturesBalance(BinanceProxyDTO dto) {
        String queryString = "timestamp=" + System.currentTimeMillis();
        String signature = createSignature(queryString, dto.apiSecret());
        String url = FUTURES_BASE_URL + "/fapi/v2/balance?" + queryString + "&signature=" + signature;

        return callBinanceApi(url, dto.apiKey());
    }

    // 3. Lấy vị thế Futures (Positions)
    public Object getFuturesPositions(BinanceProxyDTO dto) {
        String queryString = "timestamp=" + System.currentTimeMillis();
        String signature = createSignature(queryString, dto.apiSecret());
        // Binance Futures Position Risk trả về thông tin đòn bẩy, lời lỗ, giá entry...
        String url = FUTURES_BASE_URL + "/fapi/v2/positionRisk?" + queryString + "&signature=" + signature;

        return callBinanceApi(url, dto.apiKey());
    }

    // --- HELPER METHODS ---

    private Object callBinanceApi(String url, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-MBX-APIKEY", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi gọi Binance API: " + e.getMessage());
        }
    }

    private String createSignature(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo chữ ký: " + e.getMessage());
        }
    }
}
