package com.example.TradingBot.tradingbot.repository;

import com.example.TradingBot.tradingbot.entity.DemoPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemoPlanRepository extends MongoRepository<DemoPlan, String> {
    List<DemoPlan> findByUserId(String userId);
    List<DemoPlan> findByUserIdAndActive(String userId, boolean active);
}

