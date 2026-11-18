package com.univoice.translation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationCacheService {

    private final StringRedisTemplate redisTemplate;
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    public Optional<String> get(String text, String sourceLocale, String targetLocale, String mode) {
        String key = buildCacheKey(text, sourceLocale, targetLocale, mode);
        String cached = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(cached);
    }

    public void put(String text, String sourceLocale, String targetLocale, String mode, String translatedText) {
        String key = buildCacheKey(text, sourceLocale, targetLocale, mode);
        redisTemplate.opsForValue().set(key, translatedText, CACHE_TTL);
    }

    private String buildCacheKey(String text, String sourceLocale, String targetLocale, String mode) {
        return "trans:" + sourceLocale + ":" + targetLocale + ":" + mode + ":" + hashText(text.trim().toLowerCase());
    }

    private String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }
}
