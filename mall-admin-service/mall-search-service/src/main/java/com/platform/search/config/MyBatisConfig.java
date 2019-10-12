package com.platform.search.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis配置类
 * Created by wulinhao on 2019/4/8.
 */
@Configuration
@MapperScan({"com.platform.mbg.mapper", "com.platform.search.mapper"})
public class MyBatisConfig {
}
