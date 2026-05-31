package com.annasuraksha;

import com.annasuraksha.model.auth.User;
import com.annasuraksha.model.auth.UserRepository;
import com.annasuraksha.service.auth.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.h2.console.enabled=false",
    "annasuraksha.seed.demo-data=false"
})
public class AdminLedgerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepo;
    @Autowired BCryptPasswordEncoder passwordEncoder;
    @Autowired JwtTokenService jwtService;

    private String adminToken;

    @BeforeEach
    public void setup() {
        userRepo.deleteAll();
        User admin = userRepo.save(User.builder()
            .email("admin@test")
            .passwordHash(passwordEncoder.encode("password"))
            .roles(List.of("ROLE_ADMIN"))
            .active(true)
            .build());
        adminToken = jwtService.generateToken(String.valueOf(admin.getId()), admin.getEmail(), admin.getRoles(), admin.getStateCode());
    }

    @Test
    public void verifyAllRequiresAdminRole() throws Exception {
        mvc.perform(get("/api/admin/ledger/verify-all"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void verifyAllWithAdminToken() throws Exception {
        mvc.perform(get("/api/admin/ledger/verify-all")
            .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void auditRecentRequiresAdminRole() throws Exception {
        mvc.perform(get("/api/admin/audit/recent"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void auditRecentWithAdminToken() throws Exception {
        mvc.perform(get("/api/admin/audit/recent")
            .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}