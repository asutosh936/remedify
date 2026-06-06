package com.remedify.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.remedify.exception.ScanException;
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

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class ClaudeAIIntegration {

  private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
  private static final String MODEL = "claude-3-5-sonnet-20241022";
  private static final int MAX_TOKENS = 1024;
  private static final float TEMPERATURE = 0.3f;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${spring.ai.anthropic.api-key}")
  private String apiKey;

  public ClaudeAIIntegration(RestTemplate restTemplate, ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
  }

  /**
   * Generate AI recommendation for a vulnerability type with multiple instances
   *
   * @param vulnerabilityType The type of vulnerability (e.g., "Log4j RCE")
   * @param vulnerabilities   List of vulnerabilities of this type
   * @return AI-generated fix recommendation
   */
  public String generateRecommendation(String vulnerabilityType, List<Vulnerability> vulnerabilities)
      throws Exception {

    if (vulnerabilities.isEmpty()) {
      throw new ScanException("NO_VULNS", "No vulnerabilities provided for recommendation");
    }

    log.info("Generating Claude recommendation for {} ({} instances)",
        vulnerabilityType, vulnerabilities.size());

    String prompt = buildPrompt(vulnerabilityType, vulnerabilities);
    log.debug("Prompt length: {} characters", prompt.length());

    try {
      String recommendation = callClaudeAPI(prompt);
      log.info("Successfully generated recommendation for {}", vulnerabilityType);
      return recommendation;

    } catch (Exception e) {
      log.error("Failed to generate recommendation for {}: {}", vulnerabilityType, e.getMessage(), e);
      throw new ScanException("CLAUDE_API_FAILED",
          "Failed to call Claude API: " + e.getMessage(), e);
    }
  }

  /**
   * Call Claude API with prompt and return generated text
   */
  private String callClaudeAPI(String prompt) throws Exception {
    log.debug("Calling Claude API - Model: {}, Max Tokens: {}", MODEL, MAX_TOKENS);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-api-key", apiKey);
    headers.set("anthropic-version", "2023-06-01");

    // Build request body
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", MODEL);
    body.put("max_tokens", MAX_TOKENS);
    body.put("temperature", TEMPERATURE);

    // Add message
    ArrayNode messages = body.putArray("messages");
    ObjectNode message = messages.addObject();
    message.put("role", "user");
    message.put("content", prompt);

    HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

    try {
      log.debug("Sending request to Claude API: {}", CLAUDE_API_URL);
      String response = restTemplate.postForObject(CLAUDE_API_URL, request, String.class);

      if (response == null) {
        throw new Exception("Null response from Claude API");
      }

      // Parse response
      JsonNode responseNode = objectMapper.readTree(response);

      // Check for errors
      if (responseNode.has("error")) {
        String errorMsg = responseNode.get("error").get("message").asText("Unknown error");
        throw new Exception("Claude API error: " + errorMsg);
      }

      // Extract text from content array
      JsonNode contentArray = responseNode.get("content");
      if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
        String text = contentArray.get(0).get("text").asText();
        log.debug("Claude response length: {} characters", text.length());
        return text;
      }

      throw new Exception("Unexpected response format from Claude API: missing content array");

    } catch (Exception e) {
      log.error("Claude API call failed: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * Build prompt for Claude API
   */
  private String buildPrompt(String vulnerabilityType, List<Vulnerability> vulnerabilities) {
    StringBuilder prompt = new StringBuilder();

    // System context
    prompt.append("You are an expert security engineer specializing in vulnerability remediation.\n\n");

    // Task
    prompt.append("VULNERABILITY TYPE: ").append(vulnerabilityType).append("\n");
    prompt.append("NUMBER OF INSTANCES: ").append(vulnerabilities.size()).append("\n\n");

    // Vulnerability details
    prompt.append("AFFECTED FILES:\n");
    for (Vulnerability vuln : vulnerabilities) {
      prompt.append("- ").append(vuln.getFilePath());
      if (vuln.getCveId() != null && !vuln.getCveId().isEmpty()) {
        prompt.append(" (").append(vuln.getCveId()).append(")");
      }
      prompt.append("\n");
      if (vuln.getDescription() != null && !vuln.getDescription().isEmpty()) {
        prompt.append("  Description: ").append(vuln.getDescription()).append("\n");
      }
    }

    prompt.append("\n");
    prompt.append("INSTRUCTIONS:\n");
    prompt.append("1. Provide a clear, concise fix recommendation\n");
    prompt.append("2. Include step-by-step remediation steps\n");
    prompt.append("3. Provide code examples where applicable\n");
    prompt.append("4. Estimate effort level (Low/Medium/High)\n");
    prompt.append("5. Keep response under 500 words\n");
    prompt.append("6. Focus on practical, actionable advice\n");

    return prompt.toString();
  }
}
