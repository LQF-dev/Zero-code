package com.ai.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;

import com.ai.langgraph4j.model.ImageResource;
import com.ai.langgraph4j.model.enums.ImageCategoryEnum;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGeneration;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationMessage;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationOutput;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationParam;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.alibaba.dashscope.utils.Constants;

/**
 * Logo 图片生成工具
 */
@Slf4j
@Component
public class LogoGeneratorTool {

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.image-model:wan2.6-t2i}")
    private String imageModel;

    @Value("${dashscope.base-url:https://dashscope.aliyuncs.com/api/v1}")
    private String dashScopeBaseUrl;

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            // API Key 与 base URL 需要地域匹配，不匹配时可能触发 400 url error。
            Constants.baseHttpApiUrl = dashScopeBaseUrl;
            // 构建 Logo 设计提示词
            String logoPrompt = String.format("生成 Logo，Logo 中禁止包含任何文字！Logo 介绍：%s", description);
            ImageGenerationMessage message = ImageGenerationMessage.builder()
                    .role("user")
                    .content(Collections.singletonList(
                            Collections.singletonMap("text", logoPrompt)
                    ))
                    .build();
            ImageGenerationParam param = ImageGenerationParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(imageModel)
                    .messages(Collections.singletonList(message))
                    .size("1280*1280")
                    .n(1) // 生成 1 张足够，因为 AI 不知道哪张最好
                    .build();
            ImageGeneration imageGeneration = new ImageGeneration();
            ImageGenerationResult result = imageGeneration.call(param);
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null) {
                List<ImageGenerationOutput.Choice> choices = result.getOutput().getChoices();
                for (ImageGenerationOutput.Choice choice : choices) {
                    if (choice == null || choice.getMessage() == null || choice.getMessage().getContent() == null) {
                        continue;
                    }
                    for (Map<String, Object> contentPart : choice.getMessage().getContent()) {
                        if (contentPart == null) {
                            continue;
                        }
                        Object image = contentPart.get("image");
                        String imageUrl = image == null ? null : String.valueOf(image);
                        if (StrUtil.isNotBlank(imageUrl)) {
                            logoList.add(ImageResource.builder()
                                    .category(ImageCategoryEnum.LOGO)
                                    .description(description)
                                    .url(imageUrl)
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("生成 Logo 失败: {}", e.getMessage(), e);
        }
        return logoList;
    }
}
