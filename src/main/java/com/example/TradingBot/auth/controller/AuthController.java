package com.example.TradingBot.auth.controller;

import com.example.TradingBot.auth.dto.AuthResponse;
import com.example.TradingBot.auth.dto.LoginRequest;
import com.example.TradingBot.auth.dto.RegisterRequest;
import com.example.TradingBot.auth.model.Role;
import com.example.TradingBot.auth.model.User;
import com.example.TradingBot.auth.repository.UserRepository;
import com.example.TradingBot.auth.service.AuthService;
import com.example.TradingBot.auth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Autowired
    private AuthService authService;


    @Autowired
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // 1. Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email đã tồn tại");
        }

        // 2. Tạo người dùng mới
        User user = User.builder()
                .username(request.getUsername())  // nếu có
                .email(request.getEmail()).referrerCode(request.getReferrerCode())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        // 3. Tạo mã giới thiệu riêng
        user.setRefCode(UUID.randomUUID().toString().substring(0, 6));



        // 5. Lưu người dùng
        User newUser =    userRepository.save(user);
        return ResponseEntity.ok("Đăng ký thành công");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return performRoleLogin(request.getIdentifier(), request.getPassword(), null); // USER login không cần kiểm tra Role

    }


    /**
     * Endpoint để tạo một tài khoản Đại lý mới.
     * Chỉ có ADMIN mới có quyền truy cập.
     * @param request Dữ liệu đăng ký từ client
     * @return Thông tin người dùng đại lý vừa được tạo
     */


    // =============================================================
    // === CÁC API LOGIN PHÂN QUYỀN (MỚI BỔ SUNG) ===
    // =============================================================

    @PostMapping("/admin-login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest request) {
        // Chỉ cho phép đăng nhập nếu Role là ADMIN
        return performRoleLogin(request.getIdentifier(), request.getPassword(), Role.ADMIN);
    }



    private ResponseEntity<?> performRoleLogin(String identifier, String password, Role requiredRole) {
        try {
            // 1. Tìm User theo email hoặc username
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(identifier,identifier);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsernameOrEmail(identifier,identifier);
            }

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Tài khoản không tồn tại");
            }

            User user = userOpt.get();

            // 2. KIỂM TRA ROLE (CHỈ THỰC HIỆN NẾU requiredRole KHÔNG PHẢI NULL)
            if (requiredRole != null && user.getRole() != requiredRole) {
                String errorMessage = String.format("Vai trò %s không được phép đăng nhập tại đây.", user.getRole().name());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
            }

            // 3. Xác thực mật khẩu với AuthenticationManager
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), password)
            );

            // 4. Lấy Balance và update vào user (logic của bạn)

            // 5. Tạo JWT Token
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            String token = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(new AuthResponse(token));

        } catch (Exception e) {
            // Log lỗi chi tiết ở backend
            System.err.println("Login failed for identifier " + identifier + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai tài khoản hoặc mật khẩu");
        }
    }

}

