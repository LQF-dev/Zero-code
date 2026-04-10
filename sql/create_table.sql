-- 数据库初始化

-- 创建库
create database if not exists zero_code;

-- 切换库
use zero_code;

-- 用户表
-- 以下是建表语句

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 应用表
create table app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment '应用名称',
    cover        varchar(512)                       null comment '应用封面',
    initPrompt   text                               null comment '应用初始化的 prompt',
    codeGenType  varchar(64)                        null comment '代码生成类型（枚举）',
    deployKey    varchar(64)                        null comment '部署标识',
    deployedTime datetime                           null comment '部署时间',
    priority     int      default 0                 not null comment '优先级',
    userId       bigint                             not null comment '创建用户id',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_deployKey (deployKey), -- 确保部署标识唯一
    INDEX idx_appName (appName),         -- 提升基于应用名称的查询性能
    INDEX idx_userId (userId)            -- 提升基于用户 ID 的查询性能
) comment '应用' collate = utf8mb4_unicode_ci;

-- 对话历史表
create table chat_history
(
    id          bigint auto_increment comment 'id' primary key,
    message     text                               not null comment '消息',
    reasoningContent longtext                      null comment '深度思考内容（reasoning_content）',
    messageType varchar(32)                        not null comment 'user/ai',
    turnId      varchar(64)                        null comment '一轮对话标识',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_appId (appId),                       -- 提升基于应用的查询性能
    INDEX idx_createTime (createTime),             -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (appId, createTime), -- 游标查询核心索引
    INDEX idx_turnId (turnId)
) comment '对话历史' collate = utf8mb4_unicode_ci;

-- 聊天事件日志表（用于可回放的结构化会话历史）
-- 说明：chat_history 继续作为展示视图；chat_event_log 作为回放事实表
create table if not exists chat_event_log
(
    id               bigint auto_increment comment 'id' primary key,
    appId            bigint                                 not null comment '应用 id',
    memoryId         varchar(128)                           not null comment '会话内存 id（appId_codeGenType）',
    turnId           varchar(64)                            not null comment '一轮对话标识',
    seq              int                                    not null comment '同一轮内事件顺序',
    codeGenType      varchar(32)                            not null comment '代码生成类型（html/multi_file/vue_project）',
    role             varchar(16)                            not null comment '消息角色（user/assistant/tool/system）',
    eventType        varchar(32)                            not null comment '事件类型',
    content          longtext                               null comment '消息文本内容',
    reasoningContent longtext                               null comment '深度思考内容',
    toolCallId       varchar(128)                           null comment '工具调用 id',
    toolName         varchar(128)                           null comment '工具名称',
    toolArguments    longtext                               null comment '工具参数（json）',
    toolResult       longtext                               null comment '工具执行结果',
    rawEventJson     longtext                               null comment '原始事件 json（审计/排障）',
    userId           bigint                                 not null comment '用户 id',
    createTime       datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime       datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete         tinyint      default 0                 not null comment '是否删除',
    index idx_memoryId_createTime (memoryId, createTime),
    index idx_appId_codeGenType_createTime (appId, codeGenType, createTime),
    index idx_turnId_seq (turnId, seq),
    index idx_toolCallId (toolCallId)
) comment '聊天事件日志' collate = utf8mb4_unicode_ci;
