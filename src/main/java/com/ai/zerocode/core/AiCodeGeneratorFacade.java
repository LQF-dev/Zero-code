package com.ai.zerocode.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ai.zerocode.ai.AiCodeGeneratorService;
import com.ai.zerocode.ai.model.HtmlCodeResult;
import com.ai.zerocode.ai.model.MultiFileCodeResult;
import com.ai.zerocode.ai.model.message.AiResponseMessage;
import com.ai.zerocode.ai.model.message.ThinkingMessage;
import com.ai.zerocode.ai.model.message.ToolExecutedMessage;
import com.ai.zerocode.ai.model.message.ToolRequestMessage;
import com.ai.zerocode.config.AiCodeGeneratorServiceFactory;
import com.ai.zerocode.constant.AppConstant;
import com.ai.zerocode.core.builder.VueProjectBuilder;
import com.ai.zerocode.core.parser.CodeParser;
import com.ai.zerocode.core.parser.CodeParserExecutor;
import com.ai.zerocode.core.saver.CodeFileSaverExecutor;
import com.ai.zerocode.exception.BusinessException;
import com.ai.zerocode.exception.ErrorCode;
import com.ai.zerocode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

/*    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;*/

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Resource
    private VueProjectBuilder vueProjectBuilder;



    /**
     * 统一入口：根据类型生成并保存代码（使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {

        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式，使用 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 ID
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream,appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };

    }

    /**
     * 通用流式代码处理方法（使用 appId）
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            // 实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                // 使用执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 使用执行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {
        return Flux.create(sink -> {
            AtomicInteger emittedChunkCount = new AtomicInteger(0);
            TokenStream configuredStream = tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                        emittedChunkCount.incrementAndGet();
                    })
                    .beforeToolExecution(beforeToolExecution -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(beforeToolExecution);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                        emittedChunkCount.incrementAndGet();
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                        emittedChunkCount.incrementAndGet();
                    })
//                    .onCompleteResponse((ChatResponse response) -> {
//                        sink.complete();
//                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        if (emittedChunkCount.get() == 0
                                && response != null
                                && response.aiMessage() != null
                                && StrUtil.isNotBlank(response.aiMessage().text())) {
                            // 兜底：某些模型实现可能只触发 complete，不触发 partial 回调
                            sink.next(JSONUtil.toJsonStr(new AiResponseMessage(response.aiMessage().text())));
                            emittedChunkCount.incrementAndGet();
                        }
                        log.info("TokenStream 完成，appId={}, emittedChunkCount={}", appId, emittedChunkCount.get());
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath);
                        sink.complete();
                    })

                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    });
            // 仅当底层模型/实现支持时，才会接收到思考分片
            try {
                configuredStream = configuredStream.onPartialThinking((PartialThinking partialThinking) -> {
                    ThinkingMessage thinkingMessage = new ThinkingMessage(partialThinking);
                    sink.next(JSONUtil.toJsonStr(thinkingMessage));
                    emittedChunkCount.incrementAndGet();
                });
            } catch (UnsupportedOperationException e) {
                log.debug("当前 TokenStream 实现不支持 onPartialThinking，已降级为普通流式输出");
            }
            configuredStream.start();
        });
    }





}
