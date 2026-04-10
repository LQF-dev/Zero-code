package com.ai.zerocode.service;

import com.ai.zerocode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.ai.zerocode.model.entity.ChatHistory;
import com.ai.zerocode.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;


/**
 * 对话历史 服务层。
 *
 * @author Luo QinFeng
 */
public interface ChatHistoryService extends IService<ChatHistory> {


    /**
     * 添加对话历史
     *
     * @param appId       应用 id
     * @param message     消息
     * @param messageType 消息类型
     * @param userId      用户 id
     * @return 是否成功
     */
    default boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        return addChatMessage(appId, message, messageType, userId, null, null);
    }

    /**
     * 添加对话历史（支持 turnId）
     */
    default boolean addChatMessage(Long appId, String message, String messageType, Long userId, String turnId) {
        return addChatMessage(appId, message, messageType, userId, null, turnId);
    }

    /**
     * 添加对话历史（支持持久化深度思考内容 + turnId）
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId, String reasoningContent, String turnId);

    /**
     * 根据应用 id 删除对话历史
     *
     * @param appId
     * @return
     */
    boolean deleteByAppId(Long appId);

    /**
     * 分页查询某 APP 的对话记录
     *
     * @param appId
     * @param pageSize
     * @param lastCreateTime
     * @param loginUser
     * @return
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 加载对话历史到内存
     *
     * @param appId
     * @param chatMemory
     * @param maxCount 最多加载多少条
     * @return 加载成功的条数
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
//
    /**
     * 构造查询条件
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
