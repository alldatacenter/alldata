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
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageListener;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PulsarConsume extends BaseConsume {

    private static final Logger LOG = LoggerFactory.getLogger(PulsarConsume.class);
    private final ConcurrentHashMap<String, List<Consumer<byte[]>>> topicConsumerMap = new ConcurrentHashMap<>();

    /**
     * Constructor
     * @param insertServiceList
     * @param storeConfig
     * @param mqConfig
     */
    public PulsarConsume(List<InsertData> insertServiceList, StoreConfig storeConfig,
            MessageQueueConfig mqConfig) {
        super(insertServiceList, storeConfig, mqConfig);
    }

    @Override
    public void start() {
        String pulsarUrl = mqConfig.getPulsarServerUrl();
        Preconditions.checkArgument(StringUtils.isNotEmpty(pulsarUrl), "no pulsar server url specified");
        Preconditions.checkArgument(StringUtils.isNotEmpty(mqConfig.getPulsarTopic()),
                "no pulsar topic specified");
        Preconditions.checkArgument(StringUtils.isNotEmpty(mqConfig.getPulsarConsumerSubName()),
                "no pulsar consumeSubName specified");
        PulsarClient pulsarClient = getOrCreatePulsarClient(pulsarUrl);
        updateConcurrentConsumer(pulsarClient);
    }

    private PulsarClient getOrCreatePulsarClient(String pulsarServerUrl) {
        LOG.info("start consumer pulsarServerUrl = {}", pulsarServerUrl);
        PulsarClient pulsarClient = null;
        ClientBuilder builder = PulsarClient.builder();
        try {
            if (mqConfig.isPulsarEnableAuth() && StringUtils.isNotEmpty(mqConfig.getPulsarToken())) {
                builder.authentication(AuthenticationFactory.token(mqConfig.getPulsarToken()));
            }
            pulsarClient = builder.serviceUrl(pulsarServerUrl)
                    .connectionTimeout(mqConfig.getClientOperationTimeoutSecond(), TimeUnit.SECONDS).build();
        } catch (PulsarClientException e) {
            LOG.error("getOrCreatePulsarClient has pulsar {} err {}", pulsarServerUrl, e);
        }
        return pulsarClient;
    }

    protected void updateConcurrentConsumer(PulsarClient pulsarClient) {
        List<Consumer<byte[]>> list = topicConsumerMap.computeIfAbsent(mqConfig.getPulsarTopic(),
                key -> new ArrayList<Consumer<byte[]>>());
        int currentConsumerNum = list.size();
        int createNum = mqConfig.getConcurrentConsumerNum() - currentConsumerNum;
        /*
         * add consumer
         */
        if (createNum > 0) {
            for (int i = 0; i < mqConfig.getConcurrentConsumerNum(); i++) {
                Consumer<byte[]> consumer = createConsumer(pulsarClient, mqConfig.getPulsarTopic());
                if (consumer != null) {
                    list.add(consumer);
                }
            }
        } else if (createNum < 0) {
            /*
             * delete consumer
             */
            int removeIndex = currentConsumerNum - 1;
            for (int i = createNum; i < 0; i++) {
                Consumer<byte[]> consumer = list.remove(removeIndex);
                consumer.closeAsync();
                removeIndex -= 1;
            }
        }
    }

    protected Consumer<byte[]> createConsumer(PulsarClient pulsarClient, String topic) {
        Consumer<byte[]> consumer = null;
        if (pulsarClient != null && StringUtils.isNotEmpty(topic)) {
            LOG.info("createConsumer has topic {}, subName {}", topic,
                    mqConfig.getPulsarConsumerSubName());
            try {
                consumer = pulsarClient.newConsumer()
                        .subscriptionName(mqConfig.getPulsarConsumerSubName())
                        .subscriptionType(SubscriptionType.Shared)
                        .topic(topic)
                        .receiverQueueSize(mqConfig.getConsumerReceiveQueueSize())
                        .enableRetry(mqConfig.isPulsarConsumerEnableRetry())
                        .messageListener(new MessageListener<byte[]>() {

                            public void received(Consumer<byte[]> consumer, Message<byte[]> msg) {
                                try {
                                    String body = new String(msg.getData(), StandardCharsets.UTF_8);
                                    handleMessage(body);
                                    consumer.acknowledge(msg);
                                } catch (Exception e) {
                                    LOG.error("Consumer has exception topic {}, subName {}, ex {}",
                                            topic,
                                            mqConfig.getPulsarConsumerSubName(),
                                            e);
                                    if (mqConfig.isPulsarConsumerEnableRetry()) {
                                        try {
                                            consumer.reconsumeLater(msg, 10, TimeUnit.SECONDS);
                                        } catch (PulsarClientException pulsarClientException) {
                                            LOG.error("Consumer reconsumeLater has exception "
                                                    + "topic {}, subName {}, ex {}",
                                                    topic,
                                                    mqConfig.getPulsarConsumerSubName(),
                                                    pulsarClientException);
                                        }
                                    } else {
                                        consumer.negativeAcknowledge(msg);
                                    }
                                }
                            }
                        })
                        .subscribe();
            } catch (PulsarClientException e) {
                LOG.error("createConsumer has topic {}, subName {}, err {}", topic,
                        mqConfig.getPulsarConsumerSubName(), e);
            }
        }
        return consumer;
    }
}
