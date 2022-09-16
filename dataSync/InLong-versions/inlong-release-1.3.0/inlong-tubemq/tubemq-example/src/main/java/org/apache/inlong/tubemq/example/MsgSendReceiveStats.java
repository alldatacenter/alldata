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

package org.apache.inlong.tubemq.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This demo shows how to collect and report message received statistics.
 */
public class MsgSendReceiveStats implements Runnable {
    private final boolean isProducer;
    private static final Logger logger = LoggerFactory.getLogger(MsgSendReceiveStats.class);
    private static final ConcurrentHashMap<String, AtomicLong> counterMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> befCountMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isStarted = new AtomicBoolean(true);

    public MsgSendReceiveStats(boolean isProducer) {
        this.isProducer = isProducer;
    }

    @Override
    public void run() {
        // Indicator output every 30 seconds
        while (isStarted.get()) {
            try {
                for (Map.Entry<String, AtomicLong> entry : counterMap.entrySet()) {
                    long currCount = entry.getValue().get();
                    AtomicLong befCount = befCountMap.get(entry.getKey());
                    if (befCount == null) {
                        AtomicLong tmpCount = new AtomicLong(0);
                        befCount = befCountMap.putIfAbsent(entry.getKey(), tmpCount);
                        if (befCount == null) {
                            befCount = tmpCount;
                        }
                    }
                    if (isProducer) {
                        logger.info("********* Current {} Message sent count is {}, dlt is {}",
                                entry.getKey(), currCount, (currCount - befCount.get()));
                    } else {
                        logger.info("********* Current {} Message received count is {}, dlt is {}",
                                entry.getKey(), currCount, (currCount - befCount.get()));
                    }
                    befCountMap.get(entry.getKey()).set(currCount);
                }
            } catch (Throwable t) {
                // ignore
            }
            ThreadUtils.sleep(30000);
        }
    }

    /**
     * Record the number of messages by topicName dimension
     *
     * @param topicName   topic name
     * @param msgCnt      message count
     */
    public void addMsgCount(final String topicName, int msgCnt) {
        if (msgCnt > 0) {
            AtomicLong currCount = counterMap.get(topicName);
            if (currCount == null) {
                AtomicLong tmpCount = new AtomicLong(0);
                currCount = counterMap.putIfAbsent(topicName, tmpCount);
                if (currCount == null) {
                    currCount = tmpCount;
                }
            }
            // Indicator output every 1000
            if (currCount.addAndGet(msgCnt) % 1000 == 0) {
                if (isProducer) {
                    logger.info("Sent " + topicName + " messages:" + currCount.get());
                } else {
                    logger.info("Received " + topicName + " messages:" + currCount.get());
                }
            }
        }
    }

    public void stopStats() {
        isStarted.set(false);
    }
}
