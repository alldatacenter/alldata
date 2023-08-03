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

package org.apache.inlong.dataproxy.metrics.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AbsStatsDaemon
 *
 *
 * Statistics daemon that periodically outputs metrics data to a file.
 */

public abstract class AbsStatsDaemon implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbsStatsDaemon.class);
    private static final long MAX_PRINT_TIME_MS = 10000L;
    private final String name;
    private final String threadName;
    private final long intervalMs;
    private final int maxStatsCnt;
    private final Thread daemon;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicInteger writeIndex = new AtomicInteger(0);

    public AbsStatsDaemon(String name, long intervalMs, int maxCnt) {
        this.name = name;
        this.maxStatsCnt = maxCnt;
        this.intervalMs = intervalMs;
        this.threadName = "Daemon_Thread_" + name;
        this.daemon = new Thread(this, this.threadName);
        this.daemon.setDaemon(true);
    }

    public void start() {
        this.daemon.start();
        LOGGER.info("{} is started!", this.name);
    }

    public boolean isStopped() {
        return this.shutdown.get();
    }

    public boolean stop() {
        if (this.shutdown.get()) {
            return true;
        }
        if (this.shutdown.compareAndSet(false, true)) {
            LOGGER.info("{} is closing ......", this.name);
            try {
                if (this.daemon != null) {
                    this.daemon.interrupt();
                    this.daemon.join();
                }
            } catch (Throwable e) {
                //
            }
            LOGGER.info("{} is stopped", this.name);
            return false;
        }
        return true;
    }

    protected abstract int loopProcess(long startTime);

    protected abstract int exitProcess(long startTime);

    /**
     * Get the writable index
     *
     * @return the writable block index
     */
    protected int getWriteIndex() {
        return Math.abs(writeIndex.get() % 2);
    }

    /**
     * Gets the read index
     *
     * @return the read block index
     */
    protected int getReadIndex() {
        return Math.abs((writeIndex.get() - 1) % 2);
    }

    @Override
    public void run() {
        int printCnt;
        long startTime;
        LOGGER.info("{} is started", this.threadName);
        // process daemon task
        while (!shutdown.get()) {
            try {
                Thread.sleep(intervalMs);
                writeIndex.incrementAndGet();
                startTime = System.currentTimeMillis();
                printCnt = loopProcess(startTime);
                checkAndPrintStatus(printCnt, System.currentTimeMillis() - startTime);
            } catch (InterruptedException e) {
                LOGGER.info("{} has been interrupted", this.threadName);
                break;
            } catch (Throwable t) {
                LOGGER.info("{} throw a exception", this.threadName);
            }
        }
        // process exit output
        startTime = System.currentTimeMillis();
        printCnt = exitProcess(startTime);
        checkAndPrintStatus(printCnt, System.currentTimeMillis() - startTime);
        LOGGER.info("{} is stopped", this.threadName);
    }

    private void checkAndPrintStatus(int printCnt, long outputTime) {
        if (printCnt > maxStatsCnt) {
            LOGGER.warn("{} print {} items, over max allowed count {}",
                    this.threadName, printCnt, this.maxStatsCnt);
        }
        if (outputTime > MAX_PRINT_TIME_MS) {
            LOGGER.warn("{} print items wasts {} ms", this.threadName, outputTime);
        }
    }
}
