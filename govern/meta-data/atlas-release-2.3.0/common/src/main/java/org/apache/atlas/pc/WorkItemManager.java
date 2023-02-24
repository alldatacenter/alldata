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

package org.apache.atlas.pc;
import org.apache.curator.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

public class WorkItemManager<T, U extends WorkItemConsumer> {
    private static final Logger LOG = LoggerFactory.getLogger(WorkItemManager.class);

    private final int              numWorkers;
    private final BlockingQueue<T> workQueue;
    private final ExecutorService  service;
    private final List<U>          consumers = new ArrayList<>();
    private CountDownLatch         countdownLatch;
    private Queue<Object>          resultsQueue;

    public WorkItemManager(WorkItemBuilder builder, String namePrefix, int batchSize, int numWorkers, boolean collectResults) {
        this.numWorkers = numWorkers;
        this.workQueue  = new LinkedBlockingQueue<>(batchSize * numWorkers);
        this.service    = Executors.newFixedThreadPool(numWorkers, new ThreadFactoryBuilder().setNameFormat(namePrefix + "-%d").build());

        createConsumers(builder, numWorkers, collectResults);

        start();
    }

    public WorkItemManager(WorkItemBuilder builder, int batchSize, int numWorkers) {
        this(builder, "workItemConsumer", batchSize, numWorkers, false);
    }

    public void setResultsCollection(Queue<Object> resultsQueue) {
        this.resultsQueue = resultsQueue;
    }

    private void createConsumers(WorkItemBuilder builder, int numWorkers, boolean collectResults) {
        if (collectResults) {
            setResultsCollection(new ConcurrentLinkedQueue<>());
        }

        for (int i = 0; i < numWorkers; i++) {
            U c = (U) builder.build(workQueue);

            consumers.add(c);

            if (collectResults) {
                c.setResults(resultsQueue);
            }
        }
    }

    public void start() {
        this.countdownLatch = new CountDownLatch(numWorkers);

        for (U c : consumers) {
            c.setCountDownLatch(countdownLatch);

            service.execute(c);
        }
    }

    public void produce(T item) {
        try {
            workQueue.put(item);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public void checkProduce(T item) {
        if (countdownLatch.getCount() < numWorkers) {
            LOG.info("Fewer workers detected: {}", countdownLatch.getCount());

            drain();

            start();
        }

        produce(item);
    }

    public void drain() {
        try {
            if (countdownLatch == null || countdownLatch.getCount() == 0) {
                return;
            }

            LOG.debug("Drain: Stated! Queue size: {}", workQueue.size());
            this.countdownLatch.await();
            LOG.debug("Drain: Done! Queue size: {}", workQueue.size());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() throws InterruptedException {
        int avgCommitTimeSeconds = getAvgCommitTimeSeconds() * 2;

        LOG.info("WorkItemManager: Shutdown started. Will wait for: {} minutes...", avgCommitTimeSeconds);

        service.shutdown();
        service.awaitTermination(avgCommitTimeSeconds, TimeUnit.MINUTES);

        LOG.info("WorkItemManager: Shutdown done!");
    }

    public Queue getResults() {
        return this.resultsQueue;
    }

    private int getAvgCommitTimeSeconds() {
        int commitTimeSeconds = 0;

        for (U c : consumers) {
            commitTimeSeconds += c.getMaxCommitTimeInMs();
        }

        return (commitTimeSeconds / consumers.size()) / 1000;
    }
}
