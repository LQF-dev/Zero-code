package com.ai.zerocode.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Description("生成 HTML 代码文件的结果")
@Data
public class HtmlCodeResult {

    @Description("HTML代码")
    private String htmlCode;

    @Description("局部修改操作列表（修改模式优先使用）")
    private List<HtmlPatchOperation> patchOperations;

    @Description("补丁无法应用时的完整 HTML 兜底")
    private String fallbackHtml;

    @Description("生成代码的描述")
    private String description;
}
