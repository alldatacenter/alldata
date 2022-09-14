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

package org.apache.inlong.agent.plugin.sinks;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.agent.common.AgentThreadFactory;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.CommonConstants;
import org.apache.inlong.agent.message.EndMessage;
import org.apache.inlong.agent.message.ProxyMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.MessageFilter;
import org.apache.inlong.agent.plugin.message.PackProxyMessage;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_FIELD_SPLITTER;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_SEND_SYNC;
import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_PROXY_BATCH_FLUSH_INTERVAL;
import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_PROXY_INLONG_STREAM_ID_QUEUE_MAX_NUMBER;
import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_PROXY_PACKAGE_MAX_SIZE;
import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_PROXY_PACKAGE_MAX_TIMEOUT_MS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_INSTANCE_ID;
import static org.apache.inlong.agent.constant.JobConstants.PROXY_BATCH_FLUSH_INTERVAL;
import static org.apache.inlong.agent.constant.JobConstants.PROXY_INLONG_STREAM_ID_QUEUE_MAX_NUMBER;
import static org.apache.inlong.agent.constant.JobConstants.PROXY_PACKAGE_MAX_SIZE;
import static org.apache.inlong.agent.constant.JobConstants.PROXY_PACKAGE_MAX_TIMEOUT_MS;

/**
 * sink message data to inlong-dataproxy
 */
public class ProxySink extends AbstractSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxySink.class);
    private static AtomicLong index = new AtomicLong(0);
    private final ExecutorService executorService = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new AgentThreadFactory("ProxySink"));
    private MessageFilter messageFilter;
    private SenderManager senderManager;
    private byte[] fieldSplitter;
    private String sourceName;
    private String jobInstanceId;
    private int maxBatchSize;
    private int maxBatchTimeoutMs;
    private int batchFlushInterval;
    private int maxQueueNumber;
    private boolean syncSend;
    private volatile boolean shutdown = false;
    // key is stream id, value is a batch of messages belong to the same stream id
    private ConcurrentHashMap<String, PackProxyMessage> cache;

    public ProxySink() {
    }

    @Override
    public void write(Message message) {
        try {
            if (message != null) {
                senderManager.acquireSemaphore(1);
                message.getHeader().put(CommonConstants.PROXY_KEY_GROUP_ID, inlongGroupId);
                message.getHeader().put(CommonConstants.PROXY_KEY_STREAM_ID, inlongStreamId);
                extractStreamFromMessage(message, fieldSplitter);
                if (!(message instanceof EndMessage)) {
                    ProxyMessage proxyMessage = ProxyMessage.parse(message);
                    // add proxy message to cache.
                    cache.compute(proxyMessage.getBatchKey(),
                            (s, packProxyMessage) -> {
                                if (packProxyMessage == null) {
                                    packProxyMessage = new PackProxyMessage(
                                            maxBatchSize, maxQueueNumber,
                                            maxBatchTimeoutMs,
                                            proxyMessage.getInlongStreamId()
                                    );
                                    packProxyMessage.generateExtraMap(syncSend,
                                            proxyMessage.getDataKey());
                                }
                                // add message to package proxy
                                packProxyMessage.addProxyMessage(proxyMessage);
                                //
                                return packProxyMessage;
                            });
                    AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_SEND_SUCCESS,
                            inlongGroupId, inlongStreamId, System.currentTimeMillis());
                    // increment the count of successful sinks
                    sinkMetric.sinkSuccessCount.incrementAndGet();
                } else {
                    // increment the count of failed sinks
                    sinkMetric.sinkFailCount.incrementAndGet();
                }
            }
        } catch (Exception e) {
            LOGGER.error("write message to Proxy sink error", e);
        } catch (Throwable t) {
            ThreadUtils.threadThrowableHandler(Thread.currentThread(), t);
        }
    }

    /**
     * extract stream id from message if message filter is presented
     */
    private void extractStreamFromMessage(Message message, byte[] fieldSplitter) {
        if (messageFilter != null) {
            message.getHeader().put(CommonConstants.PROXY_KEY_STREAM_ID,
                    messageFilter.filterStreamId(message, fieldSplitter));
        } else {
            message.getHeader().put(CommonConstants.PROXY_KEY_STREAM_ID, inlongStreamId);
        }
    }

    @Override
    public void setSourceName(String sourceFileName) {
        this.sourceName = sourceFileName;
    }

    /**
     * flush cache by batch
     *
     * @return thread runner
     */
    private Runnable flushCache() {
        return () -> {
            LOGGER.info("start flush cache thread for {} ProxySink", inlongGroupId);
            while (!shutdown) {
                try {
                    cache.forEach((batchKey, packProxyMessage) -> {
                        Pair<String, List<byte[]>> result = packProxyMessage.fetchBatch();
                        if (result != null) {
                            long sendTime = AgentUtils.getCurrentTime();
                            if (syncSend) {
                                senderManager.sendBatchSync(inlongGroupId, result.getKey(), result.getValue(),
                                        0, sendTime, packProxyMessage.getExtraMap());
                            } else {
                                senderManager.sendBatchAsync(jobInstanceId, inlongGroupId, result.getKey(),
                                        result.getValue(), 0, sendTime);
                            }
                            LOGGER.info("send group id {}, message key {},with message size {}, the job id is {}, "
                                            + "read source is {} sendTime is {} syncSend {}", inlongGroupId, batchKey,
                                    result.getRight().size(), jobInstanceId, sourceName, sendTime, syncSend);
                        }

                    });
                    AgentUtils.silenceSleepInMs(batchFlushInterval);
                } catch (Exception ex) {
                    LOGGER.error("error caught", ex);
                } catch (Throwable t) {
                    ThreadUtils.threadThrowableHandler(Thread.currentThread(), t);
                }
            }
        };
    }

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        syncSend = jobConf.getBoolean(PROXY_SEND_SYNC, false);
        maxBatchSize = jobConf.getInt(PROXY_PACKAGE_MAX_SIZE, DEFAULT_PROXY_PACKAGE_MAX_SIZE);
        maxQueueNumber = jobConf.getInt(PROXY_INLONG_STREAM_ID_QUEUE_MAX_NUMBER,
                DEFAULT_PROXY_INLONG_STREAM_ID_QUEUE_MAX_NUMBER);
        maxBatchTimeoutMs = jobConf.getInt(
                PROXY_PACKAGE_MAX_TIMEOUT_MS, DEFAULT_PROXY_PACKAGE_MAX_TIMEOUT_MS);
        jobInstanceId = jobConf.get(JOB_INSTANCE_ID);
        batchFlushInterval = jobConf.getInt(PROXY_BATCH_FLUSH_INTERVAL,
                DEFAULT_PROXY_BATCH_FLUSH_INTERVAL);
        cache = new ConcurrentHashMap<>(10);
        messageFilter = initMessageFilter(jobConf);
        fieldSplitter = jobConf.get(CommonConstants.FIELD_SPLITTER, DEFAULT_FIELD_SPLITTER).getBytes(
                StandardCharsets.UTF_8);
        executorService.execute(flushCache());
        senderManager = new SenderManager(jobConf, inlongGroupId, sourceName);
        try {
            senderManager.addMessageSender();
        } catch (Throwable ex) {
            LOGGER.error("error while init sender for group id {}", inlongGroupId);
            ThreadUtils.threadThrowableHandler(Thread.currentThread(), ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy sink which sink from source name {}", sourceName);
        while (!sinkFinish()) {
            LOGGER.info("job {} wait until cache all flushed to proxy", jobInstanceId);
            AgentUtils.silenceSleepInMs(batchFlushInterval);
        }
        shutdown = true;
        executorService.shutdown();
    }

    /**
     * check whether all stream id messages finished
     */
    private boolean sinkFinish() {
        return cache.values().stream().allMatch(PackProxyMessage::isEmpty);
    }
}
