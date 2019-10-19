package com.platform.search.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis配置类
 * Created by wulinhao on 2019/4/8.
 */
@Configuration
@EnableTransactionManagement
@EnableAutoConfiguration
@MapperScan(basePackages = {"com.platform.search.mapper"})
public class MyBatisConfig {
}
