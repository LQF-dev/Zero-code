package com.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * 路由 / 质检链路专用稳定 ChatModel 配置
 * 通过禁用压缩规避代理/网络环境下的响应截断解压异常。
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class StableOpenAiChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens = 8192;

    private Boolean logRequests = true;

    private Boolean logResponses = true;

    @Bean("stableOpenAiChatModel")
    public ChatModel stableOpenAiChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(2)
                // 不要压缩 通过禁用压缩规避代理/网络环境下的响应截断解压异常。
                .customHeaders(Map.of("Accept-Encoding", "identity"))
                .logRequests(Boolean.TRUE.equals(logRequests))
                .logResponses(Boolean.TRUE.equals(logResponses))
                .build();
    }
}
