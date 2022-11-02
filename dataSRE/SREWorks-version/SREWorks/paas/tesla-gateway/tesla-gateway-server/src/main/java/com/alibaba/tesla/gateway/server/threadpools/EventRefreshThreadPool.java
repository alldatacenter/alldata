package com.alibaba.tesla.gateway.server.threadpools;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 用于事件刷新
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Configuration
public class EventRefreshThreadPool {
    /**
     * 核心线程数
     */
    private static final Integer CORE_SIZE = 1;

    /**
     * 最大线程数
     */
    private static final Integer MAX_SIZE = 5;

    /**
     * 存活时间
     */
    private static final Long KEEP_LIVE = 60L;

    /**
     * 队列长度
     */
    private static final Integer QUEUE_SIZE = 1024;

    /**
     * executor
     */
    private ThreadPoolExecutor executor;

    @PostConstruct
    private void init(){
        ThreadFactory factory = new ThreadFactoryBuilder()
                .setNameFormat("event-refresh-threadPool-%d")
                .build();
        executor = new ThreadPoolExecutor(CORE_SIZE,MAX_SIZE,KEEP_LIVE, TimeUnit.SECONDS,new LinkedBlockingQueue<>(QUEUE_SIZE),factory);
        executor.allowCoreThreadTimeOut(true);
        log.info("task executor thread pool started");
    }


    /**
     * get execute
     * @return thread pool
     */
    @Bean(value = "eventRefreshExecutor")
    public ThreadPoolExecutor getExecutor(){
        return executor;
    }

}
