package com.example.TradingBot.auth.controller;
import com.example.TradingBot.auth.dto.ApiCredentialsDTO;
import com.example.TradingBot.auth.dto.UserCreateDTO;
import com.example.TradingBot.auth.dto.UserDetailDTO;
import com.example.TradingBot.auth.service.UserManagementService;
import com.example.TradingBot.tradingbot.dto.TradingConfigDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.TradingBot.auth.dto.*;
import java.util.List;
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserManagementService userManagementService;

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateDTO dto) {
        return ResponseEntity.ok(userManagementService.createUser(dto));
    }

    @PutMapping("/{userId}/activate")
    public ResponseEntity<String> activateBot(@PathVariable String userId) {
        userManagementService.toggleBot(userId, true);
        return ResponseEntity.ok("Bot activated for user " + userId);
    }

    @PutMapping("/{userId}/deactivate")
    public ResponseEntity<String> deactivateBot(@PathVariable String userId) {
        userManagementService.toggleBot(userId, false);
        return ResponseEntity.ok("Bot deactivated for user " + userId);
    }

    /**
     * Endpoint để cập nhật cấu hình giao dịch của người dùng.
     */
    @PutMapping("/{userId}/config")
    public ResponseEntity<?> updateTradingConfig(@PathVariable String userId, @Valid @RequestBody TradingConfigDTO dto) {
        return ResponseEntity.ok(userManagementService.updateConfig(userId, dto));
    }

    /**
     * (MỚI) Endpoint để cập nhật API credentials của một người dùng.
     * @param userId ID của người dùng.
     * @param dto Request body chứa API key và secret key mới.
     * @return Thông tin người dùng đã được cập nhật.
     */
    @PutMapping("/{userId}/credentials")
    public ResponseEntity<?> updateApiCredentials(
            @PathVariable String userId,
            @Valid @RequestBody ApiCredentialsDTO dto) {

        return ResponseEntity.ok(userManagementService.updateApiCredentials(userId, dto));
    }

    /**
     * (MỚI) API để lấy danh sách tất cả các tài khoản người dùng trong hệ thống.
     * @return Một danh sách thông tin người dùng (không bao gồm thông tin nhạy cảm).
     */

    @GetMapping
    public ResponseEntity<List<UserDetailDTO>> getAllUsers() { // <-- THAY ĐỔI KIỂU TRẢ VỀ
        List<UserDetailDTO> users = userManagementService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailDTO> getUserById(@PathVariable String userId) {
        UserDetailDTO userDetail = userManagementService.getUserById(userId);
        return ResponseEntity.ok(userDetail);
    }

    @GetMapping("/{userId}/keys")
    public ResponseEntity<UserKeysDTO> getUserKeys(@PathVariable String userId) {
        UserKeysDTO keys = userManagementService.getUserKeys(userId);
        return ResponseEntity.ok(keys);
    }
}
