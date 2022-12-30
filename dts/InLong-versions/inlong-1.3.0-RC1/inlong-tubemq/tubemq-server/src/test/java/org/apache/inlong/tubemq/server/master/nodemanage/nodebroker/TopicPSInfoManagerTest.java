/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.nodemanage.nodebroker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TopicPSInfoManagerTest {
    private TopicPSInfoManager topicPSInfoManager;

    @Before
    public void setUp() throws Exception {
        topicPSInfoManager = new TopicPSInfoManager(null);
    }

    @After
    public void tearDown() throws Exception {
        topicPSInfoManager.clear();
    }

    @Test
    public void topicSubInfo() {
        Set<String> groupSet = new HashSet<>();
        groupSet.add("group_001");
        groupSet.add("group_002");
        groupSet.add("group_003");
        Set<String> topicSet = new HashSet<>();
        topicSet.add("topic001");
        for (String groupName : groupSet) {
            topicPSInfoManager.addGroupSubTopicInfo(groupName, topicSet);
        }

        Set<String> gs1 = topicPSInfoManager.getTopicSubInfo("topic001");
        Assert.assertEquals(3, gs1.size());

        topicPSInfoManager.rmvGroupSubTopicInfo("group_001", topicSet);
        topicPSInfoManager.rmvGroupSubTopicInfo("group_002", topicSet);
        gs1 = topicPSInfoManager.getTopicSubInfo("topic001");
        Assert.assertEquals(1, gs1.size());
    }

    @Test
    public void topicPubInfo() {
        HashSet<String> topicList = new HashSet<>();
        topicList.add("topic001");
        topicList.add("topic002");
        topicList.add("topic003");

        topicPSInfoManager.addProducerTopicPubInfo("producer_001", topicList);
        Set<String> ti1 = topicPSInfoManager.getTopicPubInfo("topic001");
        Assert.assertEquals(1, ti1.size());
        Assert.assertTrue(ti1.contains("producer_001"));

        topicPSInfoManager.rmvProducerTopicPubInfo("producer_001",
                new HashSet<>(Arrays.asList("topic001", "topic002")));

        Set<String> ti2 = topicPSInfoManager.getTopicPubInfo("topic003");
        Assert.assertEquals(1, ti2.size());
        Assert.assertTrue(ti2.contains("producer_001"));
    }
}
