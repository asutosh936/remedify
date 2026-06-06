package com.remedify.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.remedify.model.AIRecommendation;
import com.remedify.model.Vulnerability;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class ClaudeAIIntegration {

  private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
  private static final String MODEL = "claude-3-5-sonnet-20241022";
  private static final int MAX_TOKENS = 1024;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${spring.ai.anthropic.api-key}")
  private String apiKey;

  public ClaudeAIIntegration(RestTemplate restTemplate, ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
  }

  public List<AIRecommendation> generateRecommendations(
      List<Vulnerability> highSeverityVulnerabilities) throws Exception {
    log.info("Generating AI recommendations for {} vulnerabilities",
        highSeverityVulnerabilities.size());

    // Group vulnerabilities by type for batch processing
    var groupedByType = highSeverityVulnerabilities.stream()
        .collect(Collectors.groupingBy(Vulnerability::getType));

    return groupedByType.entrySet().stream()
        .flatMap(entry -> {
          try {
            return generateRecommendationsForType(entry.getKey(), entry.getValue()).stream();
          } catch (Exception e) {
            log.error("Failed to generate recommendations for type: {}", entry.getKey(), e);
            return java.util.stream.Stream.empty();
          }
        })
        .collect(Collectors.toList());
  }

  private List<AIRecommendation> generateRecommendationsForType(
      String vulnerabilityType,
      List<Vulnerability> vulnerabilities) throws Exception {

    String prompt = buildPrompt(vulnerabilityType, vulnerabilities);
    log.debug("Calling Claude API for vulnerability type: {}", vulnerabilityType);

    try {
      String suggestion = callClaudeAPI(prompt);

      return vulnerabilities.stream()
          .map(vuln -> {
            AIRecommendation rec = new AIRecommendation();
            rec.setVulnerability(vuln);
            rec.setSuggestion(suggestion);
            rec.setEstimatedEffort("Medium");
            return rec;
          })
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Claude API call failed for type: {}", vulnerabilityType, e);
      throw e;
    }
  }

  private String callClaudeAPI(String prompt) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-api-key", apiKey);
    headers.set("anthropic-version", "2023-06-01");

    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", MODEL);
    body.put("max_tokens", MAX_TOKENS);
    body.put("temperature", 0.3);

    ArrayNode messages = body.putArray("messages");
    ObjectNode message = messages.addObject();
    message.put("role", "user");
    message.put("content", prompt);

    HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

    try {
      String response = restTemplate.postForObject(CLAUDE_API_URL, request, String.class);
      JsonNode responseNode = objectMapper.readTree(response);
      JsonNode contentArray = responseNode.get("content");
      if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
        return contentArray.get(0).get("text").asText();
      }
      throw new Exception("Unexpected response format from Claude API");
    } catch (Exception e) {
      log.error("Error calling Claude API", e);
      throw e;
    }
  }

  private String buildPrompt(String vulnerabilityType, List<Vulnerability> vulnerabilities) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("You are a security expert. Provide a concise fix recommendation for the following ")
        .append(vulnerabilityType).append(" vulnerabilities:\n\n");

    for (Vulnerability vuln : vulnerabilities) {
      prompt.append("- File: ").append(vuln.getFilePath()).append("\n")
          .append("  Description: ").append(vuln.getDescription()).append("\n")
          .append("  CVE: ").append(vuln.getCveId()).append("\n\n");
    }

    prompt.append("\nProvide a practical, step-by-step fix that can be applied to the codebase. ")
        .append("Keep the response concise and actionable (under 500 words).");

    return prompt.toString();
  }
}
