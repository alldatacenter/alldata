package com.alibaba.tesla.appmanager.autoconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 异步执行器配置
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@ConfigurationProperties(prefix = "appmanager.thread-pool")
public class ThreadPoolProperties {

    /**
     * Component Watch Cron Manager
     */
    private Integer componentWatchCronManagerCoreSize = 100;
    private Integer componentWatchCronManagerMaxSize = 150;
    private Integer componentWatchCronManagerQueueCapacity = 100000;
    private Long componentWatchCronManagerKeepAlive = 60L;

    /**
     * Component Watch Cron Failed Tasks Manager
     */
    private Integer componentWatchCronFailedTaskManagerCoreSize = 20;
    private Integer componentWatchCronFailedTaskManagerMaxSize = 30;
    private Integer componentWatchCronFailedTaskManagerQueueCapacity = 200000;
    private Long componentWatchCronFailedTaskManagerKeepAlive = 60L;

    /**
     * Realtime App Instance Status Update Job
     */
    private Integer rtAppInstanceStatusUpdateCoreSize = 20;
    private Integer rtAppInstanceStatusUpdateMaxSize = 40;
    private Integer rtAppInstanceStatusUpdateQueueCapacity = 150000;
    private Long rtAppInstanceStatusUpdateKeepAlive = 60L;

    /**
     * Workflow Task
     */
    private Integer workflowTaskCoreSize = 20;
    private Integer workflowTaskMaxSize = 40;
    private Integer workflowTaskQueueCapacity = 10000;
    private Long workflowTaskKeepAlive = 60L;

    /**
     * Async Executor
     */
    private Integer asyncExecutorCoreSize = 200;
    private Integer asyncExecutorMaxSize = 400;
    private Integer asyncExecutorQueueCapacity = 10000;

    /**
     * 任务调度池大小
     */
    private Integer taskSchedulerSize = 100;
}
