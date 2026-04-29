package com.ai.zerocode.langgraph4j.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.zerocode.langgraph4j.model.ImageResource;
import com.ai.zerocode.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;
import java.util.regex.Pattern;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class PromptEnhancerNode {

    private static final Pattern MODIFY_INTENT_PATTERN = Pattern.compile(
            "(修改|改为|改成|替换|替换为|替换成|只改|仅改|局部修改|更新文案|更新文本|定点修改)"
    );

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 提示词增强");
            // 获取原始提示词和图片列表
            String originalPrompt = context.getOriginalPrompt();

            if (isModificationScenario(originalPrompt)) {
                // 修改场景直接透传原始提示词，避免重链路引入大段素材
                context.setCurrentStep("提示词增强(已跳过-修改场景)");
                context.setEnhancedPrompt(originalPrompt);
                return WorkflowContext.saveContext(context);
            }

            String imageListStr = context.getImageListStr();
            List<ImageResource> imageList = context.getImageList();
            // 构建增强后的提示词
            StringBuilder enhancedPromptBuilder = new StringBuilder();
            enhancedPromptBuilder.append(originalPrompt);
            // 如果有图片资源，则添加图片信息
            if (CollUtil.isNotEmpty(imageList) || StrUtil.isNotBlank(imageListStr)) {
                enhancedPromptBuilder.append("\n\n## 可用素材资源\n");
                enhancedPromptBuilder.append("请在生成网站使用以下图片资源，将这些图片合理地嵌入到网站的相应位置中。\n");
                if (CollUtil.isNotEmpty(imageList)) {
                    for (ImageResource image : imageList) {
                        enhancedPromptBuilder.append("- ")
                                .append(image.getCategory().getText())
                                .append("：")
                                .append(image.getDescription())
                                .append("（")
                                .append(image.getUrl())
                                .append("）\n");
                    }
                } else {
                    enhancedPromptBuilder.append(imageListStr);
                }
            }
            String enhancedPrompt = enhancedPromptBuilder.toString();
            // 更新状态
            context.setCurrentStep("提示词增强");
            context.setEnhancedPrompt(enhancedPrompt);
            log.info("提示词增强完成，增强后长度: {} 字符", enhancedPrompt.length());
            return WorkflowContext.saveContext(context);
        });
    }

    private static boolean isModificationScenario(String prompt) {
        if (StrUtil.isBlank(prompt)) {
            return false;
        }
        if (prompt.contains("选中元素信息")) {
            return true;
        }
        return MODIFY_INTENT_PATTERN.matcher(prompt).find();
    }
}
