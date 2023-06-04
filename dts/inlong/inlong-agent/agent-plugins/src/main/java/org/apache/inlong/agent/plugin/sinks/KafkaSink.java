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
import org.apache.inlong.common.pojo.dataproxy.MQClusterInfo;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_ENABLE_ASYNC_SEND;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_KAFKA_SINK_SEND_ACKS;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_KAFKA_SINK_SEND_COMPRESSION_TYPE;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_KAFKA_SINK_SEND_KEY_SERIALIZER;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_KAFKA_SINK_SEND_VALUE_SERIALIZER;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_KAFKA_SINK_SYNC_SEND_TIMEOUT_MS;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_PRODUCER_NUM;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_SEND_QUEUE_SIZE;
import static org.apache.inlong.agent.constant.AgentConstants.KAFKA_PRODUCER_ENABLE_ASYNC_SEND;
import static org.apache.inlong.agent.constant.AgentConstants.KAFKA_SINK_PRODUCER_NUM;
import static org.apache.inlong.agent.constant.AgentConstants.KAFKA_SINK_SEND_QUEUE_SIZE;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

public class KafkaSink extends AbstractSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSink.class);
    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new AgentThreadFactory("KafkaSink"));
    private final AgentConfiguration agentConf = AgentConfiguration.getAgentConf();
    private TaskPositionManager taskPositionManager;
    private volatile boolean shutdown = false;

    private List<MQClusterInfo> mqClusterInfos;
    private String topic;
    private List<KafkaSender> kafkaSenders;
    private static final AtomicInteger KAFKA_SENDER_INDEX = new AtomicInteger(0);

    private LinkedBlockingQueue<BatchProxyMessage> kafkaSendQueue;

    private int producerNum;
    private boolean asyncSend;

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        taskPositionManager = TaskPositionManager.getInstance();
        int sendQueueSize = agentConf.getInt(KAFKA_SINK_SEND_QUEUE_SIZE, DEFAULT_SEND_QUEUE_SIZE);
        kafkaSendQueue = new LinkedBlockingQueue<>(sendQueueSize);
        producerNum = agentConf.getInt(KAFKA_SINK_PRODUCER_NUM, DEFAULT_PRODUCER_NUM);
        asyncSend = agentConf.getBoolean(KAFKA_PRODUCER_ENABLE_ASYNC_SEND, DEFAULT_ENABLE_ASYNC_SEND);

        mqClusterInfos = jobConf.getMqClusters();
        Preconditions.checkArgument(ObjectUtils.isNotEmpty(jobConf.getMqTopic()) && jobConf.getMqTopic().isValid(),
                "no valid kafka topic config");
        topic = jobConf.getMqTopic().getTopic();

        kafkaSenders = new ArrayList<>();
        initKafkaSender();
        EXECUTOR_SERVICE.execute(sendDataThread());
        EXECUTOR_SERVICE.execute(flushCache());
    }

    @Override
    public void write(Message message) {
        if (message == null || message instanceof EndMessage) {
            return;
        }

        try {
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
        } catch (Exception e) {
            sinkMetric.sinkFailCount.incrementAndGet();
            LOGGER.error("write job[{}] data to cache error", jobInstanceId, e);
        } catch (Throwable t) {
            ThreadUtils.threadThrowableHandler(Thread.currentThread(), t);
        }
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy job[{}] kafka sink", jobInstanceId);
        while (!sinkFinish()) {
            LOGGER.info("job[{}] wait until cache all data to kafka", jobInstanceId);
            AgentUtils.silenceSleepInMs(batchFlushInterval);
        }
        shutdown = true;
        if (CollectionUtils.isNotEmpty(kafkaSenders)) {
            for (KafkaSender sender : kafkaSenders) {
                sender.close();
            }
            kafkaSenders.clear();
        }
    }

    private boolean sinkFinish() {
        return cache.values().stream().allMatch(PackProxyMessage::isEmpty) && kafkaSendQueue.isEmpty();
    }

    /**
     * flush cache by batch
     *
     * @return thread runner
     */
    private Runnable flushCache() {
        return () -> {
            LOGGER.info("start kafka sink flush cache thread, job[{}], groupId[{}]", jobInstanceId, inlongGroupId);
            while (!shutdown) {
                try {
                    cache.forEach((batchKey, packProxyMessage) -> {
                        BatchProxyMessage batchProxyMessage = packProxyMessage.fetchBatch();
                        if (batchProxyMessage == null) {
                            return;
                        }
                        try {
                            kafkaSendQueue.put(batchProxyMessage);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(
                                        "send group id {}, message key {},with message size {}, the job id is {}, "
                                                + "read source is {} sendTime is {}",
                                        inlongGroupId, batchKey,
                                        batchProxyMessage.getDataList().size(), jobInstanceId, sourceName,
                                        batchProxyMessage.getDataTime());
                            }
                        } catch (Exception e) {
                            LOGGER.error("flush job[{}] data to send queue exception", jobInstanceId, e);
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
     * take batchMsg from sendQueue and send to kafka
     */
    private Runnable sendDataThread() {
        return () -> {
            LOGGER.info("start kafka sink send data thread, job[{}], groupId[{}]", jobInstanceId, inlongGroupId);
            while (!shutdown) {
                try {
                    BatchProxyMessage data = kafkaSendQueue.poll(1, TimeUnit.MILLISECONDS);
                    if (ObjectUtils.isEmpty(data)) {
                        continue;
                    }
                    sendData(data);
                } catch (Throwable t) {
                    LOGGER.error("send job[{}] data to kafka error", jobInstanceId, t);
                }
            }
        };
    }

    private void sendData(BatchProxyMessage batchMsg) throws InterruptedException {
        if (ObjectUtils.isEmpty(batchMsg)) {
            return;
        }

        KafkaProducer<String, byte[]> producer = selectProducer();
        if (ObjectUtils.isEmpty(producer)) {
            kafkaSendQueue.put(batchMsg);
            LOGGER.error("send job[{}] data err, empty kafka producer", jobInstanceId);
            return;
        }

        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, batchMsg.getInLongMsg().buildArray());
        sinkMetric.pluginSendCount.addAndGet(batchMsg.getMsgCnt());
        if (asyncSend) {
            producer.send(record, new AsyncSinkCallback(System.currentTimeMillis(), batchMsg));
        } else {
            try {
                Future<RecordMetadata> future = producer.send(record);
                future.get(DEFAULT_KAFKA_SINK_SYNC_SEND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                updateSuccessSendMetrics(batchMsg);
            } catch (Exception e) {
                sinkMetric.pluginSendFailCount.addAndGet(batchMsg.getMsgCnt());
                LOGGER.error("send job[{}] data fail to kafka, add back to send queue, send queue size {}",
                        jobInstanceId,
                        kafkaSendQueue.size(), e);
                kafkaSendQueue.put(batchMsg);
            }
        }
    }

    private void updateSuccessSendMetrics(BatchProxyMessage batchMsg) {
        AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_SEND_SUCCESS, batchMsg.getGroupId(),
                batchMsg.getStreamId(), batchMsg.getDataTime(), batchMsg.getMsgCnt(),
                batchMsg.getTotalSize());
        sinkMetric.pluginSendSuccessCount.addAndGet(batchMsg.getMsgCnt());
        if (sourceName != null) {
            taskPositionManager.updateSinkPosition(batchMsg.getJobId(), sourceName, batchMsg.getMsgCnt(), false);
        }
    }

    private KafkaProducer<String, byte[]> selectProducer() {
        if (CollectionUtils.isEmpty(kafkaSenders)) {
            LOGGER.error("send job[{}] data err, empty kafka sender", jobInstanceId);
            return null;
        }
        KafkaSender sender = kafkaSenders.get(
                (KAFKA_SENDER_INDEX.getAndIncrement() & Integer.MAX_VALUE) % kafkaSenders.size());
        return sender.getProducer();
    }

    private void initKafkaSender() {
        if (CollectionUtils.isEmpty(mqClusterInfos)) {
            LOGGER.error("init job[{}] kafka producer fail, empty mqCluster info", jobInstanceId);
            return;
        }
        for (MQClusterInfo clusterInfo : mqClusterInfos) {
            if (!clusterInfo.isValid()) {
                continue;
            }
            kafkaSenders.add(new KafkaSender(clusterInfo, producerNum));
        }
    }

    class KafkaSender {

        private final Properties kafkaProps = new Properties();
        private List<KafkaProducer<String, byte[]>> producers;
        private final AtomicInteger producerIndex = new AtomicInteger(0);

        public KafkaSender(MQClusterInfo clusterInfo, int producerNum) {
            setKafkaProps(clusterInfo);
            initKafkaProducer(kafkaProps, producerNum);
        }

        private void setKafkaProps(MQClusterInfo clusterInfo) {
            kafkaProps.clear();
            Map<String, String> params = clusterInfo.getParams();

            // set bootstrap server
            String bootStrapServers = params.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
            if (bootStrapServers == null) {
                throw new IllegalArgumentException("kafka param bootstrap.servers is null");
            }
            kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);

            // set ack
            String acks = params.get(ProducerConfig.ACKS_CONFIG);
            if (StringUtils.isNotEmpty(acks)) {
                kafkaProps.put(ProducerConfig.ACKS_CONFIG, acks);
            } else {
                kafkaProps.put(ProducerConfig.ACKS_CONFIG, DEFAULT_KAFKA_SINK_SEND_ACKS);
            }

            // set compression
            String compressionType = params.get(ProducerConfig.COMPRESSION_TYPE_CONFIG);
            if (StringUtils.isNotEmpty(compressionType)) {
                kafkaProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
            } else {
                kafkaProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, DEFAULT_KAFKA_SINK_SEND_COMPRESSION_TYPE);
            }

            // set serializer
            String keySerializer = params.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
            if (StringUtils.isNotEmpty(keySerializer)) {
                kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
            } else {
                kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, DEFAULT_KAFKA_SINK_SEND_KEY_SERIALIZER);
            }

            String valueSerializer = params.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
            if (StringUtils.isNotEmpty(keySerializer)) {
                kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
            } else {
                kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, DEFAULT_KAFKA_SINK_SEND_VALUE_SERIALIZER);
            }

            // set linger
            String lingerMs = params.get(ProducerConfig.LINGER_MS_CONFIG);
            if (StringUtils.isNotEmpty(lingerMs)) {
                kafkaProps.put(ProducerConfig.LINGER_MS_CONFIG, Integer.parseInt(lingerMs));
            }

            // set batch size
            String batchSize = params.get(ProducerConfig.BATCH_SIZE_CONFIG);
            if (StringUtils.isNotEmpty(batchSize)) {
                kafkaProps.put(ProducerConfig.BATCH_SIZE_CONFIG, Integer.parseInt(batchSize));
            }

            // set buffer memory
            String bufferMemory = params.get(ProducerConfig.BUFFER_MEMORY_CONFIG);
            if (StringUtils.isNotEmpty(batchSize)) {
                kafkaProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, Integer.parseInt(bufferMemory));
            }

            // set authentication
            String securityProtocol = params.get(SECURITY_PROTOCOL_CONFIG);
            if (StringUtils.isNotEmpty(securityProtocol)) {
                kafkaProps.put(SECURITY_PROTOCOL_CONFIG, securityProtocol);
            }

            String saslMechanism = params.get(SASL_MECHANISM);
            if (StringUtils.isNotEmpty(saslMechanism)) {
                kafkaProps.put(SASL_MECHANISM, saslMechanism);
            }

            String saslJaasConfig = params.get(SASL_JAAS_CONFIG);
            if (StringUtils.isNotEmpty(saslJaasConfig)) {
                kafkaProps.put(SASL_JAAS_CONFIG, saslJaasConfig);
            }
        }

        private void initKafkaProducer(Properties kafkaProps, int producerNum) {
            producers = new ArrayList<>(producerNum);
            for (int i = 0; i < producerNum; i++) {
                producers.add(new KafkaProducer<>(kafkaProps));
            }
        }

        public KafkaProducer<String, byte[]> getProducer() {
            if (CollectionUtils.isEmpty(producers)) {
                LOGGER.error("job[{}] empty producers", jobInstanceId);
                return null;
            }
            int index = (producerIndex.getAndIncrement() & Integer.MAX_VALUE) % producers.size();
            return producers.get(index);
        }

        /**
         * close all kafka producer
         */
        public void close() {
            if (CollectionUtils.isEmpty(producers)) {
                return;
            }

            for (KafkaProducer<String, byte[]> producer : producers) {
                producer.close();
            }
        }
    }

    class AsyncSinkCallback implements Callback {

        private long startTime;
        private BatchProxyMessage batchMsg;

        public AsyncSinkCallback(long startTime, BatchProxyMessage batchMsg) {
            this.startTime = startTime;
            this.batchMsg = batchMsg;
        }

        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (exception != null) {
                sinkMetric.pluginSendFailCount.addAndGet(batchMsg.getMsgCnt());
                LOGGER.error("send job[{}] data fail to kafka, will add back to sendqueue, current sendqueue size {}",
                        jobInstanceId,
                        kafkaSendQueue.size(), exception);
                try {
                    kafkaSendQueue.put(batchMsg);
                } catch (InterruptedException ex) {
                    LOGGER.error("put job[{}] data back to queue fail, send queue size {}", jobInstanceId,
                            kafkaSendQueue.size(), ex);
                }
            } else {
                updateSuccessSendMetrics(batchMsg);
            }

            if (LOGGER.isDebugEnabled()) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (metadata != null) {
                    LOGGER.debug("acked job[{}] message partition:{} ofset:{}", jobInstanceId, metadata.partition(),
                            metadata.offset());
                }
                LOGGER.debug("job[{}] send data to kafka elapsed time: {}", jobInstanceId, elapsedTime);
            }
        }
    }
}
