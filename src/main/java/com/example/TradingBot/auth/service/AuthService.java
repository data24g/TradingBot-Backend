package com.example.TradingBot.auth.service;
import com.example.TradingBot.auth.dto.AgentRegistrationRequest;
import com.example.TradingBot.auth.model.AgentDetails;
import com.example.TradingBot.auth.model.Role;
import com.example.TradingBot.auth.model.User;
import com.example.TradingBot.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Giả sử bạn đã có bean này trong cấu hình Spring Security

    public User registerAgent(AgentRegistrationRequest request) {
        // 1. Kiểm tra username hoặc email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Lỗi: Tên đăng nhập đã được sử dụng!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Lỗi: Email đã được sử dụng!");
        }

        // 2. Tạo đối tượng AgentDetails từ request
        AgentDetails agentDetails = new AgentDetails();
        agentDetails.setAgentCode(request.getAgentCode());
        agentDetails.setCommissionRate(request.getCommissionRate());
        agentDetails.setMaxCommissionRate(request.getMaxCommissionRate());
        agentDetails.setOperatingArea(request.getOperatingArea());
        agentDetails.setAllowSubAgents(request.isAllowSubAgents());
        agentDetails.setContactAddress(request.getContactAddress());

        // Gán các trường không bắt buộc nếu chúng tồn tại
        agentDetails.setUplineAgentId(request.getUplineAgentId());
        agentDetails.setBusinessLicenseNumber(request.getBusinessLicenseNumber());
        agentDetails.setEmergencyContactName(request.getEmergencyContactName());
        agentDetails.setEmergencyContactPhone(request.getEmergencyContactPhone());
        agentDetails.setNotes(request.getNotes());

        // 3. Tạo đối tượng User mới với vai trò là AGENT
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // Luôn mã hóa mật khẩu
                .phone(request.getPhone())
                .role(Role.DAILY) // Gán vai trò là Đại lý
                .agentDetails(agentDetails) // Nhúng thông tin chi tiết của đại lý
                .build();

        // 4. Lưu User vào cơ sở dữ liệu
        return userRepository.save(user);
    }
}
