package com.ai.zerocode.service;

import com.ai.zerocode.model.entity.ChatEventLog;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 聊天事件日志服务。
 */
public interface ChatEventLogService extends IService<ChatEventLog> {

    /**
     * 追加事件日志。
     */
    boolean appendEvent(ChatEventLog eventLog);

    /**
     * 按 memoryId 查询可用于回放的最近事件。
     */
    List<ChatEventLog> listRecentEventsForReplay(String memoryId, int limit);

    /**
     * 按 turnId 查询事件（按 seq 正序）。
     */
    List<ChatEventLog> listEventsByTurnId(String turnId);

    /**
     * 按 appId 逻辑删除事件日志（isDelete 标记为 1）。
     */
    boolean deleteByAppId(Long appId);
}
