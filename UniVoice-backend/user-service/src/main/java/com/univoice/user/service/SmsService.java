package com.univoice.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {
    public void sendOtp(String phoneNumber, String otp) {
        log.info("===============================================================");
        log.info(" [SMS GATEWAY] OTP for {}: {}", phoneNumber, otp);
        log.info("===============================================================");
    }
}
