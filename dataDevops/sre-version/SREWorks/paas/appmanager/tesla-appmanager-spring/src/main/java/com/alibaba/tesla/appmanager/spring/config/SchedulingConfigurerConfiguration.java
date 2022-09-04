package com.alibaba.tesla.appmanager.spring.config;

import com.alibaba.tesla.appmanager.autoconfig.ThreadPoolProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Scheduler Pool Settings
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Configuration
public class SchedulingConfigurerConfiguration implements SchedulingConfigurer {

    @Autowired
    private ThreadPoolProperties threadPoolProperties;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(threadPoolProperties.getTaskSchedulerSize());
        taskScheduler.initialize();
        taskRegistrar.setTaskScheduler(taskScheduler);
    }
}