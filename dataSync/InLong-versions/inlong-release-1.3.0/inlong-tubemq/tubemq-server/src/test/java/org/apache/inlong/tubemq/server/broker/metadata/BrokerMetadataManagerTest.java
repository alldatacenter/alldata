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

package org.apache.inlong.tubemq.server.broker.metadata;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * BrokerMetadataManage test
 */
public class BrokerMetadataManagerTest {

    // brokerMetadataManage
    BrokerMetadataManager brokerMetadataManager;

    @Test
    public void updateBrokerTopicConfigMap() {
        brokerMetadataManager = new BrokerMetadataManager();
        // topic default config
        String newBrokerDefMetaConfInfo = "1:true:true:1000:10000:0,0,6:delete,168h:1:1000:1024:1000:1000";
        List<String> newTopicMetaConfInfoList = new LinkedList<>();
        // add topic custom config.
        newTopicMetaConfInfoList.add("topic1:2:true:true:1000:10000:0,0,6:delete,168h:1:1000:1024:1000:1000:1");
        newTopicMetaConfInfoList.add("topic2:4:true:true:1000:10000:0,0,6:delete,168h:1:1000:1024:1000:1000:1");
        brokerMetadataManager.updateBrokerTopicConfigMap(0L, 0,
                newBrokerDefMetaConfInfo, newTopicMetaConfInfoList, true, new StringBuilder());
        // get topic custom config.
        long count = brokerMetadataManager.getNumPartitions("topic2");
        Assert.assertEquals(count, 4);
        // add topic custom config.
        newTopicMetaConfInfoList.add("topic3:6:true:true:1000:10000:0,0,6:delete,168h:1:1000:1024:1000:1000:1");
        brokerMetadataManager.updateBrokerTopicConfigMap(0L, 1,
                newBrokerDefMetaConfInfo, newTopicMetaConfInfoList, true, new StringBuilder());
        count = brokerMetadataManager.getNumPartitions("topic3");
        Assert.assertEquals(count, 6);
    }

    @Test
    public void updateBrokerRemoveTopicMap() {
        brokerMetadataManager = new BrokerMetadataManager();
        String newBrokerDefMetaConfInfo = "1:true:true:1000:10000:0,0,6:delete,168h:1:1000:1024:1000:1000";
        List<String> newTopicMetaConfInfoList = new LinkedList<>();
        // add topic custom config.
        newTopicMetaConfInfoList.add("topic1:2:true:true:1000:10000:0,0,6:delete,168h:1:1000:1024:1000:1000:1");
        newTopicMetaConfInfoList.add("topic2:4:true:true:1000:10000:0,0,6:delete,168h:1:1000:1024:1000:1000:1");
        brokerMetadataManager.updateBrokerTopicConfigMap(0L, 0,
                newBrokerDefMetaConfInfo, newTopicMetaConfInfoList, true, new StringBuilder());
        Map<String, TopicMetadata> topicMetadataMap = brokerMetadataManager.getRemovedTopicConfigMap();
        Assert.assertEquals(topicMetadataMap.size(), 0);
        List<String> rmvTopics = new LinkedList<>();
        rmvTopics.add("topic2:4:true:true:1000:10000:0,0,6:delete,168h:1:1000:1024:1000:1000:1");
        // update topic custom config.
        brokerMetadataManager.updateBrokerRemoveTopicMap(true, rmvTopics, new StringBuilder());
        topicMetadataMap = brokerMetadataManager.getRemovedTopicConfigMap();
        Assert.assertEquals(topicMetadataMap.size(), 1);
    }
}
