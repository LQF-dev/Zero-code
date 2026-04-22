package com.ai.core.handler;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 单轮对话聚合上下文（流式阶段仅聚合，不落库）。
 */
@Getter
public class TurnAccumulator {

    private final long appId;

    private final long userId;

    private final String memoryId;

    private final String turnId;

    private final String codeGenType;

    private final String userMessage;

    private final StringBuilder assistantBuilder = new StringBuilder();

    private final StringBuilder thinkingBuilder = new StringBuilder();

    private final List<ToolTrace> toolTraces = new ArrayList<>();

    private boolean flushing;

    public TurnAccumulator(long appId, long userId, String memoryId, String turnId, String codeGenType, String userMessage) {
        this.appId = appId;
        this.userId = userId;
        this.memoryId = memoryId;
        this.turnId = turnId;
        this.codeGenType = codeGenType;
        this.userMessage = userMessage;
    }

    public synchronized boolean beginFlush() {
        if (flushing) {
            return false;
        }
        flushing = true;
        return true;
    }

    public synchronized void resetFlushFlagOnFailure() {
        flushing = false;
    }

    public synchronized void appendAssistant(String chunk) {
        if (chunk != null && !chunk.isEmpty()) {
            assistantBuilder.append(chunk);
        }
    }

    public synchronized void appendThinking(String chunk) {
        if (chunk != null && !chunk.isEmpty()) {
            thinkingBuilder.append(chunk);
        }
    }

    public synchronized String assistantText() {
        return assistantBuilder.toString();
    }

    public synchronized String thinkingText() {
        return thinkingBuilder.toString();
    }

    public synchronized void addToolRequest(String toolCallId, String toolName, String toolArguments, String content, String rawEventJson) {
        toolTraces.add(ToolTrace.request(toolCallId, toolName, toolArguments, content, rawEventJson));
    }

    public synchronized void addToolResult(String toolCallId, String toolName, String toolArguments, String toolResult, String content, String rawEventJson) {
        toolTraces.add(ToolTrace.result(toolCallId, toolName, toolArguments, toolResult, content, rawEventJson));
    }

    public synchronized List<ToolTrace> copyToolTraces() {
        return new ArrayList<>(toolTraces);
    }

    @Getter
    public static class ToolTrace {

        private final String eventType;

        private final String role;

        private final String toolCallId;

        private final String toolName;

        private final String toolArguments;

        private final String toolResult;

        private final String content;

        private final String rawEventJson;

        private ToolTrace(String eventType, String role, String toolCallId, String toolName, String toolArguments,
                          String toolResult, String content, String rawEventJson) {
            this.eventType = eventType;
            this.role = role;
            this.toolCallId = toolCallId;
            this.toolName = toolName;
            this.toolArguments = toolArguments;
            this.toolResult = toolResult;
            this.content = content;
            this.rawEventJson = rawEventJson;
        }

        public static ToolTrace request(String toolCallId, String toolName, String toolArguments, String content, String rawEventJson) {
            return new ToolTrace("TOOL_REQUEST", "assistant", toolCallId, toolName, toolArguments, null, content, rawEventJson);
        }

        public static ToolTrace result(String toolCallId, String toolName, String toolArguments, String toolResult, String content, String rawEventJson) {
            return new ToolTrace("TOOL_RESULT", "tool", toolCallId, toolName, toolArguments, toolResult, content, rawEventJson);
        }
    }
}
