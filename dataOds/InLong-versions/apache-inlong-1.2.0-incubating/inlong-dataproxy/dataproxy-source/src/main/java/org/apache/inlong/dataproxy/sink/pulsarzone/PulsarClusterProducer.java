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

package org.apache.inlong.dataproxy.sink.pulsarzone;

import org.apache.flume.Context;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.apache.inlong.dataproxy.dispatch.DispatchProfile;
import org.apache.inlong.sdk.commons.protocol.EventConstants;
import org.apache.inlong.sdk.commons.protocol.EventUtils;
import org.apache.pulsar.client.api.AuthenticationFactory;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.sdk.commons.protocol.EventConstants.HEADER_CACHE_VERSION_1;
import static org.apache.inlong.sdk.commons.protocol.EventConstants.HEADER_KEY_VERSION;

/**
 * PulsarClusterProducer
 */
public class PulsarClusterProducer implements LifecycleAware {

    public static final Logger LOG = LoggerFactory.getLogger(PulsarClusterProducer.class);

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

    private final String workerName;
    private final CacheClusterConfig config;
    private final PulsarZoneSinkContext sinkContext;
    private final Context context;
    private final String cacheClusterName;
    private LifecycleState state;

    private String tenant;
    private String namespace;

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
    public PulsarClusterProducer(String workerName, CacheClusterConfig config, PulsarZoneSinkContext context) {
        this.workerName = workerName;
        this.config = config;
        this.sinkContext = context;
        this.context = context.getProducerContext();
        this.state = LifecycleState.IDLE;
        this.cacheClusterName = config.getClusterName();
        this.tenant = config.getParams().getOrDefault(KEY_TENANT, "pulsar");
        this.namespace = config.getParams().getOrDefault(KEY_NAMESPACE, "inlong");
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
                    .ioThreads(context.getInteger(KEY_IOTHREADS, 1))
                    .memoryLimit(context.getLong(KEY_MEMORYLIMIT, 1073741824L), SizeUnit.BYTES)
                    .connectionsPerBroker(context.getInteger(KEY_CONNECTIONSPERBROKER, 10))
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
    }

    /**
     * getPulsarCompressionType
     * 
     * @return CompressionType
     */
    private CompressionType getPulsarCompressionType() {
        String type = this.context.getString(KEY_COMPRESSIONTYPE, CompressionType.SNAPPY.name());
        switch (type) {
            case "LZ4" :
                return CompressionType.LZ4;
            case "NONE" :
                return CompressionType.NONE;
            case "ZLIB" :
                return CompressionType.ZLIB;
            case "ZSTD" :
                return CompressionType.ZSTD;
            case "SNAPPY" :
                return CompressionType.SNAPPY;
            default :
                return CompressionType.NONE;
        }
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        this.state = LifecycleState.STOP;
        //
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
     * @param event
     */
    public boolean send(DispatchProfile event) {
        try {
            // topic
            String baseTopic = sinkContext.getIdTopicHolder().getTopic(event.getUid());
            if (baseTopic == null) {
                sinkContext.addSendResultMetric(event, event.getUid(), false, 0);
                event.fail();
                return false;
            }
            // get producer
            String producerTopic = tenant + '/' + namespace + '/' + baseTopic;
            Producer<byte[]> producer = this.producerMap.get(producerTopic);
            if (producer == null) {
                try {
                    LOG.info("try to new a object for topic " + producerTopic);
                    SecureRandom secureRandom = new SecureRandom(
                            (workerName + "-" + cacheClusterName + "-" + producerTopic + System.currentTimeMillis())
                                    .getBytes());
                    String producerName = workerName + "-" + cacheClusterName + "-" + producerTopic + "-"
                            + secureRandom.nextLong();
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
                sinkContext.processSendFail(event, producerTopic, 0);
                return false;
            }
            // headers
            Map<String, String> headers = this.encodeCacheMessageHeaders(event);
            // compress
            byte[] bodyBytes = EventUtils.encodeCacheMessageBody(sinkContext.getCompressType(), event.getEvents());
            // sendAsync
            long sendTime = System.currentTimeMillis();
            CompletableFuture<MessageId> future = producer.newMessage().properties(headers)
                    .value(bodyBytes).sendAsync();
            // callback
            future.whenCompleteAsync((msgId, ex) -> {
                if (ex != null) {
                    LOG.error("Send fail:{}", ex.getMessage());
                    LOG.error(ex.getMessage(), ex);
                    sinkContext.processSendFail(event, producerTopic, sendTime);
                } else {
                    sinkContext.addSendResultMetric(event, producerTopic, true, sendTime);
                    event.ack();
                }
            });
            return true;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            sinkContext.processSendFail(event, event.getUid(), 0);
            return false;
        }
    }

    /**
     * encodeCacheMessageHeaders
     * 
     * @param  event
     * @return       Map
     */
    public Map<String, String> encodeCacheMessageHeaders(DispatchProfile event) {
        Map<String, String> headers = new HashMap<>();
        // version int32 protocol version, the value is 1
        headers.put(HEADER_KEY_VERSION, HEADER_CACHE_VERSION_1);
        // inlongGroupId string inlongGroupId
        headers.put(EventConstants.INLONG_GROUP_ID, event.getInlongGroupId());
        // inlongStreamId string inlongStreamId
        headers.put(EventConstants.INLONG_STREAM_ID, event.getInlongStreamId());
        // proxyName string proxy node id, IP or conainer name
        headers.put(EventConstants.HEADER_KEY_PROXY_NAME, sinkContext.getNodeId());
        // packTime int64 pack time, milliseconds
        headers.put(EventConstants.HEADER_KEY_PACK_TIME, String.valueOf(System.currentTimeMillis()));
        // msgCount int32 message count
        headers.put(EventConstants.HEADER_KEY_MSG_COUNT, String.valueOf(event.getEvents().size()));
        // srcLength int32 total length of raw messages body
        headers.put(EventConstants.HEADER_KEY_SRC_LENGTH, String.valueOf(event.getSize()));
        // compressType int
        // compress type of body data
        // INLONG_NO_COMPRESS = 0,
        // INLONG_GZ = 1,
        // INLONG_SNAPPY = 2
        headers.put(EventConstants.HEADER_KEY_COMPRESS_TYPE,
                String.valueOf(sinkContext.getCompressType().getNumber()));
        // messageKey string partition hash key, optional
        return headers;
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
