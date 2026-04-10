package com.ai.zerocode.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

@Description("生成多个代码文件的结果")
@Data
public class MultiFileCodeResult {

    @Description("HTML代码")
    private String htmlCode;

    @Description("CSS代码")
    private String cssCode;

    @Description("JS代码")
    private String jsCode;

    @Description("局部修改操作列表（修改场景优先）")
    private List<MultiFilePatchOperation> patchOperations;

    @Description("生成代码的描述")
    private String description;
}
