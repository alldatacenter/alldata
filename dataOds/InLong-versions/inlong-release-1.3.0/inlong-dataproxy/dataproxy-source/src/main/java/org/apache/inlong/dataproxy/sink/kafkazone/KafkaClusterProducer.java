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

package org.apache.inlong.dataproxy.sink.kafkazone;

import org.apache.flume.Context;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.apache.inlong.dataproxy.dispatch.DispatchProfile;
import org.apache.inlong.sdk.commons.protocol.EventConstants;
import org.apache.inlong.sdk.commons.protocol.EventUtils;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.inlong.sdk.commons.protocol.EventConstants.HEADER_CACHE_VERSION_1;
import static org.apache.inlong.sdk.commons.protocol.EventConstants.HEADER_KEY_VERSION;

/**
 * KafkaClusterProducer
 */
public class KafkaClusterProducer implements LifecycleAware {

    public static final Logger LOG = LoggerFactory.getLogger(KafkaClusterProducer.class);

    protected final String workerName;
    private final CacheClusterConfig config;
    private final KafkaZoneSinkContext sinkContext;
    private final Context producerContext;
    private final String cacheClusterName;
    private LifecycleState state;

    // kafka producer
    private KafkaProducer<String, byte[]> producer;

    /**
     * Constructor
     * 
     * @param workerName
     * @param config
     * @param context
     */
    public KafkaClusterProducer(String workerName, CacheClusterConfig config, KafkaZoneSinkContext context) {
        this.workerName = workerName;
        this.config = config;
        this.sinkContext = context;
        this.producerContext = context.getProducerContext();
        this.state = LifecycleState.IDLE;
        this.cacheClusterName = config.getClusterName();
    }

    /**
     * start
     */
    @Override
    public void start() {
        this.state = LifecycleState.START;
        // create kafka producer
        try {
            // prepare configuration
            Properties props = new Properties();
            props.putAll(this.producerContext.getParameters());
            props.putAll(config.getParams());
            LOG.info("try to create kafka client:{}", props);
            producer = new KafkaProducer<>(props, new StringSerializer(), new ByteArraySerializer());
            LOG.info("create new producer success:{}", producer);
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        this.state = LifecycleState.STOP;
        // kafka producer
        this.producer.close();
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
            String topic = sinkContext.getIdTopicHolder().getTopic(event.getUid());
            if (topic == null) {
                sinkContext.addSendResultMetric(event, event.getUid(), false, 0);
                return false;
            }
            // create producer failed
            if (producer == null) {
                sinkContext.processSendFail(event, topic, 0);
                return false;
            }
            // headers
            Map<String, String> headers = this.encodeCacheMessageHeaders(event);
            // compress
            byte[] bodyBytes = EventUtils.encodeCacheMessageBody(sinkContext.getCompressType(), event.getEvents());
            // sendAsync
            long sendTime = System.currentTimeMillis();

            // prepare ProducerRecord
            ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(topic, bodyBytes);
            // add headers
            headers.forEach((key, value) -> {
                producerRecord.headers().add(key, value.getBytes());
            });

            // callback
            Callback callback = new Callback() {

                @Override
                public void onCompletion(RecordMetadata arg0, Exception ex) {
                    if (ex != null) {
                        LOG.error("Send fail:{}", ex.getMessage());
                        LOG.error(ex.getMessage(), ex);
                        if (event.isResend()) {
                            sinkContext.processSendFail(event, topic, sendTime);
                        } else {
                            event.fail();
                        }
                    } else {
                        sinkContext.addSendResultMetric(event, topic, true, sendTime);
                        event.ack();
                    }
                }
            };
            producer.send(producerRecord, callback);
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
