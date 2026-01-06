package com.example.TradingBot.tradingbot.service;

import com.example.TradingBot.auth.model.UserAccount;
import com.example.TradingBot.auth.repository.UserAccountRepository;
import com.example.TradingBot.tradingbot.entity.DemoPlan;
import com.example.TradingBot.tradingbot.repository.DemoPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class DemoPlanService {

    private static final Logger logger = LoggerFactory.getLogger(DemoPlanService.class);

    @Autowired
    private DemoPlanRepository demoPlanRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private MarketDataService marketDataService;

    public DemoPlan createDemoPlan(DemoPlan plan) {
        if (plan.getUserId() != null) {
            UserAccount user = userAccountRepository.findById(plan.getUserId()).orElse(null);
            if (user != null) {
                plan.setUsername(user.getUsername());
            }
        }
        
        if (plan.getNextRun() == null) {
            plan.setNextRun(computeNextRun(plan));
        }
        
        plan.setActive(true);
        return demoPlanRepository.save(plan);
    }

    public List<DemoPlan> findByUser(String userId) {
        return demoPlanRepository.findByUserId(userId);
    }

    public DemoPlan findById(String id) {
        return demoPlanRepository.findById(id).orElse(null);
    }

    public void deleteDemoPlan(String id) {
        demoPlanRepository.deleteById(id);
    }

    public DemoPlan updateDemoPlan(DemoPlan plan) {
        return demoPlanRepository.save(plan);
    }

    public DemoPlan toggleActive(String id) {
        DemoPlan plan = findById(id);
        if (plan != null) {
            plan.setActive(!plan.isActive());
            return demoPlanRepository.save(plan);
        }
        return null;
    }

    public void executeDueDemoPlans() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        List<DemoPlan> allActivePlans = demoPlanRepository.findAll();
        
        for (DemoPlan plan : allActivePlans) {
            if (!plan.isActive()) continue;
            if (plan.getNextRun() == null) continue;
            
            if (plan.getNextRun().isBefore(now) || plan.getNextRun().isEqual(now)) {
                try {
                    logger.info("Executing demo plan id {} for user {}", plan.getId(), plan.getUserId());
                    executeDemoTrade(plan);
                    
                    plan.setNextRun(computeNextRun(plan));
                    if ("NONE".equals(plan.getRecurrence())) {
                        plan.setActive(false);
                    }
                    demoPlanRepository.save(plan);
                    
                } catch (Exception e) {
                    logger.error("Failed to execute demo plan {}: {}", plan.getId(), e.getMessage());
                }
            }
        }
    }

    private void executeDemoTrade(DemoPlan plan) {
        try {
            double currentPrice = marketDataService.getCurrentPrice(plan.getSymbol());
            
            if ("BUY".equals(plan.getSide())) {
                double coins = plan.getAmountUSD() / currentPrice;
                plan.setTotalCoins(plan.getTotalCoins() + coins);
                plan.setTotalInvested(plan.getTotalInvested() + plan.getAmountUSD());
            } else if ("SELL".equals(plan.getSide())) {
                double coins = plan.getAmountUSD() / currentPrice;
                plan.setTotalCoins(plan.getTotalCoins() - coins);
                plan.setTotalInvested(plan.getTotalInvested() - plan.getAmountUSD());
            }
            
            plan.setCurrentValue(plan.getTotalCoins() * currentPrice);
            
            logger.info("Demo trade executed: {} {} {} at ${}. Total coins: {}", 
                plan.getSide(), plan.getAmountUSD(), plan.getSymbol(), currentPrice, plan.getTotalCoins());
                
        } catch (Exception e) {
            logger.error("Error executing demo trade: {}", e.getMessage());
        }
    }

    private LocalDateTime computeNextRun(DemoPlan plan) {
        LocalDateTime base = plan.getNextRun() != null ? plan.getNextRun() : LocalDateTime.now(ZoneId.of("UTC"));
        
        if (plan.getTargetHour() != null) {
            base = base.withHour(plan.getTargetHour()).withMinute(0).withSecond(0).withNano(0);
        }
        
        switch (plan.getRecurrence()) {
            case "DAILY":
                return base.plusDays(1);
            case "WEEKLY":
                if (plan.getTargetWeekday() != null) {
                    base = base.with(java.time.temporal.TemporalAdjusters.nextOrSame(
                        java.time.DayOfWeek.of(plan.getTargetWeekday() == 0 ? 7 : plan.getTargetWeekday())));
                }
                return base.plusWeeks(1);
            case "MONTHLY":
                if (plan.getTargetDate() != null) {
                    base = base.withDayOfMonth(plan.getTargetDate());
                    if (base.isBefore(LocalDateTime.now())) {
                        base = base.plusMonths(1);
                    }
                }
                return base.plusMonths(1);
            case "NONE":
            default:
                return base;
        }
    }
}

