/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.client.consumer;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetch messages with multiple threads.
 */
public class MessageFetchManager {

    private static final Logger logger =
            LoggerFactory.getLogger(MessageFetchManager.class);
    private final ConcurrentHashMap<Long, Integer> fetchWorkerStatusMap =
            new ConcurrentHashMap<>();
    private final ConsumerConfig consumerConfig;
    private final SimplePushMessageConsumer pushConsumer;
    // Manager status:
    // -1: Undefined
    // 0: Stopped
    // 1: Started
    private AtomicInteger managerStatus = new AtomicInteger(-1);
    private Thread[] fetchWorkerPool;

    public MessageFetchManager(final ConsumerConfig consumerConfig,
                               final SimplePushMessageConsumer pushConsumer) {
        this.consumerConfig = consumerConfig;
        this.pushConsumer = pushConsumer;
    }

    /**
     * Start a worker pool to fetch the message.
     *
     * @throws TubeClientException   Exception thrown
     */
    public void startFetchWorkers() throws TubeClientException {
        this.pushConsumer.getBaseConsumer().checkClientRunning();
        if (managerStatus.get() == 1) {
            logger.info("Duplicated call startFetchWorkers, MessageFetchManager has started !");
            return;
        }
        if (!managerStatus.compareAndSet(-1, 1)) {
            return;
        }
        StringBuilder sBuilder = new StringBuilder(256);
        logger.info("Starting Fetch Worker Pool !");
        this.fetchWorkerPool =
                new Thread[this.consumerConfig.getPushFetchThreadCnt()];
        logger.info(sBuilder
                .append("Prepare to start Fetch Worker Pool, total count:")
                .append(fetchWorkerPool.length).toString());
        sBuilder.delete(0, sBuilder.length());
        for (int i = 0; i < this.fetchWorkerPool.length; i++) {
            this.fetchWorkerPool[i] = new Thread(new FetchTaskWorker());
            this.fetchWorkerStatusMap.put(this.fetchWorkerPool[i].getId(), -1);
            this.fetchWorkerPool[i].setName(sBuilder.append("Fetch_Worker_")
                    .append(this.consumerConfig.getConsumerGroup())
                    .append("-").append(i).toString());
            sBuilder.delete(0, sBuilder.length());
        }
        for (final Thread thread : this.fetchWorkerPool) {
            thread.start();
        }
        logger.info("Fetch Worker Pool started !");
    }

    /**
     * Check if the fetch manager is shut down.
     *
     * @return true if shut down
     */
    public boolean isShutdown() {
        return this.managerStatus.get() == 0;
    }

    /**
     * Stop fetch worker threads
     *
     * @param onlySetStatus  Whether to only set the state without stopping the operation
     */
    public void stopFetchWorkers(boolean onlySetStatus) throws InterruptedException {
        if (onlySetStatus) {
            if (this.managerStatus.get() == 0) {
                return;
            }
            this.managerStatus.set(0);
            if (MessageFetchManager.this.pushConsumer.isConsumePaused()) {
                MessageFetchManager.this.pushConsumer.resumeConsume();
            }
            return;
        } else {
            this.managerStatus.set(0);
        }
        if (this.fetchWorkerPool == null) {
            return;
        }
        StringBuilder sBuilder = new StringBuilder(256);
        logger.info(sBuilder.append("[STOP_FetchWorker] All fetch workers:")
                .append(Arrays.toString(fetchWorkerPool)).toString());
        sBuilder.delete(0, sBuilder.length());
        if (MessageFetchManager.this.pushConsumer.isConsumePaused()) {
            MessageFetchManager.this.pushConsumer.resumeConsume();
        }
        logger.info("[STOP_FetchWorker] Wait all fetch workers exist:");
        if (waitAllFetchRequestHolds(this.consumerConfig.getPushListenerWaitPeriodMs())) {
            for (final Thread thread : this.fetchWorkerPool) {
                if (thread != null) {
                    thread.interrupt();
                }
            }
            for (final Thread thread : this.fetchWorkerPool) {
                if (thread != null) {
                    thread.join();
                    logger.info(sBuilder.append("[STOP_FetchWorker]").append(thread).toString());
                    sBuilder.delete(0, sBuilder.length());
                }
            }
        }
        this.pushConsumer
                .getBaseConsumer().notifyAllMessageListenerStopped();
        Thread.sleep(200);
        logger.info("[STOP_FetchWorker] All fetch workers are stopped.");
    }

    private boolean waitAllFetchRequestHolds(long waitTimeInMills) {
        boolean haveProcessingThread = false;
        long startWaitTime = System.currentTimeMillis();
        do {
            haveProcessingThread =
                    (!this.fetchWorkerStatusMap.isEmpty());
            if (haveProcessingThread) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e1) {
                    break;
                }
            }
        } while ((haveProcessingThread)
                && ((System.currentTimeMillis() - startWaitTime) < waitTimeInMills));
        return haveProcessingThread;
    }

    private class FetchTaskWorker implements Runnable {
        @Override
        public void run() {
            StringBuilder sBuilder = new StringBuilder(256);
            final Long curThreadId = Thread.currentThread().getId();
            fetchWorkerStatusMap.put(curThreadId, 0);
            while (!isShutdown()) {
                PartitionSelectResult partSelectResult = null;
                fetchWorkerStatusMap.put(curThreadId, 0);
                try {
                    if (isShutdown()) {
                        break;
                    }
                    fetchWorkerStatusMap.put(curThreadId, 1);
                    MessageFetchManager.this.pushConsumer.allowConsumeWait();
                    partSelectResult =
                            MessageFetchManager.this.pushConsumer
                                    .getBaseConsumer().pushSelectPartition();
                    if (partSelectResult == null) {
                        continue;
                    }
                    Partition partition = partSelectResult.getPartition();
                    long usedToken = partSelectResult.getUsedToken();
                    boolean isLastConsumed = partSelectResult.isLastPackConsumed();
                    if (isShutdown()) {
                        MessageFetchManager.this.pushConsumer
                                .getBaseConsumer()
                                .pushReqReleasePartition(partition.getPartitionKey(), usedToken, isLastConsumed);
                        partSelectResult = null;
                        break;
                    }
                    if (MessageFetchManager.this.pushConsumer.isConsumePaused()) {
                        boolean result = partSelectResult.isLastPackConsumed();
                        if (result) {
                            result =
                                    MessageFetchManager.this.pushConsumer
                                            .getBaseConsumer().flushLastRequest(partition);
                        }
                        MessageFetchManager.this.pushConsumer
                                .getBaseConsumer().pushReqReleasePartition(partition.getPartitionKey(),
                                usedToken, result);
                        partSelectResult = null;
                        continue;
                    }
                } catch (Throwable e) {
                    if (partSelectResult != null) {
                        MessageFetchManager.this.pushConsumer
                                .getBaseConsumer()
                                .pushReqReleasePartition(partSelectResult.getPartition().getPartitionKey(),
                                        partSelectResult.getUsedToken(), false);
                    }
                    sBuilder.delete(0, sBuilder.length());
                    logger.warn(sBuilder.append("Thread {} has been interrupted 3.")
                            .append(Thread.currentThread().getName()).toString());
                    sBuilder.delete(0, sBuilder.length());
                }
                fetchWorkerStatusMap.put(curThreadId, 2);
                if (partSelectResult != null) {
                    MessageFetchManager.this.pushConsumer.processRequest(
                            partSelectResult, sBuilder);
                }
            }
            fetchWorkerStatusMap.remove(curThreadId);
        }
    }
}
