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

package org.apache.inlong.sdk.sort.impl;

import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.InLongTopicFetcher;
import org.apache.inlong.sdk.sort.api.InLongTopicManager;
import org.apache.inlong.sdk.sort.api.QueryConsumeConfig;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.entity.CacheZoneCluster;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ClientContext.class})
public class InLongTopicManagerImplTest {

    private InLongTopic inLongTopic;
    private ClientContext clientContext;
    private QueryConsumeConfig queryConsumeConfig;
    private InLongTopicManager inLongTopicManager;

    {
        System.setProperty("log4j2.disable.jmx", Boolean.TRUE.toString());

        inLongTopic = new InLongTopic();
        inLongTopic.setTopic("testTopic");
        inLongTopic.setPartitionId(0);
        inLongTopic.setTopicType("pulsar");

        CacheZoneCluster cacheZoneCluster = new CacheZoneCluster("clusterId", "bootstraps", "token");
        inLongTopic.setInLongCluster(cacheZoneCluster);

        clientContext = PowerMockito.mock(ClientContextImpl.class);

        SortClientConfig sortClientConfig = PowerMockito.mock(SortClientConfig.class);
        when(clientContext.getConfig()).thenReturn(sortClientConfig);
        when(sortClientConfig.getSortTaskId()).thenReturn("test");
        when(sortClientConfig.getUpdateMetaDataIntervalSec()).thenReturn(60);
        queryConsumeConfig = PowerMockito.mock(QueryConsumeConfigImpl.class);
        inLongTopicManager = new InLongTopicManagerImpl(clientContext, queryConsumeConfig);
    }

    @Test
    public void testAddFetcher() {
        InLongTopicManager inLongTopicManager = new InLongTopicManagerImpl(clientContext, queryConsumeConfig);

        InLongTopicFetcher inLongTopicFetcher = inLongTopicManager.addFetcher(inLongTopic);
        Assert.assertNull(inLongTopicFetcher);
    }

    @Test
    public void testRemoveFetcher() {

        InLongTopicFetcher inLongTopicFetcher = inLongTopicManager.removeFetcher(inLongTopic, true);
        Assert.assertNull(inLongTopicFetcher);

        ConcurrentHashMap<String, InLongTopicFetcher> fetchers = new ConcurrentHashMap<>();
        InLongTopicFetcher inLongTopicFetcherRmMock = PowerMockito.mock(InLongTopicFetcher.class);
        fetchers.put(inLongTopic.getTopicKey(), inLongTopicFetcherRmMock);

        Whitebox.setInternalState(inLongTopicManager, "fetchers", fetchers);

        inLongTopicFetcher = inLongTopicManager.removeFetcher(inLongTopic, true);
        Assert.assertNotNull(inLongTopicFetcher);

    }

    @Test
    public void testGetFetcher() {
        InLongTopicFetcher fetcher = inLongTopicManager.getFetcher(inLongTopic.getTopicKey());
        Assert.assertNull(fetcher);
        ConcurrentHashMap<String, InLongTopicFetcher> fetchers = new ConcurrentHashMap<>();
        InLongTopicFetcher inLongTopicFetcherRmMock = PowerMockito.mock(InLongTopicFetcher.class);
        fetchers.put(inLongTopic.getTopicKey(), inLongTopicFetcherRmMock);

        Whitebox.setInternalState(inLongTopicManager, "fetchers", fetchers);

        fetcher = inLongTopicManager.getFetcher(inLongTopic.getTopicKey());
        Assert.assertNotNull(fetcher);

    }

    @Test
    public void testGetManagedInLongTopics() {
        Set<String> managedInLongTopics = inLongTopicManager.getManagedInLongTopics();
        Assert.assertEquals(0, managedInLongTopics.size());

        ConcurrentHashMap<String, InLongTopicFetcher> fetchers = new ConcurrentHashMap<>();
        InLongTopicFetcher inLongTopicFetcherRmMock = PowerMockito.mock(InLongTopicFetcher.class);
        fetchers.put(inLongTopic.getTopicKey(), inLongTopicFetcherRmMock);
        Whitebox.setInternalState(inLongTopicManager, "fetchers", fetchers);
        managedInLongTopics = inLongTopicManager.getManagedInLongTopics();
        Assert.assertEquals(1, managedInLongTopics.size());

    }

    @Test
    public void testGetAllFetchers() {
        Collection<InLongTopicFetcher> allFetchers = inLongTopicManager.getAllFetchers();
        Assert.assertEquals(0, allFetchers.size());

        ConcurrentHashMap<String, InLongTopicFetcher> fetchers = new ConcurrentHashMap<>();
        InLongTopicFetcher inLongTopicFetcherRmMock = PowerMockito.mock(InLongTopicFetcher.class);
        fetchers.put(inLongTopic.getTopicKey(), inLongTopicFetcherRmMock);
        Whitebox.setInternalState(inLongTopicManager, "fetchers", fetchers);
        allFetchers = inLongTopicManager.getAllFetchers();
        Assert.assertEquals(1, allFetchers.size());
    }

    @Test
    public void testOfflineAllTp() {
        inLongTopicManager.offlineAllTp();
    }

    @Test
    public void testClean() {
        boolean clean = inLongTopicManager.clean();
        Assert.assertTrue(clean);
    }

    @Test
    public void testClose() {
        inLongTopicManager.close();
    }
}