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

package org.apache.inlong.sdk.sort.impl.kafka;

import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.entity.CacheZoneCluster;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.impl.ClientContextImpl;
import org.apache.inlong.sdk.sort.stat.SortClientStateCounter;
import org.apache.inlong.sdk.sort.stat.StatManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ClientContext.class})
public class InLongKafkaFetcherImplTest {

    private ClientContext clientContext;
    private InLongTopic inLongTopic;
    private SortClientConfig sortClientConfig;
    private StatManager statManager;

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

        CacheZoneCluster cacheZoneCluster = new CacheZoneCluster("clusterId", "bootstraps", "token");
        inLongTopic.setInLongCluster(cacheZoneCluster);
        clientContext = PowerMockito.mock(ClientContextImpl.class);

        sortClientConfig = PowerMockito.mock(SortClientConfig.class);
        statManager = PowerMockito.mock(StatManager.class);

        when(clientContext.getConfig()).thenReturn(sortClientConfig);
        when(clientContext.getStatManager()).thenReturn(statManager);
        SortClientStateCounter sortClientStateCounter = new SortClientStateCounter("sortTaskId",
                cacheZoneCluster.getClusterId(),
                inLongTopic.getTopic(), 0);
        when(statManager.getStatistics(anyString(), anyString(), anyString())).thenReturn(sortClientStateCounter);
        when(sortClientConfig.getSortTaskId()).thenReturn("sortTaskId");

    }

    @Test
    public void pause() {
        InLongKafkaFetcherImpl inLongTopicFetcher = new InLongKafkaFetcherImpl(inLongTopic, clientContext);
        inLongTopicFetcher.pause();
    }

    @Test
    public void resume() {
        InLongKafkaFetcherImpl inLongTopicFetcher = new InLongKafkaFetcherImpl(inLongTopic, clientContext);
        inLongTopicFetcher.resume();
    }

    @Test
    public void close() {
        InLongKafkaFetcherImpl inLongTopicFetcher = new InLongKafkaFetcherImpl(inLongTopic, clientContext);
        boolean close = inLongTopicFetcher.close();
        Assert.assertTrue(close);
    }
}