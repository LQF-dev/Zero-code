package com.ai.model.enums;

import lombok.Getter;

/**
 * 聊天生成模式枚举
 */
@Getter
public enum ChatGenModeEnum {

    CLASSIC("classic", "标准模式"),
    WORKFLOW("workflow", "工作流模式");

    private final String value;
    private final String text;

    ChatGenModeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static ChatGenModeEnum getByValue(String value) {
        if (value == null || value.isBlank()) {
            return CLASSIC;
        }
        for (ChatGenModeEnum mode : values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return CLASSIC;
    }
}
