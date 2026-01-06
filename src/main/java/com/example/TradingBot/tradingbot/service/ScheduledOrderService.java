package com.example.TradingBot.tradingbot.service;

import com.example.TradingBot.binance.service.BinanceTradeService;
import com.example.TradingBot.auth.model.UserAccount;
import com.example.TradingBot.auth.repository.UserAccountRepository;
import com.example.TradingBot.tradingbot.entity.ScheduledOrder;
import com.example.TradingBot.tradingbot.entity.TradeHistory;
import com.example.TradingBot.tradingbot.repository.ScheduledOrderRepository;
import com.example.TradingBot.tradingbot.repository.TradeHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ScheduledOrderService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledOrderService.class);

    @Autowired
    private ScheduledOrderRepository scheduledOrderRepository;

    @Autowired
    private BinanceTradeService binanceTradeService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TradeHistoryRepository tradeHistoryRepository;

    public ScheduledOrder createOrder(ScheduledOrder order) {
        if (order.getNextRun() == null) {
            order.setNextRun(computeNextRun(order));
        }
        return scheduledOrderRepository.save(order);
    }

    public List<ScheduledOrder> findByUser(String userId) {
        return scheduledOrderRepository.findByUserId(userId);
    }

    public void deleteOrder(String id) {
        scheduledOrderRepository.deleteById(id);
    }

    public List<ScheduledOrder> findDueOrders(LocalDateTime now) {
        return scheduledOrderRepository.findByActiveTrueAndNextRunBefore(now);
    }

    public void executeDueOrders() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        List<ScheduledOrder> due = findDueOrders(now);
        if (due.isEmpty()) return;

        for (ScheduledOrder order : due) {
            try {
                logger.info("Executing scheduled order id {} for user {}", order.getId(), order.getUserId());
                // place spot market buy using quote amount
                binanceTradeService.executeMarketBuy(order.getUserId(), order.getSymbol(), BigDecimal.valueOf(order.getAmountUSD()));

                // Save a minimal trade history record
                TradeHistory th = new TradeHistory();
                th.setUserId(order.getUserId());
                UserAccount u = userAccountRepository.findById(order.getUserId()).orElse(null);
                if (u != null) th.setUsername(u.getUsername());
                th.setSymbol(order.getSymbol());
                th.setEntryTime(LocalDateTime.now(ZoneId.of("UTC")));
                th.setOrderSizeUSD(order.getAmountUSD());
                th.setStatus("EXECUTED");
                tradeHistoryRepository.save(th);

                // schedule next run
                order.setNextRun(computeNextRun(order));
                if (order.getRecurrence() == ScheduledOrder.Recurrence.NONE) {
                    order.setActive(false);
                }
                scheduledOrderRepository.save(order);

            } catch (Exception e) {
                logger.error("Failed to execute scheduled order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    private LocalDateTime computeNextRun(ScheduledOrder order) {
        LocalDateTime base = order.getNextRun() != null ? order.getNextRun() : order.getScheduleTime();
        if (base == null) base = LocalDateTime.now(ZoneId.of(order.getTimezone() == null ? "UTC" : order.getTimezone()));

        switch (order.getRecurrence()) {
            case DAILY:
                return base.plusDays(1);
            case WEEKLY:
                return base.plusWeeks(1);
            case MONTHLY:
                return base.plusMonths(1);
            case NONE:
            default:
                return base; // one-shot
        }
    }
}
