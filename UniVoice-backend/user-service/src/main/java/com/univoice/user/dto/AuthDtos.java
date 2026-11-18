package com.univoice.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

public class AuthDtos {

    @Data
    public static class SendOtpRequest {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be 10 to 15 digits, optionally prefixed with '+'")
        private String phoneNumber;
    }

    @Data
    public static class VerifyOtpRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        @NotBlank(message = "OTP is required")
        private String otp;
    }

    @Data
    public static class SignUpRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        @NotBlank(message = "OTP is required")
        private String otp;

        @NotBlank(message = "Password is required")
        private String password;

        @NotBlank(message = "Display name is required")
        private String displayName;

        @NotBlank(message = "Mother tongue is required (e.g. 'ta', 'te', 'hi')")
        private String motherTongue;

        private String preferredLanguage;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    public static class LogoutRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }
    
    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be 10 to 15 digits")
        private String phoneNumber;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        @NotBlank(message = "OTP is required")
        private String otp;

        @NotBlank(message = "New password is required")
        private String newPassword;
    }
    
    @Data
    @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String userUuid;
        private String displayName;
        private String motherTongue;
        private String preferredLanguage;
    }
}
