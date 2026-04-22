package com.ai.core.engine;

import com.ai.model.entity.User;
import com.ai.model.enums.ChatGenModeEnum;
import com.ai.model.enums.CodeGenTypeEnum;
import reactor.core.publisher.Flux;

/**
 * 聊天代码生成引擎
 */
public interface ChatCodeGenerationEngine {

    /**
     * 当前引擎支持的模式
     */
    ChatGenModeEnum mode();

    /**
     * 生成代码流
     */
    Flux<String> generate(Long appId, String message, User loginUser, CodeGenTypeEnum codeGenTypeEnum);
}

