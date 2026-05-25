package com.annasuraksha;

import com.annasuraksha.model.auth.User;
import com.annasuraksha.model.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DevTokenIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepo;
    @Autowired BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        userRepo.deleteAll();
        userRepo.save(User.builder()
            .email("admin@local")
            .passwordHash(passwordEncoder.encode("password"))
            .roles(List.of("ROLE_ADMIN"))
            .active(true)
            .build());
    }

    @Test
    public void devTokenEndpointReturnsErrorWhenDisabled() throws Exception {
        // By default tests run with spring.h2.console.enabled=false so the endpoint should return 200
        // with success=false and error code "NOT_ALLOWED".
        mvc.perform(post("/api/auth/dev-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"admin@local\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.meta.code").value("NOT_ALLOWED"));
    }
}
