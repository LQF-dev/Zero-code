package com.ai.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 聊天事件类型（用于结构化可回放日志）。
 */
@Getter
public enum ChatEventTypeEnum {

    USER_MESSAGE("用户输入", "USER_MESSAGE"),
    ASSISTANT_PARTIAL("AI 增量输出", "ASSISTANT_PARTIAL"),
    ASSISTANT_FINAL("AI 最终输出", "ASSISTANT_FINAL"),
    THINKING_PARTIAL("深度思考增量", "THINKING_PARTIAL"),
    THINKING_FINAL("深度思考最终结果", "THINKING_FINAL"),
    TOOL_REQUEST("工具调用请求", "TOOL_REQUEST"),
    TOOL_RESULT("工具执行结果", "TOOL_RESULT"),
    ERROR("错误事件", "ERROR");

    private final String text;

    private final String value;

    ChatEventTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ChatEventTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ChatEventTypeEnum anEnum : ChatEventTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
