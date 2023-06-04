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

package org.apache.inlong.dataproxy.sink.mq.pulsar;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Context;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.apache.inlong.dataproxy.config.pojo.IdTopicConfig;
import org.apache.inlong.dataproxy.sink.common.EventHandler;
import org.apache.inlong.dataproxy.sink.mq.BatchPackProfile;
import org.apache.inlong.dataproxy.sink.mq.MessageQueueHandler;
import org.apache.inlong.dataproxy.sink.mq.MessageQueueZoneSinkContext;
import org.apache.inlong.dataproxy.sink.mq.OrderBatchPackProfileV0;
import org.apache.inlong.dataproxy.sink.mq.SimpleBatchPackProfileV0;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SizeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.dataproxy.consts.ConfigConstants.KEY_STATS_INTERVAL_SECONDS;

/**
 * PulsarHandler
 */
public class PulsarHandler implements MessageQueueHandler {

    public static final Logger LOG = LoggerFactory.getLogger(PulsarHandler.class);

    public static final String KEY_TENANT = "tenant";
    public static final String KEY_NAMESPACE = "namespace";

    public static final String KEY_SERVICE_URL = "serviceUrl";
    public static final String KEY_AUTHENTICATION = "authentication";

    public static final String KEY_ENABLEBATCHING = "enableBatching";
    public static final String KEY_BATCHINGMAXBYTES = "batchingMaxBytes";
    public static final String KEY_BATCHINGMAXMESSAGES = "batchingMaxMessages";
    public static final String KEY_BATCHINGMAXPUBLISHDELAY = "batchingMaxPublishDelay";
    public static final String KEY_MAXPENDINGMESSAGES = "maxPendingMessages";
    public static final String KEY_MAXPENDINGMESSAGESACROSSPARTITIONS = "maxPendingMessagesAcrossPartitions";
    public static final String KEY_SENDTIMEOUT = "sendTimeout";
    public static final String KEY_COMPRESSIONTYPE = "compressionType";
    public static final String KEY_BLOCKIFQUEUEFULL = "blockIfQueueFull";
    public static final String KEY_ROUNDROBINROUTERBATCHINGPARTITIONSWITCHFREQUENCY = "roundRobinRouter"
            + "BatchingPartitionSwitchFrequency";

    public static final String KEY_IOTHREADS = "ioThreads";
    public static final String KEY_MEMORYLIMIT = "memoryLimit";
    public static final String KEY_CONNECTIONSPERBROKER = "connectionsPerBroker";

    private CacheClusterConfig config;
    private String clusterName;
    private MessageQueueZoneSinkContext sinkContext;

    private String tenant;
    private String namespace;
    private ThreadLocal<EventHandler> handlerLocal = new ThreadLocal<>();

    /**
     * pulsar client
     */
    private PulsarClient client;
    private ProducerBuilder<byte[]> baseBuilder;

    private ConcurrentHashMap<String, Producer<byte[]>> producerMap = new ConcurrentHashMap<>();

    /**
     * init
     * @param config
     * @param sinkContext
     */
    public void init(CacheClusterConfig config, MessageQueueZoneSinkContext sinkContext) {
        this.config = config;
        this.clusterName = config.getClusterName();
        this.sinkContext = sinkContext;
        this.tenant = config.getParams().get(KEY_TENANT);
        this.namespace = config.getParams().get(KEY_NAMESPACE);
    }

    /**
     * start
     */
    @Override
    public void start() {
        // create pulsar client
        try {
            String serviceUrl = config.getParams().get(KEY_SERVICE_URL);
            String authentication = config.getParams().get(KEY_AUTHENTICATION);
            if (StringUtils.isEmpty(authentication)) {
                authentication = config.getToken();
            }
            Context context = sinkContext.getProducerContext();
            ClientBuilder builder = PulsarClient.builder();
            if (StringUtils.isNotEmpty(authentication)) {
                builder.authentication(AuthenticationFactory.token(authentication));
            }
            this.client = builder
                    .serviceUrl(serviceUrl)
                    .ioThreads(context.getInteger(KEY_IOTHREADS, 1))
                    .memoryLimit(context.getLong(KEY_MEMORYLIMIT, 1073741824L), SizeUnit.BYTES)
                    .connectionsPerBroker(context.getInteger(KEY_CONNECTIONSPERBROKER, 10))
                    .statsInterval(NumberUtils.toLong(config.getParams().get(KEY_STATS_INTERVAL_SECONDS), -1),
                            TimeUnit.SECONDS)
                    .build();
            this.baseBuilder = client.newProducer();
            // Map<String, Object> builderConf = new HashMap<>();
            // builderConf.putAll(context.getParameters());
            this.baseBuilder
                    .sendTimeout(context.getInteger(KEY_SENDTIMEOUT, 0), TimeUnit.MILLISECONDS)
                    .maxPendingMessages(context.getInteger(KEY_MAXPENDINGMESSAGES, 500))
                    .maxPendingMessagesAcrossPartitions(
                            context.getInteger(KEY_MAXPENDINGMESSAGESACROSSPARTITIONS, 60000));
            this.baseBuilder
                    .batchingMaxMessages(context.getInteger(KEY_BATCHINGMAXMESSAGES, 500))
                    .batchingMaxPublishDelay(context.getInteger(KEY_BATCHINGMAXPUBLISHDELAY, 100),
                            TimeUnit.MILLISECONDS)
                    .batchingMaxBytes(context.getInteger(KEY_BATCHINGMAXBYTES, 131072));
            this.baseBuilder
                    .accessMode(ProducerAccessMode.Shared)
                    .messageRoutingMode(MessageRoutingMode.RoundRobinPartition)
                    .blockIfQueueFull(context.getBoolean(KEY_BLOCKIFQUEUEFULL, true));
            this.baseBuilder
                    .roundRobinRouterBatchingPartitionSwitchFrequency(
                            context.getInteger(KEY_ROUNDROBINROUTERBATCHINGPARTITIONSWITCHFREQUENCY, 60))
                    .enableBatching(context.getBoolean(KEY_ENABLEBATCHING, true))
                    .compressionType(this.getPulsarCompressionType());
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info("pulsar handler started");
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        for (Entry<String, Producer<byte[]>> entry : this.producerMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (PulsarClientException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        try {
            this.client.close();
        } catch (PulsarClientException e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info("pulsar handler stopped");
    }

    /**
     * send
     * @param event
     * @return
     */
    @Override
    public boolean send(BatchPackProfile event) {
        try {
            // idConfig
            IdTopicConfig idConfig = sinkContext.getIdTopicHolder().getIdConfig(event.getUid());
            if (idConfig == null) {
                sinkContext.addSendResultMetric(event, clusterName, event.getUid(), false, 0);
                sinkContext.getDispatchQueue().release(event.getSize());
                return false;
            }
            String baseTopic = idConfig.getTopicName();
            if (baseTopic == null) {
                sinkContext.addSendResultMetric(event, clusterName, event.getUid(), false, 0);
                sinkContext.getDispatchQueue().release(event.getSize());
                return false;
            }
            // topic
            String producerTopic = this.getProducerTopic(baseTopic, idConfig);
            if (producerTopic == null) {
                sinkContext.addSendResultMetric(event, clusterName, event.getUid(), false, 0);
                sinkContext.getDispatchQueue().release(event.getSize());
                event.fail();
                return false;
            }
            // get producer
            Producer<byte[]> producer = this.producerMap.get(producerTopic);
            if (producer == null) {
                try {
                    LOG.info("try to new a object for topic " + producerTopic);
                    SecureRandom secureRandom = new SecureRandom(
                            (producerTopic + System.currentTimeMillis()).getBytes());
                    String producerName = producerTopic + "-" + secureRandom.nextLong();
                    producer = baseBuilder.clone().topic(producerTopic)
                            .producerName(producerName)
                            .create();
                    LOG.info("create new producer success:{}", producer.getProducerName());
                    Producer<byte[]> oldProducer = this.producerMap.putIfAbsent(producerTopic, producer);
                    if (oldProducer != null) {
                        producer.close();
                        LOG.info("close producer success:{}", producer.getProducerName());
                        producer = oldProducer;
                    }
                } catch (Throwable ex) {
                    LOG.error("create new producer failed", ex);
                }
            }
            // create producer failed
            if (producer == null) {
                sinkContext.processSendFail(event, clusterName, producerTopic, 0);
                return false;
            }
            // send
            if (event instanceof SimpleBatchPackProfileV0) {
                this.sendSimpleProfileV0((SimpleBatchPackProfileV0) event, idConfig, producer, producerTopic);
            } else if (event instanceof OrderBatchPackProfileV0) {
                this.sendOrderProfileV0((OrderBatchPackProfileV0) event, idConfig, producer, producerTopic);
            } else {
                this.sendProfileV1(event, idConfig, producer, producerTopic);
            }
            return true;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            sinkContext.processSendFail(event, clusterName, event.getUid(), 0);
            return false;
        }
    }

    /**
     * getProducerTopic
     */
    private String getProducerTopic(String baseTopic, IdTopicConfig config) {
        StringBuilder builder = new StringBuilder();
        if (tenant != null) {
            builder.append(tenant).append("/");
        }
        String namespace = this.namespace;
        if (namespace == null) {
            namespace = config.getParams().get(PulsarHandler.KEY_NAMESPACE);
        }
        if (namespace != null) {
            builder.append(namespace).append("/");
        }
        builder.append(baseTopic);
        return builder.toString();
    }

    /**
     * getPulsarCompressionType
     * 
     * @return CompressionType
     */
    private CompressionType getPulsarCompressionType() {
        Context context = sinkContext.getProducerContext();
        String type = context.getString(KEY_COMPRESSIONTYPE, CompressionType.SNAPPY.name());
        switch (type) {
            case "LZ4":
                return CompressionType.LZ4;
            case "ZLIB":
                return CompressionType.ZLIB;
            case "ZSTD":
                return CompressionType.ZSTD;
            case "SNAPPY":
                return CompressionType.SNAPPY;
            case "NONE":
            default:
                return CompressionType.NONE;
        }
    }

    /**
     * sendProfileV1
     */
    private void sendProfileV1(BatchPackProfile event, IdTopicConfig idConfig, Producer<byte[]> producer,
            String producerTopic) throws Exception {
        EventHandler handler = handlerLocal.get();
        if (handler == null) {
            handler = this.sinkContext.createEventHandler();
            handlerLocal.set(handler);
        }
        // headers
        Map<String, String> headers = handler.parseHeader(idConfig, event, sinkContext.getNodeId(),
                sinkContext.getCompressType());
        // compress
        byte[] bodyBytes = handler.parseBody(idConfig, event, sinkContext.getCompressType());
        // metric
        sinkContext.addSendMetric(event, clusterName, producerTopic, bodyBytes.length);
        // sendAsync
        long sendTime = System.currentTimeMillis();
        CompletableFuture<MessageId> future = producer.newMessage().properties(headers)
                .value(bodyBytes).sendAsync();
        // callback
        future.whenCompleteAsync((msgId, ex) -> {
            if (ex != null) {
                LOG.error("Send fail:{}", ex.getMessage());
                LOG.error(ex.getMessage(), ex);
                sinkContext.processSendFail(event, clusterName, producerTopic, sendTime);
            } else {
                sinkContext.addSendResultMetric(event, clusterName, producerTopic, true, sendTime);
                sinkContext.getDispatchQueue().release(event.getSize());
                event.ack();
            }
        });
    }

    /**
     * sendSimpleProfileV0
     */
    private void sendSimpleProfileV0(SimpleBatchPackProfileV0 event, IdTopicConfig idConfig,
            Producer<byte[]> producer,
            String producerTopic) throws Exception {
        // headers
        Map<String, String> headers = event.getProperties();
        if (MapUtils.isEmpty(headers)) {
            headers = event.getSimpleProfile().getHeaders();
        }
        // body
        byte[] bodyBytes = event.getSimpleProfile().getBody();
        // metric
        sinkContext.addSendMetric(event, clusterName, producerTopic, bodyBytes.length);
        // sendAsync
        long sendTime = System.currentTimeMillis();
        CompletableFuture<MessageId> future = producer.newMessage().properties(headers)
                .value(bodyBytes).sendAsync();
        // callback
        future.whenCompleteAsync((msgId, ex) -> {
            if (ex != null) {
                LOG.error("Send fail:{}", ex.getMessage());
                LOG.error(ex.getMessage(), ex);
                sinkContext.processSendFail(event, clusterName, producerTopic, sendTime);
            } else {
                sinkContext.addSendResultMetric(event, clusterName, producerTopic, true, sendTime);
                sinkContext.getDispatchQueue().release(event.getSize());
                event.ack();
            }
        });
    }

    /**
     * sendOrderProfileV0
     */
    private void sendOrderProfileV0(OrderBatchPackProfileV0 event, IdTopicConfig idConfig, Producer<byte[]> producer,
            String producerTopic) throws Exception {
        // headers
        Map<String, String> headers = event.getOrderProfile().getHeaders();
        // compress
        byte[] bodyBytes = event.getOrderProfile().getBody();
        // metric
        sinkContext.addSendMetric(event, clusterName, producerTopic, bodyBytes.length);
        // sendAsync
        long sendTime = System.currentTimeMillis();
        CompletableFuture<MessageId> future = producer.newMessage().properties(headers)
                .value(bodyBytes).sendAsync();
        // callback
        future.whenCompleteAsync((msgId, ex) -> {
            if (ex != null) {
                LOG.error("Send fail:{}", ex.getMessage());
                LOG.error(ex.getMessage(), ex);
                sinkContext.processSendFail(event, clusterName, producerTopic, sendTime);
            } else {
                sinkContext.addSendResultMetric(event, clusterName, producerTopic, true, sendTime);
                sinkContext.getDispatchQueue().release(event.getSize());
                event.ack();
                event.ackOrder();
            }
        });
    }
}
