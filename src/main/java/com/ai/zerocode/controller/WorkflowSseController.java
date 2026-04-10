package com.ai.zerocode.controller;

import com.ai.zerocode.langgraph4j.CodeGenWorkflow;
import com.ai.zerocode.langgraph4j.state.WorkflowContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * 工作流 SSE 控制器
 * 演示 LangGraph4j 工作流的流式输出功能
 */
@RestController
@RequestMapping("/workflow")
@Slf4j
public class WorkflowSseController {

    @Resource
    private CodeGenWorkflow codeGenWorkflow;

    /**
     * 同步执行工作流
     */
    @PostMapping("/execute")
    public WorkflowContext executeWorkflow(@RequestParam String prompt) {
        log.info("收到同步工作流执行请求: {}", prompt);
        return codeGenWorkflow.executeWorkflow(prompt);
    }

    /**
     * Flux 流式执行工作流
     */
    @GetMapping(value = "/execute-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeWorkflowWithFlux(@RequestParam String prompt) {
        log.info("收到 Flux 工作流执行请求: {}", prompt);
        return codeGenWorkflow.executeWorkflowWithFlux(prompt);
    }

    /**
     * SSE 流式执行工作流
     */
    @GetMapping(value = "/execute-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeWorkflowWithSse(@RequestParam String prompt) {
        log.info("收到 SSE 工作流执行请求: {}", prompt);
        return codeGenWorkflow.executeWorkflowWithSse(prompt);
    }
}


