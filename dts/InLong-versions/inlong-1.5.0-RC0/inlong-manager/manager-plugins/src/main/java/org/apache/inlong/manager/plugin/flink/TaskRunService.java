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

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.manager.common.consts.InlongConstants.ALIVE_TIME_MS;
import static org.apache.inlong.manager.common.consts.InlongConstants.CORE_POOL_SIZE;
import static org.apache.inlong.manager.common.consts.InlongConstants.MAX_POOL_SIZE;
import static org.apache.inlong.manager.common.consts.InlongConstants.QUEUE_SIZE;

/**
 * Task run service.
 */
public class TaskRunService {

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            ALIVE_TIME_MS,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_SIZE),
            new ThreadFactoryBuilder().setNameFormat("inlong-plugin-%s").build(),
            new CallerRunsPolicy());

    /**
     * execute
     */
    public static void execute(Runnable runnable) {
        EXECUTOR_SERVICE.execute(runnable);
    }

    /**
     * submit
     */
    public static Future<?> submit(Runnable runnable) {
        return EXECUTOR_SERVICE.submit(runnable);
    }

    /**
     * submit
     */
    public static <T> Future<T> submit(Runnable runnable, T defaultValue) {
        return EXECUTOR_SERVICE.submit(runnable, defaultValue);
    }

    /**
     * submit
     */
    public static <T> Future<T> submit(Callable<T> callable) {
        return EXECUTOR_SERVICE.submit(callable);
    }

}
