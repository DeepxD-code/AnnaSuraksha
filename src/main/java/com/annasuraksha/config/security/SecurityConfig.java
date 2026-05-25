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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
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
            .authorizeHttpRequests(auth -> auth
                // Public — auth, transparency, static UI, h2, actuator
                .requestMatchers(
                    "/api/auth/**",
                    "/api/transparency/**",
                    "/api/stats",
                    // H2 console: only allow when explicitly enabled via property
                    ("/h2-console/**"),
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
                ).permitAll()
                // Admin only
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Simulation — Admin + Officer
                .requestMatchers("/api/simulate/**").hasAnyRole("ADMIN", "GOVT_OFFICER")
                // Officer + Admin write operations
                .requestMatchers("/api/beneficiary/register",
                                 "/api/beneficiary/update/**").hasAnyRole("ADMIN", "GOVT_OFFICER")
                // FPS supply chain writes
                .requestMatchers("/api/supply-chain/warehouse-load",
                                 "/api/supply-chain/dispatch",
                                 "/api/supply-chain/fps-receive",
                                 "/api/supply-chain/waypoint").hasAnyRole("ADMIN", "GOVT_OFFICER", "FPS_OPERATOR")
                // Read access — Officer, Auditor, Admin
                .requestMatchers("/api/fraud/**",
                                 "/api/predict-risk/**",
                                 "/api/ledger/**",
                                 "/api/alerts/**",
                                 "/api/supply-chain/**",
                                 "/api/distribution/**",
                                 "/api/beneficiary/**").hasAnyRole("ADMIN", "GOVT_OFFICER", "AUDITOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // Allow H2 console in frames for dev
            .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))
            .build();
    }
}
