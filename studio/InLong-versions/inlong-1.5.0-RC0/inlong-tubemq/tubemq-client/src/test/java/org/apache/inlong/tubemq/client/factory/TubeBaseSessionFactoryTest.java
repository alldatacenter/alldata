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

package org.apache.inlong.tubemq.client.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.consumer.PullMessageConsumer;
import org.apache.inlong.tubemq.client.consumer.PushMessageConsumer;
import org.apache.inlong.tubemq.client.producer.MessageProducer;
import org.apache.inlong.tubemq.client.producer.ProducerManager;
import org.apache.inlong.tubemq.corebase.cluster.MasterInfo;
import org.apache.inlong.tubemq.corebase.utils.AddressUtils;
import org.apache.inlong.tubemq.corerpc.client.ClientFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest(AddressUtils.class)
public class TubeBaseSessionFactoryTest {

    @Test
    public void testTubeBaseSessionFactory() throws Exception {
        TubeClientConfig config = mock(TubeClientConfig.class);
        when(config.getMasterInfo()).thenReturn(new MasterInfo("127.0.0.1:18080"));

        ClientFactory clientFactory = mock(ClientFactory.class);
        PowerMockito.mockStatic(AddressUtils.class);
        PowerMockito.when(AddressUtils.getLocalAddress()).thenReturn("127.0.0.1:18080");

        TubeBaseSessionFactory factory = new TubeBaseSessionFactory(clientFactory, config);
        assertFalse(factory.isShutdown());

        // Use reflection to change the producer manager to a mock one
        ProducerManager mockPM = mock(ProducerManager.class);
        Field field = factory.getClass().getDeclaredField("producerManager");
        field.setAccessible(true);
        field.set(factory, mockPM);
        MessageProducer producer = factory.createProducer();
        assertEquals(1, factory.getCurrClients().size());

        final PullMessageConsumer pullMessageConsumer = factory.createPullConsumer(
                new ConsumerConfig("127.0.0.1:18080", "test"));
        final PushMessageConsumer pushMessageConsumer = factory.createPushConsumer(
                new ConsumerConfig("127.0.0.1:18080", "test"));

        assertEquals(3, factory.getCurrClients().size());
        factory.removeClient(producer);
        factory.removeClient(pullMessageConsumer);
        factory.removeClient(pushMessageConsumer);
        assertEquals(0, factory.getCurrClients().size());

        factory.shutdown();
        assertTrue(factory.isShutdown());
    }
}
