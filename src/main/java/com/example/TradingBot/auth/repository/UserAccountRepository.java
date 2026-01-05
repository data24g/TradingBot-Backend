package com.example.TradingBot.auth.repository;


import com.example.TradingBot.auth.model.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
// Kiểu ID bây giờ là String
public interface UserAccountRepository extends MongoRepository<UserAccount, String> {
    // Spring Data MongoDB đủ thông minh để tạo truy vấn cho các trường nhúng
    // Tên phương thức có nghĩa là: "Tìm tất cả các tài liệu UserAccount
    // nơi mà trường tradingConfig có trường con isActive bằng true"
    List<UserAccount> findByTradingConfig_IsActiveTrue();
}
