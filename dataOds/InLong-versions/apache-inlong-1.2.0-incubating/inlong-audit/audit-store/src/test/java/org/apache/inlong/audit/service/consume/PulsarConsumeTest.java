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

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.impl.PulsarClientImpl;
import org.apache.pulsar.client.impl.conf.ClientConfigurationData;
import org.apache.pulsar.client.util.ExecutorProvider;
import org.apache.pulsar.shade.io.netty.channel.EventLoop;
import org.apache.pulsar.shade.io.netty.util.Timer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PulsarConsumeTest {

    private PulsarClientImpl client;
    private PulsarConsume pulsarConsume;

    static <T> PulsarClientImpl createPulsarClientMock() {
        PulsarClientImpl mockClient = mock(PulsarClientImpl.class, Mockito.RETURNS_DEEP_STUBS);
        ClientConfigurationData clientConf = new ClientConfigurationData();
        when(mockClient.getConfiguration()).thenReturn(clientConf);
        when(mockClient.timer()).thenReturn(mock(Timer.class));

        when(mockClient.externalExecutorProvider()).thenReturn(mock(ExecutorProvider.class));
        when(mockClient.eventLoopGroup().next()).thenReturn(mock(EventLoop.class));

        return mockClient;
    }

    @Before
    public void setUp() throws Exception {
        client = createPulsarClientMock();
        ClientConfigurationData clientConf = client.getConfiguration();
        clientConf.setOperationTimeoutMs(100);
        clientConf.setStatsIntervalSeconds(0);

        Consumer<byte[]> consumer = client.newConsumer().subscribe();
        pulsarConsume = mock(PulsarConsume.class);
        when(pulsarConsume.createConsumer(any(), any())).thenReturn(consumer);
    }

    @Test
    public void testConsumer() {
        String topic = "non-persistent://public/default/audit-test";
        pulsarConsume.createConsumer(client, topic);
    }

}
