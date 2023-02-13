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

import com.google.common.base.Preconditions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.common.AgentThreadFactory;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.core.task.TaskPositionManager;
import org.apache.inlong.agent.message.BatchProxyMessage;
import org.apache.inlong.agent.message.EndMessage;
import org.apache.inlong.agent.message.PackProxyMessage;
import org.apache.inlong.agent.message.ProxyMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.ThreadUtils;
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.common.pojo.dataproxy.MQClusterInfo;
import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_BLOCK_IF_QUEUE_FULL;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_COMPRESSION_TYPE;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_ENABLE_ASYNC_SEND;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_MAX_BATCH_BYTES;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_MAX_BATCH_INTERVAL_MILLIS;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_MAX_BATCH_MESSAGES;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_MAX_PENDING_MESSAGES;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_MAX_PENDING_MESSAGES_ACROSS_PARTITION;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_PRODUCER_NUM;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_PULSAR_CLIENT_ENABLE_BATCH;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_PULSAR_CLIENT_IO_TREHAD_NUM;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_PULSAR_CLIENT_TIMEOUT_SECOND;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_PULSAR_CONNECTION_PRE_BROKER;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_SEND_QUEUE_SIZE;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_BLOCK_IF_QUEUE_FULL;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_COMPRESSION_TYPE;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_ENABLE_ASYNC_SEND;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_ENABLE_BATCH;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_IO_TREHAD_NUM;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_MAX_BATCH_BYTES;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_MAX_BATCH_INTERVAL_MILLIS;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_MAX_BATCH_MESSAGES;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_MAX_PENDING_MESSAGES;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_MAX_PENDING_MESSAGES_ACROSS_PARTITION;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_PRODUCER_NUM;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CLIENT_TIMEOUT_SECOND;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_CONNECTION_PRE_BROKER;
import static org.apache.inlong.agent.constant.AgentConstants.PULSAR_SINK_SEND_QUEUE_SIZE;

public class PulsarSink extends AbstractSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerFactory.class);
    private static final AtomicInteger CLIENT_INDEX = new AtomicInteger(0);
    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new AgentThreadFactory("PulsarSink"));
    private final AgentConfiguration agentConf = AgentConfiguration.getAgentConf();
    private TaskPositionManager taskPositionManager;
    private volatile boolean shutdown = false;
    private List<MQClusterInfo> mqClusterInfos;
    private String topic;
    private List<PulsarTopicSender> pulsarSenders;
    private int clientIoThreads;

    private int sendQueueSize;
    private Semaphore sendQueueSemaphore; // limit the count of batchProxyMessage waiting to be sent
    private LinkedBlockingQueue<BatchProxyMessage> pulsarSendQueue;

    // pulsar client parameters
    private int connectionsPreBroker;
    private boolean enableBatch;
    private boolean blockIfQueueFull;
    private int maxPendingMessages;
    private int maxPendingMessagesAcrossPartitions;
    private CompressionType compressionType;
    private int maxBatchingBytes;
    private int maxBatchingMessages;
    private long maxBatchingPublishDelayMillis;
    private int sendTimeoutSecond;
    private int producerNum;
    private boolean asyncSend;

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        taskPositionManager = TaskPositionManager.getInstance();
        // agentConf
        sendQueueSize = agentConf.getInt(PULSAR_SINK_SEND_QUEUE_SIZE, DEFAULT_SEND_QUEUE_SIZE);
        sendQueueSemaphore = new Semaphore(sendQueueSize);
        pulsarSendQueue = new LinkedBlockingQueue<>(sendQueueSize);
        clientIoThreads = agentConf.getInt(PULSAR_CLIENT_IO_TREHAD_NUM, DEFAULT_PULSAR_CLIENT_IO_TREHAD_NUM);
        connectionsPreBroker = agentConf.getInt(PULSAR_CONNECTION_PRE_BROKER, DEFAULT_PULSAR_CONNECTION_PRE_BROKER);
        sendTimeoutSecond = agentConf.getInt(PULSAR_CLIENT_TIMEOUT_SECOND, DEFAULT_PULSAR_CLIENT_TIMEOUT_SECOND);
        enableBatch = agentConf.getBoolean(PULSAR_CLIENT_ENABLE_BATCH, DEFAULT_PULSAR_CLIENT_ENABLE_BATCH);
        blockIfQueueFull = agentConf.getBoolean(PULSAR_CLIENT_BLOCK_IF_QUEUE_FULL, DEFAULT_BLOCK_IF_QUEUE_FULL);
        maxPendingMessages = agentConf.getInt(PULSAR_CLIENT_MAX_PENDING_MESSAGES, DEFAULT_MAX_PENDING_MESSAGES);
        maxBatchingBytes = agentConf.getInt(PULSAR_CLIENT_MAX_BATCH_BYTES, DEFAULT_MAX_BATCH_BYTES);
        maxBatchingMessages = agentConf.getInt(PULSAR_CLIENT_MAX_BATCH_MESSAGES, DEFAULT_MAX_BATCH_MESSAGES);
        maxBatchingPublishDelayMillis = agentConf.getInt(PULSAR_CLIENT_MAX_BATCH_INTERVAL_MILLIS,
                DEFAULT_MAX_BATCH_INTERVAL_MILLIS);
        maxPendingMessagesAcrossPartitions = agentConf.getInt(PULSAR_CLIENT_MAX_PENDING_MESSAGES_ACROSS_PARTITION,
                DEFAULT_MAX_PENDING_MESSAGES_ACROSS_PARTITION);
        producerNum = agentConf.getInt(PULSAR_CLIENT_PRODUCER_NUM, DEFAULT_PRODUCER_NUM);
        asyncSend = agentConf.getBoolean(PULSAR_CLIENT_ENABLE_ASYNC_SEND, DEFAULT_ENABLE_ASYNC_SEND);
        String compresstion = agentConf.get(PULSAR_CLIENT_COMPRESSION_TYPE, DEFAULT_COMPRESSION_TYPE);
        if (StringUtils.isNotEmpty(compresstion)) {
            compressionType = CompressionType.valueOf(compresstion);
        } else {
            compressionType = CompressionType.NONE;
        }
        // jobConf
        mqClusterInfos = jobConf.getMqClusters();
        Preconditions.checkArgument(ObjectUtils.isNotEmpty(jobConf.getMqTopic()) && jobConf.getMqTopic().isValid(),
                "no valid pulsar topic config");
        topic = jobConf.getMqTopic().getTopic();
        pulsarSenders = new ArrayList<>();
        initPulsarSender();
        EXECUTOR_SERVICE.execute(sendDataThread());
        EXECUTOR_SERVICE.execute(flushCache());
    }

    @Override
    public void write(Message message) {
        try {
            if (message != null) {
                if (!(message instanceof EndMessage)) {
                    ProxyMessage proxyMessage = new ProxyMessage(message);
                    // add proxy message to cache.
                    cache.compute(proxyMessage.getBatchKey(),
                            (s, packProxyMessage) -> {
                                if (packProxyMessage == null) {
                                    packProxyMessage =
                                            new PackProxyMessage(jobInstanceId, jobConf, inlongGroupId, inlongStreamId);
                                    packProxyMessage.generateExtraMap(proxyMessage.getDataKey());
                                    packProxyMessage.addTopicAndDataTime(topic, System.currentTimeMillis());
                                }
                                // add message to package proxy
                                packProxyMessage.addProxyMessage(proxyMessage);
                                return packProxyMessage;
                            });
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

    @Override
    public void destroy() {
        LOGGER.info("destroy pulsar sink, job[{}], source[{}]", jobInstanceId, sourceName);
        while (!sinkFinish()) {
            LOGGER.info("job {} wait until cache all data to pulsar", jobInstanceId);
            AgentUtils.silenceSleepInMs(batchFlushInterval);
        }
        shutdown = true;
        EXECUTOR_SERVICE.shutdown();
        if (CollectionUtils.isNotEmpty(pulsarSenders)) {
            for (PulsarTopicSender sender : pulsarSenders) {
                sender.close();
            }
            pulsarSenders.clear();
        }
    }

    private boolean sinkFinish() {
        return cache.values().stream().allMatch(PackProxyMessage::isEmpty) && pulsarSendQueue.isEmpty();
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
                        BatchProxyMessage batchProxyMessage = packProxyMessage.fetchBatch();
                        if (batchProxyMessage != null) {
                            try {
                                sendQueueSemaphore.acquire();
                                pulsarSendQueue.put(batchProxyMessage);
                                LOGGER.info("send group id {}, message key {},with message size {}, the job id is {}, "
                                        + "read source is {} sendTime is {}", inlongGroupId, batchKey,
                                        batchProxyMessage.getDataList().size(), jobInstanceId, sourceName,
                                        batchProxyMessage.getDataTime());
                            } catch (Exception e) {
                                sendQueueSemaphore.release();
                                LOGGER.error("flush data to send queue", e);
                            }
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

    /**
     * take batchMsg from sendQueue and send to pulsar
     */
    private Runnable sendDataThread() {
        return () -> {
            LOGGER.info("start pulsar sink send data thread, job[{}], groupId[{}]", jobInstanceId, inlongGroupId);
            while (!shutdown) {
                try {
                    BatchProxyMessage data = pulsarSendQueue.poll(1, TimeUnit.MILLISECONDS);
                    if (ObjectUtils.isEmpty(data)) {
                        continue;
                    }
                    sendData(data);
                } catch (Throwable t) {
                    LOGGER.error("Send data error", t);
                }
            }
        };
    }

    private void sendData(BatchProxyMessage batchMsg) throws InterruptedException {
        if (ObjectUtils.isEmpty(batchMsg)) {
            return;
        }

        Producer producer = selectProducer();
        if (ObjectUtils.isEmpty(producer)) {
            pulsarSendQueue.put(batchMsg);
            LOGGER.error("send job[{}] data err, empty pulsar producer", jobInstanceId);
            return;
        }
        InLongMsg message = batchMsg.getInLongMsg();
        sinkMetric.pluginSendCount.addAndGet(batchMsg.getMsgCnt());
        if (asyncSend) {
            CompletableFuture<MessageId> future = producer.newMessage().eventTime(batchMsg.getDataTime())
                    .value(message.buildArray()).sendAsync();
            future.whenCompleteAsync((m, t) -> {
                if (t != null) {
                    // send error
                    sinkMetric.pluginSendFailCount.addAndGet(batchMsg.getMsgCnt());
                    LOGGER.error("send data fail to pulsar, add back to sendqueue, current queue size {}",
                            pulsarSendQueue.size(), t);
                    try {
                        pulsarSendQueue.put(batchMsg);
                    } catch (InterruptedException e) {
                        LOGGER.error("put back to queue fail send queue size {}", pulsarSendQueue.size(), t);
                    }
                } else {
                    // send succerss, update metrics
                    sendQueueSemaphore.release();
                    updateSuccessSendMetrics(batchMsg);
                }
            });

        } else {
            try {
                producer.newMessage().eventTime(batchMsg.getDataTime()).value(message.buildArray()).send();
                sendQueueSemaphore.release();
                updateSuccessSendMetrics(batchMsg);
            } catch (PulsarClientException e) {
                sinkMetric.pluginSendFailCount.addAndGet(batchMsg.getMsgCnt());
                LOGGER.error("send data fail to pulsar, add back to send queue, send queue size {}",
                        pulsarSendQueue.size(), e);
                pulsarSendQueue.put(batchMsg);
            }
        }
    }

    private void updateSuccessSendMetrics(BatchProxyMessage batchMsg) {
        AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_SEND_SUCCESS, batchMsg.getGroupId(),
                batchMsg.getStreamId(), batchMsg.getDataTime(), batchMsg.getMsgCnt(),
                batchMsg.getTotalSize());
        sinkMetric.pluginSendSuccessCount.addAndGet(batchMsg.getMsgCnt());
        if (sourceName != null) {
            taskPositionManager.updateSinkPosition(batchMsg, sourceName, batchMsg.getMsgCnt());
        }
    }

    private Producer selectProducer() {
        if (CollectionUtils.isEmpty(pulsarSenders)) {
            LOGGER.error("send job[{}] data err, empty pulsar sender", jobInstanceId);
            return null;
        }
        PulsarTopicSender sender = pulsarSenders.get(
                (CLIENT_INDEX.getAndIncrement() & Integer.MAX_VALUE) % pulsarSenders.size());
        return sender.getProducer();
    }

    private void initPulsarSender() {
        if (CollectionUtils.isEmpty(mqClusterInfos)) {
            LOGGER.error("init job[{}] pulsar client fail, empty mqCluster info", jobInstanceId);
            return;
        }
        for (MQClusterInfo clusterInfo : mqClusterInfos) {
            if (clusterInfo.isValid()) {
                try {
                    PulsarClient client = PulsarClient.builder().serviceUrl(clusterInfo.getUrl())
                            .ioThreads(clientIoThreads)
                            .connectionsPerBroker(connectionsPreBroker).build();
                    pulsarSenders.add(new PulsarTopicSender(client, producerNum));
                    LOGGER.info("job[{}] init pulsar client url={}", jobInstanceId, clusterInfo.getUrl());
                } catch (PulsarClientException e) {
                    LOGGER.error("init job[{}] pulsar client fail", jobInstanceId, e);
                }
            }
        }
    }

    class PulsarTopicSender {

        private final AtomicInteger producerIndex = new AtomicInteger(0);
        private final PulsarClient pulsarClient;
        private List<Producer> producers;

        public PulsarTopicSender(PulsarClient client, int producerNum) {
            pulsarClient = client;
            initProducer(producerNum);
        }

        public Producer getProducer() {
            if (CollectionUtils.isEmpty(producers)) {
                LOGGER.error("job[{}] empty producers", jobInstanceId);
                return null;
            }
            int index = (producerIndex.getAndIncrement() & Integer.MAX_VALUE) % producers.size();
            return producers.get(index);
        }

        /**
         * close all pulsar producer and shutdown client
         */
        public void close() {
            if (CollectionUtils.isEmpty(producers)) {
                return;
            }
            for (Producer producer : producers) {
                try {
                    producer.close();
                } catch (Throwable e) {
                    LOGGER.error("job[{}] close pulsar producer error", jobInstanceId, e);
                }
            }
            try {
                pulsarClient.shutdown();
            } catch (PulsarClientException e) {
                LOGGER.error("job[{}] close pulsar client error", jobInstanceId, e);
            }
        }

        private void initProducer(int producerNum) {
            producers = new ArrayList<>(producerNum);
            for (int i = 0; i < producerNum; i++) {
                producers.add(createProducer());
            }
        }

        private Producer<byte[]> createProducer() {
            try {
                return pulsarClient.newProducer().topic(topic)
                        .sendTimeout(sendTimeoutSecond, TimeUnit.SECONDS)
                        .topic(topic)
                        .enableBatching(enableBatch)
                        .blockIfQueueFull(blockIfQueueFull)
                        .maxPendingMessages(maxPendingMessages)
                        .maxPendingMessagesAcrossPartitions(maxPendingMessagesAcrossPartitions)
                        .compressionType(compressionType)
                        .batchingMaxMessages(maxBatchingMessages)
                        .batchingMaxBytes(maxBatchingBytes)
                        .batchingMaxPublishDelay(maxBatchingPublishDelayMillis, TimeUnit.MILLISECONDS)
                        .create();
            } catch (Throwable e) {
                LOGGER.error("job[{}] create producer[topic:{}] error", jobInstanceId, topic, e);
                return null;
            }
        }

    }
}
