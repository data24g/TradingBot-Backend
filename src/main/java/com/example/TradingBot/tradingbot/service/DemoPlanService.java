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
            if (!plan.isActive())
                continue;
            if (plan.getNextRun() == null)
                continue;

            if (plan.getNextRun().isBefore(now) || plan.getNextRun().isEqual(now)) {
                try {
                    logger.info("Executing demo plan id {} for user {}", plan.getId(), plan.getUserId());
                    executeDemoTrade(plan);

                    plan.setNextRun(computeNextRun(plan));
                    if ("NONE".equals(plan.getRecurrence())) {
                        plan.setActive(false);
                        plan.setCompletedAt(LocalDateTime.now());
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
            transaction.setDate(LocalDateTime.now().toString()); // Store as ISO string
            transaction.setPlanCreatedAt(
                    plan.getCreatedAt() != null ? plan.getCreatedAt().toString() : LocalDateTime.now().toString());
            transaction.setStatus("SUCCESS");
            demoTransactionRepository.save(transaction);

            logger.info("Demo trade executed: {} {} {} at ${}. Total coins: {}. Transaction saved: {}",
                    plan.getSide(), plan.getAmountUSD(), plan.getSymbol(), currentPrice, plan.getTotalCoins(),
                    transaction.getId());

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
        // Use the system default timezone which should match application.properties
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = plan.getNextRun() != null ? plan.getNextRun() : now;

        if (plan.getTargetHour() != null) {
            int minute = plan.getTargetMinute() != null ? plan.getTargetMinute() : 0;
            base = base.withHour(plan.getTargetHour()).withMinute(minute).withSecond(0).withNano(0);
        }

        // If this is the initial calculation (creating plan)
        boolean isInitial = (plan.getNextRun() == null);

        switch (plan.getRecurrence()) {
            case "DAILY":
                if (isInitial && base.isAfter(now))
                    return base;
                return base.plusDays(1);
            case "WEEKLY":
                if (plan.getTargetWeekday() != null) {
                    int targetDoW = plan.getTargetWeekday() == 0 ? 7 : plan.getTargetWeekday();
                    base = base
                            .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(targetDoW)));
                }
                if (isInitial && base.isAfter(now))
                    return base;
                return base.plusWeeks(1);
            case "MONTHLY":
                if (plan.getTargetDate() != null) {
                    try {
                        base = base.withDayOfMonth(plan.getTargetDate());
                    } catch (java.time.DateTimeException e) {
                        // Handle months with fewer days by taking the last day
                        base = base.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
                    }
                }
                if (isInitial && base.isAfter(now))
                    return base;
                return base.plusMonths(1);
            case "NONE":
            default:
                if (isInitial && base.isAfter(now))
                    return base;
                return base;
        }
    }
}
