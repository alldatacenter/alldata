package com.alibaba.tesla.appmanager.spring.config;

import com.alibaba.tesla.appmanager.autoconfig.ThreadPoolProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * 全局异步配置 (在非单元测试环境下使用)
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final String THREAD_NAME_PREFIX = "async-thread-";

    @Autowired
    private ThreadPoolProperties threadPoolProperties;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(threadPoolProperties.getAsyncExecutorCoreSize());
        taskExecutor.setMaxPoolSize(threadPoolProperties.getAsyncExecutorMaxSize());
        taskExecutor.setQueueCapacity(threadPoolProperties.getAsyncExecutorQueueCapacity());
        taskExecutor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("action=asyncTaskException|message={}|methodName={}|params={}|fullStack={}",
                ex.getMessage(), method.getName(), ReflectionToStringBuilder.toString(params),
                ExceptionUtils.getStackTrace(ex));
        }
    }
}
