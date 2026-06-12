package com.trototn.boardinghouse.config;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void existingAccountIsNeverModified() {
        User existing = new User();
        existing.setRole(Role.TENANT);
        existing.setPasswordHash("existing-hash");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));
        DataInitializer initializer = configuredInitializer(
                "existing@example.com", "strong-password-123");

        initializer.run(null);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        assertEquals(Role.TENANT, existing.getRole());
        assertEquals("existing-hash", existing.getPasswordHash());
    }

    @Test
    void missingAccountIsCreatedAsAdmin() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("strong-password-123")).thenReturn("encoded");
        DataInitializer initializer = configuredInitializer(
                " Admin@Example.com ", "strong-password-123");

        initializer.run(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertEquals("admin@example.com", admin.getEmail());
        assertEquals("encoded", admin.getPasswordHash());
        assertEquals(Role.ADMIN, admin.getRole());
    }

    private DataInitializer configuredInitializer(String email, String password) {
        DataInitializer initializer = new DataInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "adminEmail", email);
        ReflectionTestUtils.setField(initializer, "adminPassword", password);
        return initializer;
    }
}
