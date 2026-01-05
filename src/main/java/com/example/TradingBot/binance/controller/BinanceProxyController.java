package com.example.TradingBot.binance.controller;


import com.example.TradingBot.binance.dto.BinanceProxyDTO;
import com.example.TradingBot.binance.service.BinanceProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/binance/proxy")
@CrossOrigin(origins = "*") // Cho phép Frontend gọi sang (tránh lỗi CORS khác)
public class BinanceProxyController {

    @Autowired
    private BinanceProxyService binanceProxyService;

    @PostMapping("/spot/account")
    public ResponseEntity<?> getSpotAccount(@RequestBody BinanceProxyDTO dto) {
        try {
            return ResponseEntity.ok(binanceProxyService.getSpotAccount(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/futures/balance")
    public ResponseEntity<?> getFuturesBalance(@RequestBody BinanceProxyDTO dto) {
        try {
            return ResponseEntity.ok(binanceProxyService.getFuturesBalance(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/futures/positions")
    public ResponseEntity<?> getFuturesPositions(@RequestBody BinanceProxyDTO dto) {
        try {
            return ResponseEntity.ok(binanceProxyService.getFuturesPositions(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
