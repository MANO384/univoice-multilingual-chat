package com.univoice.user.service;

import com.univoice.user.dto.AuthDtos.*;
import com.univoice.user.entity.User;
import com.univoice.user.exception.UserExceptions.InvalidOtpException;
import com.univoice.user.exception.UserExceptions.UserAlreadyExistsException;
import com.univoice.user.repository.UserRepository;
import com.univoice.user.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final EmailOtpService emailOtpService;

    private static final String OTP_PREFIX = "otp:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final String RESET_OTP_PREFIX = "reset_otp:";

    private Optional<User> findUserByNormalizedPhone(String normalizedPhone) {
        String digits = normalizedPhone.replaceAll("[^0-9]", "");
        String last10 = digits.length() >= 10 ? digits.substring(digits.length() - 10) : digits;
        return userRepository.findByPhoneNumber(normalizedPhone)
                .or(() -> userRepository.findByPhoneNumber(last10));
    }

    public void sendOtp(SendOtpRequest request) {
        String phone = PhoneNormalizer.normalize(request.getPhoneNumber());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (findUserByNormalizedPhone(phone).isPresent()) {
            throw new UserAlreadyExistsException("Phone number already registered. Please login.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException("Email address already registered. Please login.");
        }

        String otp = String.format("%06d", new SecureRandom().nextInt(999999));
        redisTemplate.opsForValue().set(OTP_PREFIX + phone, otp, OTP_TTL);
        emailOtpService.sendOtpEmail(email, otp, "account registration");
    }

    public void verifyOtp(VerifyOtpRequest request) {
        String phone = PhoneNormalizer.normalize(request.getPhoneNumber());
        String cachedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + phone);

        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            throw new InvalidOtpException("Invalid or expired OTP");
        }
    }

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        String phone = PhoneNormalizer.normalize(request.getPhoneNumber());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        request.setPhoneNumber(phone);

        verifyOtp(new VerifyOtpRequest() {{
            setPhoneNumber(phone);
            setOtp(request.getOtp());
        }});

        if (findUserByNormalizedPhone(phone).isPresent()) {
            throw new UserAlreadyExistsException("Phone number already registered. Please login.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException("Email address already registered. Please login.");
        }

        redisTemplate.delete(OTP_PREFIX + phone);

        String preferredLang = request.getPreferredLanguage() != null && !request.getPreferredLanguage().isBlank()
                ? request.getPreferredLanguage().toLowerCase()
                : request.getMotherTongue().toLowerCase();

        String userUuid = UUID.randomUUID().toString();
        String refreshToken = jwtProvider.generateRefreshToken(userUuid);

        User user = User.builder()
                .userUuid(userUuid)
                .phoneNumber(phone)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName().trim())
                .motherTongue(request.getMotherTongue().toLowerCase())
                .defaultLanguage(preferredLang)
                .refreshToken(refreshToken)
                .build();

        userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(
                userUuid,
                user.getPhoneNumber(),
                Map.of("motherTongue", user.getMotherTongue(), "preferredLang", user.getDefaultLanguage())
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userUuid(userUuid)
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .motherTongue(user.getMotherTongue())
                .preferredLanguage(user.getDefaultLanguage())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String phone = PhoneNormalizer.normalize(request.getPhoneNumber());

        User user = findUserByNormalizedPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Invalid phone number or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid phone number or password");
        }

        String accessToken = jwtProvider.generateAccessToken(
                user.getUserUuid(),
                user.getPhoneNumber(),
                Map.of("motherTongue", user.getMotherTongue(), "preferredLang", user.getDefaultLanguage())
        );

        String refreshToken = jwtProvider.generateRefreshToken(user.getUserUuid());
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userUuid(user.getUserUuid())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .motherTongue(user.getMotherTongue())
                .preferredLanguage(user.getDefaultLanguage())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtProvider.validateToken(request.getRefreshToken())) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String userUuid = jwtProvider.extractUserUuid(request.getRefreshToken());
        User user = userRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!request.getRefreshToken().equals(user.getRefreshToken())) {
            throw new IllegalArgumentException("Revoked refresh token");
        }

        String newAccessToken = jwtProvider.generateAccessToken(
                user.getUserUuid(),
                user.getPhoneNumber(),
                Map.of("motherTongue", user.getMotherTongue(), "preferredLang", user.getDefaultLanguage())
        );

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(user.getRefreshToken())
                .userUuid(user.getUserUuid())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .motherTongue(user.getMotherTongue())
                .preferredLanguage(user.getDefaultLanguage())
                .build();
    }

    public String sendForgotPasswordOtp(ForgotPasswordRequest request) {
        String phone = PhoneNormalizer.normalize(request.getPhoneNumber());

        User user = findUserByNormalizedPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("No account found with this phone number"));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("No recovery email linked to this legacy account.");
        }

        String otp = String.format("%06d", new SecureRandom().nextInt(999999));
        redisTemplate.opsForValue().set(RESET_OTP_PREFIX + phone, otp, OTP_TTL);

        emailOtpService.sendOtpEmail(user.getEmail(), otp, "password reset");
        return maskEmail(user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String phone = PhoneNormalizer.normalize(request.getPhoneNumber());
        String key = RESET_OTP_PREFIX + phone;
        String cachedOtp = redisTemplate.opsForValue().get(key);

        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            throw new InvalidOtpException("Invalid or expired OTP");
        }

        User user = findUserByNormalizedPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRefreshToken(null);
        userRepository.save(user);

        redisTemplate.delete(key);
        log.info("Password reset successfully for user {}", user.getUserUuid());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        if (jwtProvider.validateToken(request.getRefreshToken())) {
            String userUuid = jwtProvider.extractUserUuid(request.getRefreshToken());
            userRepository.findByUserUuid(userUuid).ifPresent(u -> {
                u.setRefreshToken(null);
                userRepository.save(u);
                log.info("User {} logged out successfully.", userUuid);
            });
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return "***" + email.substring(Math.max(at, 0));
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}