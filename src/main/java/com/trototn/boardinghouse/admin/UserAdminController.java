package com.trototn.boardinghouse.admin;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.common.dto.Responses;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import com.trototn.boardinghouse.common.MapperUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> list(@RequestParam(required = false) Role role,
                                    @RequestParam(required = false) Boolean locked,
                                    @RequestParam(required = false) String keyword) {
        List<Responses.UserView> users = userRepository.findAll().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> locked == null || u.isLocked() == locked)
                .filter(u -> keyword == null || keyword.isBlank() ||
                        containsIgnoreCase(u.getFullName(), keyword) || containsIgnoreCase(u.getEmail(), keyword))
                .map(MapperUtil::toUserView)
                .collect(Collectors.toList());
        return Map.of("users", users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getProfile(@PathVariable Long id, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isOwner = currentUser.getId().equals(user.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("Not allowed");
        }
        return Map.of("user", MapperUtil.toUserDetail(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> updateProfile(@PathVariable Long id, @RequestBody UpdateProfileRequest request,
                                             Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        User targetUser = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isOwner = currentUser.getId().equals(targetUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new IllegalArgumentException("Not allowed");
        }

        String fullName = normalizeRequired(request.fullName(), "Họ và tên không được để trống");
        targetUser.setFullName(fullName);
        targetUser.setPhone(normalizeOptional(request.phone()));
        targetUser.setAddress(normalizeOptional(request.address()));
        userRepository.save(targetUser);

        return Map.of("user", MapperUtil.toUserView(targetUser));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(Optional.ofNullable(request.role()).orElse(user.getRole()));
        userRepository.save(user);
        return Map.of("user", MapperUtil.toUserView(user));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (request.locked() != null) {
            user.setLocked(request.locked());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        userRepository.save(user);
        return Map.of("user", MapperUtil.toUserView(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
        if (currentUser.getId().equals(id)) {
            throw new IllegalArgumentException("Không thể xóa tài khoản đang đăng nhập");
        }
        User targetUser = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));

        deleteUserDependencies(id);
        userRepository.delete(targetUser);
        return ResponseEntity.ok(Map.of("message", "deleted"));
    }

    private void deleteUserDependencies(Long userId) {
        executeDelete("delete m from messages m join conversations c on m.conversation_id = c.id where c.tenant_id = :userId or c.landlord_id = :userId or c.room_id in (select id from rooms where owner_id = :userId)", userId);
        executeDelete("delete cm from chat_messages cm join conversations c on cm.conversation_id = c.id where c.tenant_id = :userId or c.landlord_id = :userId or c.room_id in (select id from rooms where owner_id = :userId)", userId);
        executeDelete("delete from messages where sender_id = :userId", userId);
        executeDelete("delete from chat_messages where sender_id = :userId", userId);
        executeDelete("delete from conversations where tenant_id = :userId or landlord_id = :userId or room_id in (select id from rooms where owner_id = :userId)", userId);
        executeDelete("delete from favorites where tenant_id = :userId or user_id = :userId or room_id in (select id from rooms where owner_id = :userId)", userId);
        executeDelete("delete from surveys where user_id = :userId or room_id in (select id from rooms where owner_id = :userId)", userId);
        executeDelete("delete from rental_requests where tenant_id = :userId or landlord_id = :userId or room_id in (select id from rooms where owner_id = :userId)", userId);
        executeDelete("delete from room_views where viewer_id = :userId or room_id in (select id from rooms where owner_id = :userId)", userId);
        executeDelete("delete from room_amenities where room_id in (select id from rooms where owner_id = :userId)", userId);
        executeDelete("delete from room_images where room_id in (select id from rooms where owner_id = :userId)", userId);
        executeDelete("delete from rooms where owner_id = :userId", userId);
        executeDelete("delete from password_reset_tokens where user_id = :userId", userId);
        executeDelete("delete from account_activation_token where user_id = :userId", userId);
    }

    private void executeDelete(String sql, Long userId) {
        entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    private boolean containsIgnoreCase(String source, String term) {
        if (source == null || term == null) return false;
        return source.toLowerCase().contains(term.toLowerCase());
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isBlank()) return null;
        return value.trim();
    }

    public record UpdateProfileRequest(String fullName, String phone, String address) {}

    public record UpdateRoleRequest(Role role) {}

    public record UpdateStatusRequest(Boolean locked, Boolean active) {}
}
