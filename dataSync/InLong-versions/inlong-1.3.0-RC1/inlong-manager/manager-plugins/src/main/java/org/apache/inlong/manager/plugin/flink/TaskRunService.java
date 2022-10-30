/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.plugin.flink;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Task run service.
 */
public class TaskRunService {

    private static final ExecutorService executorService;

    private static final int CORE_POOL_SIZE = 16;
    private static final int MAXIMUM_POOL_SIZE = 32;
    private static final int QUEUE_SIZE = 10000;
    private static final long KEEP_ALIVE_TIME = 0L;

    static {
        executorService = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(QUEUE_SIZE));
    }

    /**
     * execute
     *
     * @param runnable
     */
    public static void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

    /**
     * submit
     *
     * @param runnable
     * @return
     */
    public static Future<?> submit(Runnable runnable) {
        return executorService.submit(runnable);
    }

    /**
     * submit
     *
     * @param runnable
     * @param defaultValue
     * @param <T>
     * @return
     */
    public static <T> Future<T> submit(Runnable runnable, T defaultValue) {
        return executorService.submit(runnable, defaultValue);
    }

    /**
     * submit
     *
     * @param callable
     * @param <T>
     * @return
     */
    public static <T> Future<T> submit(Callable<T> callable) {
        return executorService.submit(callable);
    }

}
