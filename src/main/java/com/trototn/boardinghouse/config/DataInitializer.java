package com.trototn.boardinghouse.config;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin-email:}")
    private String adminEmail;

    @Value("${app.bootstrap-admin-password:}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                existing -> {
                    if (existing.getRole() != Role.ADMIN) {
                        existing.setRole(Role.ADMIN);
                    }
                    existing.setLocked(false);
                    existing.setActive(true);
                    existing.setPasswordHash(passwordEncoder.encode(adminPassword));
                    userRepository.save(existing);
                },
                () -> {
                    User admin = new User();
                    admin.setFullName("System Admin");
                    admin.setEmail(adminEmail);
                    admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                    admin.setRole(Role.ADMIN);
                    admin.setLocked(false);
                    admin.setActive(true);
                    userRepository.save(admin);
                }
        );
    }

}
