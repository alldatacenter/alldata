package com.alibaba.sreworks.common.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author jinghua.yjh
 */
public class ThreadPoolUtil {

    public static ThreadPoolExecutor createThreadPool(int maxPoolSize, String threadPoolName) {
        BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();
        return createThreadPoolWithQueue(maxPoolSize, threadPoolName, blockingQueue);
    }

    public static ThreadPoolExecutor createThreadPoolWithQueue(int maxPoolSize, String threadPoolName,
        BlockingQueue<Runnable> blockingQueue) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat(String.format("%s-%s", threadPoolName, maxPoolSize) + "-%d").build();
        return new ThreadPoolExecutor(maxPoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS, blockingQueue,
            threadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

}
