package com.annasuraksha.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI annasurakshaOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AnnaSuraksha v5 — PDS Fraud Intelligence API")
                .description("""
                    Production-grade fraud detection and supply chain integrity platform for India's
                    Public Distribution System (PDS). Features include deterministic fraud scoring,
                    multi-layer ghost detection, Merkle tree proofs, blockchain anchoring,
                    and real-time SSE alerts.

                    ## Authentication
                    All protected endpoints require a JWT Bearer token in the Authorization header.
                    Obtain a token via POST /api/auth/login.

                    ## Roles
                    - ROLE_ADMIN: Full access including admin ledger operations
                    - ROLE_GOVT_OFFICER: Fraud analytics, beneficiary management, simulations
                    - ROLE_AUDITOR: Read-only access to fraud data and ledger
                    - ROLE_FPS_OPERATOR: Supply chain operations and distribution

                    ## Response Format
                    All responses follow the envelope: { success, data, error, meta }
                    """)
                .version("5.1.0")
                .contact(new Contact()
                    .name("AnnaSuraksha Team")
                    .email("admin@annasuraksha.gov.in"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server().url("http://localhost:8081").description("Local Development"),
                new Server().url("https://api.annasuraksha.gov.in").description("Production")
            ));
    }
}
