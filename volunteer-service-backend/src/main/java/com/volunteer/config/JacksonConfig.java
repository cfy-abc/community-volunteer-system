package com.volunteer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Jackson 日期格式化配置
 * 支持 yyyy-MM-dd HH:mm:ss 格式的日期解析
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                // 配置日期格式
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss")
                // 设置时区
                .timeZone(TimeZone.getTimeZone("GMT+8"))
                // 禁用将日期序列化为时间戳
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 注册 JavaTimeModule 支持 Java 8 日期 API
                .modules(new JavaTimeModule())
                .build();
    }
}
