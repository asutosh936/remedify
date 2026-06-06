package com.remedify.integration;

import com.remedify.model.AIRecommendation;
import com.remedify.model.Vulnerability;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class ClaudeAIIntegration {

  private final AnthropicChatClient chatClient;

  public ClaudeAIIntegration(AnthropicChatClient chatClient) {
    this.chatClient = chatClient;
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
      ChatResponse response = chatClient.call(new Prompt(new UserMessage(prompt)));
      String suggestion = response.getResult().getOutput().getContent();

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
