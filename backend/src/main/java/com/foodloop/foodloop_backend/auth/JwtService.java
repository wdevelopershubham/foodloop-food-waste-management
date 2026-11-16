package com.foodloop.foodloop_backend.auth;

import com.foodloop.foodloop_backend.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expMs;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expMs = expMs;
    }
    public String generate(User u) {
        return Jwts.builder().subject(u.getEmail()).claim("role", u.getRole().name())
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(key).compact();
    }
    public String subject(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
