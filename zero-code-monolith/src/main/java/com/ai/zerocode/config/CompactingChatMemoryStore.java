package com.ai.zerocode.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Layer 1 微压缩：装饰 ChatMemoryStore，压缩旧的工具执行结果。
 * <p>
 * 策略：保留最近 {@link #KEEP_RECENT_RESULTS} 条 ToolExecutionResultMessage 的原始内容，
 * 更早的工具结果（内容超过 {@link #MIN_COMPRESS_LENGTH} 字符）替换为简短占位符。
 * <p>
 * 目的：减少发送给 LLM 的历史上下文中工具结果占用的 token，同时保留工具调用的痕迹，
 * 让模型知道"做过什么"但不浪费 token 在已处理过的详细内容上。
 */
@Slf4j
public class CompactingChatMemoryStore implements ChatMemoryStore {

    private final ChatMemoryStore delegate;

    /**
     * 保留最近多少条工具结果不压缩。
     * 对应约 1-2 轮工具交互（每轮可能包含多次工具调用）。
     */
    private static final int KEEP_RECENT_RESULTS = 3;

    /**
     * 工具结果内容低于此长度不压缩（短结果压缩没有意义）。
     */
    private static final int MIN_COMPRESS_LENGTH = 100;

    public CompactingChatMemoryStore(ChatMemoryStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<ChatMessage> messages = delegate.getMessages(memoryId);
        if (messages == null || messages.isEmpty()) {
            return messages == null ? List.of() : messages;
        }
        return microCompact(messages);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        delegate.updateMessages(memoryId, messages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        delegate.deleteMessages(memoryId);
    }

    /**
     * 调用 LLM 前执行持久微压缩：读取 Redis 中的工作上下文，压缩旧工具结果并写回 Redis。
     *
     * @return true 表示本次实际写回了压缩后的消息
     */
    public boolean compactAndPersist(Object memoryId) {
        List<ChatMessage> messages = delegate.getMessages(memoryId);
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        MicroCompactResult result = microCompactWithStats(messages);
        if (result.compactedCount() <= 0) {
            return false;
        }
        delegate.updateMessages(memoryId, result.messages());
        log.info("持久微压缩完成，memoryId={}, 压缩了 {} 条工具结果", memoryId, result.compactedCount());
        return true;
    }

    /**
     * 微压缩：将旧的 ToolExecutionResultMessage 内容替换为占位符。
     */
    private List<ChatMessage> microCompact(List<ChatMessage> messages) {
        return microCompactWithStats(messages).messages();
    }

    /**
     * 微压缩：将旧的 ToolExecutionResultMessage 内容替换为占位符。
     */
    private MicroCompactResult microCompactWithStats(List<ChatMessage> messages) {
        // 1. 收集所有 ToolExecutionResultMessage 的索引
        List<Integer> toolResultIndices = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof ToolExecutionResultMessage) {
                toolResultIndices.add(i);
            }
        }

        // 工具结果数量不超过保留阈值，无需压缩
        if (toolResultIndices.size() <= KEEP_RECENT_RESULTS) {
            return new MicroCompactResult(messages, 0);
        }

        // 2. 需要压缩的：除了最后 KEEP_RECENT_RESULTS 条之外的所有工具结果
        int compactBoundary = toolResultIndices.size() - KEEP_RECENT_RESULTS;
        List<Integer> toCompactIndices = toolResultIndices.subList(0, compactBoundary);

        // 3. 构建新的消息列表，替换需要压缩的工具结果
        List<ChatMessage> compacted = new ArrayList<>(messages.size());
        int compactedCount = 0;

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (toCompactIndices.contains(i) && msg instanceof ToolExecutionResultMessage toolResult) {
                String text = toolResult.text();
                if (text != null && text.length() > MIN_COMPRESS_LENGTH) {
                    // 替换为占位符，保留工具名和 ID 以维持消息配对完整性
                    String placeholder = "[已执行: " + toolResult.toolName() + "]";
                    compacted.add(ToolExecutionResultMessage.from(
                            toolResult.id(), toolResult.toolName(), placeholder));
                    compactedCount++;
                } else {
                    compacted.add(msg);
                }
            } else {
                compacted.add(msg);
            }
        }

        return new MicroCompactResult(compacted, compactedCount);
    }

    private record MicroCompactResult(List<ChatMessage> messages, int compactedCount) {
    }
}
