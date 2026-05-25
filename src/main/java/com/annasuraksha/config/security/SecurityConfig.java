package com.annasuraksha.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final Environment env;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, Environment env) {
        this.jwtFilter = jwtFilter;
        this.env = env;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                List<String> publicMatchers = new ArrayList<>(List.of(
                    "/api/auth/**",
                    "/api/transparency/**",
                    "/api/stats",
                    "/actuator/health",
                    "/actuator/info",
                    "/",
                    "/index.html",
                    "/dashboard.html",
                    "/*.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/error"
                ));

                // H2 console is not public. When enabled in dev, restrict access to admin/dev/gov roles only.
                auth.requestMatchers(publicMatchers.toArray(new String[0])).permitAll();

                if (Boolean.parseBoolean(env.getProperty("spring.h2.console.enabled", "false"))) {
                    // Restrict H2 console and the dev landing page to privileged roles so only devs/admins/gov
                    // officers can access it.
                    auth.requestMatchers("/h2-console/**").hasAnyRole("ADMIN", "GOVT_OFFICER", "DEV");
                    auth.requestMatchers("/dev/console").hasAnyRole("ADMIN", "GOVT_OFFICER", "DEV");
                }

                // Admin only
                auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                auth.requestMatchers("/admin/**").hasRole("ADMIN");
                // Simulation — Admin + Officer
                auth.requestMatchers("/api/simulate/**").hasAnyRole("ADMIN", "GOVT_OFFICER");
                // Officer + Admin write operations
                auth.requestMatchers("/api/beneficiary/register",
                                     "/api/beneficiary/update/**").hasAnyRole("ADMIN", "GOVT_OFFICER");
                // FPS supply chain writes
                auth.requestMatchers("/api/supply-chain/warehouse-load",
                                     "/api/supply-chain/dispatch",
                                     "/api/supply-chain/fps-receive",
                                     "/api/supply-chain/waypoint").hasAnyRole("ADMIN", "GOVT_OFFICER", "FPS_OPERATOR");
                // Read access — Officer, Auditor, Admin
                auth.requestMatchers("/api/fraud/**",
                                     "/api/predict-risk/**",
                                     "/api/ledger/**",
                                     "/api/alerts/**",
                                     "/api/supply-chain/**",
                                     "/api/distribution/**",
                                     "/api/beneficiary/**").hasAnyRole("ADMIN", "GOVT_OFFICER", "AUDITOR");
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // Allow H2 console in frames for dev
            .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))
            .build();
    }
}
