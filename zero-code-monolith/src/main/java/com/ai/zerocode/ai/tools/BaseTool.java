package com.ai.zerocode.ai.tools;

import cn.hutool.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 工具基类
 * 定义所有工具的通用接口
 */
public abstract class BaseTool {

    @Autowired
    protected PlanTracker planTracker;

    /**
     * 获取工具的英文名称（对应方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的中文显示名称
     *
     * @return 工具中文名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的返回值（显示给用户）
     *
     * @return 工具请求显示内容
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行结果格式（保存到数据库）
     *
     * @param arguments 工具执行参数
     * @return 格式化的工具执行结果
     */
    public abstract String generateToolExecutedResult(JSONObject arguments);

    /**
     * 将工具执行结果与可选的 Nag 提醒拼接。
     * 当模型连续多次调用工具但未更新计划时，在工具返回值末尾追加提醒。
     *
     * @param result 工具原始返回值
     * @param appId  应用 ID
     * @return 拼接 Nag 后的返回值
     */
    protected String withNag(String result, Long appId) {
        if (planTracker == null) {
            return result;
        }
        String nag = planTracker.onToolExecuted(appId);
        return nag != null ? result + nag : result;
    }
} 