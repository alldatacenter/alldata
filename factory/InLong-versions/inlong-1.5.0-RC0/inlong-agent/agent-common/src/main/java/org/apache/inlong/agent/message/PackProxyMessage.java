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

package org.apache.inlong.agent.message;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.common.msg.AttributeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_INLONG_STREAM_ID_QUEUE_MAX_NUMBER;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_PACKAGE_MAX_SIZE;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_PACKAGE_MAX_TIMEOUT_MS;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_INLONG_STREAM_ID_QUEUE_MAX_NUMBER;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_PACKAGE_MAX_SIZE;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_PACKAGE_MAX_TIMEOUT_MS;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_SEND_SYNC;
import static org.apache.inlong.common.msg.AttributeConstants.DATA_TIME;
import static org.apache.inlong.common.msg.AttributeConstants.MESSAGE_TOPIC;
import static org.apache.inlong.common.msg.AttributeConstants.STREAM_ID;

/**
 * Handle List of BusMessage, which belong to the same stream id.
 */
public class PackProxyMessage {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackProxyMessage.class);

    private final String groupId;
    private final String streamId;
    private final String jobId;
    private final int maxPackSize;
    private final int maxQueueNumber;
    // ms
    private final int cacheTimeout;
    // streamId -> list of proxyMessage
    private final LinkedBlockingQueue<ProxyMessage> messageQueue;
    private final AtomicLong queueSize = new AtomicLong(0);
    private boolean syncSend;
    private int currentSize;
    /**
     * extra map used when sending to dataproxy
     */
    private Map<String, String> extraMap = new HashMap<>();
    private volatile long currentCacheTime = System.currentTimeMillis();

    /**
     * Init PackBusMessage
     */
    public PackProxyMessage(String jobId, JobProfile jobConf, String groupId, String streamId) {
        this.jobId = jobId;
        this.maxPackSize = jobConf.getInt(PROXY_PACKAGE_MAX_SIZE, DEFAULT_PROXY_PACKAGE_MAX_SIZE);
        this.maxQueueNumber = jobConf.getInt(PROXY_INLONG_STREAM_ID_QUEUE_MAX_NUMBER,
                DEFAULT_PROXY_INLONG_STREAM_ID_QUEUE_MAX_NUMBER);
        this.cacheTimeout = jobConf.getInt(PROXY_PACKAGE_MAX_TIMEOUT_MS, DEFAULT_PROXY_PACKAGE_MAX_TIMEOUT_MS);
        // double size of package
        this.messageQueue = new LinkedBlockingQueue<>(maxQueueNumber);
        this.groupId = groupId;
        this.streamId = streamId;
        // handle syncSend flag
        this.syncSend = jobConf.getBoolean(PROXY_SEND_SYNC, false);
        extraMap.put(AttributeConstants.MESSAGE_SYNC_SEND, String.valueOf(syncSend));
    }

    public void generateExtraMap(String dataKey) {
        this.extraMap.put(AttributeConstants.MESSAGE_PARTITION_KEY, dataKey);
    }

    public void addTopicAndDataTime(String topic, long dataTime) {
        this.extraMap.put(STREAM_ID, streamId);
        this.extraMap.put(MESSAGE_TOPIC, topic);
        this.extraMap.put(DATA_TIME, String.valueOf(dataTime));
    }

    /**
     * Check whether queue is nearly full
     *
     * @return true if is nearly full else false.
     */
    private boolean queueIsFull() {
        return messageQueue.size() >= maxQueueNumber - 1;
    }

    /**
     * Add proxy message to cache, proxy message should belong to the same stream id.
     */
    public void addProxyMessage(ProxyMessage message) {
        assert streamId.equals(message.getInlongStreamId());
        try {
            if (queueIsFull()) {
                LOGGER.warn("message queue is greater than {}, stop adding message, "
                        + "maybe proxy get stuck", maxQueueNumber);
            }
            messageQueue.put(message);
            queueSize.addAndGet(message.getBody().length);
        } catch (Exception ex) {
            LOGGER.error("exception caught", ex);
        }
    }

    /**
     * check message queue is empty or not
     */
    public boolean isEmpty() {
        return messageQueue.isEmpty();
    }

    /**
     * Fetch batch of proxy message, timeout message or max number of list satisfied.
     *
     * @return map of message list, key is stream id for the batch; return null if there are no valid messages.
     */
    public BatchProxyMessage fetchBatch() {
        // if queue is nearly full or package size is satisfied or timeout
        long currentTime = System.currentTimeMillis();
        if (queueSize.get() > maxPackSize || queueIsFull()
                || currentTime - currentCacheTime > cacheTimeout) {
            // refresh cache time.
            currentCacheTime = currentTime;
            long resultBatchSize = 0;
            List<byte[]> result = new ArrayList<>();
            while (!messageQueue.isEmpty()) {
                // pre check message size
                ProxyMessage peekMessage = messageQueue.peek();
                if (peekMessage == null
                        || resultBatchSize + peekMessage.getBody().length > maxPackSize) {
                    break;
                }
                ProxyMessage message = messageQueue.remove();
                if (message != null) {
                    int bodySize = message.getBody().length;
                    resultBatchSize += bodySize;
                    // decrease queue size.
                    queueSize.addAndGet(-bodySize);
                    result.add(message.getBody());
                }
            }
            // make sure result is not empty.
            if (!result.isEmpty()) {
                return new BatchProxyMessage(jobId, groupId, streamId, result, AgentUtils.getCurrentTime(), extraMap,
                        syncSend);
            }
        }
        return null;
    }

    public Map<String, String> getExtraMap() {
        return extraMap;
    }

}
