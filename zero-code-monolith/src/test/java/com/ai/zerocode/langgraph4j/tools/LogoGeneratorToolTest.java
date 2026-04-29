package com.ai.zerocode.langgraph4j.tools;

import com.ai.zerocode.langgraph4j.model.ImageResource;
import com.ai.zerocode.langgraph4j.model.enums.ImageCategoryEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author: Luo qinfeng
 * @Date: 2026/3/12 15:53
 * @Description:
 */
@SpringBootTest
class LogoGeneratorToolTest {

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    @Test
    void testGenerateLogos() {
        // 测试生成Logo
        List<ImageResource> logos = logoGeneratorTool.generateLogos("技术公司现代简约风格Logo");
        assertNotNull(logos);
        assertFalse(logos.isEmpty(), "Logo 列表为空，请检查 dashscope 模型、base-url 和 API Key 配置");
        ImageResource firstLogo = logos.getFirst();
        assertEquals(ImageCategoryEnum.LOGO, firstLogo.getCategory());
        assertNotNull(firstLogo.getDescription());
        assertNotNull(firstLogo.getUrl());
        logos.forEach(logo ->
                System.out.println("Logo: " + logo.getDescription() + " - " + logo.getUrl())
        );
    }
}
