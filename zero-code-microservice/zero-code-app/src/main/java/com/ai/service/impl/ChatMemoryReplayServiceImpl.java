package com.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ai.model.entity.ChatEventLog;
import com.ai.model.enums.ChatEventTypeEnum;
import com.ai.service.ChatEventLogService;
import com.ai.service.ChatMemoryReplayService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于 chat_event_log 的 memory 回放实现。
 */
@Service
@Slf4j
public class ChatMemoryReplayServiceImpl implements ChatMemoryReplayService {

    @Resource
    private ChatEventLogService chatEventLogService;

    @Override
    public int rebuildFromEventLog(String memoryId, MessageWindowChatMemory chatMemory, int maxEvents) {
        chatMemory.clear();
        List<ChatEventLog> events = chatEventLogService.listRecentEventsForReplay(memoryId, maxEvents);
        if (events.isEmpty()) {
            return 0;
        }
        int loadedCount = 0;
        for (ChatEventLog event : events) {
            ChatEventTypeEnum eventType = ChatEventTypeEnum.getEnumByValue(event.getEventType());
            if (eventType == null) {
                continue;
            }
            switch (eventType) {
                case USER_MESSAGE -> {
                    if (StrUtil.isNotBlank(event.getContent())) {
                        chatMemory.add(UserMessage.from(event.getContent()));
                        loadedCount++;
                    }
                }
                case TOOL_REQUEST -> {
                    if (StrUtil.hasBlank(event.getToolCallId(), event.getToolName())) {
                        continue;
                    }
                    ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                            .id(event.getToolCallId())
                            .name(event.getToolName())
                            .arguments(StrUtil.nullToEmpty(event.getToolArguments()))
                            .build();
                    chatMemory.add(AiMessage.from(toolRequest));
                    loadedCount++;
                }
                case TOOL_RESULT -> {
                    if (StrUtil.hasBlank(event.getToolCallId(), event.getToolName())) {
                        continue;
                    }
                    String resultText = StrUtil.isNotBlank(event.getToolResult()) ? event.getToolResult() : event.getContent();
                    if (StrUtil.isBlank(resultText)) {
                        continue;
                    }
                    chatMemory.add(ToolExecutionResultMessage.from(event.getToolCallId(), event.getToolName(), resultText));
                    loadedCount++;
                }
                case ASSISTANT_FINAL -> {
                    if (StrUtil.isBlank(event.getContent())) {
                        continue;
                    }
                    AiMessage.Builder builder = AiMessage.builder().text(event.getContent());
                    if (StrUtil.isNotBlank(event.getReasoningContent())) {
                        builder.thinking(event.getReasoningContent());
                    }
                    chatMemory.add(builder.build());
                    loadedCount++;
                }
                default -> {
                    // PARTIAL/THINKING 事件当前不直接回放，避免重复扩散。
                }
            }
        }
        log.info("从 chat_event_log 重建 memory 完成，memoryId={}, loadedCount={}", memoryId, loadedCount);
        return loadedCount;
    }
}
