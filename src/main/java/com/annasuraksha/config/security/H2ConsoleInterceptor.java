package com.annasuraksha.config.security;

import com.annasuraksha.service.AuditLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interceptor that protects H2 console and dev console routes.
 * Requirements: request must originate from localhost and the authenticated user must have one
 * of the privileged roles (ROLE_ADMIN, ROLE_GOVT_OFFICER, ROLE_DEV).
 * All access attempts are recorded via AuditLogService.
 */
@Component
public class H2ConsoleInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;

    public H2ConsoleInterceptor(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (!(path.startsWith("/h2-console") || path.startsWith("/dev/console"))) {
            return true;
        }

        String clientIp = getClientIp(request);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";

        boolean isLocal = isLocalhost(clientIp);
        boolean hasRole = false;
        if (auth != null && auth.isAuthenticated()) {
            Set<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
            hasRole = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_GOVT_OFFICER") || roles.contains("ROLE_DEV");
        }

        boolean allowed = isLocal && hasRole;

        // Log the attempt
        auditLogService.logAuthEvent(allowed, clientIp, userId, path);

        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to dev console is restricted to local privileged users.");
            return false;
        }

        return true;
    }

    private boolean isLocalhost(String ip) {
        if (ip == null) return false;
        return ip.equals("127.0.0.1") || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1");
    }

    private String getClientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
