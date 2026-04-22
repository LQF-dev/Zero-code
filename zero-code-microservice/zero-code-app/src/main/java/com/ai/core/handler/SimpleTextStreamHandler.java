package com.ai.core.handler;

import com.ai.model.entity.User;
import com.ai.model.enums.CodeGenTypeEnum;
import com.ai.service.impl.TurnFlushService;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 简单文本流处理器
 * 处理 HTML 和 MULTI_FILE 类型的流式响应
 */
@Component
public class SimpleTextStreamHandler {

    @Resource
    private TurnAccumulatorManager turnAccumulatorManager;
    @Resource
    private TurnFlushService turnFlushService;

    /**
     * 处理传统流（HTML, MULTI_FILE）
     * 直接收集完整的文本响应
     *
     * @param originFlux         原始流
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               long appId, User loginUser,
                               CodeGenTypeEnum codeGenType,
                               String turnId) {
        return originFlux
                .doOnNext(chunk -> {
                    // 流式阶段仅做内存聚合
                    turnAccumulatorManager.getRequired(turnId).appendAssistant(chunk);
                })
                .doOnComplete(() -> {
                    // 本轮结束统一落库
                    turnFlushService.flushSuccess(turnId);
                })
                .doOnError(error -> {
                    // 失败也统一落库
                    turnFlushService.flushError(turnId, error.getMessage());
                });
    }
}
