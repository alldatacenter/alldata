package com.alibaba.tesla.appmanager.spring.config;

import com.alibaba.tesla.appmanager.autoconfig.RedisProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redission 配置
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Configuration
public class RedissonConfig {

    /**
     * 生成全局 Redisson Client
     *
     * @param redisProperties Redis 配置文件
     * @return RedissonClient
     */
    @Bean
    RedissonClient redissonClient(RedisProperties redisProperties) {
        String nodeAddress = redisProperties.getAddress();
        Config config = new Config();
        config.useSingleServer().setAddress(redisProperties.getAddress());
        return Redisson.create(config);
    }
}
