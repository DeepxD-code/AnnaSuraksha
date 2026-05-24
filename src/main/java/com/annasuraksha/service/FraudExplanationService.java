package com.annasuraksha.service;

import com.annasuraksha.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class FraudExplanationService {

    private final BeneficiaryRepository beneRepo;
    private final ObjectMapper          mapper = new ObjectMapper();

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    private static final String SYSTEM_PROMPT = """
        You are an AI fraud analyst for India's Public Distribution System (PDS).
        Given a structured fraud risk record, generate a concise 3-sentence explanation
        suitable for a government audit report.
        Sentence 1: Who is flagged, risk level and score.
        Sentence 2: The specific fraud signals detected.
        Sentence 3: Policy implication and estimated monthly fraud value in ₹.
        Use formal English. Reference NFSA 2013 where relevant.
        Do NOT use markdown. Be specific about numbers.
        """;

    public String explain(FraudRiskScore score) {
        Optional<Beneficiary> bOpt = beneRepo.findById(score.subjectId());
        Beneficiary b = bOpt.orElse(null);
        String context = buildContext(score, b);
        try {
            return callGroq(context);
        } catch (Exception e) {
            log.warn("Groq unavailable for beneficiary {} — using fallback: {}", score.subjectId(), e.getMessage());
            return buildFallback(score, b);
        }
    }

    public Map<Long, String> explainBatch(List<FraudRiskScore> scores, int limit) {
        Map<Long, String> results = new LinkedHashMap<>();
        scores.stream()
            .filter(s -> s.riskLevel() == FraudRiskScore.RiskLevel.HIGH)
            .limit(limit)
            .forEach(s -> results.put(s.subjectId(), explain(s)));
        return results;
    }

    private String buildContext(FraudRiskScore score, Beneficiary b) {
        ObjectNode ctx = mapper.createObjectNode();
        ctx.put("beneficiary_id", score.subjectId());
        ctx.put("name",           score.subjectName());
        ctx.put("state",          score.stateCode());
        ctx.put("risk_score",     String.format("%.3f", score.riskScore()));
        ctx.put("risk_level",     score.riskLevel().name());
        if (b != null) {
            ctx.put("category",    b.getCategory() != null ? b.getCategory() : "unknown");
            ctx.put("family_size", b.getFamilySize() != null ? b.getFamilySize() : 0);
            ctx.put("claim_count", b.getClaimCount() != null ? b.getClaimCount() : 0);
            ctx.put("is_migrant",  Boolean.TRUE.equals(b.getMigrant()));
        }
        var f = score.features();
        if (f != null) {
            ObjectNode signals = ctx.putObject("fraud_signals");
            signals.put("duplicate_aadhaar",    f.duplicateAadhaarSignal());
            signals.put("impossible_travel",    f.impossibleTravelSignal());
            signals.put("cross_state_fraud",    f.crossStateFraudSignal());
            signals.put("ration_anomaly",       f.rationUsageAnomalyScore());
            signals.put("category_mismatch",    f.categoryMismatchSignal());
            signals.put("dealer_diversion",     f.dealerDiversionRate());
            signals.put("night_time_claim",     f.nightTimeClaimSignal());
        }
        ArrayNode factors = ctx.putArray("top_factors");
        score.topFactors().forEach(factors::add);
        return ctx.toString();
    }

    private String callGroq(String contextJson) {
        WebClient client = WebClient.builder()
            .baseUrl("https://api.groq.com/openai/v1")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 300);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
        messages.addObject().put("role", "user").put("content", "Explain:\n" + contextJson);

        String response = client.post()
            .uri("/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        try {
            JsonNode root = mapper.readTree(response);
            String text = root.path("choices").path(0).path("message").path("content").asText();
            return text.isBlank() ? "Explanation unavailable." : text.trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Groq response: " + e.getMessage());
        }
    }

    String buildFallback(FraudRiskScore score, Beneficiary b) {
        var f    = score.features();
        String name     = score.subjectName() != null ? score.subjectName() : "Beneficiary #" + score.subjectId();
        String state    = score.stateCode()   != null ? score.stateCode()   : "unknown";
        String category = b != null && b.getCategory() != null ? b.getCategory() : "unknown";

        String s1 = String.format("%s (ID #%d, %s, %s) flagged as %s risk — score %.3f/1.00.",
            name, score.subjectId(), state, category, score.riskLevel().name(), score.riskScore());

        List<String> signals = new ArrayList<>();
        if (f != null) {
            if (f.duplicateAadhaarSignal()  > 0) signals.add("duplicate Aadhaar across states");
            if (f.impossibleTravelSignal()  > 0) signals.add("physically impossible inter-state travel");
            if (f.crossStateFraudSignal()   > 0) signals.add("illegal cross-state claim (non-ONORC)");
            if (f.rationUsageAnomalyScore() > 0.3) signals.add("over-entitlement above NFSA maximum");
            if (f.categoryMismatchSignal()  > 0) signals.add("AAY category for undersized household");
            if (f.dealerDiversionRate()     > 0.2) signals.add(String.format("%.0f%% FPS deliveries flagged", f.dealerDiversionRate()*100));
            if (f.nightTimeClaimSignal()    > 0) signals.add("claim recorded outside legal FPS hours");
        }
        String s2 = signals.isEmpty() ? "Anomalous claim patterns detected."
            : "Signals: " + String.join("; ", signals) + ".";

        long loss = estimateLoss(category);
        String s3 = String.format("Under NFSA 2013, this requires immediate review; estimated monthly loss ₹%,d.", loss);
        return s1 + " " + s2 + " " + s3;
    }

    private long estimateLoss(String category) {
        if (category == null) return 96L;
        return switch (category) {
            case "AAY" -> 350L;
            case "BPL" -> 96L;
            case "PHH" -> 75L;
            default    -> 96L;
        };
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FraudExplanationService.class);
    public FraudExplanationService(BeneficiaryRepository beneRepo) {
        this.beneRepo = beneRepo;
    }
}
