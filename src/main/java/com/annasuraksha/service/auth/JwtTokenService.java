package com.annasuraksha.service.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class JwtTokenService {

    /** Well-known demo key that shipped in source; never acceptable outside the dev/demo profile. */
    static final String KNOWN_DEFAULT_SECRET = "ANNASURAKSHA_V5_JWT_SECRET_KEY_32CHARS_MIN";

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Autowired
    private Environment environment;

    @PostConstruct
    void validateSecret() {
        if (environment.acceptsProfiles(Profiles.of("dev", "demo"))) {
            if (jwtSecret == null || jwtSecret.isBlank()) jwtSecret = KNOWN_DEFAULT_SECRET;
            return;
        }
        if (jwtSecret == null || jwtSecret.isBlank()
                || KNOWN_DEFAULT_SECRET.equals(jwtSecret)
                || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must be set to a unique value of at least 32 characters "
                + "(the built-in demo key is only allowed with the 'dev' or 'demo' Spring profile).");
        }
    }

    @Value("${jwt.expiry-minutes:480}")
    private int expiryMinutes;

    private SecretKey key() {
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) bytes = Arrays.copyOf(bytes, 32);
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String userId, String email, List<String> roles, String stateCode) {
        return generateToken(userId, email, roles, stateCode, false);
    }

    /**
     * Generate a JWT and optionally include an explicit isDev claim. The isDev claim must be
     * present for developer tokens that should receive DEV privileges (used for local access to
     * dev-only consoles).
     */
    public String generateToken(String userId, String email, List<String> roles, String stateCode, boolean isDev) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(userId)
            .claim("email",     email)
            .claim("roles",     roles)
            .claim("stateCode", stateCode)
            .claim("isDev",     isDev)
            .issuedAt(new Date(now))
            .expiration(new Date(now + (long) expiryMinutes * 60_000L))
            .signWith(key())
            .compact();
    }

    public Claims validateAndParse(String token) {
        return Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload();
    }

    public boolean isTokenValid(String token) {
        try { validateAndParse(token); return true; }
        catch (JwtException e) { log.warn("Invalid JWT: {}", e.getMessage()); return false; }
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        try {
            Claims c = validateAndParse(token);
            return (List<String>) c.get("roles", List.class);
        } catch (Exception e) { return List.of(); }
    }

    public String getStateCode(String token) {
        try { return validateAndParse(token).get("stateCode", String.class); }
        catch (Exception e) { return null; }
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtTokenService.class);
}
