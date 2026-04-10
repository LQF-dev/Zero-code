package com.ai.zerocode.core.engine;

import com.ai.zerocode.model.entity.User;
import com.ai.zerocode.model.enums.ChatGenModeEnum;
import com.ai.zerocode.model.enums.CodeGenTypeEnum;
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

