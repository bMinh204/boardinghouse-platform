package com.trototn.boardinghouse.auth;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.auth.repository.UserRepository;

import java.time.LocalDate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String fullName, String email, String password, String phone, String address, LocalDate dateOfBirth, Role role) {
        Role registrationRole = validatePublicRegistrationRole(role);
        userRepository.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("Email đã tồn tại");
        });
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setAddress(address);
        user.setDateOfBirth(dateOfBirth);
        user.setRole(registrationRole);
        user.setActive(false); // Tài khoản sẽ ở trạng thái chưa kích hoạt
        return userRepository.save(user);
    }

    private Role validatePublicRegistrationRole(Role requestedRole) {
        Role role = requestedRole == null ? Role.TENANT : requestedRole;
        if (role != Role.TENANT && role != Role.LANDLORD) {
            throw new IllegalArgumentException("Vai trò đăng ký không hợp lệ");
        }
        return role;
    }
}
