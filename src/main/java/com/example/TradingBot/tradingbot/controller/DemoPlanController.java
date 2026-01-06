package com.example.TradingBot.tradingbot.controller;

import com.example.TradingBot.tradingbot.entity.DemoPlan;
import com.example.TradingBot.tradingbot.service.DemoPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dca/demo")
@CrossOrigin(origins = "*")
public class DemoPlanController {

    @Autowired
    private DemoPlanService demoPlanService;

    @PostMapping("/plan")
    public ResponseEntity<DemoPlan> createDemoPlan(@RequestBody DemoPlan plan) {
        DemoPlan created = demoPlanService.createDemoPlan(plan);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/plans/{userId}")
    public ResponseEntity<List<DemoPlan>> getDemoPlansByUser(@PathVariable String userId) {
        List<DemoPlan> plans = demoPlanService.findByUser(userId);
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/plan/{id}")
    public ResponseEntity<DemoPlan> getDemoPlan(@PathVariable String id) {
        DemoPlan plan = demoPlanService.findById(id);
        if (plan != null) return ResponseEntity.ok(plan);
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/plan/{id}")
    public ResponseEntity<DemoPlan> updateDemoPlan(@PathVariable String id, @RequestBody DemoPlan plan) {
        DemoPlan existing = demoPlanService.findById(id);
        if (existing != null) {
            plan.setId(id);
            return ResponseEntity.ok(demoPlanService.updateDemoPlan(plan));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/plan/{id}/toggle")
    public ResponseEntity<DemoPlan> toggleDemoPlan(@PathVariable String id) {
        DemoPlan plan = demoPlanService.toggleActive(id);
        if (plan != null) return ResponseEntity.ok(plan);
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/plan/{id}")
    public ResponseEntity<Void> deleteDemoPlan(@PathVariable String id) {
        demoPlanService.deleteDemoPlan(id);
        return ResponseEntity.noContent().build();
    }
}

