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

package org.apache.inlong.dataproxy.sink.mq;

import org.apache.inlong.common.monitor.LogCounter;

import org.apache.flume.lifecycle.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageQueueZoneWorker
 */
public class MessageQueueZoneWorker extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueZoneWorker.class);
    // log print count
    private static final LogCounter logCounter = new LogCounter(10, 100000, 30 * 1000);
    private final String workerName;
    private final long fetchWaitMs;
    private final MessageQueueZoneSink mqZoneSink;
    private final MessageQueueZoneProducer zoneProducer;
    private LifecycleState status;

    /**
     * Constructor
     */
    public MessageQueueZoneWorker(MessageQueueZoneSink mqZoneSink, int workerIndex,
            long fetchWaitMs, MessageQueueZoneProducer zoneProducer) {
        super();
        this.mqZoneSink = mqZoneSink;
        this.workerName = mqZoneSink.getName() + "-worker-" + workerIndex;
        this.fetchWaitMs = fetchWaitMs;
        this.zoneProducer = zoneProducer;
        this.status = LifecycleState.IDLE;
    }

    /**
     * start
     */
    @Override
    public void start() {
        this.status = LifecycleState.START;
        super.start();
    }

    /**
     * 
     * close
     */
    public void close() {
        // close all producers
        this.zoneProducer.close();
        this.status = LifecycleState.STOP;
    }

    /**
     * run
     */
    @Override
    public void run() {
        logger.info("{} start message zone worker", this.workerName);
        PackProfile profile = null;
        while (status != LifecycleState.STOP) {
            try {
                profile = this.mqZoneSink.takeDispatchedRecord();
                if (profile == null) {
                    this.sleepOneInterval();
                    continue;
                }
                // send
                this.zoneProducer.send(profile);
            } catch (Throwable e1) {
                if (profile != null) {
                    this.mqZoneSink.offerDispatchRecord(profile);
                }
                if (logCounter.shouldPrint()) {
                    logger.error("{} send message failure", workerName, e1);
                }
                this.sleepOneInterval();
            }
        }
        logger.info("{} exit message zone worker", this.workerName);
    }

    /**
     * sleepOneInterval
     */
    private void sleepOneInterval() {
        try {
            Thread.sleep(fetchWaitMs);
        } catch (InterruptedException e1) {
            if (logCounter.shouldPrint()) {
                logger.error("{} wait poll record interrupted", workerName, e1);
            }
        }
    }
}
