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

package org.apache.inlong.sort.base.util.concurrent;

import org.apache.inlong.sort.base.util.FatalExitExceptionHandler;

import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorThreadFactory implements ThreadFactory {

    private static final String DEFAULT_POOL_NAME = "flink-executor-pool";
    private final AtomicInteger threadNumber;
    private final ThreadGroup group;
    private final String namePrefix;
    private final int threadPriority;
    @Nullable
    private final UncaughtExceptionHandler exceptionHandler;

    public ExecutorThreadFactory() {
        this("flink-executor-pool");
    }

    public ExecutorThreadFactory(String poolName) {
        this(poolName, FatalExitExceptionHandler.INSTANCE);
    }

    public ExecutorThreadFactory(String poolName, UncaughtExceptionHandler exceptionHandler) {
        this(poolName, 5, exceptionHandler);
    }

    ExecutorThreadFactory(String poolName, int threadPriority, @Nullable UncaughtExceptionHandler exceptionHandler) {
        this.threadNumber = new AtomicInteger(1);
        this.namePrefix = (String) Preconditions.checkNotNull(poolName, "poolName") + "-thread-";
        this.threadPriority = threadPriority;
        this.exceptionHandler = exceptionHandler;
        SecurityManager securityManager = System.getSecurityManager();
        this.group =
                securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
    }

    public Thread newThread(Runnable runnable) {
        Thread t = new Thread(this.group, runnable, this.namePrefix + this.threadNumber.getAndIncrement());
        t.setDaemon(true);
        t.setPriority(this.threadPriority);
        if (this.exceptionHandler != null) {
            t.setUncaughtExceptionHandler(this.exceptionHandler);
        }

        return t;
    }

    public static final class Builder {

        private String poolName;
        private int priority = 5;
        private UncaughtExceptionHandler exceptionHandler;

        public Builder() {
            this.exceptionHandler = FatalExitExceptionHandler.INSTANCE;
        }

        public ExecutorThreadFactory.Builder setPoolName(String poolName) {
            this.poolName = poolName;
            return this;
        }

        public ExecutorThreadFactory.Builder setThreadPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public ExecutorThreadFactory.Builder setExceptionHandler(UncaughtExceptionHandler exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        public ExecutorThreadFactory build() {
            return new ExecutorThreadFactory(this.poolName, this.priority, this.exceptionHandler);
        }
    }
}
