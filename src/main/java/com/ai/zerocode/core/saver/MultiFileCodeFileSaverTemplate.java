package com.ai.zerocode.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.zerocode.ai.model.MultiFileCodeResult;
import com.ai.zerocode.ai.model.MultiFilePatchOperation;
import com.ai.zerocode.exception.BusinessException;
import com.ai.zerocode.exception.ErrorCode;
import com.ai.zerocode.model.enums.CodeGenTypeEnum;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * 多文件代码保存器
 *
 * @author yupi
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    public CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        List<MultiFilePatchOperation> operations = result.getPatchOperations();
        boolean hasPatch = operations != null && !operations.isEmpty();
        if (hasPatch) {
            applyMultiFilePatch(baseDirPath, result, operations);
            return;
        }
        // 全量兜底保存
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        boolean hasPatch = result.getPatchOperations() != null && !result.getPatchOperations().isEmpty();
        if (!hasPatch && StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空（全量模式）");
        }
    }

    private void applyMultiFilePatch(String baseDirPath, MultiFileCodeResult result, List<MultiFilePatchOperation> operations) {
        String htmlPath = baseDirPath + File.separator + "index.html";
        String cssPath = baseDirPath + File.separator + "style.css";
        String jsPath = baseDirPath + File.separator + "script.js";

        String html = loadFileOrFallback(htmlPath, result.getHtmlCode());
        String css = loadFileOrFallback(cssPath, result.getCssCode());
        String js = loadFileOrFallback(jsPath, result.getJsCode());

        for (MultiFilePatchOperation op : operations) {
            if (op == null || StrUtil.isBlank(op.getAction())) {
                continue;
            }
            String targetFile = normalizeFile(op.getFile());
            if ("index.html".equals(targetFile)) {
                html = applyHtmlOperation(html, op);
            } else if ("style.css".equals(targetFile)) {
                css = applyTextOperation(css, op);
            } else if ("script.js".equals(targetFile)) {
                js = applyTextOperation(js, op);
            }
        }

        if (StrUtil.isNotBlank(html)) {
            writeToFile(baseDirPath, "index.html", html);
        }
        if (StrUtil.isNotBlank(css)) {
            writeToFile(baseDirPath, "style.css", css);
        }
        if (StrUtil.isNotBlank(js)) {
            writeToFile(baseDirPath, "script.js", js);
        }
    }

    private String loadFileOrFallback(String filePath, String fallback) {
        if (FileUtil.exist(filePath)) {
            return FileUtil.readString(filePath, StandardCharsets.UTF_8);
        }
        return StrUtil.nullToEmpty(fallback);
    }

    private String normalizeFile(String file) {
        String value = StrUtil.blankToDefault(file, "index.html").trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "html", "index.html" -> "index.html";
            case "css", "style.css" -> "style.css";
            case "js", "javascript", "script.js" -> "script.js";
            default -> "index.html";
        };
    }

    private String applyHtmlOperation(String html, MultiFilePatchOperation op) {
        if (StrUtil.isBlank(html) || StrUtil.isBlank(op.getSelector())) {
            return html;
        }
        Document document = Jsoup.parse(html, "", Parser.htmlParser());
        document.outputSettings().prettyPrint(false);
        Elements elements = document.select(op.getSelector());
        if (elements.isEmpty()) {
            return html;
        }
        for (Element element : elements) {
            String action = op.getAction().toLowerCase(Locale.ROOT);
            String value = StrUtil.nullToEmpty(op.getValue());
            switch (action) {
                case "set_text" -> element.text(value);
                case "set_html" -> element.html(value);
                case "replace_outer_html" -> element.before(value).remove();
                case "set_attr" -> {
                    if (StrUtil.isNotBlank(op.getAttribute())) {
                        element.attr(op.getAttribute(), value);
                    }
                }
                case "remove_attr" -> {
                    if (StrUtil.isNotBlank(op.getAttribute())) {
                        element.removeAttr(op.getAttribute());
                    }
                }
                case "append_html" -> element.append(value);
                case "prepend_html" -> element.prepend(value);
                case "remove_element" -> element.remove();
                default -> {
                }
            }
        }
        return document.outerHtml();
    }

    private String applyTextOperation(String source, MultiFilePatchOperation op) {
        String text = StrUtil.nullToEmpty(source);
        String action = op.getAction().toLowerCase(Locale.ROOT);
        String value = StrUtil.nullToEmpty(op.getValue());
        return switch (action) {
            case "set_file" -> value;
            case "append_text" -> text + value;
            case "prepend_text" -> value + text;
            case "replace_text" -> {
                String oldValue = StrUtil.nullToEmpty(op.getOldValue());
                if (StrUtil.isBlank(oldValue)) {
                    yield text;
                }
                yield text.replace(oldValue, value);
            }
            default -> text;
        };
    }
}
