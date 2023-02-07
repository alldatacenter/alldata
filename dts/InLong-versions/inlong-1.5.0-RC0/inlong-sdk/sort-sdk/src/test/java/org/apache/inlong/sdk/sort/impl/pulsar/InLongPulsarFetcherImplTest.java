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

package org.apache.inlong.sdk.sort.impl.pulsar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.api.TopicFetcher;
import org.apache.inlong.sdk.sort.entity.CacheZoneCluster;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.fetcher.pulsar.PulsarSingleTopicFetcher;
import org.apache.inlong.sdk.sort.impl.ClientContextImpl;
import org.apache.inlong.sdk.sort.impl.decode.MessageDeserializer;
import org.apache.inlong.sdk.sort.interceptor.MsgTimeInterceptor;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ClientContext.class})
public class InLongPulsarFetcherImplTest {

    private ClientContext clientContext;
    private InLongTopic inLongTopic;
    private SortClientConfig sortClientConfig;

    /**
     * setUp
     */
    @Before
    public void setUp() throws Exception {
        System.setProperty("log4j2.disable.jmx", Boolean.TRUE.toString());

        inLongTopic = new InLongTopic();
        inLongTopic.setTopic("testTopic");
        inLongTopic.setPartitionId(0);
        inLongTopic.setTopicType("pulsar");
        inLongTopic.setProperties(new HashMap<>());

        CacheZoneCluster cacheZoneCluster = new CacheZoneCluster("clusterId", "bootstraps", "token");
        inLongTopic.setInLongCluster(cacheZoneCluster);
        clientContext = PowerMockito.mock(ClientContextImpl.class);

        sortClientConfig = PowerMockito.mock(SortClientConfig.class);

        when(clientContext.getConfig()).thenReturn(sortClientConfig);
        when(sortClientConfig.getSortTaskId()).thenReturn("sortTaskId");

    }

    @Test
    public void stopConsume() {
        TopicFetcher inLongTopicFetcher = new PulsarSingleTopicFetcher(inLongTopic, clientContext,
                new MsgTimeInterceptor(), new MessageDeserializer(), null);
        boolean consumeStop = inLongTopicFetcher.isStopConsume();
        Assert.assertFalse(consumeStop);
        inLongTopicFetcher.setStopConsume(true);
        consumeStop = inLongTopicFetcher.isStopConsume();
        Assert.assertTrue(consumeStop);
    }

    @Test
    public void getInLongTopic() {
        TopicFetcher inLongTopicFetcher = new PulsarSingleTopicFetcher(inLongTopic, clientContext,
                new MsgTimeInterceptor(), new MessageDeserializer(), null);
        InLongTopic inLongTopic = inLongTopicFetcher.getTopics().get(0);
        Assert.assertEquals(inLongTopic.getInLongCluster(), inLongTopic.getInLongCluster());
    }

    @Test
    public void ack() {
        TopicFetcher inLongTopicFetcher = new PulsarSingleTopicFetcher(inLongTopic, clientContext,
                new MsgTimeInterceptor(), new MessageDeserializer(), null);
        MessageId messageId = PowerMockito.mock(MessageId.class);
        ConcurrentHashMap<String, MessageId> offsetCache = new ConcurrentHashMap<>();
        offsetCache.put("test", messageId);

        Whitebox.setInternalState(inLongTopicFetcher, "offsetCache", offsetCache);

        try {
            inLongTopicFetcher.ack("test");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void init() {
        PulsarClient pulsarClient = PowerMockito.mock(PulsarClient.class);
        ConsumerBuilder consumerBuilder = PowerMockito.mock(ConsumerBuilder.class);

        TopicFetcher inLongTopicFetcher = new PulsarSingleTopicFetcher(inLongTopic, clientContext,
                new MsgTimeInterceptor(), new MessageDeserializer(), pulsarClient);
        try {
            when(pulsarClient.newConsumer(any())).thenReturn(consumerBuilder);
            when(consumerBuilder.topic(anyString())).thenReturn(consumerBuilder);
            when(consumerBuilder.subscriptionName(anyString())).thenReturn(consumerBuilder);
            when(consumerBuilder.subscriptionType(any())).thenReturn(consumerBuilder);
            when(consumerBuilder.startMessageIdInclusive()).thenReturn(consumerBuilder);
            when(consumerBuilder.ackTimeout(anyLong(), any())).thenReturn(consumerBuilder);
            when(consumerBuilder.subscriptionInitialPosition(any())).thenReturn(consumerBuilder);

            when(consumerBuilder.receiverQueueSize(anyInt())).thenReturn(consumerBuilder);
            when(consumerBuilder.messageListener(any())).thenReturn(consumerBuilder);

            Consumer consumer = PowerMockito.mock(Consumer.class);
            when(consumerBuilder.subscribe()).thenReturn(consumer);
            doNothing().when(consumer).close();
            boolean init = inLongTopicFetcher.init();
            inLongTopicFetcher.close();
            Assert.assertTrue(init);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void pause() {
        TopicFetcher inLongTopicFetcher = new PulsarSingleTopicFetcher(inLongTopic, clientContext,
                new MsgTimeInterceptor(), new MessageDeserializer(), null);
        inLongTopicFetcher.pause();
    }

    @Test
    public void resume() {
        TopicFetcher inLongTopicFetcher = new PulsarSingleTopicFetcher(inLongTopic, clientContext,
                new MsgTimeInterceptor(), new MessageDeserializer(), null);
        inLongTopicFetcher.resume();
    }

    @Test
    public void close() {
        TopicFetcher inLongTopicFetcher = new PulsarSingleTopicFetcher(inLongTopic, clientContext,
                new MsgTimeInterceptor(), new MessageDeserializer(), null);
        boolean close = inLongTopicFetcher.close();
        Assert.assertTrue(close);

        boolean closed = inLongTopicFetcher.isClosed();
        Assert.assertTrue(closed);
    }
}