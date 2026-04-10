package com.ai.zerocode.service;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * 聊天记忆回放服务。
 */
public interface ChatMemoryReplayService {

    /**
     * 从结构化事件日志重建 chat memory。
     *
     * @param memoryId   会话内存 id
     * @param chatMemory 待重建的 chat memory
     * @param maxEvents  最大事件数
     * @return 回放到内存的消息条数
     */
    int rebuildFromEventLog(String memoryId, MessageWindowChatMemory chatMemory, int maxEvents);
}

