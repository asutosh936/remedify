package com.remedify.config;

import org.springframework.ai.anthropic.AnthropicChatClient;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class SpringAIConfig {

  @Bean
  public AnthropicChatOptions anthropicChatOptions() {
    return AnthropicChatOptions.builder()
        .withTemperature(0.3f)
        .withMaxTokens(1024)
        .build();
  }
}
