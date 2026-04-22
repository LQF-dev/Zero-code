package com;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.ai.mapper")
@ComponentScan("com.ai")
@EnableDubbo
public class ZeroCodeCodeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZeroCodeCodeUserApplication.class, args);
    }
}
