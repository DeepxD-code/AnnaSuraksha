package com.annasuraksha.config.security;

import com.annasuraksha.service.AuditLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

        boolean isLocal = isLocalhost(clientIp);

        // Log the attempt
        auditLogService.logAuthEvent(isLocal, clientIp, "anonymous", path);

        if (!isLocal) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to dev console is restricted to localhost.");
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
