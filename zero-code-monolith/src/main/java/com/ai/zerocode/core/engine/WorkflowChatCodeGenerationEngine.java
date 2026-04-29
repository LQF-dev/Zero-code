package com.ai.zerocode.core.engine;

import com.ai.zerocode.langgraph4j.CodeGenWorkflow;
import com.ai.zerocode.model.entity.User;
import com.ai.zerocode.model.enums.ChatGenModeEnum;
import com.ai.zerocode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 工作流模式生成引擎
 */
@Component
public class WorkflowChatCodeGenerationEngine implements ChatCodeGenerationEngine {

    @Resource
    private CodeGenWorkflow codeGenWorkflow;

    @Override
    public ChatGenModeEnum mode() {
        return ChatGenModeEnum.WORKFLOW;
    }

    @Override
    public Flux<String> generate(Long appId, String message, User loginUser, CodeGenTypeEnum codeGenTypeEnum) {
        return codeGenWorkflow.executeWorkflowForChat(message, appId, codeGenTypeEnum);
    }
}

