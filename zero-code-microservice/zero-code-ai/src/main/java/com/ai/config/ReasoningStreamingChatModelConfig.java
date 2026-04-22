package com.ai.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    /**
     * 流式请求超时时间，默认 240 秒。
     */
    private Duration timeout = Duration.ofSeconds(240);

    /**
     * 是否打印请求日志。
     */
    private Boolean logRequests = true;

    /**
     * 是否打印响应日志。
     */
    private Boolean logResponses = true;

    /**
     * 推理流式模型名（可通过配置覆盖）
     */
    @Value("${langchain4j.open-ai.reasoning-model-name:deepseek-reasoner}")
    private String reasoningModelName;

    /**
     * 推理流式最大输出 token（可通过配置覆盖）
     */
    @Value("${langchain4j.open-ai.reasoning-max-tokens:32768}")
    private Integer reasoningMaxTokens;

    /**
     * 是否返回深度思考内容
     */
    @Value("${langchain4j.open-ai.reasoning-return-thinking:true}")
    private Boolean reasoningReturnThinking;

    /**
     * 是否在请求中回放深度思考内容
     */
    @Value("${langchain4j.open-ai.reasoning-send-thinking:true}")
    private Boolean reasoningSendThinking;

    /**
     * 深度思考字段名称
     */
    @Value("${langchain4j.open-ai.reasoning-thinking-field:reasoning_content}")
    private String reasoningThinkingField;

    /**
     * 推理流式模型（用于 Vue 项目生成，带工具调用）
     */
    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(reasoningModelName)
                .maxTokens(reasoningMaxTokens)
                .strictJsonSchema(true)
                .strictTools(true)
                .parallelToolCalls(false)
                .returnThinking(Boolean.TRUE.equals(reasoningReturnThinking))
                .sendThinking(Boolean.TRUE.equals(reasoningSendThinking), reasoningThinkingField)
                .timeout(timeout)
                // 禁用压缩，降低代理/DNS 抖动时的异常概率
                .customHeaders(Map.of("Accept-Encoding", "identity"))
                .logRequests(Boolean.TRUE.equals(logRequests))
                .logResponses(Boolean.TRUE.equals(logResponses))
                .build();
    }
}
