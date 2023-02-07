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

package org.apache.inlong.sdk.sort.manager;

import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.InlongTopicManagerFactory;
import org.apache.inlong.sdk.sort.api.QueryConsumeConfig;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.api.TopicFetcher;
import org.apache.inlong.sdk.sort.api.TopicManager;
import org.apache.inlong.sdk.sort.entity.CacheZoneCluster;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.impl.ClientContextImpl;
import org.apache.inlong.sdk.sort.impl.QueryConsumeConfigImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ClientContext.class})
public class InlongSingleTopicManagerTest {

    private InLongTopic inLongTopic;
    private ClientContext clientContext;
    private QueryConsumeConfig queryConsumeConfig;
    private TopicManager topicManager;

    {
        System.setProperty("log4j2.disable.jmx", Boolean.TRUE.toString());

        inLongTopic = new InLongTopic();
        inLongTopic.setTopic("testTopic");
        inLongTopic.setPartitionId(0);
        inLongTopic.setTopicType("pulsar");
        inLongTopic.setProperties(new HashMap<>());

        CacheZoneCluster cacheZoneCluster = new CacheZoneCluster("clusterId", "bootstraps", "token");
        inLongTopic.setInLongCluster(cacheZoneCluster);

        clientContext = PowerMockito.mock(ClientContextImpl.class);

        SortClientConfig sortClientConfig = PowerMockito.mock(SortClientConfig.class);
        when(clientContext.getConfig()).thenReturn(sortClientConfig);
        when(sortClientConfig.getSortTaskId()).thenReturn("test");
        when(sortClientConfig.getUpdateMetaDataIntervalSec()).thenReturn(60);
        queryConsumeConfig = PowerMockito.mock(QueryConsumeConfigImpl.class);
        topicManager = InlongTopicManagerFactory
                .createSingleTopicManager(clientContext, queryConsumeConfig);
    }

    @Test
    public void testAddFetcher() {
        TopicManager inLongTopicManager = InlongTopicManagerFactory
                .createSingleTopicManager(clientContext, queryConsumeConfig);

        TopicFetcher inLongTopicFetcher = inLongTopicManager.addTopic(inLongTopic);
        Assert.assertNull(inLongTopicFetcher);
    }

    @Test
    public void testRemoveFetcher() {

        TopicFetcher topicFetcher = topicManager.removeTopic(inLongTopic, true);
        Assert.assertNull(topicFetcher);

        ConcurrentHashMap<String, TopicFetcher> fetchers = new ConcurrentHashMap<>();
        TopicFetcher inLongTopicFetcherRmMock = PowerMockito.mock(TopicFetcher.class);
        fetchers.put(inLongTopic.getTopicKey(), inLongTopicFetcherRmMock);

        Whitebox.setInternalState(topicManager, "fetchers", fetchers);

        topicFetcher = topicManager.removeTopic(inLongTopic, true);
        Assert.assertNotNull(topicFetcher);

    }

    @Test
    public void testGetFetcher() {
        TopicFetcher fetcher = topicManager.getFetcher(inLongTopic.getTopicKey());
        Assert.assertNull(fetcher);
        ConcurrentHashMap<String, TopicFetcher> fetchers = new ConcurrentHashMap<>();
        TopicFetcher inLongTopicFetcherRmMock = PowerMockito.mock(TopicFetcher.class);
        fetchers.put(inLongTopic.getTopicKey(), inLongTopicFetcherRmMock);

        Whitebox.setInternalState(topicManager, "fetchers", fetchers);

        fetcher = topicManager.getFetcher(inLongTopic.getTopicKey());
        Assert.assertNotNull(fetcher);

    }

    @Test
    public void testGetManagedInLongTopics() {
        Set<String> managedInLongTopics = topicManager.getManagedInLongTopics();
        Assert.assertEquals(0, managedInLongTopics.size());

        ConcurrentHashMap<String, TopicFetcher> fetchers = new ConcurrentHashMap<>();
        TopicFetcher inLongTopicFetcherRmMock = PowerMockito.mock(TopicFetcher.class);
        fetchers.put(inLongTopic.getTopicKey(), inLongTopicFetcherRmMock);
        Whitebox.setInternalState(topicManager, "fetchers", fetchers);
        managedInLongTopics = topicManager.getManagedInLongTopics();
        Assert.assertEquals(1, managedInLongTopics.size());

    }

    @Test
    public void testGetAllFetchers() {
        Collection<TopicFetcher> allFetchers = topicManager.getAllFetchers();
        Assert.assertEquals(0, allFetchers.size());

        ConcurrentHashMap<String, TopicFetcher> fetchers = new ConcurrentHashMap<>();
        TopicFetcher inLongTopicFetcherRmMock = PowerMockito.mock(TopicFetcher.class);
        fetchers.put(inLongTopic.getTopicKey(), inLongTopicFetcherRmMock);
        Whitebox.setInternalState(topicManager, "fetchers", fetchers);
        allFetchers = topicManager.getAllFetchers();
        Assert.assertEquals(1, allFetchers.size());
    }

    @Test
    public void offlineAllTopicsAndPartitions() {
        topicManager.offlineAllTopicsAndPartitions();
    }

    @Test
    public void testClean() {
        boolean clean = topicManager.clean();
        Assert.assertTrue(clean);
    }

    @Test
    public void testClose() {
        topicManager.close();
    }

}