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

import org.apache.inlong.agent.common.AgentThreadFactory;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.CommonConstants;
import org.apache.inlong.agent.core.task.MemoryManager;
import org.apache.inlong.agent.message.BatchProxyMessage;
import org.apache.inlong.agent.message.EndMessage;
import org.apache.inlong.agent.message.PackProxyMessage;
import org.apache.inlong.agent.message.ProxyMessage;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.MessageFilter;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.ThreadUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_FIELD_SPLITTER;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_PACKAGE_MAX_SIZE;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_PACKAGE_MAX_SIZE;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_GLOBAL_CHANNEL_PERMIT;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_GLOBAL_WRITER_PERMIT;

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
    private volatile boolean shutdown = false;
    private int maxPackSize;

    public ProxySink() {
    }

    @Override
    public void write(Message message) {
        if (message == null) {
            return;
        }
        boolean suc = false;
        while (!suc) {
            suc = putInCache(message);
            if (!suc) {
                AgentUtils.silenceSleepInMs(batchFlushInterval);
            }
        }
    }

    private boolean putInCache(Message message) {
        try {
            if (message == null) {
                return true;
            }
            message.getHeader().put(CommonConstants.PROXY_KEY_GROUP_ID, inlongGroupId);
            message.getHeader().put(CommonConstants.PROXY_KEY_STREAM_ID, inlongStreamId);
            extractStreamFromMessage(message, fieldSplitter);
            if (message instanceof EndMessage) {
                // increment the count of failed sinks
                sinkMetric.sinkFailCount.incrementAndGet();
                return true;
            }
            AtomicBoolean suc = new AtomicBoolean(false);
            ProxyMessage proxyMessage = new ProxyMessage(message);
            boolean writerPermitSuc = MemoryManager.getInstance()
                    .tryAcquire(AGENT_GLOBAL_WRITER_PERMIT, message.getBody().length);
            if (!writerPermitSuc) {
                LOGGER.warn("writer tryAcquire failed");
                MemoryManager.getInstance().printDetail(AGENT_GLOBAL_WRITER_PERMIT);
                return false;
            }
            // add proxy message to cache.
            cache.compute(proxyMessage.getBatchKey(),
                    (s, packProxyMessage) -> {
                        if (packProxyMessage == null) {
                            packProxyMessage = new PackProxyMessage(jobInstanceId, jobConf, inlongGroupId,
                                    proxyMessage.getInlongStreamId());
                            packProxyMessage.generateExtraMap(proxyMessage.getDataKey());
                        }
                        // add message to package proxy
                        suc.set(packProxyMessage.addProxyMessage(proxyMessage));
                        return packProxyMessage;
                    });
            if (suc.get()) {
                MemoryManager.getInstance().release(AGENT_GLOBAL_CHANNEL_PERMIT, message.getBody().length);
                // increment the count of successful sinks
                sinkMetric.sinkSuccessCount.incrementAndGet();
            } else {
                MemoryManager.getInstance().release(AGENT_GLOBAL_WRITER_PERMIT, message.getBody().length);
                // increment the count of failed sinks
                sinkMetric.sinkFailCount.incrementAndGet();
            }
            return suc.get();
        } catch (Exception e) {
            LOGGER.error("write message to Proxy sink error", e);
        } catch (Throwable t) {
            ThreadUtils.threadThrowableHandler(Thread.currentThread(), t);
        }
        return false;
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

    /**
     * flush cache by batch
     *
     * @return thread runner
     */
    private Runnable flushCache() {
        return () -> {
            LOGGER.info("start flush cache {}:{}", inlongGroupId, sourceName);
            while (!shutdown) {
                try {
                    cache.forEach((batchKey, packProxyMessage) -> {
                        BatchProxyMessage batchProxyMessage = packProxyMessage.fetchBatch();
                        if (batchProxyMessage != null) {
                            senderManager.sendBatch(batchProxyMessage);
                            LOGGER.info("send group id {}, message key {},with message size {}, the job id is {}, "
                                    + "read source is {} sendTime is {}", inlongGroupId, batchKey,
                                    batchProxyMessage.getDataList().size(), jobInstanceId, sourceName,
                                    batchProxyMessage.getDataTime());
                        }
                    });
                } catch (Exception ex) {
                    LOGGER.error("error caught", ex);
                } catch (Throwable t) {
                    ThreadUtils.threadThrowableHandler(Thread.currentThread(), t);
                } finally {
                    AgentUtils.silenceSleepInMs(batchFlushInterval);
                }
            }
            LOGGER.info("stop flush cache {}:{}", inlongGroupId, sourceName);
        };
    }

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        this.maxPackSize = jobConf.getInt(PROXY_PACKAGE_MAX_SIZE, DEFAULT_PROXY_PACKAGE_MAX_SIZE);
        messageFilter = initMessageFilter(jobConf);
        fieldSplitter = jobConf.get(CommonConstants.FIELD_SPLITTER, DEFAULT_FIELD_SPLITTER).getBytes(
                StandardCharsets.UTF_8);
        executorService.execute(flushCache());
        senderManager = new SenderManager(jobConf, inlongGroupId, sourceName);
        try {
            senderManager.Start();
        } catch (Throwable ex) {
            LOGGER.error("error while init sender for group id {}", inlongGroupId);
            ThreadUtils.threadThrowableHandler(Thread.currentThread(), ex);
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy sink source name {}", sourceName);
        while (!sinkFinish()) {
            LOGGER.info("sourceName {} wait until cache all flushed to proxy", sourceName);
            AgentUtils.silenceSleepInMs(batchFlushInterval);
        }
        shutdown = true;
        executorService.shutdown();
        senderManager.Stop();
        LOGGER.info("destroy sink source name {} end", sourceName);
    }

    /**
     * check whether all stream id messages finished
     */
    private boolean sinkFinish() {
        return cache.values().stream().allMatch(PackProxyMessage::isEmpty);
    }
}
