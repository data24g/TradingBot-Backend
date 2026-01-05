//package com.example.TradingBot.auth.controller;
//import com.example.TradingBot.auth.dto.ChangePasswordRequest;
//import com.example.TradingBot.auth.dto.CreateUserRequest;
//import com.example.TradingBot.auth.model.Role;
//import com.example.TradingBot.auth.model.User;
//import com.example.TradingBot.auth.repository.UserRepository;
//import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/user")
//public class UserController {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Autowired
//    public UserController(UserRepository userRepository,  PasswordEncoder passwordEncoder) {
//        this.userRepository = userRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    @GetMapping("/profile")
//    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
//        User user = userRepository.findByEmail(userDetails.getUsername())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + userDetails.getUsername()));
//
//
//        if (user.getRefCode() != null && !user.getRefCode().isBlank()) {
//            long referralCount = userRepository.countByReferrerCode(user.getRefCode());
//            user.setReferralCount((int) referralCount);
//        }
//
//        return ResponseEntity.ok(user);
//    }
//
//    @PostMapping("/change-password")
//    public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordRequest request,
//                                            @AuthenticationPrincipal UserDetails userDetails) {
//
//        User user = userRepository.findByEmail(userDetails.getUsername())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
//
//        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác");
//        }
//
//        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
//        userRepository.save(user);
//
//        return ResponseEntity.ok("Đổi mật khẩu thành công");
//    }
//
//    /**
//     * [ADMIN] Lấy danh sách tất cả người dùng trong hệ thống.
//     */
//    @PreAuthorize("hasRole('ADMIN')")
//    @GetMapping("/all")
//    public ResponseEntity<List<User>> getAllUsers() {
//        List<User> users = userRepository.findAll();
//        users.forEach(user -> {
//            if (user.getRefCode() != null && !user.getRefCode().isBlank()) {
//                user.setReferralCount((int) userRepository.countByReferrerCode(user.getRefCode()));
//            }
//        });
//
//        return ResponseEntity.ok(users);
//    }
//
//
//
//
//
//
//
//    // =============================================================
//    // === CÁC HÀM XỬ LÝ NỘI BỘ (PRIVATE METHODS) ===
//    // =============================================================
//
//    /**
//     * Hàm dùng chung để tạo người dùng với một Role cụ thể (trừ DAILY).
//     */
//    private User createUser(@Valid CreateUserRequest request, Role role) {
//        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng.");
//        }
//
//
//        User newUser = User.builder()
//                .email(request.getEmail())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .role(role)
//                .phone(request.getPhone())
//                .username(request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : request.getEmail())
//                .referrerCode(request.getReferrerCode() != null && !request.getReferrerCode().isBlank() ? request.getReferrerCode() : null)
//                // Chỉ set AgentDetails cho DAILY
//                .build();
//
//        newUser = userRepository.save(newUser);
//        return newUser;
//    }
//
//
//}