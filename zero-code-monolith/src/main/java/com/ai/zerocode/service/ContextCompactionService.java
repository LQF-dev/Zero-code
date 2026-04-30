package com.ai.zerocode.service;

import com.ai.zerocode.config.ContextCompactionProperties;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Layer 2 & Layer 3：上下文自动压缩 / 手动压缩服务。
 * <p>
 * 当对话上下文的 token 估算值超过阈值时，调用轻量 LLM 生成对话摘要，
 * 用一条摘要消息替换全部历史消息，从而实现"无限会话"。
 * <p>
 * 完整的事件记录始终保留在 MySQL chat_event_log 表中，
 * 压缩只影响 Redis 中的实时上下文，不丢失任何数据。
 */
@Slf4j
@Service
public class ContextCompactionService {

    private final ContextCompactionProperties compactionProperties;

    /**
     * 用于生成摘要的轻量模型（非推理模型，节省成本）。
     */
    private final ChatModel chatModel;

    /**
     * Redis 记忆存储，用于直接操作持久化的消息。
     */
    private final ChatMemoryStore chatMemoryStore;

    private static final String SUMMARY_PROMPT = """
            请总结以下对话的关键信息，确保包含：
            1) 已完成的操作（创建/修改/删除了哪些文件），每次修改必须记录【修改前的原始值 → 修改后的新值】的完整对照，
               例如：「将首页标题从『静夜思』修改为『测试』」，而不是只写「将标题修改为『测试』」。
               这一点非常重要，因为用户可能需要回退到之前的状态，必须保留原始值。
            2) 当前项目状态（项目结构、关键组件）
            3) 用户的核心需求和偏好
            4) 尚未完成的任务或待解决的问题
            5) 关键数据的变更历史链（按时间顺序），包括文本内容、配置参数、样式属性等的前后值

            要求：
            - 对于每一处内容修改，必须同时保留修改前和修改后的值，确保可回溯可回退
            - 使用「A → B」格式记录变更对照
            - 简洁但不遗漏任何修改细节，使用中文回复

            对话内容：
            """;

    private static final String COMPACT_MARKER = "[对话已压缩，完整记录保留在事件日志中]";

    public ContextCompactionService(
            ContextCompactionProperties compactionProperties,
            @Qualifier("openAiChatModel") ChatModel chatModel,
            ChatMemoryStore redisChatMemoryStore) {
        this.compactionProperties = compactionProperties;
        this.chatModel = chatModel;
        this.chatMemoryStore = redisChatMemoryStore;
    }

    /**
     * Layer 2：自动压缩。检查当前上下文是否超过阈值，超过则执行压缩。
     *
     * @param chatMemory 当前会话的 ChatMemory
     * @param memoryId   会话 ID
     * @return true 如果执行了压缩
     */
    public boolean autoCompactIfNeeded(MessageWindowChatMemory chatMemory, String memoryId) {
        List<ChatMessage> messages = chatMemory.messages();
        if (messages == null || messages.isEmpty()) {
            return false;
        }

        int estimatedTokens = estimateTokens(messages);
        int threshold = compactionProperties.getTokenThreshold();
        if (estimatedTokens <= threshold) {
            return false;
        }

        log.info("自动压缩触发，memoryId={}, estimatedTokens={}, threshold={}",
                memoryId, estimatedTokens, threshold);
        return doCompact(chatMemory, memoryId, messages);
    }

    /**
     * Layer 3：手动压缩。无论是否超阈值，立即执行压缩。
     *
     * @param memoryId 会话 ID
     * @return true 如果执行了压缩
     */
    public boolean forceCompact(String memoryId) {
        List<ChatMessage> messages = chatMemoryStore.getMessages(memoryId);
        if (messages == null || messages.size() <= 2) {
            log.info("消息太少，无需压缩，memoryId={}", memoryId);
            return false;
        }

        // 手动压缩不经过 MessageWindowChatMemory，直接操作 store
        String summary = generateSummary(messages);
        List<ChatMessage> compacted = List.of(
                UserMessage.from(COMPACT_MARKER + "\n\n" + summary)
        );
        chatMemoryStore.updateMessages(memoryId, compacted);

        log.info("手动压缩完成，memoryId={}, 原消息数={}, 压缩后=1", memoryId, messages.size());
        return true;
    }

    /**
     * 执行压缩逻辑：生成摘要 → 替换消息。
     */
    private boolean doCompact(MessageWindowChatMemory chatMemory, String memoryId,
                              List<ChatMessage> messages) {
        try {
            String summary = generateSummary(messages);

            // 清空当前 memory 并写入摘要
            chatMemory.clear();
            chatMemory.add(UserMessage.from(COMPACT_MARKER + "\n\n" + summary));

            log.info("压缩完成，memoryId={}, 原消息数={}, 摘要长度={}",
                    memoryId, messages.size(), summary.length());
            return true;
        } catch (Exception e) {
            log.error("压缩失败，memoryId={}, 保持原始上下文不变", memoryId, e);
            return false;
        }
    }

    /**
     * 调用模型生成对话摘要。
     */
    private String generateSummary(List<ChatMessage> messages) {
        // 将消息序列化为文本（截取最后 maxSummaryInputChars 字符避免超长）
        StringBuilder conversationText = new StringBuilder();
        for (ChatMessage msg : messages) {
            conversationText.append(formatMessage(msg)).append("\n");
        }
        String text = conversationText.toString();
        int maxChars = compactionProperties.getMaxSummaryInputChars();
        if (text.length() > maxChars) {
            text = text.substring(text.length() - maxChars);
        }

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(UserMessage.from(SUMMARY_PROMPT + text)))
                .build();
        ChatResponse response = chatModel.chat(request);
        String summary = response.aiMessage().text();

        if (summary == null || summary.isBlank()) {
            summary = "无法生成摘要，请查看事件日志获取完整记录。";
        }
        return summary;
    }

    /**
     * 将 ChatMessage 格式化为可读文本。
     */
    private String formatMessage(ChatMessage msg) {
        if (msg instanceof UserMessage userMsg) {
            return "[用户] " + userMsg.singleText();
        } else if (msg instanceof dev.langchain4j.data.message.AiMessage aiMsg) {
            if (aiMsg.hasToolExecutionRequests()) {
                return "[AI 工具调用] " + aiMsg.toolExecutionRequests().toString();
            }
            return "[AI] " + (aiMsg.text() != null ? aiMsg.text() : "");
        } else if (msg instanceof dev.langchain4j.data.message.ToolExecutionResultMessage toolMsg) {
            String text = toolMsg.text();
            String toolName = toolMsg.toolName();
            if (text != null && text.length() > 2000) {
                // 对文件修改类工具，优先提取 oldContent/newContent 的关键差异
                if ("modifyFile".equals(toolName) || "createFile".equals(toolName)
                        || "deleteFile".equals(toolName) || "modifyFileContent".equals(toolName)) {
                    text = extractModificationDetails(text);
                } else {
                    text = text.substring(0, 1500) + "...[截断]";
                }
            }
            return "[工具结果: " + toolName + "] " + text;
        }
        return msg.toString();
    }

    /**
     * 从文件修改类工具结果中提取关键的修改前后信息。
     * 优先保留 oldContent/newContent 字段，确保摘要模型能看到完整的变更对照。
     */
    private String extractModificationDetails(String text) {
        StringBuilder details = new StringBuilder();
        // 提取 oldContent
        int oldIdx = text.indexOf("oldContent");
        if (oldIdx == -1) oldIdx = text.indexOf("\"old\"");
        if (oldIdx >= 0) {
            int end = Math.min(oldIdx + 1500, text.length());
            details.append("[修改前后内容] ").append(text, oldIdx, end);
        }
        // 提取 newContent
        int newIdx = text.indexOf("newContent");
        if (newIdx == -1) newIdx = text.indexOf("\"new\"");
        if (newIdx >= 0 && newIdx != oldIdx) {
            int end = Math.min(newIdx + 1500, text.length());
            if (!details.isEmpty()) details.append(" | ");
            details.append(text, newIdx, end);
        }
        // 提取文件路径信息
        int pathIdx = text.indexOf("filePath");
        if (pathIdx == -1) pathIdx = text.indexOf("\"path\"");
        if (pathIdx >= 0) {
            int end = Math.min(pathIdx + 200, text.length());
            details.insert(0, text.substring(pathIdx, end) + " | ");
        }
        // 如果未能提取到结构化信息，回退到截取前 2000 字符
        if (details.isEmpty()) {
            return text.substring(0, Math.min(2000, text.length())) + "...[截断]";
        }
        return details.toString();
    }

    /**
     * 粗略估算 token 数量：中英混合场景约 3 字符/token。
     */
    private int estimateTokens(List<ChatMessage> messages) {
        int totalChars = 0;
        for (ChatMessage msg : messages) {
            totalChars += msg.toString().length();
        }
        return totalChars / 3;
    }
}
