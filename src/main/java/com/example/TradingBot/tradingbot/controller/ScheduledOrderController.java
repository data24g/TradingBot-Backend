package com.example.TradingBot.tradingbot.controller;

import com.example.TradingBot.tradingbot.entity.ScheduledOrder;
import com.example.TradingBot.tradingbot.service.ScheduledOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dca")
@CrossOrigin(origins = "*")
public class ScheduledOrderController {

    @Autowired
    private ScheduledOrderService scheduledOrderService;

    /**
     * Tạo mới một lệnh DCA
     * POST /api/dca/orders
     */
    @PostMapping("/orders")
    public ResponseEntity<ScheduledOrder> createOrder(@RequestBody ScheduledOrder order) {
        ScheduledOrder created = scheduledOrderService.createOrder(order);
        return ResponseEntity.ok(created);
    }

    /**
     * Lấy danh sách lệnh DCA của một user
     * GET /api/dca/orders/{userId}
     */
    @GetMapping("/orders/{userId}")
    public ResponseEntity<List<ScheduledOrder>> getOrdersByUser(@PathVariable String userId) {
        List<ScheduledOrder> orders = scheduledOrderService.findByUser(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Xóa một lệnh DCA
     * DELETE /api/dca/orders/{id}
     */
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        // Cần thêm method delete trong ScheduledOrderService
        // scheduledOrderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}

