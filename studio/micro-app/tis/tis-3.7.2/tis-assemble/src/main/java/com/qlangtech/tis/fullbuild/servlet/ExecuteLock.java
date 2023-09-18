/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.fullbuild.servlet;

import com.qlangtech.tis.exec.IExecChainContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-01 09:50
 **/
public class ExecuteLock {

    private static final Logger logger = LoggerFactory.getLogger(ExecuteLock.class);
    private final Queue<TaskFuture<?>> futureQueue = new ConcurrentLinkedQueue<>();

    // private final ReentrantLock lock;
    private final AtomicBoolean lock = new AtomicBoolean(false);

    // 开始时间，需要用它判断是否超时
    private AtomicLong startTimestamp;

    // 超时时间为9个小时
    private static final long EXPIR_TIME = 1000 * 60 * 60 * 9;

    private final String taskOwnerUniqueName;

    public ExecuteLock(String indexName) {
        this.taskOwnerUniqueName = indexName;
        // 这个lock 的问题是必须要由拥有这个lock的owner thread 来释放锁，不然的话就会抛异常
        // this.lock = new ReentrantLock();
        this.startTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    public boolean matchTask(int taskId) {
        for (TaskFuture<?> f : futureQueue) {
            if (f.taskId != null && f.taskId == taskId) {
                return true;
            }
        }
        return false;
    }

    public void cancelAllFuture() {
        for (TaskFuture<?> f : this.futureQueue) {
            try {
                f.cancelTask();
                f.future.cancel(true);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    public void addTaskFuture(TaskFuture future) {
        this.futureQueue.add(future);
    }

    public String getTaskOwnerUniqueName() {
        return taskOwnerUniqueName;
    }

    boolean isExpire() {
        long start = startTimestamp.get();
        long now = System.currentTimeMillis();
        // 没有完成
        // 查看是否超时
        boolean expire = ((start + EXPIR_TIME) < now);
        if (expire) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            logger.info("time:" + format.format(new Date(start)) + "is expire");
        }
        return expire;
    }

    /**
     * 尝试加锁
     *
     * @return
     */
    public boolean lock() {
        if (this.lock.compareAndSet(false, true)) {
            this.startTimestamp.getAndSet(System.currentTimeMillis());
            return true;
        } else {
            return false;
        }
    }

    /**
     * 释放锁
     */
    public void unlock() {
        // this.lock.unlock();
        this.lock.lazySet(false);
    }

    public void clearLockFutureQueue() {
        synchronized (TisServlet.class) {
            this.unlock();
            this.futureQueue.clear();
            TisServlet.idles.remove(this.taskOwnerUniqueName, this);
        }
    }


    public static class TaskFuture<T> {
        private Integer taskId;
        private Future<T> future;
        private IExecChainContext execChainContext;

        public TaskFuture(IExecChainContext execChainContext) {
            this.execChainContext = execChainContext;
        }

        public void cancelTask() {
            this.execChainContext.cancelTask();
        }

        public Future<T> getFuture() {
            return future;
        }

        public void setFuture(Future<T> future) {
            this.future = future;
        }

        public Integer getTaskId() {
            return taskId;
        }

        public void setTaskId(Integer taskId) {
            this.taskId = taskId;
        }
    }
}
