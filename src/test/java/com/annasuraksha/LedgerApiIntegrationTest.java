package com.annasuraksha;

import com.annasuraksha.model.Beneficiary;
import com.annasuraksha.model.BeneficiaryRepository;
import com.annasuraksha.model.auth.User;
import com.annasuraksha.model.auth.UserRepository;
import com.annasuraksha.service.auth.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LedgerApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired BeneficiaryRepository beneRepo;
    @Autowired UserRepository userRepo;
    @Autowired BCryptPasswordEncoder passwordEncoder;
    @Autowired JwtTokenService jwtService;

    private String officerToken;

    @BeforeEach
    void setup() {
        beneRepo.deleteAll();
        userRepo.deleteAll();
        User officer = userRepo.save(User.builder()
                .email("officer@test")
                .passwordHash(passwordEncoder.encode("pass"))
                .roles(List.of("ROLE_GOVT_OFFICER"))
                .active(true).build());
        officerToken = jwtService.generateToken(String.valueOf(officer.getId()), officer.getEmail(), officer.getRoles(), officer.getStateCode());

        beneRepo.saveAll(List.of(
                bene("HASH_AAA", "IN-UP"),
                bene("HASH_BBB", "IN-MH"),
                bene("HASH_CCC", "IN-KA")
        ));
    }

    private Beneficiary bene(String aadhaarHash, String stateCode) {
        Beneficiary b = new Beneficiary();
        b.setAadhaarHash(aadhaarHash);
        b.setStateCode(stateCode);
        b.setFamilySize(3);
        b.setStatus("ACTIVE");
        return b;
    }

    @Test
    void createSnapshotReturnsRoot() throws Exception {
        mvc.perform(post("/api/ledger/snapshot")
                .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.root").isNotEmpty());
    }

    @Test
    void proofAfterSnapshotIsAccessible() throws Exception {
        String snapshotJson = mvc.perform(post("/api/ledger/snapshot")
                        .header("Authorization", "Bearer " + officerToken))
                .andReturn().getResponse().getContentAsString();
        long snapId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(snapshotJson).get("data").get("id").asLong();
        long benId = beneRepo.findAll().get(0).getId();

        mvc.perform(get("/api/ledger/snapshot/{id}/proof/{benId}", snapId, benId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.proofJson").isNotEmpty());
    }

    @Test
    void verifyProofReturnsVerifiedTrue() throws Exception {
        String snapshotJson = mvc.perform(post("/api/ledger/snapshot")
                        .header("Authorization", "Bearer " + officerToken))
                .andReturn().getResponse().getContentAsString();
        long snapId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(snapshotJson).get("data").get("id").asLong();
        long benId = beneRepo.findAll().get(0).getId();

        mvc.perform(get("/api/ledger/snapshot/{id}/verify/{benId}", snapId, benId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true));
    }
}