package com.ai.langgraph4j.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 工作流流式分片回调注册器。
 * 使用 sessionId 在节点间恢复 streamConsumer，避免 transient 字段在状态流转中丢失。
 */
@Component
public class WorkflowStreamConsumerRegistry {

    private final Map<String, Consumer<String>> consumerMap = new ConcurrentHashMap<>();

    public void register(String sessionId, Consumer<String> consumer) {
        if (sessionId == null || sessionId.isBlank() || consumer == null) {
            return;
        }
        consumerMap.put(sessionId, consumer);
    }

    public Consumer<String> get(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return consumerMap.get(sessionId);
    }

    public void remove(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        consumerMap.remove(sessionId);
    }
}
