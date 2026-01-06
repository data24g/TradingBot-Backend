package com.example.TradingBot.tradingbot.service;

import com.example.TradingBot.auth.model.UserAccount;
import com.example.TradingBot.auth.repository.UserAccountRepository;
import com.example.TradingBot.tradingbot.entity.DemoPlan;
import com.example.TradingBot.tradingbot.entity.DemoTransaction;
import com.example.TradingBot.tradingbot.repository.DemoPlanRepository;
import com.example.TradingBot.tradingbot.repository.DemoTransactionRepository;
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
    private DemoTransactionRepository demoTransactionRepository;

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
            double coins = plan.getAmountUSD() / currentPrice;
            
            if ("BUY".equals(plan.getSide())) {
                plan.setTotalCoins(plan.getTotalCoins() + coins);
                plan.setTotalInvested(plan.getTotalInvested() + plan.getAmountUSD());
            } else if ("SELL".equals(plan.getSide())) {
                plan.setTotalCoins(plan.getTotalCoins() - coins);
                plan.setTotalInvested(plan.getTotalInvested() - plan.getAmountUSD());
            }
            
            plan.setCurrentValue(plan.getTotalCoins() * currentPrice);
            
            // Lưu transaction vào database
            DemoTransaction transaction = new DemoTransaction();
            transaction.setPlanId(plan.getId());
            transaction.setUserId(plan.getUserId());
            transaction.setSymbol(plan.getSymbol());
            transaction.setSide(plan.getSide());
            transaction.setPrice(currentPrice);
            transaction.setAmountUSDT(plan.getAmountUSD());
            transaction.setAmountCoin(coins);
            // Date will be set by constructor using Instant.now().toString() for proper ISO format
            transaction.setStatus("SUCCESS");
            demoTransactionRepository.save(transaction);
            
            logger.info("Demo trade executed: {} {} {} at ${}. Total coins: {}. Transaction saved: {}", 
                plan.getSide(), plan.getAmountUSD(), plan.getSymbol(), currentPrice, plan.getTotalCoins(), transaction.getId());
                
        } catch (Exception e) {
            logger.error("Error executing demo trade: {}", e.getMessage());
        }
    }

    // Method để tạo transaction từ API (Frontend gọi)
    public DemoTransaction createTransaction(DemoTransaction transaction) {
        DemoPlan plan = findById(transaction.getPlanId());
        if (plan != null) {
            // Cập nhật plan totals
            double currentPrice = transaction.getPrice();
            double coins = transaction.getAmountCoin();
            
            if ("BUY".equals(transaction.getSide())) {
                plan.setTotalCoins(plan.getTotalCoins() + coins);
                plan.setTotalInvested(plan.getTotalInvested() + transaction.getAmountUSDT());
            } else if ("SELL".equals(transaction.getSide())) {
                plan.setTotalCoins(plan.getTotalCoins() - coins);
                plan.setTotalInvested(plan.getTotalInvested() - transaction.getAmountUSDT());
            }
            
            plan.setCurrentValue(plan.getTotalCoins() * currentPrice);
            demoPlanRepository.save(plan);
        }
        
        return demoTransactionRepository.save(transaction);
    }

    // Lấy tất cả transactions của một plan
    public List<DemoTransaction> getTransactionsByPlanId(String planId) {
        return demoTransactionRepository.findByPlanIdOrderByDateDesc(planId);
    }

    // Lấy tất cả transactions của một user
    public List<DemoTransaction> getTransactionsByUserId(String userId) {
        return demoTransactionRepository.findByUserId(userId);
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

