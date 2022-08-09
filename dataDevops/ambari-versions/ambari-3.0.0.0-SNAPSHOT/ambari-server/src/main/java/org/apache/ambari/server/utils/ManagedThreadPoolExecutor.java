/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ThreadPoolExecutor extension which is stopped by default and can be started & stopped.
 */
public class ManagedThreadPoolExecutor extends ThreadPoolExecutor {

  private volatile boolean isStopped;
  private final ReentrantLock pauseLock = new ReentrantLock();
  private final Condition unpaused = pauseLock.newCondition();

  public ManagedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                            long keepAliveTime, TimeUnit unit,
                            BlockingQueue<Runnable> workQueue) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
            Executors.defaultThreadFactory());
    isStopped = true;
  }

  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    super.beforeExecute(t, r);
    pauseLock.lock();
    try {
      while (isStopped) {
        unpaused.await();
      }
    } catch (InterruptedException ie) {
      t.interrupt();
    } finally {
      pauseLock.unlock();
    }
  }

  public void start() {
    pauseLock.lock();
    try {
      isStopped = false;
      unpaused.signalAll();
    } finally {
      pauseLock.unlock();
    }
  }

  public void stop() {
    pauseLock.lock();
    try {
      isStopped = true;
    } finally {
      pauseLock.unlock();
    }
  }

  public boolean isRunning() {
    return !isStopped;
  }

}
