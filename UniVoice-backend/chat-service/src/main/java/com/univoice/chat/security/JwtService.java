package com.univoice.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@Slf4j
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<ChatUser> authenticate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userUuid = claims.getSubject();
            String phoneNumber = claims.get("phone", String.class);
            String motherTongue = claims.get("motherTongue", String.class);

            if (userUuid == null || userUuid.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new ChatUser(userUuid, phoneNumber, motherTongue != null ? motherTongue : "en"));
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("STOMP JWT validation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
