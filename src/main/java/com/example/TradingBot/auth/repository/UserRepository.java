package com.example.TradingBot.auth.repository;

import com.example.TradingBot.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByRefCode(String refCode);
    List<User> findByReferrerCode(String referrerCode); // ✅ dùng để tìm F1
    boolean existsByRefCode(String refCode);
    long countByReferrerCode(String refCode);
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameOrEmail(String username, String email);
    List<User> findAllByReferrerCode(String referrerCode);

}

