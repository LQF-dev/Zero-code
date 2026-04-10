package com.ai.zerocode;


import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@EnableCaching
@MapperScan("com.ai.zerocode.mapper")
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})

public class ZeroCodeApplication {

	public static void main(String[] args) {
		// 禁用 DevTools 重启类加载器，避免长流程异步任务出现双 ClassLoader 类型冲突
		System.setProperty("spring.devtools.restart.enabled", "false");
		SpringApplication.run(ZeroCodeApplication.class, args);
	}

}
