package com.ai.zerocode.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    private String host;

    private int port;

    private String password;

    private long ttl;

    @Bean
    public ChatMemoryStore redisChatMemoryStore() {
        RedisChatMemoryStore redisChatMemoryStore = RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .ttl(ttl)
                .build();
        // 装饰链：Redis → Sanitizing（清洗非法消息） → Compacting（微压缩旧工具结果）
        return new CompactingChatMemoryStore(new SanitizingChatMemoryStore(redisChatMemoryStore));
    }
}
