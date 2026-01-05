package com.example.TradingBot.tradingbot.service;

import com.example.TradingBot.tradingbot.entity.TradeHistory;
import com.example.TradingBot.tradingbot.repository.TradeHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // Import tiện ích kiểm tra chuỗi

import java.util.List;

@Service
public class TradeHistoryService {

    @Autowired
    private TradeHistoryRepository tradeHistoryRepository;

    /**
     * Lấy danh sách lịch sử giao dịch dựa trên các bộ lọc.
     * @param userId Lọc theo ID người dùng.
     * @param symbol Lọc theo tên token/symbol.
     * @param status Lọc theo trạng thái (OPEN/CLOSED).
     * @return Danh sách TradeHistory.
     */
    public List<TradeHistory> getTradeHistory(String userId, String symbol, String status) {

        // Sử dụng StringUtils.hasText để kiểm tra một chuỗi không null và không rỗng
        boolean hasUser = StringUtils.hasText(userId);
        boolean hasSymbol = StringUtils.hasText(symbol);
        boolean hasStatus = StringUtils.hasText(status);

        // Logic để gọi đúng phương thức repository dựa trên các tham số được cung cấp
        if (hasUser && hasSymbol && hasStatus) {
            return tradeHistoryRepository.findByUserIdAndSymbolAndStatus(userId, symbol, status);
        } else if (hasUser && hasSymbol) {
            return tradeHistoryRepository.findByUserIdAndSymbol(userId, symbol);
        } else if (hasUser && hasStatus) {
            return tradeHistoryRepository.findByUserIdAndStatus(userId, status);
        } else if (hasSymbol && hasStatus) {
            return tradeHistoryRepository.findBySymbolAndStatus(symbol, status);
        } else if (hasUser) {
            return tradeHistoryRepository.findByUserId(userId);
        } else if (hasSymbol) {
            return tradeHistoryRepository.findBySymbol(symbol);
        } else if (hasStatus) {
            return tradeHistoryRepository.findByStatus(status);
        } else {
            // Nếu không có bộ lọc nào, trả về tất cả
            return tradeHistoryRepository.findAll();
        }
    }
}
