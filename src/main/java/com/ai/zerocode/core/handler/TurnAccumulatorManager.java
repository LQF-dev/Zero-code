package com.ai.zerocode.core.handler;

import cn.hutool.core.util.StrUtil;
import com.ai.zerocode.exception.BusinessException;
import com.ai.zerocode.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TurnAccumulator 管理器。
 */
@Component
public class TurnAccumulatorManager {

    private final Map<String, TurnAccumulator> turnAccumulatorMap = new ConcurrentHashMap<>();

    public void startTurn(long appId, long userId, String memoryId, String turnId, String codeGenType, String userMessage) {
        TurnAccumulator accumulator = new TurnAccumulator(appId, userId, memoryId, turnId, codeGenType, userMessage);
        turnAccumulatorMap.put(turnId, accumulator);
    }

    public TurnAccumulator getRequired(String turnId) {
        TurnAccumulator accumulator = turnAccumulatorMap.get(turnId);
        if (accumulator == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "轮次聚合上下文不存在: " + turnId);
        }
        return accumulator;
    }

    public TurnAccumulator get(String turnId) {
        return turnAccumulatorMap.get(turnId);
    }

    public void remove(String turnId) {
        if (StrUtil.isBlank(turnId)) {
            return;
        }
        turnAccumulatorMap.remove(turnId);
    }
}
