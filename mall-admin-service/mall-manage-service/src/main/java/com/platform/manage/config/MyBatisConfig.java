package com.platform.manage.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis配置类
 * Created by wulinhao on 2019/4/8.
 */
@Configuration
@EnableTransactionManagement
@MapperScan({"com.platform.mbg.mapper", "com.platform.manage.mapper"})
public class MyBatisConfig {
}
