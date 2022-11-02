package com.alibaba.tdata.aisp.server.common.config;

import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.properties.CacheProperties;
import com.alibaba.tdata.aisp.server.common.utils.TaskCacheUtil;
import com.alibaba.tdata.aisp.server.repository.AnalyseTaskRepository;
import com.alibaba.tdata.aisp.server.repository.domain.TaskDO;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheMonitor;
import com.alicp.jetcache.embedded.CaffeineCacheBuilder;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCacheBuilder;
import com.alicp.jetcache.support.DefaultCacheMonitor;
import com.alicp.jetcache.support.JavaValueDecoder;
import com.alicp.jetcache.support.JavaValueEncoder;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ClientOptions.DisconnectedBehavior;
import io.lettuce.core.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

/**
 * @ClassName: CacheConfiguration
 * @Author: dyj
 * @DATE: 2021-11-25
 * @Description:
 **/
@Slf4j
@Configuration
public class CacheConfiguration {
    @Autowired
    private CacheProperties cacheProperties;
    @Autowired
    private AnalyseTaskRepository taskRepository;

    @Bean(
        name = {"taskResultCache"}
    )
    @ConditionalOnMissingBean(
        name = {"taskResultCache"}
    )
    @ConditionalOnProperty(
        prefix = "aisp.cache",
        value = {"type"},
        havingValue = "local"
    )
    public Cache<String, JSONObject> creatLocalCache() {
        CacheMonitor cacheMonitor = new DefaultCacheMonitor("local_memory_cache");
        return CaffeineCacheBuilder
            .createCaffeineCacheBuilder()
            .expireAfterWrite(cacheProperties.getExpire(), TimeUnit.SECONDS)
            .addMonitor(cacheMonitor)
            .loader((key) -> {
                String taskUuid = TaskCacheUtil.cleanKey((String)key);
                TaskDO taskDO = taskRepository.queryByIdWithBlobs(taskUuid);
                if (taskDO!=null && ((String)key).endsWith("_result") && !StringUtils.isEmpty(taskDO.getTaskResult())){
                    return JSONObject.parseObject(taskDO.getTaskResult());
                } else if (taskDO!=null && ((String)key).endsWith("_req") && !StringUtils.isEmpty(taskDO.getTaskReq())) {
                    return JSONObject.parseObject(taskDO.getTaskReq());
                } else{
                    return null;
                }
            })
            .limit(100)
            .buildCache();
    }

    @Bean(
        name = {"taskResultCache"}
    )
    @Order(1)
    @ConditionalOnMissingBean(
        name = {"taskResultCache"}
    )
    @ConditionalOnProperty(
        prefix = "aisp.cache",
        value = {"type"},
        havingValue = "remote"
    )
    public Cache<String, JSONObject> createRedisCache() {
        CacheMonitor cacheMonitor = new DefaultCacheMonitor("remote_redis_cache");
        RedisClient redisClient = RedisClient.create(cacheProperties.getUri());
        redisClient.setOptions(ClientOptions.builder()
            .disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)
            .build());
        return RedisLettuceCacheBuilder
            .createRedisLettuceCacheBuilder()
            .addMonitor(cacheMonitor)
            .valueEncoder(JavaValueEncoder.INSTANCE)
            .valueDecoder(JavaValueDecoder.INSTANCE)
            .redisClient(redisClient)
            .keyPrefix("aisp:")
            .expireAfterWrite(cacheProperties.getExpire(), TimeUnit.SECONDS)
            .loader((key) -> {
                String taskUuid = TaskCacheUtil.cleanKey((String)key);
                TaskDO taskDO = taskRepository.queryByIdWithBlobs(taskUuid);
                if (taskDO!=null && ((String)key).endsWith("_result") && !StringUtils.isEmpty(taskDO.getTaskResult())){
                    return JSONObject.parseObject(taskDO.getTaskResult());
                } else if (taskDO!=null && ((String)key).endsWith("_req") && !StringUtils.isEmpty(taskDO.getTaskReq())) {
                    return JSONObject.parseObject(taskDO.getTaskReq());
                } else{
                    return null;
                }
            })
            .buildCache();
    }
}
