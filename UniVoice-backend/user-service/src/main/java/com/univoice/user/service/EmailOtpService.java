package com.univoice.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOtpService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("[MAIL NOT CONFIGURED] OTP for {} ({}): {}", toEmail, purpose, otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("UniVoice Verification Code: " + otp);
        message.setText(
                "Hello,\n\n" +
                "Your UniVoice verification code for " + purpose + " is:\n\n" +
                "    " + otp + "\n\n" +
                "This code expires in 5 minutes. Do not share this code with anyone.\n\n" +
                "— UniVoice Team"
        );

        mailSender.send(message);
        log.info("Verification OTP email sent to {}", toEmail);
    }
}