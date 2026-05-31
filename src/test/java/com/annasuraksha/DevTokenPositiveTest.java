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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.h2.console.enabled=true",
    "dev.bootstrap.secret=test-secret"
})
public class DevTokenPositiveTest {

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
    public void mintDevTokenWithBootstrapSecret() throws Exception {
        mvc.perform(post("/api/auth/dev-token")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-BOOTSTRAP-SECRET", "test-secret")
            .content("{\"email\":\"admin@local\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").exists());
    }
}
