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
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbBrokerConfEntity;
import org.junit.Assert;
import org.junit.Test;

public class BrokerConfEntityTest {

    @Test
    public void brokerConfEntityTest() {
        // case 1
        int brokerId1 = 22;
        String brokerIp1 = "127.0.0.1";
        int brokerPort1 = 555;
        int numPartitions1 = 2;
        int unflushThreshold1 = 3;
        int unflushInterval1 = 4;
        String deleteWhen1 = "";
        String deletePolicy1 = "delete,5h";
        int manageStatus1 = 1;
        boolean acceptPublish1 = true;
        boolean acceptSubscribe1 = false;
        String attributes1 = "key1=test&key2=tas";
        boolean isConfDataUpdated1 = true;
        boolean isBrokerLoaded1 = false;
        String createUser1 = "creater";
        Date createDate1 = new Date();
        String modifyUser1 = "modifyer";
        Date modifyDate1 = new Date();
        BdbBrokerConfEntity bdbEntity1 =
                new BdbBrokerConfEntity(brokerId1, brokerIp1, brokerPort1, numPartitions1,
                        unflushThreshold1, unflushInterval1, deleteWhen1, deletePolicy1,
                        manageStatus1, acceptPublish1, acceptSubscribe1, attributes1,
                        isConfDataUpdated1, isBrokerLoaded1, createUser1, createDate1,
                        modifyUser1, modifyDate1);
        BrokerConfEntity confEntity1 = new BrokerConfEntity(bdbEntity1);
        // check confEntity1
        Assert.assertEquals(confEntity1.getBrokerId(), brokerId1);
        Assert.assertEquals(confEntity1.getBrokerIp(), brokerIp1);
        Assert.assertEquals(confEntity1.getBrokerPort(), brokerPort1);
        Assert.assertEquals(confEntity1.getBrokerTLSPort(), bdbEntity1.getBrokerTLSPort());
        Assert.assertEquals(confEntity1.getBrokerWebPort(), bdbEntity1.getBrokerWebPort());
        Assert.assertEquals(confEntity1.getGroupId(), bdbEntity1.getBrokerGroupId());
        Assert.assertEquals(confEntity1.getManageStatus().getCode(), bdbEntity1.getManageStatus());
        Assert.assertEquals(confEntity1.getRegionId(), bdbEntity1.getRegionId());
        Assert.assertEquals(confEntity1.getCreateUser(), bdbEntity1.getRecordCreateUser());
        Assert.assertEquals(confEntity1.getModifyUser(), bdbEntity1.getRecordModifyUser());
        Assert.assertEquals(confEntity1.getCreateDate(), bdbEntity1.getRecordCreateDate());
        Assert.assertEquals(confEntity1.getModifyDate(), bdbEntity1.getRecordModifyDate());
        TopicPropGroup props1 = confEntity1.getTopicProps();
        Assert.assertEquals(props1.getNumTopicStores(), bdbEntity1.getNumTopicStores());
        Assert.assertEquals(props1.getNumPartitions(), bdbEntity1.getDftNumPartitions());
        Assert.assertEquals(props1.getUnflushThreshold(), bdbEntity1.getDftUnflushThreshold());
        Assert.assertEquals(props1.getUnflushInterval(), bdbEntity1.getDftUnflushInterval());
        Assert.assertEquals(props1.getUnflushDataHold(), bdbEntity1.getDftUnFlushDataHold());
        Assert.assertEquals(props1.getMemCacheMsgSizeInMB(), bdbEntity1.getDftMemCacheMsgSizeInMB());
        Assert.assertEquals(props1.getMemCacheFlushIntvl(), bdbEntity1.getDftMemCacheFlushIntvl());
        Assert.assertEquals(props1.getMemCacheMsgCntInK(), bdbEntity1.getDftMemCacheMsgCntInK());
        Assert.assertEquals(props1.getAcceptPublish(), bdbEntity1.isAcceptPublish());
        Assert.assertEquals(props1.getAcceptSubscribe(), bdbEntity1.isAcceptSubscribe());
        Assert.assertEquals(props1.getDataStoreType(), bdbEntity1.getDataStoreType());
        Assert.assertEquals(props1.getDataPath(), "");
        Assert.assertEquals(props1.getDeletePolicy(), bdbEntity1.getDftDeletePolicy());
        // case 2
        int dataVerId1 = 25;
        int regionId1 = 95343;
        int numTopicStores1 = 9;
        int brokerTLSPort1 = 666;
        int brokerWebPort1 = 888;
        int memCacheFlushIntvl1 = 200;
        int memCacheMsgCntInK1 = 250;
        int memCacheMsgSizeInMB1 = 3;
        int unFlushDataHold1 = 1000;
        int groupId1 = 55;
        int dataType1 = 2;
        String dataPath1 = "/test";
        bdbEntity1.setRegionId(regionId1);
        bdbEntity1.setDataVerId(dataVerId1);
        bdbEntity1.setBrokerGroupId(groupId1);
        bdbEntity1.setBrokerTLSPort(brokerTLSPort1);
        bdbEntity1.setBrokerWebPort(brokerWebPort1);
        bdbEntity1.setNumTopicStores(numTopicStores1);
        bdbEntity1.setDftMemCacheFlushIntvl(memCacheFlushIntvl1);
        bdbEntity1.setDftMemCacheMsgCntInK(memCacheMsgCntInK1);
        bdbEntity1.setDftMemCacheMsgSizeInMB(memCacheMsgSizeInMB1);
        bdbEntity1.setDftUnFlushDataHold(unFlushDataHold1);
        bdbEntity1.setDataStore(dataType1, dataPath1);
        BrokerConfEntity confEntity2 = new BrokerConfEntity(bdbEntity1);
        Assert.assertEquals(confEntity2.getBrokerId(), bdbEntity1.getBrokerId());
        Assert.assertEquals(confEntity2.getBrokerIp(), bdbEntity1.getBrokerIp());
        Assert.assertEquals(confEntity2.getBrokerPort(), bdbEntity1.getBrokerPort());
        Assert.assertEquals(confEntity2.getBrokerTLSPort(), bdbEntity1.getBrokerTLSPort());
        Assert.assertEquals(confEntity2.getBrokerWebPort(), bdbEntity1.getBrokerWebPort());
        Assert.assertEquals(confEntity2.getGroupId(), bdbEntity1.getBrokerGroupId());
        Assert.assertEquals(confEntity2.getManageStatus().getCode(), bdbEntity1.getManageStatus());
        Assert.assertEquals(confEntity2.getRegionId(), bdbEntity1.getRegionId());
        TopicPropGroup props2 = confEntity2.getTopicProps();
        Assert.assertEquals(props2.getNumTopicStores(), bdbEntity1.getNumTopicStores());
        Assert.assertEquals(props2.getNumPartitions(), bdbEntity1.getDftNumPartitions());
        Assert.assertEquals(props2.getUnflushThreshold(), bdbEntity1.getDftUnflushThreshold());
        Assert.assertEquals(props2.getUnflushInterval(), bdbEntity1.getDftUnflushInterval());
        Assert.assertEquals(props2.getUnflushDataHold(), bdbEntity1.getDftUnFlushDataHold());
        Assert.assertEquals(props2.getMemCacheMsgSizeInMB(), bdbEntity1.getDftMemCacheMsgSizeInMB());
        Assert.assertEquals(props2.getMemCacheFlushIntvl(), bdbEntity1.getDftMemCacheFlushIntvl());
        Assert.assertEquals(props2.getMemCacheMsgCntInK(), bdbEntity1.getDftMemCacheMsgCntInK());
        Assert.assertEquals(props2.getAcceptPublish(), bdbEntity1.isAcceptPublish());
        Assert.assertEquals(props2.getAcceptSubscribe(), bdbEntity1.isAcceptSubscribe());
        Assert.assertEquals(props2.getDataStoreType(), bdbEntity1.getDataStoreType());
        Assert.assertEquals(props2.getDataPath(), bdbEntity1.getDataPath());
        Assert.assertEquals(props2.getDeletePolicy(), bdbEntity1.getDftDeletePolicy());
        // check value
        Assert.assertEquals(confEntity2.getDataVerId(), dataVerId1);
        Assert.assertEquals(confEntity2.getBrokerId(), brokerId1);
        Assert.assertEquals(confEntity2.getBrokerIp(), brokerIp1);
        Assert.assertEquals(confEntity2.getBrokerPort(), brokerPort1);
        Assert.assertEquals(confEntity2.getBrokerTLSPort(), brokerTLSPort1);
        Assert.assertEquals(confEntity2.getBrokerWebPort(), brokerWebPort1);
        Assert.assertEquals(confEntity2.getGroupId(), groupId1);
        Assert.assertEquals(confEntity2.getManageStatus().getCode(), manageStatus1);
        Assert.assertEquals(confEntity2.getRegionId(), regionId1);
        Assert.assertEquals(props2.getNumTopicStores(), numTopicStores1);
        Assert.assertEquals(props2.getNumPartitions(), numPartitions1);
        Assert.assertEquals(props2.getUnflushThreshold(), unflushThreshold1);
        Assert.assertEquals(props2.getUnflushInterval(), unflushInterval1);
        Assert.assertEquals(props2.getUnflushDataHold(), unFlushDataHold1);
        Assert.assertEquals(props2.getMemCacheMsgSizeInMB(), memCacheMsgSizeInMB1);
        Assert.assertEquals(props2.getMemCacheFlushIntvl(), memCacheFlushIntvl1);
        Assert.assertEquals(props2.getMemCacheMsgCntInK(), memCacheMsgCntInK1);
        Assert.assertEquals(props2.getAcceptPublish(), acceptPublish1);
        Assert.assertEquals(props2.getAcceptSubscribe(), acceptSubscribe1);
        Assert.assertEquals(props2.getDataStoreType(), dataType1);
        Assert.assertEquals(props2.getDataPath(), dataPath1);
        Assert.assertEquals(props2.getDeletePolicy(), deletePolicy1);
        Assert.assertEquals(confEntity2.getCreateUser(), createUser1);
        Assert.assertEquals(confEntity2.getModifyUser(), modifyUser1);
        Assert.assertEquals(confEntity2.getCreateDate(), createDate1);
        Assert.assertEquals(confEntity2.getModifyDate(), modifyDate1);
        // case 3
        long dataVerId3 = 777;
        int brokerPort3 = 29;
        int brokerTlsPort3 = 39;
        int brokerWebPort3 = 49;
        int regionId3 = 59;
        int groupId3 = 69;
        ManageStatus manageStatus3 = ManageStatus.STATUS_MANAGE_OFFLINE;
        int numTopicStores3 = 1;
        int numPartitions3 = 2;
        int unflushThreshold3 = 3;
        int unflushInterval3 = 4;
        int unflushDataHold3 = 5;
        int memCacheMsgSizeInMB3 = 6;
        int memCacheMsgCntInK3 = 7;
        int memCacheFlushIntvl3 = 8;
        boolean acceptPublish3 = true;
        boolean acceptSubscribe3 = false;
        String deletePolicy3 = "delete,12h";
        int dataStoreType3 = 9;
        String dataPath3 = "testasest";
        TopicPropGroup topicProps3 =
                new TopicPropGroup(numTopicStores3, numPartitions3, unflushThreshold3,
                        unflushInterval3, unflushDataHold3, memCacheMsgSizeInMB3,
                        memCacheMsgCntInK3, memCacheFlushIntvl3, acceptPublish3,
                        acceptSubscribe3, deletePolicy3, dataStoreType3, dataPath3);
        BrokerConfEntity confEntity31 = confEntity2.clone();
        Assert.assertTrue(confEntity31.isDataEquals(confEntity2));
        Assert.assertTrue(confEntity31.updModifyInfo(dataVerId3, brokerPort3,
                brokerTlsPort3, brokerWebPort3, regionId3, groupId3, manageStatus3, topicProps3));
        BdbBrokerConfEntity bdbEntry3 =
                confEntity31.buildBdbBrokerConfEntity();
        BrokerConfEntity confEntity32 = new BrokerConfEntity(bdbEntry3);
        Assert.assertTrue(confEntity32.isDataEquals(confEntity31));

    }

}
