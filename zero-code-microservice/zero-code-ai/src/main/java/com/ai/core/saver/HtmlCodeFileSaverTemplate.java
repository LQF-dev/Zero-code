package com.ai.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.ai.model.HtmlCodeResult;
import com.ai.ai.model.HtmlPatchOperation;
import com.ai.exception.BusinessException;
import com.ai.exception.ErrorCode;
import com.ai.model.enums.CodeGenTypeEnum;
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
 * HTML代码文件保存器
 *
 * @author yupi
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        String targetFilePath = baseDirPath + File.separator + "index.html";
        List<HtmlPatchOperation> operations = result.getPatchOperations();
        boolean hasPatch = operations != null && !operations.isEmpty();
        boolean exists = FileUtil.exist(targetFilePath);
        if (hasPatch && exists) {
            String existingHtml = FileUtil.readString(targetFilePath, StandardCharsets.UTF_8);
            String patchedHtml = applyPatch(existingHtml, operations);
            writeToFile(baseDirPath, "index.html", patchedHtml);
            return;
        }
        String fullHtml = resolveFullHtml(result);
        writeToFile(baseDirPath, "index.html", fullHtml);
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        boolean hasFullHtml = StrUtil.isNotBlank(resolveFullHtml(result));
        boolean hasPatch = result.getPatchOperations() != null && !result.getPatchOperations().isEmpty();
        if (!hasFullHtml && !hasPatch) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容或补丁不能为空");
        }
    }

    private String resolveFullHtml(HtmlCodeResult result) {
        if (StrUtil.isNotBlank(result.getHtmlCode())) {
            return result.getHtmlCode();
        }
        return result.getFallbackHtml();
    }

    private String applyPatch(String originalHtml, List<HtmlPatchOperation> operations) {
        Document document = Jsoup.parse(originalHtml, "", Parser.htmlParser());
        document.outputSettings().prettyPrint(false);
        for (HtmlPatchOperation op : operations) {
            if (op == null || StrUtil.isBlank(op.getSelector()) || StrUtil.isBlank(op.getAction())) {
                continue;
            }
            Elements elements = document.select(op.getSelector());
            if (elements.isEmpty()) {
                continue;
            }
            for (Element element : elements) {
                applySingleOperation(element, op);
            }
        }
        return document.outerHtml();
    }

    private void applySingleOperation(Element element, HtmlPatchOperation op) {
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
            default -> element.text(value);
        }
    }
}
