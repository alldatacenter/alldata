package com.platform.mobile.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis配置类
 * Created by wulinhao on 2019/4/8.
 */
@Configuration
@EnableTransactionManagement
@MapperScan({"com.platform.mapper", "com.platform.mobile.dao"})
public class MyBatisConfig {
}
