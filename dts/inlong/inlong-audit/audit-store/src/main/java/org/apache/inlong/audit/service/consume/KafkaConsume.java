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

package org.apache.inlong.audit.service.consume;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.audit.config.MessageQueueConfig;
import org.apache.inlong.audit.config.StoreConfig;
import org.apache.inlong.audit.service.InsertData;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class KafkaConsume extends BaseConsume {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsume.class);
    private KafkaConsumer<String, byte[]> consumer;
    private String serverUrl;
    private String topic;

    /**
     * Constructor
     *
     * @param insertServiceList
     * @param storeConfig
     * @param mqConfig
     */
    public KafkaConsume(List<InsertData> insertServiceList, StoreConfig storeConfig, MessageQueueConfig mqConfig) {
        super(insertServiceList, storeConfig, mqConfig);
    }

    @Override
    public void start() {
        serverUrl = mqConfig.getKafkaServerUrl();
        topic = mqConfig.getKafkaTopic();
        boolean isAutoCommit = Boolean.getBoolean(mqConfig.getEnableAutoCommit());
        Preconditions.checkArgument(StringUtils.isNotEmpty(serverUrl), "no kafka server url specified");
        Preconditions.checkArgument(StringUtils.isNotEmpty(mqConfig.getKafkaTopic()),
                "no kafka topic topic specified");
        Preconditions.checkArgument(StringUtils.isNotEmpty(mqConfig.getKafkaConsumerName()),
                "no kafka consume name specified");

        initConsumer(mqConfig);

        Thread thread = new Thread(new Fetcher(consumer, topic, isAutoCommit, mqConfig.getFetchWaitMs()),
                "KafkaConsume_Fetcher_Thread");
        thread.start();
    }

    protected void initConsumer(MessageQueueConfig mqConfig) {
        LOG.info("init kafka consumer, topic:{}, serverUrl:{}", topic, serverUrl);

        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverUrl);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, mqConfig.getKafkaGroupId());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, mqConfig.getEnableAutoCommit());
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, mqConfig.getAutoCommitIntervalMs());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, mqConfig.getAutoOffsetReset());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Collections.singleton(topic));
    }

    public class Fetcher implements Runnable {

        private final KafkaConsumer<String, byte[]> consumer;
        private final String topic;
        private final boolean isAutoCommit;
        private final long fetchWaitMs;

        public Fetcher(KafkaConsumer<String, byte[]> consumer, String topic, boolean isAutoCommit, long fetchWaitMs) {
            this.consumer = consumer;
            this.topic = topic;
            this.isAutoCommit = isAutoCommit;
            this.fetchWaitMs = fetchWaitMs;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    // Set the waiting time of the consumer to 100ms
                    ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(fetchWaitMs));
                    if (records != null && !records.isEmpty()) {
                        for (ConsumerRecord<String, byte[]> record : records) {
                            if (StringUtils.equals(record.topic(), topic)) {
                                String body = new String(record.value(), StandardCharsets.UTF_8);
                                handleMessage(body);
                            }
                        }

                        if (!isAutoCommit) {
                            consumer.commitAsync();
                        }
                    }
                } catch (Exception e) {
                    LOG.error("kafka consumer get message error {}", e.getMessage());
                }
            }
        }
    }
}
