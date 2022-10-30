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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity;

import java.util.Date;
import org.apache.inlong.tubemq.corebase.utils.KeyBuilderUtils;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbTopicConfEntity;
import org.junit.Assert;
import org.junit.Test;

public class TopicDeployEntityTest {

    @Test
    public void topicDeployEntityTest() {
        // case 1
        int brokerId1 = 1;
        String brokerIp1 = "127.0.0.1";
        int brokerPort1 = 3;
        String topicName1 = "test";
        int numPartitions1 = 4;
        int unflushThreshold1 = 5;
        int unflushInterval1 = 6;
        String deleteWhen1 = "";
        String deletePolicy1 = "delete,9h";
        boolean acceptPublish1 = false;
        boolean acceptSubscribe1 = true;
        int numTopicStores1 = 7;
        String attributes1 = "key=val&aaa=bbb";
        String createUser1 = "creater";
        Date createDate1 = new Date();
        String modifyUser1 = "modifyer";
        Date modifyDate1 = new Date();
        BdbTopicConfEntity bdbEntry1 =
                new BdbTopicConfEntity(brokerId1, brokerIp1, brokerPort1, topicName1,
                        numPartitions1, unflushThreshold1, unflushInterval1, deleteWhen1,
                        deletePolicy1, acceptPublish1, acceptSubscribe1, numTopicStores1,
                        attributes1, createUser1, createDate1, modifyUser1, modifyDate1);
        TopicDeployEntity deployEntity1 = new TopicDeployEntity(bdbEntry1);
        // check confEntity1
        Assert.assertEquals(deployEntity1.getRecordKey(), bdbEntry1.getRecordKey());
        Assert.assertEquals(deployEntity1.getBrokerId(), bdbEntry1.getBrokerId());
        Assert.assertEquals(deployEntity1.getBrokerIp(), bdbEntry1.getBrokerIp());
        Assert.assertEquals(deployEntity1.getBrokerPort(), bdbEntry1.getBrokerPort());
        Assert.assertEquals(deployEntity1.getTopicName(), bdbEntry1.getTopicName());
        Assert.assertEquals(deployEntity1.getTopicId(), bdbEntry1.getTopicId());
        Assert.assertEquals(deployEntity1.getBrokerAddress(), bdbEntry1.getBrokerAddress());
        Assert.assertEquals(deployEntity1.getDeployStatus().getCode(),
                bdbEntry1.getTopicStatusId());
        TopicPropGroup props1 = deployEntity1.getTopicProps();
        Assert.assertEquals(props1.getNumTopicStores(), bdbEntry1.getNumTopicStores());
        Assert.assertEquals(props1.getNumPartitions(), bdbEntry1.getNumPartitions());
        Assert.assertEquals(props1.getUnflushThreshold(), bdbEntry1.getUnflushThreshold());
        Assert.assertEquals(props1.getUnflushInterval(), bdbEntry1.getUnflushInterval());
        Assert.assertEquals(props1.getUnflushDataHold(), bdbEntry1.getUnflushDataHold());
        Assert.assertEquals(props1.getMemCacheMsgSizeInMB(), bdbEntry1.getMemCacheMsgSizeInMB());
        Assert.assertEquals(props1.getMemCacheFlushIntvl(), bdbEntry1.getMemCacheFlushIntvl());
        Assert.assertEquals(props1.getMemCacheMsgCntInK(), bdbEntry1.getMemCacheMsgCntInK());
        Assert.assertEquals(props1.getAcceptPublish(), bdbEntry1.getAcceptPublish());
        Assert.assertEquals(props1.getAcceptSubscribe(), bdbEntry1.getAcceptSubscribe());
        Assert.assertEquals(props1.getDataStoreType(), bdbEntry1.getDataStoreType());
        Assert.assertEquals(props1.getDataPath(), "");
        Assert.assertEquals(props1.getDeletePolicy(), bdbEntry1.getDeletePolicy());
        // case 2
        final int dataVerId2 = 25;
        final int topicNameId2 = 95;
        final int brokerPort2 = 33;
        final String brokerIp2 = "127.0.0.2";
        final TopicStatus deployStatus2 = TopicStatus.STATUS_TOPIC_HARD_REMOVE;
        final int numPartitions2 = 8;
        final int unflushThreshold2 = 2;
        final int unflushInterval2 = 5;
        final String deletePolicy2 = "delete,3h";
        final boolean acceptPublish2 = true;
        final boolean acceptSubscribe2 = false;
        final int numTopicStores2 = 3;
        final String attributes2 = "ay=val&aaa=bbb";
        final int dataStoreType2 = 5;
        final String dataPath2 = "aaa";
        TopicPropGroup topicProps2 = props1.clone();
        topicProps2.setNumTopicStores(numTopicStores2);
        topicProps2.setNumPartitions(numPartitions2);
        topicProps2.setUnflushThreshold(unflushThreshold2);
        topicProps2.setUnflushInterval(unflushInterval2);
        topicProps2.setAcceptPublish(acceptPublish2);
        topicProps2.setAcceptSubscribe(acceptSubscribe2);
        topicProps2.setDeletePolicy(deletePolicy2);
        topicProps2.setDataStoreInfo(dataStoreType2, dataPath2);
        TopicDeployEntity deployEntity2 = deployEntity1.clone();
        Assert.assertTrue(deployEntity2.isMatched(deployEntity1, true));
        Assert.assertTrue(deployEntity2.updModifyInfo(dataVerId2,
                topicNameId2, brokerPort2, brokerIp2, deployStatus2, topicProps2));
        TopicDeployEntity deployEntity31 = deployEntity2.clone();
        BdbTopicConfEntity bdbEntry3 =
                deployEntity31.buildBdbTopicConfEntity();
        TopicDeployEntity deployEntity32 = new TopicDeployEntity(bdbEntry3);
        Assert.assertTrue(deployEntity32.isDataEquals(deployEntity32));
        // check value
        Assert.assertEquals(deployEntity32.getRecordKey(), bdbEntry3.getRecordKey());
        Assert.assertEquals(deployEntity32.getBrokerId(), bdbEntry3.getBrokerId());
        Assert.assertEquals(deployEntity32.getBrokerIp(), bdbEntry3.getBrokerIp());
        Assert.assertEquals(deployEntity32.getBrokerPort(), bdbEntry3.getBrokerPort());
        Assert.assertEquals(deployEntity32.getTopicName(), bdbEntry3.getTopicName());
        Assert.assertEquals(deployEntity32.getTopicId(), bdbEntry3.getTopicId());
        Assert.assertEquals(deployEntity32.getBrokerAddress(), bdbEntry3.getBrokerAddress());
        Assert.assertEquals(deployEntity32.getDeployStatus().getCode(),
                bdbEntry3.getTopicStatusId());
        TopicPropGroup props2 = deployEntity32.getTopicProps();
        Assert.assertEquals(props2.getNumTopicStores(), bdbEntry3.getNumTopicStores());
        Assert.assertEquals(props2.getNumPartitions(), bdbEntry3.getNumPartitions());
        Assert.assertEquals(props2.getUnflushThreshold(), bdbEntry3.getUnflushThreshold());
        Assert.assertEquals(props2.getUnflushInterval(), bdbEntry3.getUnflushInterval());
        Assert.assertEquals(props2.getUnflushDataHold(), bdbEntry3.getUnflushDataHold());
        Assert.assertEquals(props2.getMemCacheMsgSizeInMB(), bdbEntry3.getMemCacheMsgSizeInMB());
        Assert.assertEquals(props2.getMemCacheFlushIntvl(), bdbEntry3.getMemCacheFlushIntvl());
        Assert.assertEquals(props2.getMemCacheMsgCntInK(), bdbEntry3.getMemCacheMsgCntInK());
        Assert.assertEquals(props2.getAcceptPublish(), bdbEntry3.getAcceptPublish());
        Assert.assertEquals(props2.getAcceptSubscribe(), bdbEntry3.getAcceptSubscribe());
        Assert.assertEquals(props2.getDataStoreType(), bdbEntry3.getDataStoreType());
        Assert.assertEquals(props2.getDataPath(), bdbEntry3.getDataPath());
        Assert.assertEquals(props2.getDeletePolicy(), bdbEntry3.getDeletePolicy());
        //
        Assert.assertEquals(deployEntity32.getDataVerId(), dataVerId2);
        Assert.assertEquals(deployEntity32.getBrokerId(), brokerId1);
        Assert.assertEquals(deployEntity32.getBrokerIp(), brokerIp2);
        Assert.assertEquals(deployEntity32.getBrokerPort(), brokerPort2);
        Assert.assertEquals(deployEntity32.getTopicName(), topicName1);
        Assert.assertEquals(deployEntity32.getTopicId(), topicNameId2);
        Assert.assertEquals(deployEntity32.getBrokerAddress(),
                KeyBuilderUtils.buildAddressInfo(brokerIp2, brokerPort2));
        Assert.assertEquals(deployEntity32.getDeployStatus(), deployStatus2);
        Assert.assertEquals(props2.getNumTopicStores(), numTopicStores2);
        Assert.assertEquals(props2.getNumPartitions(), numPartitions2);
        Assert.assertEquals(props2.getUnflushThreshold(), unflushThreshold2);
        Assert.assertEquals(props2.getUnflushInterval(), unflushInterval2);
        Assert.assertEquals(props2.getAcceptPublish(), acceptPublish2);
        Assert.assertEquals(props2.getAcceptSubscribe(), acceptSubscribe2);
        Assert.assertEquals(props2.getDataStoreType(), dataStoreType2);
        Assert.assertEquals(props2.getDataPath(), dataPath2);
        Assert.assertEquals(props2.getDeletePolicy(), deletePolicy2);
    }

}
