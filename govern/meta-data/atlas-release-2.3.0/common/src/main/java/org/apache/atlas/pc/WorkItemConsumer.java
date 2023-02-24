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

package org.apache.atlas.pc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public abstract class WorkItemConsumer<T> implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(WorkItemConsumer.class);

    private static final int POLLING_DURATION_SECONDS  = 5;
    private static final int DEFAULT_COMMIT_TIME_IN_MS = 15000;

    private final BlockingQueue<T> queue;
    private final AtomicBoolean    isDirty           = new AtomicBoolean(false);
    private final AtomicLong       maxCommitTimeInMs = new AtomicLong(DEFAULT_COMMIT_TIME_IN_MS);
    private CountDownLatch         countdownLatch;
    private Queue<Object>          results;

    public WorkItemConsumer(BlockingQueue<T> queue) {
        this.queue          = queue;
        this.countdownLatch = null;
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                T item = queue.poll(POLLING_DURATION_SECONDS, TimeUnit.SECONDS);

                if (item == null) {
                    LOG.debug("WorkItemConsumer.run(): no more items found in the queue. Will exit after committing");

                    commitDirty();

                    return;
                }

                isDirty.set(true);

                processItem(item);
            }
        } catch (InterruptedException e) {
            LOG.error("WorkItemConsumer: Interrupted: ", e);
        } finally {
            maxCommitTimeInMs.set(0);
            countdownLatch.countDown();
        }
    }

    public long getMaxCommitTimeInMs() {
        long commitTime = this.maxCommitTimeInMs.get();

        return ((commitTime > DEFAULT_COMMIT_TIME_IN_MS) ? commitTime : DEFAULT_COMMIT_TIME_IN_MS);
    }

    protected void commitDirty() {
        if (!isDirty.get()) {
            return;
        }

        LOG.info("isDirty");
        commit();
    }

    protected void commit() {
        long start = System.currentTimeMillis();

        doCommit();

        long end = System.currentTimeMillis();

        updateCommitTime((end - start));

        isDirty.set(false);
    }

    protected abstract void doCommit();

    protected abstract void processItem(T item);

    protected void addResult(Object value) {
        results.add(value);
    }

    protected void updateCommitTime(long commitTime) {
        if (this.maxCommitTimeInMs.get() < commitTime) {
            this.maxCommitTimeInMs.set(commitTime);
        }
    }

    public void setCountDownLatch(CountDownLatch countdownLatch) {
        this.countdownLatch = countdownLatch;
    }

    public <V> void setResults(Queue<Object> queue) {
        this.results = queue;
    }
}
