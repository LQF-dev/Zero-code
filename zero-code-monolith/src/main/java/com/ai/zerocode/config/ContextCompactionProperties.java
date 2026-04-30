package com.ai.zerocode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 上下文压缩配置属性。
 * 对应 application-ai.yml 中的 context-compaction 配置段。
 */
@Data
@Component
@ConfigurationProperties(prefix = "context-compaction")
public class ContextCompactionProperties {

    /**
     * 触发自动压缩的 token 估算阈值。
     * 建议设为所用模型最大上下文长度的 80%。
     * 例如：DeepSeek V4-Pro 最大上下文 1M tokens → 800000。
     */
    private int tokenThreshold = 800000;

    /**
     * 送入摘要模型的最大字符数，避免摘要请求本身超长。
     */
    private int maxSummaryInputChars = 300000;
}
