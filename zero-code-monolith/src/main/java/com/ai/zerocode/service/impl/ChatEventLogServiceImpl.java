package com.ai.zerocode.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ai.zerocode.exception.ErrorCode;
import com.ai.zerocode.exception.ThrowUtils;
import com.ai.zerocode.mapper.ChatEventLogMapper;
import com.ai.zerocode.model.entity.ChatEventLog;
import com.ai.zerocode.service.ChatEventLogService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天事件日志服务实现。
 */
@Service
public class ChatEventLogServiceImpl extends ServiceImpl<ChatEventLogMapper, ChatEventLog>
        implements ChatEventLogService {

    @Override
    public boolean appendEvent(ChatEventLog eventLog) {
        ThrowUtils.throwIf(eventLog == null, ErrorCode.PARAMS_ERROR, "事件日志不能为空");
        ThrowUtils.throwIf(eventLog.getAppId() == null || eventLog.getAppId() <= 0,
                ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(eventLog.getMemoryId()), ErrorCode.PARAMS_ERROR, "memoryId 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(eventLog.getTurnId()), ErrorCode.PARAMS_ERROR, "turnId 不能为空");
        ThrowUtils.throwIf(eventLog.getSeq() == null || eventLog.getSeq() < 0,
                ErrorCode.PARAMS_ERROR, "seq 非法");
        ThrowUtils.throwIf(StrUtil.isBlank(eventLog.getCodeGenType()), ErrorCode.PARAMS_ERROR, "codeGenType 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(eventLog.getRole()), ErrorCode.PARAMS_ERROR, "role 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(eventLog.getEventType()), ErrorCode.PARAMS_ERROR, "eventType 不能为空");
        ThrowUtils.throwIf(eventLog.getUserId() == null || eventLog.getUserId() <= 0,
                ErrorCode.PARAMS_ERROR, "userId 非法");
        return this.save(eventLog);
    }

    @Override
    public List<ChatEventLog> listRecentEventsForReplay(String memoryId, int limit) {
        ThrowUtils.throwIf(StrUtil.isBlank(memoryId), ErrorCode.PARAMS_ERROR, "memoryId 不能为空");
        int safeLimit = Math.max(1, Math.min(limit, 5000));
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatEventLog::getMemoryId, memoryId)
                .orderBy(ChatEventLog::getCreateTime, false)
                .limit(0, safeLimit);
        List<ChatEventLog> list = this.list(queryWrapper);
        return list.reversed();
    }

    @Override
    public List<ChatEventLog> listEventsByTurnId(String turnId) {
        ThrowUtils.throwIf(StrUtil.isBlank(turnId), ErrorCode.PARAMS_ERROR, "turnId 不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatEventLog::getTurnId, turnId)
                .orderBy(ChatEventLog::getSeq, true)
                .orderBy(ChatEventLog::getCreateTime, true);
        return this.list(queryWrapper);
    }

    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatEventLog::getAppId, appId);
        return this.remove(queryWrapper);
    }
}
