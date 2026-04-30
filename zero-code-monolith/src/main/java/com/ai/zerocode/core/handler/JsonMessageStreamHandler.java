package com.ai.zerocode.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ai.zerocode.ai.model.message.AiResponseMessage;
import com.ai.zerocode.ai.model.message.StreamMessage;
import com.ai.zerocode.ai.model.message.StreamMessageTypeEnum;
import com.ai.zerocode.ai.model.message.ThinkingMessage;
import com.ai.zerocode.ai.model.message.ToolExecutedMessage;
import com.ai.zerocode.ai.model.message.ToolRequestMessage;
import com.ai.zerocode.ai.tools.BaseTool;
import com.ai.zerocode.ai.tools.ToolManager;
import com.ai.zerocode.model.entity.User;
import com.ai.zerocode.service.impl.TurnFlushService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息流处理器（VUE_PROJECT）。
 * 流式阶段仅内存聚合，本轮结束后统一落库。
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Resource
    private ToolManager toolManager;
    @Resource
    private TurnAccumulatorManager turnAccumulatorManager;
    @Resource
    private TurnFlushService turnFlushService;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     */
    public Flux<String> handle(Flux<String> originFlux, long appId, User loginUser, String turnId) {
        // 用于去重工具请求（同一个 toolCallId 仅记录一次）
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> handleJsonMessageChunk(chunk, seenToolIds, turnId))
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(
                        () -> turnFlushService.flushSuccess(turnId)
                )
                .doOnError(error -> turnFlushService.flushError(turnId, error.getMessage()));
    }

    /**
     * 解析并聚合 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, Set<String> seenToolIds, String turnId) {
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        if (typeEnum == null) {
            log.warn("无法识别的流式消息类型: {}", streamMessage.getType());
            return "";
        }
        TurnAccumulator accumulator = turnAccumulatorManager.getRequired(turnId);
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                if (StrUtil.isBlank(data)) {
                    return "";
                }
                accumulator.appendAssistant(data);
                return data;
            }
            case THINKING -> {
                ThinkingMessage thinkingMessage = JSONUtil.toBean(chunk, ThinkingMessage.class);
                String data = thinkingMessage.getData();
                if (StrUtil.isBlank(data)) {
                    return "";
                }
                accumulator.appendThinking(data);
                // Thinking 仍实时返回前端，用于流式展示
                return JSONUtil.createObj()
                        .set("type", StreamMessageTypeEnum.THINKING.getValue())
                        .set("data", data)
                        .toString();
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    seenToolIds.add(toolId);
                    // updatePlan：发送结构化 JSON，前端用于渲染计划进度面板
                    if ("updatePlan".equals(toolName)) {
                        accumulator.addToolRequest(
                                toolId, toolName, toolRequestMessage.getArguments(), "", chunk);
                        return JSONUtil.createObj()
                                .set("type", "tool_request")
                                .set("id", toolId)
                                .set("name", toolName)
                                .set("arguments", toolRequestMessage.getArguments())
                                .toString();
                    }
                    BaseTool tool = toolManager.getTool(toolName);
                    String output = tool.generateToolRequestResponse();
                    accumulator.appendAssistant(output);
                    accumulator.addToolRequest(
                            toolId,
                            toolName,
                            toolRequestMessage.getArguments(),
                            output,
                            chunk
                    );
                    return output;
                }
                return "";
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = toolExecutedMessage.getName();
                // updatePlan：发送结构化 JSON，不作为纯文本追加到助手内容
                if ("updatePlan".equals(toolName)) {
                    accumulator.addToolResult(
                            toolExecutedMessage.getId(), toolName,
                            toolExecutedMessage.getArguments(),
                            toolExecutedMessage.getResult(), "", chunk);
                    return JSONUtil.createObj()
                            .set("type", "tool_executed")
                            .set("id", toolExecutedMessage.getId())
                            .set("name", toolName)
                            .set("arguments", toolExecutedMessage.getArguments())
                            .set("result", toolExecutedMessage.getResult())
                            .toString();
                }
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                BaseTool tool = toolManager.getTool(toolName);
                String result = tool.generateToolExecutedResult(jsonObject);
                String output = String.format("\n\n%s\n\n", result);
                accumulator.appendAssistant(output);
                accumulator.addToolResult(
                        toolExecutedMessage.getId(),
                        toolName,
                        toolExecutedMessage.getArguments(),
                        toolExecutedMessage.getResult(),
                        output,
                        chunk
                );
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }
}
