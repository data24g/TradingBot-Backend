package com.example.TradingBot.auth.service;

import com.example.TradingBot.auth.dto.ApiCredentialsDTO;
import com.example.TradingBot.auth.dto.UserCreateDTO;
import com.example.TradingBot.auth.dto.UserDetailDTO;
import com.example.TradingBot.auth.dto.UserResponseDTO;
import com.example.TradingBot.auth.model.UserAccount;
import com.example.TradingBot.auth.repository.UserAccountRepository;
import com.example.TradingBot.tradingbot.config.TradingConfig;
import com.example.TradingBot.tradingbot.dto.TradingConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.TradingBot.auth.dto.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
// Không cần @Transactional với MongoDB theo cách này
public class UserManagementService {

    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private EncryptionService encryptionService;

    public UserAccount createUser(UserCreateDTO dto) {
        UserAccount user = new UserAccount();
        user.setUsername(dto.username());
        user.setEncryptedApiKey(encryptionService.encrypt(dto.apiKey()));
        user.setEncryptedApiSecret(encryptionService.encrypt(dto.apiSecret()));
        // Tạo và nhúng đối tượng config
        TradingConfig config = new TradingConfig();
        config.setActive(false); // Mặc định là tắt
        user.setTradingConfig(config);

        return userAccountRepository.save(user);
    }

    public void toggleBot(String userId, boolean activate) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        if (user.getTradingConfig() == null) {
            user.setTradingConfig(new TradingConfig());
        }
        user.getTradingConfig().setActive(activate);
        userAccountRepository.save(user); // Lưu lại toàn bộ document user
    }

    /**
     * Cập nhật cấu hình giao dịch cho một người dùng.
     * @param userId ID của người dùng.
     * @param dto DTO chứa các thông tin cấu hình mới.
     * @return UserAccount đã được cập nhật.
     */
    public UserAccount updateConfig(String userId, TradingConfigDTO dto) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        TradingConfig config = user.getTradingConfig();
        if (config == null) {
            config = new TradingConfig();
            user.setTradingConfig(config);
        }

        config.setOrderSizeUSD(dto.orderSizeUSD());

        return userAccountRepository.save(user);
    }

    /**
     * (MỚI) Cập nhật API Key và Secret Key cho một người dùng.
     * @param userId ID của người dùng cần cập nhật.
     * @param dto DTO chứa key mới.
     * @return UserAccount đã được cập nhật (với key đã mã hóa).
     */
    public UserAccount updateApiCredentials(String userId, ApiCredentialsDTO dto) {
        // 1. Tìm người dùng trong database hoặc ném ra lỗi nếu không tồn tại
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // 2. Mã hóa các key mới nhận được
        String encryptedNewApiKey = encryptionService.encrypt(dto.newApiKey());
        String encryptedNewApiSecret = encryptionService.encrypt(dto.newApiSecret());

        // 3. Cập nhật các trường tương ứng trong đối tượng user
        user.setEncryptedApiKey(encryptedNewApiKey);
        user.setEncryptedApiSecret(encryptedNewApiSecret);

        // 4. Lưu lại đối tượng user đã được cập nhật vào database
        return userAccountRepository.save(user);
    }


    /**
     * (MỚI) Hàm phụ trợ để chuyển đổi một UserAccount sang UserResponseDTO.
     */
    private UserResponseDTO convertToDto(UserAccount user) {
        // Kiểm tra an toàn để tránh NullPointerException nếu config chưa được tạo
        boolean isActive = (user.getTradingConfig() != null) && user.getTradingConfig().isActive();

        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                isActive,
                user.getTradesToday(),
                user.getLastTradeDate()
        );
    }


    /**
     * (MỚI) Lấy thông tin chi tiết của một người dùng theo ID.
     * @param userId ID của người dùng cần tìm.
     * @return một UserDetailDTO chứa thông tin chi tiết.
     */
//    public UserDetailDTO getUserById(String userId) {
//        // Tìm người dùng trong database, nếu không thấy sẽ ném ra lỗi
//        UserAccount user = userAccountRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
//
//        // Chuyển đổi sang DTO chi tiết
//        return convertToDetailDto(user);
//    }

    /**
     * Hàm phụ trợ để chuyển đổi một UserAccount sang UserResponseDTO (dạng tóm tắt).
     */
    private UserResponseDTO convertToSummaryDto(UserAccount user) {
        boolean isActive = (user.getTradingConfig() != null) && user.getTradingConfig().isActive();
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                isActive,
                user.getTradesToday(),
                user.getLastTradeDate()
        );
    }

    /**
     * (MỚI) Hàm phụ trợ để chuyển đổi một UserAccount sang UserDetailDTO (dạng chi tiết).
     */
//    private UserDetailDTO convertToDetailDto(UserAccount user) {
//        boolean isActive = (user.getTradingConfig() != null) && user.getTradingConfig().isActive();
//        Double orderSizeUSD = (user.getTradingConfig() != null) ? user.getTradingConfig().getOrderSizeUSD() : null;
//
//        return new UserDetailDTO(
//                user.getId(),
//                user.getUsername(),
//                isActive,
//                orderSizeUSD, // <-- Bao gồm cả orderSizeUSD
//                user.getTradesToday(),
//                user.getLastTradeDate()
//        );
//    }


    /**
     * (CẬP NHẬT) Lấy danh sách tất cả người dùng dưới dạng DTO chi tiết.
     * @return một danh sách các UserDetailDTO.
     */
    public List<UserDetailDTO> getAllUsers() {
        return userAccountRepository.findAll().stream()
                .map(this::convertToDetailDto) // <-- THAY ĐỔI QUAN TRỌNG: Gọi hàm chuyển đổi chi tiết
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin chi tiết của một người dùng theo ID.
     */
    public UserDetailDTO getUserById(String userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return convertToDetailDto(user);
    }

    /**
     * Hàm phụ trợ để chuyển đổi một UserAccount sang UserDetailDTO (dạng chi tiết).
     */
    private UserDetailDTO convertToDetailDto(UserAccount user) {
        boolean isActive = (user.getTradingConfig() != null) && user.getTradingConfig().isActive();
        Double orderSizeUSD = (user.getTradingConfig() != null) ? user.getTradingConfig().getOrderSizeUSD() : null;

        return new UserDetailDTO(
                user.getId(),
                user.getUsername(),
                isActive,
                orderSizeUSD,
                user.getTradesToday(),
                user.getLastTradeDate()
        );
    }

    /**
     * (MỚI) Lấy API Key và Secret Key đã giải mã của người dùng.
     * Dùng cho Bot khi cần ký giao dịch hoặc hiển thị lại cho người dùng (cần bảo mật kỹ).
     */
    public UserKeysDTO getUserKeys(String userId) {
        // 1. Tìm user
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // 2. Giải mã (Decrypt) thông tin
        // Giả định EncryptionService có hàm decrypt(String encryptedText)
        String decryptedApiKey = encryptionService.decrypt(user.getEncryptedApiKey());
        String decryptedApiSecret = encryptionService.decrypt(user.getEncryptedApiSecret());

        // 3. Trả về DTO
        return new UserKeysDTO(decryptedApiKey, decryptedApiSecret);
    }
}