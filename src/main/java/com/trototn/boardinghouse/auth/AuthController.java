package com.trototn.boardinghouse.auth;

import com.trototn.boardinghouse.auth.domain.Role;
import com.trototn.boardinghouse.auth.domain.User;
import com.trototn.boardinghouse.auth.repository.UserRepository;
import com.trototn.boardinghouse.common.MapperUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.trototn.boardinghouse.auth.domain.AccountActivationToken;
import com.trototn.boardinghouse.auth.repository.AccountActivationTokenRepository;
import com.trototn.boardinghouse.auth.domain.PasswordResetToken;
import com.trototn.boardinghouse.auth.repository.PasswordResetTokenRepository;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final AccountActivationTokenRepository activationTokenRepository;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager, UserRepository userRepository,
                          PasswordResetTokenRepository tokenRepository, JavaMailSender mailSender, PasswordEncoder passwordEncoder,
                          AccountActivationTokenRepository activationTokenRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.activationTokenRepository = activationTokenRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        if (request.password() == null || request.password().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự");
        }
        Role role = request.role() != null ? request.role() : Role.TENANT;
        User user = authService.register(request.fullName(), request.email(), request.password(), request.phone(), request.address(), role);

        // Tạo và gửi link kích hoạt
        String otp = generateOtp();
        String token = otpToken(otp);
        AccountActivationToken activationToken = new AccountActivationToken();
        activationToken.setUser(user);
        activationToken.setToken(token);
        activationToken.setExpiryDate(Instant.now().plus(10, ChronoUnit.MINUTES));
        activationTokenRepository.save(activationToken);

        String activationLink = frontendUrl + "/api/auth/activate-account?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Ma OTP kich hoat tai khoan - Tro Tot ICTU");
        message.setText("Chao " + user.getFullName() + ",\n\n"
                + "Ma OTP kich hoat tai khoan cua ban la: " + otp + "\n"
                + "Ma nay se het han sau 10 phut.\n\n"
                + "Neu can kich hoat bang link, ban co the mo: " + activationLink + "\n");
        boolean emailSent = sendMail(message);

        Map<String, Object> response = new HashMap<>();
        response.put("message", emailSent
                ? "Dang ky thanh cong. Vui long kiem tra email de lay OTP kich hoat."
                : "Dang ky thanh cong, nhung email chua duoc cau hinh. Dung OTP ben duoi de kiem thu.");
        response.put("emailSent", emailSent);
        response.put("otpRequired", true);
        response.put("email", user.getEmail());
        if (!emailSent) {
            response.put("devOtp", otp);
            response.put("activationLink", activationLink);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // Tìm người dùng trước khi xác thực
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Sai email hoặc mật khẩu"));

        // Nếu tài khoản chưa được kích hoạt, đưa ra thông báo lỗi cụ thể
        if (!user.isActive()) {
            throw new DisabledException("Tài khoản của bạn chưa được kích hoạt. Vui lòng kiểm tra email.");
        }

        upgradeLegacyPasswordIfNeeded(user, request.password());

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);
        Map<String, Object> body = new HashMap<>();
        body.put("user", MapperUtil.toUserView(user));
        return ResponseEntity.ok(body);
    }

    private void upgradeLegacyPasswordIfNeeded(User user, String rawPassword) {
        String storedPassword = user.getPasswordHash();
        if (storedPassword == null || rawPassword == null || isBcryptHash(storedPassword)) {
            return;
        }
        if (!storedPassword.equals(rawPassword) && !storedPassword.equals(sha256Hex(rawPassword))) {
            throw new BadCredentialsException("Sai email hoặc mật khẩu");
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private boolean isBcryptHash(String passwordHash) {
        return passwordHash.startsWith("$2a$")
                || passwordHash.startsWith("$2b$")
                || passwordHash.startsWith("$2y$");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        Map<String, Object> body = new HashMap<>();
        if (principal == null) {
            body.put("user", null);
            return ResponseEntity.ok(body);
        }
        Optional<User> user = userRepository.findByEmail(principal.getName());
        body.put("user", user.map(MapperUtil::toUserView).orElse(null));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logoutGet(HttpServletRequest request) {
        return logout(request);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập email");
        }
        
        // Bảo mật: Chống rò rỉ dò quét email (Email enumeration). Luôn trả về cùng một câu thông báo
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String otp = generateOtp();
            String token = otpToken(otp);
            
            PasswordResetToken resetToken = tokenRepository.findByUserId(user.getId())
                    .orElse(new PasswordResetToken());
            resetToken.setUser(user);
            resetToken.setToken(token);
            resetToken.setExpiryDate(Instant.now().plus(15, ChronoUnit.MINUTES)); // Token sống trong 15 phút
            tokenRepository.save(resetToken);

            String resetUrl = frontendUrl + "/reset-password.html?email=" + user.getEmail();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Ma OTP khoi phuc mat khau - Tro Tot ICTU");
            message.setText("Chao " + user.getFullName() + ",\n\n"
                    + "Ma OTP dat lai mat khau cua ban la: " + otp + "\n"
                    + "Ma nay se het han sau 15 phut.\n\n"
                    + "Trang dat lai mat khau: " + resetUrl + "\n");
            boolean emailSent = sendMail(message);
            if (!emailSent) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Email chưa được cấu hình. Dùng link khôi phục bên dưới để kiểm thử.");
                response.put("emailSent", false);
                response.put("resetLink", resetUrl);
                response.put("devOtp", otp);
                response.put("email", user.getEmail());
                return ResponseEntity.ok(response);
            }
        }
        return ResponseEntity.ok(Map.of("message", "Nếu email hợp lệ và tồn tại trong hệ thống, một liên kết khôi phục đã được gửi."));
    }

    @GetMapping("/activate-account")
    public ResponseEntity<?> activateAccount(@RequestParam("token") String token) {
        AccountActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Liên kết kích hoạt không hợp lệ hoặc đã hết hạn."));

        if (activationToken.getExpiryDate().isBefore(Instant.now())) {
            activationTokenRepository.delete(activationToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(frontendUrl + "/auth.html?error=activation_expired"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        User user = activationToken.getUser();
        if (user.isActive()) {
            activationTokenRepository.delete(activationToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(frontendUrl + "/auth.html?activated=already"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        user.setActive(true);
        userRepository.save(user);
        activationTokenRepository.delete(activationToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(frontendUrl + "/auth.html?activated=true"));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/activate-account")
    public ResponseEntity<?> activateAccountByOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = normalizeOtp(request.get("otp"));
        if (email == null || email.isBlank() || otp == null) {
            throw new IllegalArgumentException("Vui long nhap email va ma OTP");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Ma OTP khong hop le hoac da het han"));
        AccountActivationToken activationToken = activationTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Ma OTP khong hop le hoac da het han"));
        if (activationToken.getExpiryDate().isBefore(Instant.now())) {
            activationTokenRepository.delete(activationToken);
            throw new IllegalArgumentException("Ma OTP da het han");
        }
        if (!otpMatches(activationToken.getToken(), otp)) {
            throw new IllegalArgumentException("Ma OTP khong dung");
        }
        user.setActive(true);
        userRepository.save(user);
        activationTokenRepository.delete(activationToken);
        return ResponseEntity.ok(Map.of("message", "Kich hoat tai khoan thanh cong"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String email = request.get("email");
        String otp = normalizeOtp(request.get("otp"));
        String newPassword = request.get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ hoặc mật khẩu quá ngắn");
        }

        PasswordResetToken resetToken;
        if (token != null && !token.isBlank()) {
            resetToken = tokenRepository.findByToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Liên kết không hợp lệ hoặc đã hết hạn"));
        } else {
            if (email == null || email.isBlank() || otp == null) {
                throw new IllegalArgumentException("Vui long nhap email va ma OTP");
            }
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Ma OTP khong hop le hoac da het han"));
            resetToken = tokenRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Ma OTP khong hop le hoac da het han"));
            if (!otpMatches(resetToken.getToken(), otp)) {
                throw new IllegalArgumentException("Ma OTP khong dung");
            }
        }

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Liên kết đã hết hạn");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(resetToken);

        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập");
        }
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }

        if (request.newPassword() == null || request.newPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới tối thiểu 6 ký tự");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<?> resendActivation(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập email");
        }

        // Chỉ gửi lại nếu email tồn tại và tài khoản CHƯA kích hoạt
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isActive()) {
                AccountActivationToken activationToken = activationTokenRepository.findByUserId(user.getId())
                        .orElse(new AccountActivationToken());
                
                String otp = generateOtp();
                String token = otpToken(otp);
                activationToken.setUser(user);
                activationToken.setToken(token);
                activationToken.setExpiryDate(Instant.now().plus(10, ChronoUnit.MINUTES));
                activationTokenRepository.save(activationToken);

                String activationLink = frontendUrl + "/api/auth/activate-account?token=" + token;
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(user.getEmail());
                message.setSubject("Gui lai ma OTP kich hoat tai khoan - Tro Tot ICTU");
                message.setText("Chao " + user.getFullName() + ",\n\nMa OTP kich hoat moi cua ban la: " + otp + "\nMa nay se het han sau 10 phut.\n\nLink du phong: " + activationLink + "\n");
                sendMail(message);
            }
        });

        return ResponseEntity.ok(Map.of("message", "Nếu tài khoản chưa được kích hoạt, một email xác nhận mới đã được gửi."));
    }

    private String generateOtp() {
        return String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
    }

    private String otpToken(String otp) {
        return otp + ":" + UUID.randomUUID();
    }

    private String normalizeOtp(String otp) {
        if (otp == null) return null;
        String normalized = otp.replaceAll("\\D", "");
        return normalized.length() == 6 ? normalized : null;
    }

    private boolean otpMatches(String storedToken, String otp) {
        return storedToken != null && otp != null && storedToken.startsWith(otp + ":");
    }

    private boolean sendMail(SimpleMailMessage message) {
        if (mailUsername == null || mailUsername.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            return false;
        }
        try {
            mailSender.send(message);
            return true;
        } catch (MailException ex) {
            return false;
        }
    }

    public record RegisterRequest(
            @NotBlank String fullName,
            @Email String email,
            @NotBlank @Size(min = 6) String password,
            String phone,
            String address,
            Role role
    ) {}

    public record LoginRequest(@Email String email, @NotBlank String password) {}

    public record ChangePasswordRequest(String oldPassword, String newPassword, String confirmPassword) {}
}
