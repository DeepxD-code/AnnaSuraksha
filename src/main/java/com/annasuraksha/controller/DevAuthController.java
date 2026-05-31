package com.annasuraksha.controller;

import com.annasuraksha.model.api.ApiResponse;
import com.annasuraksha.model.auth.User;
import com.annasuraksha.service.auth.JwtTokenService;
import com.annasuraksha.service.auth.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dev-only helper to mint short-lived isDev JWTs for local troubleshooting. This endpoint is
 * intentionally gated: it only operates when spring.h2.console.enabled=true and requires the
 * caller to be ADMIN or present a bootstrap secret header.
 */
@RestController
@RequestMapping("/api/auth")
public class DevAuthController {

    private final JwtTokenService jwtService;
    private final UserService userService;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2Enabled;

    @Value("${dev.bootstrap.secret:}")
    private String bootstrapSecret;

    public DevAuthController(JwtTokenService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/dev-token")
    public ApiResponse<Map<String, Object>> mintDevToken(@RequestHeader(value = "X-BOOTSTRAP-SECRET", required = false) String secret,
                                                          Authentication auth,
                                                          @RequestBody Map<String, String> body) {
        if (!h2Enabled) return ApiResponse.error("NOT_ALLOWED", "Dev token minting is disabled.");

        String email = body.get("email");
        if (email == null || email.isBlank()) return ApiResponse.error("VALIDATION_ERROR", "email is required");

        boolean isAdmin = false;
        if (auth != null && auth.isAuthenticated()) {
            isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_DEV"));
        }

        boolean allowed = isAdmin || (bootstrapSecret != null && !bootstrapSecret.isBlank() && bootstrapSecret.equals(secret));
        if (!allowed) return ApiResponse.error("FORBIDDEN", "Not allowed to mint dev token.");

        User user = userService.findByEmail(email).orElse(null);
        if (user == null) return ApiResponse.error("NOT_FOUND", "User not found: " + email);

        String token = jwtService.generateToken(String.valueOf(user.getId()), user.getEmail(), user.getRoles(), user.getStateCode(), true);
        return ApiResponse.success(Map.of("token", token, "expiresIn", 60 * 60), "Dev token minted.");
    }
}
