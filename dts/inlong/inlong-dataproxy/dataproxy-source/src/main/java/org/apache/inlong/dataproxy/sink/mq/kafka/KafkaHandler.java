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

package org.apache.inlong.dataproxy.sink.mq.kafka;

import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.common.monitor.LogCounter;
import org.apache.inlong.dataproxy.config.CommonConfigHolder;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.pojo.CacheClusterConfig;
import org.apache.inlong.dataproxy.config.pojo.IdTopicConfig;
import org.apache.inlong.dataproxy.consts.StatConstants;
import org.apache.inlong.dataproxy.sink.common.EventHandler;
import org.apache.inlong.dataproxy.sink.mq.BatchPackProfile;
import org.apache.inlong.dataproxy.sink.mq.MessageQueueHandler;
import org.apache.inlong.dataproxy.sink.mq.MessageQueueZoneSinkContext;
import org.apache.inlong.dataproxy.sink.mq.PackProfile;
import org.apache.inlong.dataproxy.sink.mq.SimplePackProfile;

import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Context;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * KafkaHandler
 * 
 */
public class KafkaHandler implements MessageQueueHandler {

    private static final Logger logger = LoggerFactory.getLogger(KafkaHandler.class);
    // log print count
    private static final LogCounter logCounter = new LogCounter(10, 100000, 30 * 1000);

    private CacheClusterConfig config;
    private String clusterName;
    private MessageQueueZoneSinkContext sinkContext;

    // kafka producer
    private KafkaProducer<String, byte[]> producer;
    private final ThreadLocal<EventHandler> handlerLocal = new ThreadLocal<>();

    /**
     * init
     * @param config   the kafka cluster configure
     * @param sinkContext   the sink context
     */
    @Override
    public void init(CacheClusterConfig config, MessageQueueZoneSinkContext sinkContext) {
        this.config = config;
        this.clusterName = config.getClusterName();
        this.sinkContext = sinkContext;
    }

    /**
     * start
     */
    @Override
    public void start() {
        // create kafka producer
        try {
            // prepare configuration
            Properties props = new Properties();
            Context context = this.sinkContext.getProducerContext();
            props.putAll(context.getParameters());
            props.putAll(config.getParams());
            logger.info("try to create kafka client:{}", props);
            producer = new KafkaProducer<>(props, new StringSerializer(), new ByteArraySerializer());
            logger.info("create new producer success:{}", producer);
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void publishTopic(Set<String> topicSet) {
        //
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        // kafka producer
        this.producer.close();
        logger.info("kafka handler stopped");
    }

    /**
     * send  message to mq
     * @param profile the profile need to send
     * @return whether sent the profile
     */
    @Override
    public boolean send(PackProfile profile) {
        String topic = null;
        try {
            // get idConfig
            IdTopicConfig idConfig = ConfigManager.getInstance().getIdTopicConfig(
                    profile.getInlongGroupId(), profile.getInlongStreamId());
            if (idConfig == null) {
                if (!CommonConfigHolder.getInstance().isEnableUnConfigTopicAccept()) {
                    sinkContext.fileMetricIncWithDetailStats(
                            StatConstants.EVENT_SINK_CONFIG_TOPIC_MISSING, profile.getUid());
                    sinkContext.addSendResultMetric(profile, clusterName, profile.getUid(), false, 0);
                    sinkContext.getMqZoneSink().releaseAcquiredSizePermit(profile);
                    profile.fail(DataProxyErrCode.GROUPID_OR_STREAMID_NOT_CONFIGURE, "");
                    return false;
                }
                topic = CommonConfigHolder.getInstance().getRandDefTopics();
                if (StringUtils.isEmpty(topic)) {
                    sinkContext.fileMetricIncSumStats(StatConstants.EVENT_SINK_DEFAULT_TOPIC_MISSING);
                    sinkContext.addSendResultMetric(profile, clusterName, profile.getUid(), false, 0);
                    sinkContext.getMqZoneSink().releaseAcquiredSizePermit(profile);
                    profile.fail(DataProxyErrCode.GROUPID_OR_STREAMID_NOT_CONFIGURE, "");
                    return false;
                }
                sinkContext.fileMetricIncSumStats(StatConstants.EVENT_SINK_DEFAULT_TOPIC_USED);
            } else {
                topic = idConfig.getTopicName();
            }
            // create producer failed
            if (producer == null) {
                sinkContext.fileMetricIncWithDetailStats(StatConstants.EVENT_SINK_PRODUCER_NULL, topic);
                sinkContext.processSendFail(profile, clusterName, topic, 0, DataProxyErrCode.PRODUCER_IS_NULL, "");
                return false;
            }
            // send
            if (profile instanceof SimplePackProfile) {
                this.sendSimplePackProfile((SimplePackProfile) profile, idConfig, topic);
            } else {
                this.sendBatchPackProfile((BatchPackProfile) profile, idConfig, topic);
            }
            return true;
        } catch (Exception ex) {
            sinkContext.fileMetricIncWithDetailStats(StatConstants.EVENT_SINK_SEND_EXCEPTION, topic);
            sinkContext.processSendFail(profile, clusterName, profile.getUid(), 0,
                    DataProxyErrCode.SEND_REQUEST_TO_MQ_FAILURE, ex.getMessage());
            if (logCounter.shouldPrint()) {
                logger.error("Send Message to Kafka failure", ex);
            }
            return false;
        }
    }

    /**
     * send BatchPackProfile
     */
    private void sendBatchPackProfile(BatchPackProfile batchProfile, IdTopicConfig idConfig,
            String topic) throws Exception {
        EventHandler handler = handlerLocal.get();
        if (handler == null) {
            handler = this.sinkContext.createEventHandler();
            handlerLocal.set(handler);
        }
        // headers
        Map<String, String> headers = handler.parseHeader(idConfig, batchProfile, sinkContext.getNodeId(),
                sinkContext.getCompressType());
        // compress
        byte[] bodyBytes = handler.parseBody(idConfig, batchProfile, sinkContext.getCompressType());
        // metric
        sinkContext.addSendMetric(batchProfile, clusterName, topic, bodyBytes.length);
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
                    sinkContext.fileMetricIncSumStats(StatConstants.EVENT_SINK_FAILURE);
                    sinkContext.processSendFail(batchProfile, clusterName, topic, sendTime,
                            DataProxyErrCode.MQ_RETURN_ERROR, ex.getMessage());
                    if (logCounter.shouldPrint()) {
                        logger.error("Send BatchPackProfile to Kafka failure", ex);
                    }
                } else {
                    sinkContext.fileMetricIncSumStats(StatConstants.EVENT_SINK_SUCCESS);
                    sinkContext.addSendResultMetric(batchProfile, clusterName, topic, true, sendTime);
                    sinkContext.getMqZoneSink().releaseAcquiredSizePermit(batchProfile);
                    batchProfile.ack();
                }
            }
        };
        producer.send(producerRecord, callback);
    }

    /**
     * send SimplePackProfile
     */
    private void sendSimplePackProfile(SimplePackProfile simpleProfile, IdTopicConfig idConfig,
            String topic) throws Exception {
        // headers
        Map<String, String> headers = simpleProfile.getPropsToMQ();
        // body
        byte[] bodyBytes = simpleProfile.getEvent().getBody();
        // metric
        sinkContext.addSendMetric(simpleProfile, clusterName, topic, bodyBytes.length);
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
                    sinkContext.fileMetricIncWithDetailStats(StatConstants.EVENT_SINK_FAILURE, topic);
                    sinkContext.fileMetricAddFailCnt(simpleProfile, topic,
                            arg0 == null ? "" : String.valueOf(arg0.partition()));
                    sinkContext.processSendFail(simpleProfile, clusterName, topic, sendTime,
                            DataProxyErrCode.MQ_RETURN_ERROR, ex.getMessage());
                    if (logCounter.shouldPrint()) {
                        logger.error("Send SimplePackProfile to Kafka failure", ex);
                    }
                } else {
                    sinkContext.fileMetricAddSuccCnt(simpleProfile, topic,
                            arg0 == null ? "" : String.valueOf(arg0.partition()));
                    sinkContext.fileMetricIncSumStats(StatConstants.EVENT_SINK_SUCCESS);
                    sinkContext.addSendResultMetric(simpleProfile, clusterName, topic, true, sendTime);
                    sinkContext.getMqZoneSink().releaseAcquiredSizePermit(simpleProfile);
                    simpleProfile.ack();
                }
            }
        };
        producer.send(producerRecord, callback);
    }
}
