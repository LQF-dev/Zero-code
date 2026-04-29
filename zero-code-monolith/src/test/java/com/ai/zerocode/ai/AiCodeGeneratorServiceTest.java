package com.ai.zerocode.ai;

import com.ai.zerocode.ai.model.HtmlCodeResult;
import com.ai.zerocode.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author: Luo qinfeng
 * @Date: 2026/2/2 16:57
 * @Description:
 */
@Slf4j
@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void quickHello() {
        String prompt = "你好 今天天气怎么样";
        String reply = aiCodeGeneratorService.generateCode(prompt);
        log.info("LLM reply: {}", reply);
        assertThat(reply).isNotBlank();
    }
    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个程序员的工作记录小工具 要很小的那种");
        log.info("LLM reply: {}", result);
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("做个程序员鱼皮的留言板");
        Assertions.assertNotNull(multiFileCode);
    }
}
