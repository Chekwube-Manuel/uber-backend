package com.chekwube.uber.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.time.Instant;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationSeconds;
    public JwtService(@Value("${app.security.jwt-secret}") String secret, @Value("${app.security.jwt-expiration-seconds:900}") long expirationSeconds) {
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("JWT secret must be Base64-encoded", ex); }
        if (decoded.length < 32) throw new IllegalStateException("JWT secret must decode to at least 32 bytes");
        this.key = Keys.hmacShaKeyFor(decoded);
        this.expirationSeconds = expirationSeconds;
    }
    public String issue(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getId().toString()).claim("role", user.getRole().name())
            .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(key).compact();
    }
    public String subject(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
