package com.trototn.boardinghouse.config;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DataInitializer implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private static final int MIN_BOOTSTRAP_PASSWORD_LENGTH = 12;

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
        String normalizedEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            logger.info("Bootstrap admin account already exists; credentials were left unchanged");
            return;
        }
        if (adminPassword.length() < MIN_BOOTSTRAP_PASSWORD_LENGTH) {
            throw new IllegalStateException("Bootstrap admin password must contain at least 12 characters");
        }

        User admin = new User();
        admin.setFullName("System Admin");
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setLocked(false);
        admin.setActive(true);
        userRepository.save(admin);
        logger.info("Bootstrap admin account was created");
    }

}
