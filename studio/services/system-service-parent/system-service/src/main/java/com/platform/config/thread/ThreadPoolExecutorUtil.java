
package com.platform.config.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 用于获取自定义线程池
 * @author AllDataDC
 * @date 2023-01-27 18:16:47
 */
public class ThreadPoolExecutorUtil {

    public static ExecutorService getPoll(){
        return getPoll(null);
    }

    public static ExecutorService getPoll(String threadName){
        return new ThreadPoolExecutor(
                AsyncTaskProperties.corePoolSize,
                AsyncTaskProperties.maxPoolSize,
                AsyncTaskProperties.keepAliveSeconds,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(AsyncTaskProperties.queueCapacity),
                new TheadFactoryName(threadName),
                // 队列与线程池中线程都满了时使用调用者所在的线程来执行
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
