package com.ai.zerocode.ai;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 快速烟囱测试：用极短提示验证 LLM 调用链是否通畅。
 * 通过 TestPropertySource 限制 max-tokens 和超时，避免长时间阻塞。
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "langchain4j.open-ai.chat-model.timeout=30s",
        "langchain4j.open-ai.chat-model.connect-timeout=10s",
        "langchain4j.open-ai.chat-model.max-tokens=64"
})
class AiCodeGeneratorQuickTest {

    @Autowired
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void quickHello() {
        String prompt = "你好，我叫 Eric，你叫什么名字？请简短回答。";
        String reply = aiCodeGeneratorService.generateCode(prompt);
        log.info("LLM reply: {}", reply);
        assertThat(reply).isNotBlank();
    }
}
