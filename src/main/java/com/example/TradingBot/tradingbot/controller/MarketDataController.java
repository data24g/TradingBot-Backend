package com.example.TradingBot.tradingbot.controller;


import com.example.TradingBot.tradingbot.service.MarketDataService;
import com.example.TradingBot.tradingbot.dto.MarketStateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    @Autowired
    private MarketDataService marketDataService;

    /**
     * (VIẾT LẠI) API để lấy trạng thái thị trường đầy đủ, bao gồm giá hiện tại,
     * xu hướng và biên độ (nếu có) của một cặp giao dịch.
     * @param symbol Tên cặp giao dịch, ví dụ: BTCUSDT.
     * @return Một đối tượng JSON chứa thông tin phân tích.
     */
    @GetMapping("/state/{symbol}") // <-- Đổi tên endpoint
    public ResponseEntity<?> getMarketState(@PathVariable String symbol) {
        MarketStateDTO data = marketDataService.getMarketStateData(symbol.toUpperCase());

        if (data != null) {
            return ResponseEntity.ok(data);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}