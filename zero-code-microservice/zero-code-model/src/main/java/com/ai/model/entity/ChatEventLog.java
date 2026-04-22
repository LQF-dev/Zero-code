package com.ai.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天事件日志（结构化事实表）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_event_log")
public class ChatEventLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("appId")
    private Long appId;

    @Column("memoryId")
    private String memoryId;

    @Column("turnId")
    private String turnId;

    private Integer seq;

    @Column("codeGenType")
    private String codeGenType;

    private String role;

    @Column("eventType")
    private String eventType;

    private String content;

    @Column("reasoningContent")
    private String reasoningContent;

    @Column("toolCallId")
    private String toolCallId;

    @Column("toolName")
    private String toolName;

    @Column("toolArguments")
    private String toolArguments;

    @Column("toolResult")
    private String toolResult;

    @Column("rawEventJson")
    private String rawEventJson;

    @Column("userId")
    private Long userId;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
