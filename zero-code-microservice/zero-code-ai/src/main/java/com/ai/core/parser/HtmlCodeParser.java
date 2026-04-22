package com.ai.core.parser;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ai.ai.model.HtmlCodeResult;
import com.ai.ai.model.HtmlPatchOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML 单文件代码解析器
 *
 * @author yupi
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_CODE_PATTERN = Pattern.compile("```json\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public HtmlCodeResult parseCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        // 优先尝试解析 JSON patch 协议
        if (tryParsePatchProtocol(codeContent, result)) {
            return result;
        }
        // 提取 HTML 代码
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 如果没有找到代码块，将整个内容作为HTML
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }

    private boolean tryParsePatchProtocol(String content, HtmlCodeResult result) {
        String jsonPayload = extractJsonCode(content);
        if (StrUtil.isBlank(jsonPayload)) {
            String trimmed = StrUtil.trim(content);
            if (StrUtil.startWith(trimmed, "{") && StrUtil.endWith(trimmed, "}")) {
                jsonPayload = trimmed;
            }
        }
        if (StrUtil.isBlank(jsonPayload) || !JSONUtil.isTypeJSON(jsonPayload)) {
            return false;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(jsonPayload);
            List<HtmlPatchOperation> operations = parseOperations(obj);
            String fallbackHtml = firstNonBlank(
                    obj.getStr("fallbackHtml"),
                    obj.getStr("fallback_html"),
                    obj.getStr("htmlCode"),
                    obj.getStr("html_code")
            );
            if (CollUtil.isEmpty(operations) && StrUtil.isBlank(fallbackHtml)) {
                return false;
            }
            result.setPatchOperations(operations);
            result.setFallbackHtml(StrUtil.emptyToNull(fallbackHtml));
            if (StrUtil.isNotBlank(fallbackHtml)) {
                result.setHtmlCode(fallbackHtml);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<HtmlPatchOperation> parseOperations(JSONObject obj) {
        JSONArray operationArray = obj.getJSONArray("operations");
        if (operationArray == null) {
            operationArray = obj.getJSONArray("patchOperations");
        }
        if (operationArray == null) {
            operationArray = obj.getJSONArray("patch_operations");
        }
        if (operationArray == null || operationArray.isEmpty()) {
            return new ArrayList<>();
        }
        List<HtmlPatchOperation> operations = new ArrayList<>();
        for (Object item : operationArray) {
            if (item == null) {
                continue;
            }
            JSONObject opObj = item instanceof JSONObject ? (JSONObject) item : JSONUtil.parseObj(item);
            HtmlPatchOperation op = new HtmlPatchOperation();
            op.setSelector(firstNonBlank(opObj.getStr("selector"), opObj.getStr("path")));
            op.setAction(firstNonBlank(opObj.getStr("action"), opObj.getStr("op"), "set_text"));
            op.setAttribute(firstNonBlank(opObj.getStr("attribute"), opObj.getStr("attr")));
            op.setValue(opObj.getStr("value"));
            if (StrUtil.isNotBlank(op.getSelector()) && StrUtil.isNotBlank(op.getAction())) {
                operations.add(op);
            }
        }
        return operations;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 提取HTML代码内容
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 提取 JSON 代码块内容
     */
    private String extractJsonCode(String content) {
        Matcher matcher = JSON_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
