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

package org.apache.inlong.tubemq.client.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.factory.TubeBaseSessionFactory;
import org.apache.inlong.tubemq.corebase.cluster.MasterInfo;
import org.apache.inlong.tubemq.corebase.utils.AddressUtils;
import org.apache.inlong.tubemq.corerpc.client.ClientFactory;
import org.apache.inlong.tubemq.corerpc.netty.NettyClientFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest(AddressUtils.class)
public class MessageFetchManagerTest {

    @Test
    public void testMessageFetchManager() throws Exception {
        TubeClientConfig clientConfig = mock(TubeClientConfig.class);
        PowerMockito.mockStatic(AddressUtils.class);
        PowerMockito.when(AddressUtils.getLocalAddress()).thenReturn("127.0.0.1");

        when(clientConfig.getMasterInfo()).thenReturn(new MasterInfo("127.0.0.1:18080"));
        ConsumerConfig config = new ConsumerConfig("127.0.0.1:18080", "test");
        ClientFactory clientFactory = new NettyClientFactory();
        TubeBaseSessionFactory factory = new TubeBaseSessionFactory(clientFactory, clientConfig);
        SimplePushMessageConsumer consumer = new SimplePushMessageConsumer(factory, config);
        MessageFetchManager fetchManager = new MessageFetchManager(config, consumer);

        Assert.assertFalse(fetchManager.isShutdown());
        fetchManager.startFetchWorkers();
        fetchManager.stopFetchWorkers(true);
        Assert.assertTrue(fetchManager.isShutdown());
    }
}
