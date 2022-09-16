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

package org.apache.inlong.sdk.sort.util;

import java.util.concurrent.TimeUnit;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PeriodicTask implements Runnable {

    private final TimeUnit timeUnit;
    private final SortClientConfig config;
    protected Logger logger = LoggerFactory.getLogger(PeriodicTask.class);
    private volatile long runInterval;
    private volatile boolean isRunning = false;
    private volatile boolean isStarted = false;
    private volatile Thread runThread;

    public PeriodicTask(long runInterval, TimeUnit timeUnit, SortClientConfig config) {
        this.runInterval = runInterval;
        this.timeUnit = timeUnit;
        this.config = config;
    }

    protected abstract void doWork();

    protected void beforeExit() {
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        logger.info(threadName + " start.");
        while (isRunning) {
            try {
                TimeUnit.MILLISECONDS.sleep(timeUnit.toMillis(runInterval));
                doWork();
            } catch (InterruptedException e) {
                if (!isRunning) {
                    break;
                }
            } catch (Throwable e) {
                logger.warn(threadName + " run error.", e);
            }
        }

        logger.info(threadName + " exit.");
        beforeExit();
    }

    public synchronized void start(String threadName) {
        if (isStarted) {
            return;
        }
        isRunning = true;
        this.runThread = new Thread(this, threadName);
        runThread.start();
        isStarted = true;
    }

    public synchronized void stop() {
        if (!isRunning) {
            return;
        }

        isRunning = false;
        if (runThread != null) {
            runThread.interrupt();
        }
    }

    public long getRunInterval() {
        return runInterval;
    }

    public void setRunInterval(long runInterval) {
        this.runInterval = runInterval;
    }

}
