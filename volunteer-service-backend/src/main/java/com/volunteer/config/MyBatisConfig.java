package com.volunteer.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.volunteer.mapper")
public class MyBatisConfig {
}
