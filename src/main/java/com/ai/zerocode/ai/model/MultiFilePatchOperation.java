package com.ai.zerocode.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 多文件模式局部修改操作。
 */
@Data
public class MultiFilePatchOperation {

    @Description("目标文件：index.html | style.css | script.js")
    private String file;

    @Description("操作类型。HTML: set_text|set_html|replace_outer_html|set_attr|remove_attr|append_html|prepend_html|remove_element；CSS/JS: replace_text|append_text|prepend_text|set_file")
    private String action;

    @Description("HTML 操作时的 CSS 选择器")
    private String selector;

    @Description("属性名（HTML 的 set_attr/remove_attr 使用）")
    private String attribute;

    @Description("文本替换时的旧值（replace_text 使用）")
    private String oldValue;

    @Description("操作值")
    private String value;
}
