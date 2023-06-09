/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.async.message.client.kafka;

import com.pingcap.ticdc.cdc.KafkaMessage;
import com.pingcap.ticdc.cdc.TicdcEventData;
import com.pingcap.ticdc.cdc.TicdcEventDecoder;
import com.pingcap.ticdc.cdc.TicdcEventFilter;
import com.pingcap.ticdc.cdc.value.TicdcEventDDL;
import com.pingcap.ticdc.cdc.value.TicdcEventResolve;
import com.pingcap.ticdc.cdc.value.TicdcEventRowChange;
import com.qlangtech.tis.async.message.client.consumer.*;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: baisui 百岁
 * @create: 2020-12-09 13:53
 **/
public class KafkaMQListener implements IMQListener<Object> {

    public static final String CONSUMER_ENABLE_AUTO_COMMIT = "true";                //是否自动提交（消费者）
    public static final String CONSUMER_AUTO_COMMIT_INTERVAL_MS = "1000";
    public static final String CONSUMER_SESSION_TIMEOUT_MS = "30000";                //连接超时时间
    public static final int CONSUMER_MAX_POLL_RECORDS = 10;                            //每次拉取数
    public static final Duration CONSUMER_POLL_TIME_OUT = Duration.ofMillis(6000);    //拉取数据超时时间

    private final TiKVKafkaMQListenerFactory listenerFactory;
    private IConsumerHandle consumerHandle;
    private Set<String> focusTags;


    public KafkaMQListener(TiKVKafkaMQListenerFactory listenerFactory) {
        this.listenerFactory = listenerFactory;
    }

    public static void main(String[] args) {

    }

    @Override
    public Object start(TargetResName dataxName, IDataxReader rdbmsReader, List<ISelectedTab> tabs, IDataxProcessor dataXProcessor) throws MQConsumeException {

        return null;
    }


    private KafkaConsumer<byte[], byte[]> createConsumer() {
        Properties configs = initConfig();
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(configs);
        consumer.subscribe(Arrays.asList(listenerFactory.topic));
        return consumer;
    }

    @Override
    public String getTopic() {
        return listenerFactory.topic;
    }

    @Override
    public void setDeserialize(IAsyncMsgDeserialize deserialize) {

    }


    @Override
    public IConsumerHandle getConsumerHandle() {
        return this.consumerHandle;
    }


    public void start() throws Exception {
        KafkaConsumer<byte[], byte[]> consumer = this.createConsumer();

        TicdcEventFilter filter = new TicdcEventFilter();
        String[] tags = null;//StringUtils.split(consumerHandle.getSubExpression(), "||");
        if (tags.length < 1) {
            throw new IllegalStateException("tags length can not small than 1");
        }
        this.focusTags = Arrays.stream(tags).map((t) -> StringUtils.trim(t)).collect(Collectors.toSet());

        Thread t = new Thread(() -> {
            while (true) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(CONSUMER_POLL_TIME_OUT);
                records.forEach((record) -> {
                    try {
                        parseKafkaMessage(filter, new KafkaMessage(record.key(), record.value()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }, "msg_consume_thread");

        t.start();

    }

    private void parseKafkaMessage(TicdcEventFilter filter, KafkaMessage kafkaMessage) throws Exception {
        TicdcEventDecoder ticdcEventDecoder = new TicdcEventDecoder(kafkaMessage);
        while (ticdcEventDecoder.hasNext()) {
            TicdcEventData data = ticdcEventDecoder.next();
            if (data.getTicdcEventValue() instanceof TicdcEventRowChange) {
                boolean ok = filter.check(data.getTicdcEventKey().getTbl(),
                        data.getTicdcEventValue().getKafkaPartition(), data.getTicdcEventKey().getTs());
                if (ok) {
                    if (this.focusTags.contains(data.getTicdcEventKey().getTbl())) {
                        this.consumerHandle.consume(null, convert(data), null);
                    }
                } else {
                    // ignore duplicated messages
                }
            } else if (data.getTicdcEventValue() instanceof TicdcEventDDL) {
                // deal with ddl event
            } else if (data.getTicdcEventValue() instanceof TicdcEventResolve) {
                // filter.resolveEvent(data.getTicdcEventValue().getKafkaPartition(), data.getTicdcEventKey().getTs());
                // deal with resolve event
            }
        }
    }

    /**
     * 初始化配置
     */
    private Properties initConfig() {
        Properties props = new Properties();
        props.put("bootstrap.servers", listenerFactory.mqAddress);
        props.put("group.id", listenerFactory.groupId);
        props.put("enable.auto.commit", CONSUMER_ENABLE_AUTO_COMMIT);
        props.put("auto.commit.interval.ms", CONSUMER_AUTO_COMMIT_INTERVAL_MS);
        props.put("session.timeout.ms", CONSUMER_SESSION_TIMEOUT_MS);
        props.put("max.poll.records", CONSUMER_MAX_POLL_RECORDS);
        //earliest,latest,和none
        props.put("auto.offset.reset", listenerFactory.offsetResetStrategy);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return props;
    }

    private AsyncMsg convert(TicdcEventData data) {
        return new KafkaAsyncMsg(this.getTopic(), data);
    }
}
