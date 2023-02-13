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

package org.apache.inlong.sort.standalone.sink.pulsar;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.flume.Context;
import org.apache.flume.Transaction;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.config.pojo.CacheClusterConfig;
import org.apache.inlong.sort.standalone.utils.Constants;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.BatcherBuilder;
import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 
 * PulsarProducerCluster
 */
public class PulsarProducerCluster implements LifecycleAware {

    public static final Logger LOG = InlongLoggerFactory.getLogger(PulsarProducerCluster.class);

    public static final String KEY_SERVICE_URL = "serviceUrl";
    public static final String KEY_AUTHENTICATION = "authentication";
    public static final String KEY_STATS_INTERVAL_SECONDS = "statsIntervalSeconds";

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

    private final String workerName;
    private final CacheClusterConfig config;
    private final PulsarFederationSinkContext sinkContext;
    private final Context context;
    private final String cacheClusterName;
    private LifecycleState state;
    private IEvent2PulsarRecordHandler handler;

    /**
     * pulsar client
     */
    private PulsarClient client;
    private ProducerBuilder<byte[]> baseBuilder;

    private Map<String, Producer<byte[]>> producerMap = new ConcurrentHashMap<>();

    /**
     * Constructor
     * 
     * @param workerName
     * @param config
     * @param context
     */
    public PulsarProducerCluster(String workerName, CacheClusterConfig config, PulsarFederationSinkContext context) {
        this.workerName = workerName;
        this.config = config;
        this.sinkContext = context;
        this.context = context.getProducerContext();
        this.state = LifecycleState.IDLE;
        this.cacheClusterName = config.getClusterName();
        this.handler = sinkContext.createEventHandler();
    }

    /**
     * start
     */
    @Override
    public void start() {
        this.state = LifecycleState.START;
        // create pulsar client
        try {
            String serviceUrl = config.getParams().get(KEY_SERVICE_URL);
            String authentication = config.getParams().get(KEY_AUTHENTICATION);
            this.client = PulsarClient.builder()
                    .serviceUrl(serviceUrl)
                    .authentication(AuthenticationFactory.token(authentication))
                    .statsInterval(NumberUtils.toLong(config.getParams().get(KEY_STATS_INTERVAL_SECONDS), -1),
                            TimeUnit.SECONDS)
                    .build();
            this.baseBuilder = client.newProducer();
            this.baseBuilder
                    .hashingScheme(HashingScheme.Murmur3_32Hash)
                    .enableBatching(context.getBoolean(KEY_ENABLEBATCHING, true))
                    .batchingMaxBytes(context.getInteger(KEY_BATCHINGMAXBYTES, 5242880))
                    .batchingMaxMessages(context.getInteger(KEY_BATCHINGMAXMESSAGES, 3000))
                    .batchingMaxPublishDelay(context.getInteger(KEY_BATCHINGMAXPUBLISHDELAY, 1),
                            TimeUnit.MILLISECONDS);
            this.baseBuilder.maxPendingMessages(context.getInteger(KEY_MAXPENDINGMESSAGES, 1000))
                    .maxPendingMessagesAcrossPartitions(
                            context.getInteger(KEY_MAXPENDINGMESSAGESACROSSPARTITIONS, 50000))
                    .sendTimeout(context.getInteger(KEY_SENDTIMEOUT, 0), TimeUnit.MILLISECONDS)
                    .compressionType(this.getPulsarCompressionType())
                    .blockIfQueueFull(context.getBoolean(KEY_BLOCKIFQUEUEFULL, true))
                    .roundRobinRouterBatchingPartitionSwitchFrequency(
                            context.getInteger(KEY_ROUNDROBINROUTERBATCHINGPARTITIONSWITCHFREQUENCY, 10))
                    .batcherBuilder(BatcherBuilder.DEFAULT);
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * getPulsarCompressionType
     * 
     * @return CompressionType
     */
    private CompressionType getPulsarCompressionType() {
        String type = this.context.getString(KEY_COMPRESSIONTYPE);
        switch (type) {
            case "LZ4":
                return CompressionType.LZ4;
            case "NONE":
                return CompressionType.NONE;
            case "ZLIB":
                return CompressionType.ZLIB;
            case "ZSTD":
                return CompressionType.ZSTD;
            case "SNAPPY":
                return CompressionType.SNAPPY;
            default:
                return CompressionType.NONE;
        }
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        this.state = LifecycleState.STOP;
        // close producer
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
    }

    /**
     * getLifecycleState
     * 
     * @return
     */
    @Override
    public LifecycleState getLifecycleState() {
        return state;
    }

    /**
     * send
     * 
     * @param  profileEvent
     * @param  tx
     * @return              boolean
     * @throws IOException
     */
    public boolean send(ProfileEvent profileEvent, Transaction tx) throws IOException {
        // send
        Map<String, String> headers = profileEvent.getHeaders();
        String topic = headers.get(Constants.TOPIC);
        // get producer
        Producer<byte[]> producer = this.producerMap.get(topic);
        if (producer == null) {
            try {
                LOG.info("try to new a object for topic " + topic);
                producer = baseBuilder.clone().topic(topic)
                        .producerName(workerName + "-" + cacheClusterName + "-" + topic)
                        .create();
                LOG.info("create new producer success:{}", producer.getProducerName());
                Producer<byte[]> oldProducer = this.producerMap.putIfAbsent(topic, producer);
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
            tx.rollback();
            tx.close();
            sinkContext.addSendResultMetric(profileEvent, topic, false, System.currentTimeMillis());
            return false;
        }
        String messageKey = headers.get(Constants.HEADER_KEY_MESSAGE_KEY);
        if (messageKey == null) {
            messageKey = headers.get(Constants.HEADER_KEY_SOURCE_IP);
        }
        // sendAsync
        byte[] sendBytes = this.handler.parse(sinkContext, profileEvent);
        // check
        if (sendBytes == null) {
            tx.commit();
            profileEvent.ack();
            tx.close();
            return true;
        }
        long sendTime = System.currentTimeMillis();
        CompletableFuture<MessageId> future = producer.newMessage().key(messageKey).properties(headers)
                .value(sendBytes).sendAsync();
        // callback
        future.whenCompleteAsync((msgId, ex) -> {
            if (ex != null) {
                LOG.error("Send fail:{}", ex.getMessage());
                LOG.error(ex.getMessage(), ex);
                tx.rollback();
                tx.close();
                sinkContext.addSendResultMetric(profileEvent, topic, false, sendTime);
            } else {
                tx.commit();
                tx.close();
                sinkContext.addSendResultMetric(profileEvent, topic, true, sendTime);
                profileEvent.ack();
            }
        });
        return true;
    }

    /**
     * get cacheClusterName
     * 
     * @return the cacheClusterName
     */
    public String getCacheClusterName() {
        return cacheClusterName;
    }

}
