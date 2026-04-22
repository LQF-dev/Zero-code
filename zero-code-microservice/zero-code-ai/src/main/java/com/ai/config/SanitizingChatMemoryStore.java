package com.ai.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 对 ChatMemory 做兜底清洗，避免回放非法 assistant 消息导致模型 400。
 */
@Slf4j
public class SanitizingChatMemoryStore implements ChatMemoryStore {

    private final ChatMemoryStore delegate;

    public SanitizingChatMemoryStore(ChatMemoryStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return sanitizeMessages(delegate.getMessages(memoryId), memoryId);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        delegate.updateMessages(memoryId, sanitizeMessages(messages, memoryId));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        delegate.deleteMessages(memoryId);
    }

    private List<ChatMessage> sanitizeMessages(List<ChatMessage> messages, Object memoryId) {
        if (messages == null || messages.isEmpty()) {
            return messages == null ? List.of() : messages;
        }
        List<ChatMessage> sanitized = new ArrayList<>(messages.size());
        for (ChatMessage message : messages) {
            if (message instanceof AiMessage aiMessage) {
                boolean hasText = hasText(aiMessage.text());
                boolean hasToolCalls = aiMessage.hasToolExecutionRequests();
                if (!hasText && !hasToolCalls) {
                    // OpenAI 兼容接口要求 assistant 至少有 content 或 tool_calls。
                    String thinking = aiMessage.thinking();
                    if (hasText(thinking)) {
                        AiMessage repaired = AiMessage.builder()
                                .text(thinking)
                                .thinking(thinking)
                                .toolExecutionRequests(aiMessage.toolExecutionRequests())
                                .attributes(aiMessage.attributes())
                                .build();
                        sanitized.add(repaired);
                        log.warn("修复无 content 的 assistant 消息，memoryId={}", memoryId);
                    } else {
                        log.warn("丢弃空 assistant 消息，memoryId={}", memoryId);
                    }
                    continue;
                }
            }
            sanitized.add(message);
        }
        return sanitized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
