package com.trototn.boardinghouse.auth;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void registerRejectsAdminRole() {
        AuthService service = new AuthService(userRepository, passwordEncoder);

        assertThrows(IllegalArgumentException.class, () -> service.register(
                "Admin", "admin@example.com", "password", null, null, null, Role.ADMIN));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void registerDefaultsMissingRoleToTenant() {
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthService service = new AuthService(userRepository, passwordEncoder);

        service.register("Tenant", "tenant@example.com", "password", null, null,
                LocalDate.of(2000, 1, 1), null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.TENANT, userCaptor.getValue().getRole());
    }
}
