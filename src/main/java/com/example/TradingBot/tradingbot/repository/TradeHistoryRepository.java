package com.example.TradingBot.tradingbot.repository;

import com.example.TradingBot.tradingbot.entity.TradeHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // <-- Import

@Repository
public interface TradeHistoryRepository extends MongoRepository<TradeHistory, String> {

    /**
     * (MỚI) Tìm tất cả các giao dịch theo trạng thái.
     * @param status Trạng thái cần tìm (vd: "OPEN").
     * @return Danh sách các giao dịch.
     */
    List<TradeHistory> findByStatus(String status);

    /**
     * (MỚI) Tìm kiếm lịch sử giao dịch với các bộ lọc tùy chọn.
     * Spring Data sẽ tự động tạo query:
     * - Nếu chỉ có userId -> tìm theo userId.
     * - Nếu có userId và symbol -> tìm theo cả hai.
     * - Nếu có cả 3 -> tìm theo cả 3.
     * Các tham số có giá trị `null` sẽ được bỏ qua.
     * @param userId ID của người dùng (tùy chọn).
     * @param symbol Tên token/cặp giao dịch (tùy chọn, vd: "BTCUSDT").
     * @param status Trạng thái giao dịch (tùy chọn, vd: "OPEN", "CLOSED").
     * @return Danh sách các giao dịch thỏa mãn điều kiện.
     */
    List<TradeHistory> findByUserIdAndSymbolAndStatus(String userId, String symbol, String status);

    // Chúng ta cũng có thể cần các phiên bản ít tham số hơn để tăng tính linh hoạt
    List<TradeHistory> findByUserId(String userId);
    List<TradeHistory> findByUserIdAndSymbol(String userId, String symbol);
    List<TradeHistory> findByUserIdAndStatus(String userId, String status);
    List<TradeHistory> findBySymbolAndStatus(String symbol, String status);
    List<TradeHistory> findBySymbol(String symbol);
}