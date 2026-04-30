package com.ai.zerocode.ai.tools;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 计划追踪器：管理每个应用的生成计划状态和工具调用计数。
 * <p>
 * 使用 Redis 持久化，支持任务依赖关系（DAG）。
 * <ul>
 *   <li>维护结构化的计划列表（pending / in_progress / completed）</li>
 *   <li>支持 deps 依赖声明，允许多个无依赖冲突的任务并行 in_progress</li>
 *   <li>跟踪自上次 updatePlan 以来的工具调用次数（Nag 机制）</li>
 * </ul>
 */
@Slf4j
@Component
public class PlanTracker {

    private static final int NAG_THRESHOLD = 3;
    private static final int MAX_ITEMS = 20;
    private static final String KEY_PREFIX = "zero-code:plan:";
    private static final Duration TTL = Duration.ofDays(7);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 更新计划。由 UpdatePlanTool 调用。
     */
    public String updatePlan(Long appId, List<PlanItem> items) {
        if (items.size() > MAX_ITEMS) {
            return "错误：计划条目不能超过 " + MAX_ITEMS + " 个";
        }
        // 构建 id 集合用于校验
        Set<String> idSet = items.stream().map(PlanItem::id).collect(Collectors.toSet());

        // 校验依赖关系：deps 引用的 id 必须存在
        for (PlanItem item : items) {
            if (item.deps() != null) {
                for (String depId : item.deps()) {
                    if (!idSet.contains(depId)) {
                        return "错误：任务 " + item.id() + " 依赖的 " + depId + " 不存在";
                    }
                }
            }
        }

        // 校验：in_progress 的任务，其依赖必须全部 completed
        Map<String, PlanItem> itemMap = items.stream()
                .collect(Collectors.toMap(PlanItem::id, i -> i));
        for (PlanItem item : items) {
            if ("in_progress".equals(item.status()) && item.deps() != null) {
                for (String depId : item.deps()) {
                    PlanItem dep = itemMap.get(depId);
                    if (dep != null && !"completed".equals(dep.status())) {
                        return "错误：任务 " + item.id() + " 依赖 " + depId
                                + "，但 " + depId + " 尚未完成";
                    }
                }
            }
        }

        PlanState state = new PlanState();
        state.setItems(new ArrayList<>(items));
        state.setNagCounter(0);
        saveState(appId, state);

        log.info("计划更新，appId={}, 共 {} 项", appId, items.size());
        return renderPlan(state);
    }

    /**
     * 记录一次非 plan 工具调用，返回可选的 Nag 后缀。
     */
    public String onToolExecuted(Long appId) {
        PlanState state = loadState(appId);
        if (state == null || state.getItems().isEmpty()) {
            return null;
        }
        int rounds = state.getNagCounter() + 1;
        state.setNagCounter(rounds);
        saveState(appId, state);

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
        stringRedisTemplate.delete(KEY_PREFIX + appId);
    }

    // ========== Redis 读写 ==========

    private PlanState loadState(Long appId) {
        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + appId);
        if (json == null) {
            return null;
        }
        return JSONUtil.toBean(json, PlanState.class);
    }

    private void saveState(Long appId, PlanState state) {
        stringRedisTemplate.opsForValue().set(
                KEY_PREFIX + appId, JSONUtil.toJsonStr(state), TTL);
    }

    // ========== 渲染 ==========

    private String renderPlan(PlanState state) {
        if (state.getItems().isEmpty()) {
            return "暂无计划。";
        }
        StringBuilder sb = new StringBuilder();
        int done = 0;
        for (PlanItem item : state.getItems()) {
            String marker = switch (item.status()) {
                case "completed" -> "[x]";
                case "in_progress" -> "[>]";
                default -> "[ ]";
            };
            if ("completed".equals(item.status())) {
                done++;
            }
            sb.append(marker).append(" #").append(item.id())
                    .append(" ").append(item.text());
            if (item.deps() != null && !item.deps().isEmpty()) {
                sb.append(" (依赖: ").append(String.join(", ", item.deps())).append(")");
            }
            sb.append("\n");
        }
        sb.append("\n(").append(done).append("/").append(state.getItems().size()).append(" 已完成)");
        return sb.toString();
    }

    // ========== 数据模型 ==========

    /**
     * 计划条目。使用普通类以兼容 JSON 反序列化。
     */
    @Data
    public static class PlanItem {
        private String id;
        private String text;
        private String status;
        private List<String> deps;

        public PlanItem() {}

        public PlanItem(String id, String text, String status, List<String> deps) {
            this.id = id;
            this.text = text;
            this.status = status != null ? status : "pending";
            this.deps = deps != null ? deps : List.of();
        }

        public String status() { return status != null ? status : "pending"; }
        public String id() { return id != null ? id : ""; }
        public String text() { return text != null ? text : ""; }
        public List<String> deps() { return deps != null ? deps : List.of(); }
    }

    /**
     * 持久化到 Redis 的计划状态。
     */
    @Data
    public static class PlanState {
        private List<PlanItem> items = new ArrayList<>();
        private int nagCounter = 0;
    }
}
