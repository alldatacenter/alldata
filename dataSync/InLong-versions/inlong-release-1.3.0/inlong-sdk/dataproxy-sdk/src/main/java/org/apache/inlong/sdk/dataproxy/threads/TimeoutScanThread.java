/*
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

package org.apache.inlong.sdk.dataproxy.threads;

import io.netty.channel.Channel;
import org.apache.inlong.sdk.dataproxy.FileCallback;
import org.apache.inlong.sdk.dataproxy.ProxyClientConfig;
import org.apache.inlong.sdk.dataproxy.SendResult;
import org.apache.inlong.sdk.dataproxy.network.ClientMgr;
import org.apache.inlong.sdk.dataproxy.network.QueueObject;
import org.apache.inlong.sdk.dataproxy.network.TimeScanObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Daemon threads to check timeout for asynchronous callback.
 */
public class TimeoutScanThread extends Thread {
    private static final int MAX_CHANNEL_TIMEOUT = 5 * 60 * 1000;
    private final Logger logger = LoggerFactory.getLogger(TimeoutScanThread.class);
    private final ConcurrentHashMap<Channel, ConcurrentHashMap<String, QueueObject>> callbacks;
    private final AtomicInteger currentBufferSize;
    private final ProxyClientConfig config;
    private final ClientMgr clientMgr;
    private final ConcurrentHashMap<Channel, TimeScanObject> timeoutChannelStat = new ConcurrentHashMap<>();
    private volatile boolean bShutDown = false;
    private long printCount = 0;

    public TimeoutScanThread(ConcurrentHashMap<Channel, ConcurrentHashMap<String, QueueObject>> callbacks,
            AtomicInteger currentBufferSize, ProxyClientConfig config, ClientMgr clientMgr) {
        bShutDown = false;
        printCount = 0;
        this.callbacks = callbacks;
        this.currentBufferSize = currentBufferSize;
        this.config = config;
        this.clientMgr = clientMgr;
        this.setDaemon(true);
        this.setName("TimeoutScanThread");
    }

    public void shutDown() {
        logger.info("begin to shut down TimeoutScanThread!");
        bShutDown = true;
    }

    /**
     * add timeout channel
     *
     * @param channel
     */
    public void addTimeoutChannel(Channel channel) {
        if (channel != null) {
            TimeScanObject timeScanObject = timeoutChannelStat.get(channel);
            if (timeScanObject == null) {
                TimeScanObject tmpTimeObj = new TimeScanObject();
                timeScanObject = timeoutChannelStat.putIfAbsent(channel, tmpTimeObj);
                if (timeScanObject == null) {
                    timeScanObject = tmpTimeObj;
                }
            }
            timeScanObject.incrementAndGet();
        }
    }

    /**
     * reset channel timeout
     *
     * @param channel
     */
    public void resetTimeoutChannel(Channel channel) {
        if (channel != null) {
            TimeScanObject timeScanObject = timeoutChannelStat.get(channel);
            if (timeScanObject != null) {
                timeScanObject.updateCountToZero();
            }
        }
    }

    /**
     * check timeout
     */
    private void checkTimeoutChannel() {
        //if timeout >3,set channel busy
        for (Channel tmpChannel : timeoutChannelStat.keySet()) {
            TimeScanObject timeScanObject = tmpChannel != null ? timeoutChannelStat.get(tmpChannel) : null;
            if (timeScanObject == null) {
                continue;
            }

            // If the channel exists in the list for more than 5 minutes,
            // and does not reach the maximum number of timeouts, it will be removed
            if (System.currentTimeMillis() - timeScanObject.getTime() > MAX_CHANNEL_TIMEOUT) {
                timeoutChannelStat.remove(tmpChannel);
            } else {

                if (timeScanObject.getCurTimeoutCount() > config.getMaxTimeoutCnt()) {
                    timeoutChannelStat.remove(tmpChannel);
                    if (tmpChannel.isOpen() && tmpChannel.isActive()) {
                        clientMgr.setConnectionBusy(tmpChannel);
                        logger.error("this client {} is busy!", tmpChannel);
                    }
                }
            }
        }
    }

    /**
     * check message id
     *
     * @param channel
     * @param messageIdCallbacks
     */
    private void checkMessageIdBasedCallbacks(Channel channel,
            ConcurrentHashMap<String, QueueObject> messageIdCallbacks) {
        for (String messageId : messageIdCallbacks.keySet()) {
            QueueObject queueObject = messageId != null ? messageIdCallbacks.get(messageId) : null;
            if (queueObject == null) {
                continue;
            }
            // if queueObject timeout
            if (System.currentTimeMillis() - queueObject.getSendTimeInMillis() >= queueObject.getTimeoutInMillis()) {
                // remove it before callback
                QueueObject queueObject1 = messageIdCallbacks.remove(messageId);
                if (queueObject1 != null) {
                    if (config.isFile()) {
                        ((FileCallback) queueObject1.getCallback()).onMessageAck(SendResult.TIMEOUT.toString());
                        currentBufferSize.addAndGet(-queueObject1.getSize());
                    } else {
                        queueObject1.getCallback().onMessageAck(SendResult.TIMEOUT);
                        currentBufferSize.decrementAndGet();
                    }
                }
                addTimeoutChannel(channel);
            }
        }
    }

    @Override
    public void run() {
        logger.info("TimeoutScanThread Thread=" + Thread.currentThread().getId() + " started !");
        while (!bShutDown) {
            try {
                for (Channel channel : callbacks.keySet()) {
                    ConcurrentHashMap<String, QueueObject> msgQueueMap =
                            channel != null ? callbacks.get(channel) : null;
                    if (msgQueueMap == null) {
                        continue;
                    }
                    checkMessageIdBasedCallbacks(channel, msgQueueMap);
                }
                checkTimeoutChannel();
                TimeUnit.SECONDS.sleep(1);
            } catch (Throwable e) {
                if (!bShutDown) {
                    logger.error("TimeoutScanThread exception {}", e.getMessage());
                } else {
                    logger.warn("TimeoutScanThread exception {}", e.getMessage());
                }
            }
            if (printCount++ % 20 == 0) {
                logger.info("TimeoutScanThread thread=" + Thread.currentThread().getId()
                        + "'s currentBufferSize = " + currentBufferSize.get());
            }
        }
        logger.info("TimeoutScanThread Thread=" + Thread.currentThread().getId() + " existed !");
    }
}
