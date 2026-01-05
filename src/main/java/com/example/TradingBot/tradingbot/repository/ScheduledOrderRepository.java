package com.example.TradingBot.tradingbot.repository;

import com.example.TradingBot.tradingbot.entity.ScheduledOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledOrderRepository extends MongoRepository<ScheduledOrder, String> {
    List<ScheduledOrder> findByActiveTrueAndNextRunBefore(LocalDateTime time);
    List<ScheduledOrder> findByUserId(String userId);
}
