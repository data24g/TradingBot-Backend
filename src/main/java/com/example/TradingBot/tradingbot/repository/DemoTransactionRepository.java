package com.example.TradingBot.tradingbot.repository;

import com.example.TradingBot.tradingbot.entity.DemoTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemoTransactionRepository extends MongoRepository<DemoTransaction, String> {
    List<DemoTransaction> findByPlanId(String planId);
    List<DemoTransaction> findByUserId(String userId);
    List<DemoTransaction> findByPlanIdOrderByDateDesc(String planId);
    List<DemoTransaction> findByUserIdAndSymbol(String userId, String symbol);
}

