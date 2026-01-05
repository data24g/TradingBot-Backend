package com.example.TradingBot.auth.service;

import org.jasypt.encryption.StringEncryptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {
    @Autowired
    private StringEncryptor stringEncryptor;

    public String encrypt(String data) { return stringEncryptor.encrypt(data); }
    public String decrypt(String encryptedData) { return stringEncryptor.decrypt(encryptedData); }
}
