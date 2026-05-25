package com.annasuraksha.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Lightweight controller to expose a small landing page that links to the H2 console.
 * This page is protected by the same security rules as other endpoints, so only users with
 * ADMIN, GOVT_OFFICER or DEV roles will reach it when H2 is enabled.
 */
@Controller
public class DevConsoleController {

    @GetMapping("/dev/console")
    public String consoleLanding() {
        // Forward to a simple static page which links to /h2-console. This keeps the route protected
        // by standard Spring Security rules while providing an easy navigation target for devs/admins.
        return "dev-console";
    }
}
