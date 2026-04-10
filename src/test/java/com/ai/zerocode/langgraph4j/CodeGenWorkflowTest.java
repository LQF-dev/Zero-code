package com.ai.zerocode.langgraph4j;

import com.ai.zerocode.langgraph4j.state.WorkflowContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author: Luo qinfeng
 * @Date: 2026/3/12 17:12
 * @Description:
 */
@SpringBootTest
class CodeGenWorkflowTest {

    @Test
    void testTechBlogWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("创建一个技术博客网站，代码量小于50行，使用HTML模式生成，需要展示编程教程和系统架构的图片(一定要输出架构图)");
        Assertions.assertNotNull(result);
        System.out.println("生成类型: " + result.getGenerationType());
        System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
        System.out.println("构建结果目录: " + result.getBuildResultDir());
    }

    @Test
    void testCorporateWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("创建企业官网，展示公司形象和业务介绍");
        Assertions.assertNotNull(result);
        System.out.println("生成类型: " + result.getGenerationType());
        System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
        System.out.println("构建结果目录: " + result.getBuildResultDir());
    }

    @Test
    void testVueProjectWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("创建一个Vue前端项目，包含用户管理和数据展示功能");
        Assertions.assertNotNull(result);
        System.out.println("生成类型: " + result.getGenerationType());
        System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
        System.out.println("构建结果目录: " + result.getBuildResultDir());
    }

    @Test
    void testSimpleHtmlWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("创建一个简单的个人主页");
        Assertions.assertNotNull(result);
        System.out.println("生成类型: " + result.getGenerationType());
        System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
        System.out.println("构建结果目录: " + result.getBuildResultDir());
    }
}
