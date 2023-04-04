/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.core.common;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class TaskExecutor {

    private static final ThreadPoolExecutor executor;

    static {
        final int max = Runtime.getRuntime().availableProcessors() * 2;
        executor = new ThreadPoolExecutor(max, max, 30
                , TimeUnit.SECONDS
                , new LinkedBlockingDeque<>()
                , Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        executor.allowCoreThreadTimeOut(true);
    }

    public static void submit(Runnable task) {
        log();
        executor.submit(task);
    }

    public static <T> Future<T> submit(Callable<T> task) {
        log();
        return executor.submit(task);
    }


    private static void log() {
        if (executor.getTaskCount() > Runtime.getRuntime().availableProcessors() * 4L) {
            log.warn("Too many pending tasks ({}),", executor.getTaskCount());
        }
    }

}