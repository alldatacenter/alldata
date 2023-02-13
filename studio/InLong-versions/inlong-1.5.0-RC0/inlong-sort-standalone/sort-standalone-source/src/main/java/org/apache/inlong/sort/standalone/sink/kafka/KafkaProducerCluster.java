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

package org.apache.inlong.sort.standalone.sink.kafka;

import com.google.common.base.Preconditions;

import org.apache.flume.Context;
import org.apache.flume.Transaction;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.config.pojo.CacheClusterConfig;
import org.apache.inlong.sort.standalone.utils.Constants;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Properties;

/** wrapper of kafka producer */
public class KafkaProducerCluster implements LifecycleAware {

    public static final Logger LOG = InlongLoggerFactory.getLogger(KafkaProducerCluster.class);

    private final String workerName;
    protected final CacheClusterConfig config;
    private final KafkaFederationSinkContext sinkContext;
    private final Context context;

    private final String cacheClusterName;
    private LifecycleState state;
    private IEvent2KafkaRecordHandler handler;

    private KafkaProducer<String, byte[]> producer;

    /**
     * constructor of KafkaProducerCluster
     *
     * @param workerName                 workerName
     * @param config                     config of cluster
     * @param kafkaFederationSinkContext producer context
     */
    public KafkaProducerCluster(
            String workerName,
            CacheClusterConfig config,
            KafkaFederationSinkContext kafkaFederationSinkContext) {
        this.workerName = Preconditions.checkNotNull(workerName);
        this.config = Preconditions.checkNotNull(config);
        this.sinkContext = Preconditions.checkNotNull(kafkaFederationSinkContext);
        this.context = Preconditions.checkNotNull(kafkaFederationSinkContext.getProducerContext());
        this.state = LifecycleState.IDLE;
        this.cacheClusterName = Preconditions.checkNotNull(config.getClusterName());
        this.handler = sinkContext.createEventHandler();
    }

    /** start and init kafka producer */
    @Override
    public void start() {
        this.state = LifecycleState.START;
        try {
            Properties props = new Properties();
            props.putAll(context.getParameters());
            props.put(
                    ProducerConfig.PARTITIONER_CLASS_CONFIG,
                    context.getString(ProducerConfig.PARTITIONER_CLASS_CONFIG, PartitionerSelector.class.getName()));
            props.put(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    context.getString(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
            props.put(ProducerConfig.CLIENT_ID_CONFIG,
                    context.getString(ProducerConfig.CLIENT_ID_CONFIG, cacheClusterName) + "-" + workerName);
            LOG.info("init kafka client info: " + props);
            producer = new KafkaProducer<>(props, new StringSerializer(), new ByteArraySerializer());
            Preconditions.checkNotNull(producer);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /** stop and close kafka producer */
    @Override
    public void stop() {
        this.state = LifecycleState.STOP;
        try {
            LOG.info("stop kafka producer");
            producer.close();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * get module state
     *
     * @return state
     */
    @Override
    public LifecycleState getLifecycleState() {
        return this.state;
    }

    /**
     * Send data
     *
     * @param  profileEvent data to send
     * @return              boolean
     * @throws IOException
     */
    public boolean send(ProfileEvent profileEvent, Transaction tx) throws IOException {
        String topic = profileEvent.getHeaders().get(Constants.TOPIC);
        ProducerRecord<String, byte[]> record = handler.parse(sinkContext, profileEvent);
        long sendTime = System.currentTimeMillis();
        // check
        if (record == null) {
            tx.commit();
            profileEvent.ack();
            tx.close();
            sinkContext.addSendResultMetric(profileEvent, topic, false, sendTime);
            return true;
        }
        try {
            producer.send(record,
                    (metadata, ex) -> {
                        if (ex == null) {
                            tx.commit();
                            sinkContext.addSendResultMetric(profileEvent, topic, true, sendTime);
                            profileEvent.ack();
                        } else {
                            LOG.error(String.format("send failed, topic is %s, partition is %s",
                                    metadata.topic(), metadata.partition()), ex);
                            tx.rollback();
                            sinkContext.addSendResultMetric(profileEvent, topic, false, sendTime);
                        }
                        tx.close();
                    });
            return true;
        } catch (Exception e) {
            tx.rollback();
            tx.close();
            LOG.error(e.getMessage(), e);
            sinkContext.addSendResultMetric(profileEvent, topic, false, sendTime);
            return false;
        }
    }

    /**
     * get cache cluster name
     *
     * @return cacheClusterName
     */
    public String getCacheClusterName() {
        return cacheClusterName;
    }
}
