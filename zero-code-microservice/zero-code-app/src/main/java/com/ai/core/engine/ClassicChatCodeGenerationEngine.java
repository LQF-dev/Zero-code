package com.ai.core.engine;

import com.ai.core.AiCodeGeneratorFacade;
import com.ai.model.entity.User;
import com.ai.model.enums.ChatGenModeEnum;
import com.ai.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 普通模式生成引擎
 */
@Component
public class ClassicChatCodeGenerationEngine implements ChatCodeGenerationEngine {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Override
    public ChatGenModeEnum mode() {
        return ChatGenModeEnum.CLASSIC;
    }

    @Override
    public Flux<String> generate(Long appId, String message, User loginUser, CodeGenTypeEnum codeGenTypeEnum) {
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
    }
}

