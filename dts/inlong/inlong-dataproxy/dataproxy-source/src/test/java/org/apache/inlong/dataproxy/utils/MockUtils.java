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

package org.apache.inlong.dataproxy.utils;

import org.apache.flume.Channel;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.event.EventBuilder;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.powermock.api.mockito.PowerMockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * MockUtils
 */
public class MockUtils {

    public static final String CLUSTER_ID = "proxy_inlong5th_sz";
    public static final String CONTAINER_NAME = "2222.inlong.DataProxy.sz100001";
    public static final String CONTAINER_IP = "127.0.0.1";
    public static final String SOURCE_ID = "agent-source";
    public static final String SOURCE_DATA_ID = "12069";
    public static final String INLONG_GROUP_ID1 = "03a00000026";
    public static final String INLONG_GROUP_ID2 = "03a00000126";
    public static final String INLONG_STREAM_ID = "";
    public static final String SINK_ID = "inlong5th-pulsar-sz";
    public static final String SINK_DATA_ID = "PULSAR_TOPIC_1";

    /**
     * mockChannel
     */
    public static Channel mockChannel() throws Exception {
        Transaction transaction = PowerMockito.mock(Transaction.class);
        Channel channel = PowerMockito.mock(Channel.class);
        PowerMockito.when(channel.getTransaction()).thenReturn(transaction);
        PowerMockito.doNothing().when(transaction, "begin");
        PowerMockito.doNothing().when(transaction, "commit");
        PowerMockito.doNothing().when(transaction, "rollback");
        PowerMockito.doNothing().when(transaction, "close");
        Event event = mockEvent();
        PowerMockito.when(channel.take()).thenReturn(event);
        return channel;
    }

    /**
     * mockEvent
     */
    public static Event mockEvent() throws Exception {
        Map<String, String> headers = new HashMap<>();
        String sourceTime = String.valueOf(System.currentTimeMillis());
        headers.put(Constants.HEADER_KEY_MSG_TIME, sourceTime);
        headers.put(Constants.HEADER_KEY_SOURCE_IP, CONTAINER_IP);
        headers.put(Constants.HEADER_KEY_SOURCE_TIME, sourceTime);
        headers.put(Constants.INLONG_GROUP_ID, INLONG_GROUP_ID1);
        headers.put(Constants.TOPIC, SINK_DATA_ID);
        byte[] body = "testContent".getBytes();
        return EventBuilder.withBody(body, headers);
    }

    /**
     * mockPulsarClient
     */
    @SuppressWarnings("unchecked")
    public static PulsarClient mockPulsarClient() throws Exception {
        ClientBuilder clientBuilder = PowerMockito.mock(ClientBuilder.class);
        PowerMockito.mockStatic(PulsarClient.class);

        PowerMockito.when(PulsarClient.builder()).thenReturn(clientBuilder);
        PowerMockito.when(clientBuilder.serviceUrl(anyString())).thenReturn(clientBuilder);
        PowerMockito.when(clientBuilder.authentication(any())).thenReturn(clientBuilder);
        PowerMockito.when(clientBuilder.statsInterval(anyLong(), any())).thenReturn(clientBuilder);
        PowerMockito.when(clientBuilder.ioThreads(anyInt())).thenReturn(clientBuilder);
        PowerMockito.when(clientBuilder.memoryLimit(anyLong(), any())).thenReturn(clientBuilder);
        PowerMockito.when(clientBuilder.connectionsPerBroker(anyInt())).thenReturn(clientBuilder);
        PulsarClient client = PowerMockito.mock(PulsarClient.class);
        PowerMockito.when(clientBuilder.build()).thenReturn(client);

        ProducerBuilder<byte[]> producerBuilder = PowerMockito.mock(ProducerBuilder.class);
        PowerMockito.when(client.newProducer()).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.sendTimeout(anyInt(), any())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.maxPendingMessages(anyInt())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.maxPendingMessagesAcrossPartitions(anyInt())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.batchingMaxMessages(anyInt())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.batchingMaxPublishDelay(anyInt(), any())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.batchingMaxBytes(anyInt())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.accessMode(any())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.messageRoutingMode(any())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.blockIfQueueFull(anyBoolean())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.roundRobinRouterBatchingPartitionSwitchFrequency(anyInt()))
                .thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.enableBatching(anyBoolean())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.compressionType(any())).thenReturn(producerBuilder);

        PowerMockito.when(producerBuilder.hashingScheme(any())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.batcherBuilder(any())).thenReturn(producerBuilder);

        Producer<byte[]> producer = mockProducer();
        PowerMockito.when(producerBuilder.clone()).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.topic(anyString())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.producerName(anyString())).thenReturn(producerBuilder);
        PowerMockito.when(producerBuilder.create()).thenReturn(producer);
        return client;
    }

    /**
     * mockProducer
     */
    @SuppressWarnings({"unchecked"})
    public static Producer<byte[]> mockProducer() throws Exception {
        Producer<byte[]> producer = PowerMockito.mock(Producer.class);
        PowerMockito.doNothing().when(producer, "close");
        TypedMessageBuilder<byte[]> msgBuilder = PowerMockito.mock(TypedMessageBuilder.class);
        PowerMockito.when(producer.newMessage()).thenReturn(msgBuilder);
        PowerMockito.when(msgBuilder.key(anyString())).thenReturn(msgBuilder);
        PowerMockito.when(msgBuilder.properties(any())).thenReturn(msgBuilder);
        PowerMockito.when(msgBuilder.value(any())).thenReturn(msgBuilder);
        CompletableFuture<MessageId> future = PowerMockito.mock(CompletableFuture.class);
        PowerMockito.when(future.whenCompleteAsync(any())).thenReturn(future);
        PowerMockito.when(msgBuilder.sendAsync()).thenReturn(future);
        return producer;
    }

    /**
     * mockMetricRegister
     */
    public static void mockMetricRegister() throws Exception {
        PowerMockito.mockStatic(MetricRegister.class);
        PowerMockito.doNothing().when(MetricRegister.class, "register", any());
    }

}
