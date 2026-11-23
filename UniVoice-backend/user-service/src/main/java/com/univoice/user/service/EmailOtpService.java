package com.univoice.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailOtpService {

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:palanimanojkumar924@gmail.com}")
    private String senderEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("[BREVO_API_KEY NOT SET] OTP for {} ({}): {}", toEmail, purpose, otp);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> body = Map.of(
                "sender", Map.of("name", "UniVoice", "email", senderEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", "UniVoice Verification Code: " + otp,
                "htmlContent",
                "<div style='font-family:sans-serif;padding:20px;background:#0c1317;color:#ffffff;border-radius:12px;'>" +
                "<h2 style='color:#00a884;margin-top:0;'>UniVoice Verification</h2>" +
                "<p>Your verification code for <strong>" + purpose + "</strong> is:</p>" +
                "<div style='font-size:28px;font-weight:bold;letter-spacing:6px;padding:12px 20px;background:#111b21;border:1px solid #222d34;border-radius:8px;display:inline-block;color:#00a884;'>" +
                otp +
                "</div>" +
                "<p style='color:#8696a0;font-size:12px;margin-top:16px;'>This code expires in 5 minutes. Do not share it with anyone.</p>" +
                "</div>"
        );

        HttpEntity request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.brevo.com/v3/smtp/email",
                request,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to send verification email via Brevo: " + response.getStatusCode());
        }

        log.info("Verification OTP email sent via HTTPS to {}", toEmail);
    }
}