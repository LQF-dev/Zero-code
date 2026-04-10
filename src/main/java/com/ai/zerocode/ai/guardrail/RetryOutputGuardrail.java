package com.ai.zerocode.ai.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

public class RetryOutputGuardrail implements OutputGuardrail {

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        // 说明：
        // langchain4j 1.11.0 在 OutputGuardrailExecutor.rewriteResult 中对 originalText 直接 equals，
        // 当 originalText 为 null 且返回 success() 时会 NPE。
        // 因此“放行”必须返回 successWith(rewrittenMessage)。
        if (responseFromLLM == null) {
            return OutputGuardrailResult.successWith(AiMessage.from(""));
        }
        // 工具调用阶段常见为“仅 tool_calls、无 text”，不应触发重试。
        if (responseFromLLM.hasToolExecutionRequests()) {
            String safeText = responseFromLLM.text() == null ? "" : responseFromLLM.text();
            return OutputGuardrailResult.successWith(responseFromLLM.withText(safeText));
        }
        String response = responseFromLLM.text();
        if (response == null) {
            return OutputGuardrailResult.successWith(responseFromLLM.withText(""));
        }
        // 检查响应是否为空或过短
        if (response.trim().isEmpty()) {
            return OutputGuardrailResult.successWith(responseFromLLM.withText(""));
        }
        if (response.trim().length() < 2) {
            return reprompt("响应内容过短", "请提供更详细的内容");
        }
        // 检查是否包含敏感信息或不当内容
        if (containsSensitiveContent(response)) {
            return reprompt("包含敏感信息", "请重新生成内容，避免包含敏感信息");
        }
        return OutputGuardrailResult.successWith(responseFromLLM.withText(response));
    }
    
    /**
     * 检查是否包含敏感内容
     */
    private boolean containsSensitiveContent(String response) {
        String lowerResponse = response.toLowerCase();
        String[] sensitiveWords = {
            "密码", "password", "secret", "token", 
            "api key", "私钥", "证书", "credential"
        };
        for (String word : sensitiveWords) {
            if (lowerResponse.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
