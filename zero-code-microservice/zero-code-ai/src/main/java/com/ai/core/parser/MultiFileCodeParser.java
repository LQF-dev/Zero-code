package com.ai.core.parser;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ai.ai.model.MultiFileCodeResult;
import com.ai.ai.model.MultiFilePatchOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多文件代码解析器（HTML + CSS + JS）
 *
 * @author yupi
 */
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {

    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_CODE_PATTERN = Pattern.compile("```json\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    @Override
    public MultiFileCodeResult parseCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        if (tryParsePatchProtocol(codeContent, result)) {
            return result;
        }
        // 提取各类代码
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);
        // 设置HTML代码
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        // 设置CSS代码
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        // 设置JS代码
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        return result;
    }

    private boolean tryParsePatchProtocol(String content, MultiFileCodeResult result) {
        String jsonPayload = extractCodeByPattern(content, JSON_CODE_PATTERN);
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
            List<MultiFilePatchOperation> operations = parseOperations(obj);
            String htmlCode = firstNonBlank(obj.getStr("htmlCode"), obj.getStr("html_code"), obj.getStr("fallbackHtml"));
            String cssCode = firstNonBlank(obj.getStr("cssCode"), obj.getStr("css_code"), obj.getStr("fallbackCss"));
            String jsCode = firstNonBlank(obj.getStr("jsCode"), obj.getStr("js_code"), obj.getStr("fallbackJs"));
            if (CollUtil.isEmpty(operations) && StrUtil.isAllBlank(htmlCode, cssCode, jsCode)) {
                return false;
            }
            result.setPatchOperations(operations);
            result.setHtmlCode(StrUtil.emptyToNull(htmlCode));
            result.setCssCode(StrUtil.emptyToNull(cssCode));
            result.setJsCode(StrUtil.emptyToNull(jsCode));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<MultiFilePatchOperation> parseOperations(JSONObject obj) {
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
        List<MultiFilePatchOperation> operations = new ArrayList<>();
        for (Object item : operationArray) {
            if (item == null) {
                continue;
            }
            JSONObject opObj = item instanceof JSONObject ? (JSONObject) item : JSONUtil.parseObj(item);
            MultiFilePatchOperation op = new MultiFilePatchOperation();
            op.setFile(firstNonBlank(opObj.getStr("file"), "index.html"));
            op.setAction(firstNonBlank(opObj.getStr("action"), opObj.getStr("op")));
            op.setSelector(opObj.getStr("selector"));
            op.setAttribute(firstNonBlank(opObj.getStr("attribute"), opObj.getStr("attr")));
            op.setOldValue(firstNonBlank(opObj.getStr("oldValue"), opObj.getStr("old_value")));
            op.setValue(opObj.getStr("value"));
            if (StrUtil.isNotBlank(op.getAction())) {
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
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
