package com.hw.lineage.server.infrastructure.persistence.mybatis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @description: MybatisConfig
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Configuration
@MapperScan("com.hw.lineage.server.infrastructure.persistence.mapper")
public class MybatisConfig {
}
