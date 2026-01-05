package com.example.TradingBot.tradingbot.controller;




import com.example.TradingBot.tradingbot.entity.TradeHistory;
import com.example.TradingBot.tradingbot.service.TradeHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trades") // Một prefix mới cho các API liên quan đến trade
public class TradeHistoryController {

    @Autowired
    private TradeHistoryService tradeHistoryService;

    /**
     * API để lấy lịch sử giao dịch với các bộ lọc linh hoạt.
     * @param userId Lọc theo ID người dùng (tùy chọn).
     * @param symbol Lọc theo symbol (tùy chọn, vd: BTCUSDT).
     * @param status Lọc theo trạng thái (tùy chọn, vd: OPEN, CLOSED).
     * @return Danh sách các giao dịch thỏa mãn điều kiện.
     */
    @GetMapping
    public ResponseEntity<List<TradeHistory>> getTradeHistory(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String status
    ) {
        List<TradeHistory> trades = tradeHistoryService.getTradeHistory(userId, symbol, status);
        return ResponseEntity.ok(trades);
    }
}