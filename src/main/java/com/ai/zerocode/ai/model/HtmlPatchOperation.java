package com.ai.zerocode.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * HTML 局部修改操作。
 */
@Data
public class HtmlPatchOperation {

    @Description("CSS 选择器，用于定位要修改的元素")
    private String selector;

    @Description("操作类型：set_text | set_html | replace_outer_html | set_attr | remove_attr | append_html | prepend_html | remove_element")
    private String action;

    @Description("属性名（仅 set_attr / remove_attr 使用）")
    private String attribute;

    @Description("操作值（remove_attr / remove_element 可为空）")
    private String value;
}
