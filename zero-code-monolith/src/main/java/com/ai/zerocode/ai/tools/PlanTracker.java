package com.ai.zerocode.ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 计划追踪器：管理每个应用的生成计划状态和工具调用计数。
 * <p>
 * 核心机制（来自 Claude Code S03 TodoWrite 模式）：
 * <ul>
 *   <li>维护结构化的计划列表（pending / in_progress / completed）</li>
 *   <li>跟踪自上次 updatePlan 以来的工具调用次数</li>
 *   <li>当计数超过阈值时，生成 Nag 提醒注入到工具返回值中</li>
 * </ul>
 */
@Slf4j
@Component
public class PlanTracker {

    /**
     * 连续多少次工具调用没更新计划，就触发 Nag 提醒。
     */
    private static final int NAG_THRESHOLD = 3;

    /**
     * 每个 appId 的计划状态。
     */
    private final ConcurrentHashMap<Long, PlanState> states = new ConcurrentHashMap<>();

    /**
     * 更新计划。由 UpdatePlanTool 调用。
     *
     * @param appId 应用 ID
     * @param items 计划条目列表
     * @return 渲染后的计划文本
     */
    public String updatePlan(Long appId, List<PlanItem> items) {
        // 校验：最多只能有一个 in_progress
        long inProgressCount = items.stream()
                .filter(item -> "in_progress".equals(item.status()))
                .count();
        if (inProgressCount > 1) {
            return "错误：同一时间只能有一个进行中的任务";
        }

        PlanState state = states.computeIfAbsent(appId, k -> new PlanState());
        state.items = new ArrayList<>(items);
        state.roundsSincePlan.set(0);

        log.info("计划更新，appId={}, 共 {} 项", appId, items.size());
        return renderPlan(state);
    }

    /**
     * 记录一次非 plan 工具调用，返回可选的 Nag 后缀。
     *
     * @param appId 应用 ID
     * @return Nag 提醒文本，无需提醒时返回 null
     */
    public String onToolExecuted(Long appId) {
        PlanState state = states.get(appId);
        if (state == null || state.items.isEmpty()) {
            // 还没有创建过计划，不 nag
            return null;
        }
        int rounds = state.roundsSincePlan.incrementAndGet();
        if (rounds >= NAG_THRESHOLD) {
            return "\n\n<reminder>你已连续执行 " + rounds
                    + " 次工具调用未更新计划。请调用 updatePlan 工具更新任务进度。"
                    + "\n当前计划：\n" + renderPlan(state) + "</reminder>";
        }
        return null;
    }

    /**
     * 清除指定 appId 的计划状态。
     */
    public void clear(Long appId) {
        states.remove(appId);
    }

    /**
     * 渲染计划为可读文本。
     */
    private String renderPlan(PlanState state) {
        if (state.items.isEmpty()) {
            return "暂无计划。";
        }
        StringBuilder sb = new StringBuilder();
        int done = 0;
        for (PlanItem item : state.items) {
            String marker = switch (item.status()) {
                case "completed" -> "[x]";
                case "in_progress" -> "[>]";
                default -> "[ ]";
            };
            if ("completed".equals(item.status())) {
                done++;
            }
            sb.append(marker).append(" #").append(item.id())
                    .append(" ").append(item.text()).append("\n");
        }
        sb.append("\n(").append(done).append("/").append(state.items.size()).append(" 已完成)");
        return sb.toString();
    }

    /**
     * 计划条目。
     */
    public record PlanItem(String id, String text, String status) {
    }

    /**
     * 每个 appId 的内部状态。
     */
    private static class PlanState {
        volatile List<PlanItem> items = new ArrayList<>();
        final AtomicInteger roundsSincePlan = new AtomicInteger(0);
    }
}
