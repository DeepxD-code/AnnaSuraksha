package com.annasuraksha.controller;

import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.model.auth.User;
import com.annasuraksha.service.auth.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService           userService;
    private final JwtTokenService       jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");

        if (email == null || password == null)
            return ApiResponse.error("VALIDATION_ERROR", "email and password are required.");

        var userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty())
            return ApiResponse.error("AUTH_FAILED", "Invalid credentials.");

        User user = userOpt.get();
        if (!user.isActive())
            return ApiResponse.error("AUTH_FAILED", "Account is disabled.");

        if (!passwordEncoder.matches(password, user.getPasswordHash()))
            return ApiResponse.error("AUTH_FAILED", "Invalid credentials.");

        String token = jwtService.generateToken(
            String.valueOf(user.getId()), user.getEmail(), user.getRoles(), user.getStateCode());

        log.info("Login success: {} ({})", email, user.getRoles());

        return ApiResponse.success(Map.of(
            "token",      token,
            "expiresIn",  28800,
            "roles",      user.getRoles(),
            "stateCode",  user.getStateCode() != null ? user.getStateCode() : "ALL",
            "fullName",   user.getFullName() != null ? user.getFullName() : email
        ), "Login successful.");
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ApiResponse.error("AUTH_FAILED", "Missing token.");
        String oldToken = authHeader.substring(7);
        if (!jwtService.isTokenValid(oldToken))
            return ApiResponse.error("AUTH_FAILED", "Token invalid or expired.");

        var claims    = jwtService.validateAndParse(oldToken);
        String userId = claims.getSubject();
        var userOpt   = userService.findByEmail(claims.get("email", String.class));
        if (userOpt.isEmpty())
            return ApiResponse.error("AUTH_FAILED", "User not found.");

        User user     = userOpt.get();
        String newToken = jwtService.generateToken(
            userId, user.getEmail(), user.getRoles(), user.getStateCode());

        return ApiResponse.success(Map.of("token", newToken, "expiresIn", 28800), "Token refreshed.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // Stateless — client discards token. In production: add token to Redis blacklist.
        return ApiResponse.success(null, "Logged out successfully.");
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthController.class);
    public AuthController(UserService userService, JwtTokenService jwtService, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }
}
