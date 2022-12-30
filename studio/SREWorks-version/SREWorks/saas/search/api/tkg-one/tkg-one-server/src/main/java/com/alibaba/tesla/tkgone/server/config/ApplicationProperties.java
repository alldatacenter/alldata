package com.alibaba.tesla.tkgone.server.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author jialiang.tjl
 */
@Service
@Data
public class ApplicationProperties {
    @Value("${productops.std.index}")
    private String productopsStdIndex;

    @Value("${productops.path.mapping.index}")
    private String productopsPathMappingIndex;

    @Value("${productops.path.endpoint}")
    private String productopsPathEndpoint;

    @Value("${robot.url}")
    private String robotUrl;

    @Value("${robot.default.appKey}")
    private String robotDefaultAppKey;

    @Value("${grafana.url}")
    private String grafanaUrl;

    @Value("${redis.host}")
    private String redisHost;

    @Value("${redis.port}")
    private Integer redisPort;

    @Value("${redis.pwd}")
    private String redisPwd;

    @Value("${redis.db}")
    private Integer redisDb;
}
