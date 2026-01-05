package com.example.TradingBot.auth.database;

import com.example.TradingBot.auth.model.Role;
import com.example.TradingBot.auth.model.User;
import com.example.TradingBot.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class Database {

    @Bean
    CommandLineRunner initDatabase( UserRepository userRepository,
                                   PasswordEncoder passwordEncoder ) {
        return args -> {


            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = User.builder()
                        .username("admin")
                        .email("admin@gmail.com")
                        .phone("84965741051")
                        .password(passwordEncoder.encode("A123456a@"))  // Mã hóa mật khẩu
                        .role(Role.ADMIN)
                        .createdAt(LocalDateTime.now())
                        .balance(java.math.BigDecimal.ZERO)
                        .build();
               User newUser =   userRepository.save(admin);

                System.out.println("✅ Tài khoản admin mặc định đã được khởi tạo.");
            }

        };
    }
}
