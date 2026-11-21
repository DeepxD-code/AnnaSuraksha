package com.annasuraksha.service.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceSecretTest {

    private JwtTokenService service(String secret, String... profiles) {
        JwtTokenService s = new JwtTokenService();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        ReflectionTestUtils.setField(s, "environment", env);
        ReflectionTestUtils.setField(s, "jwtSecret", secret);
        return s;
    }

    @Test
    void productionRejectsMissingSecret() {
        assertThrows(IllegalStateException.class, () -> service("").validateSecret());
    }

    @Test
    void productionRejectsKnownDefault() {
        assertThrows(IllegalStateException.class,
            () -> service(JwtTokenService.KNOWN_DEFAULT_SECRET).validateSecret());
    }

    @Test
    void productionRejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> service("short").validateSecret());
    }

    @Test
    void productionAcceptsStrongSecret() {
        assertDoesNotThrow(() -> service("x".repeat(40)).validateSecret());
    }

    @Test
    void devProfileAllowsDefault() {
        assertDoesNotThrow(() -> service("", "dev").validateSecret());
    }
}
