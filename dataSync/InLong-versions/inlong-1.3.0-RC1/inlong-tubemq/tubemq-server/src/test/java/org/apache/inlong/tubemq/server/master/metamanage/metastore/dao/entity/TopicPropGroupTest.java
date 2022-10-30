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

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.server.common.statusdef.CleanPolType;
import org.junit.Assert;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

public class TopicPropGroupTest {

    @Test
    public void topicPropGroupTest() {
        // case 1
        TopicPropGroup propsCase1 = new TopicPropGroup();
        Assert.assertEquals(propsCase1.getNumTopicStores(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(propsCase1.getNumPartitions(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(propsCase1.getUnflushThreshold(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(propsCase1.getUnflushInterval(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(propsCase1.getUnflushDataHold(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(propsCase1.getMemCacheMsgSizeInMB(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(propsCase1.getMemCacheFlushIntvl(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(propsCase1.getMemCacheMsgCntInK(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertTrue((propsCase1.getAcceptPublish() == null));
        Assert.assertTrue((propsCase1.getAcceptSubscribe() == null));
        Assert.assertEquals(propsCase1.getDataStoreType(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(propsCase1.getDataPath(), "");
        Assert.assertEquals(propsCase1.getDeletePolicy(), "");
        // case 2
        int numTopicStores = 1;
        int numPartitions = 2;
        int unflushThreshold = 3;
        int unflushInterval = 4;
        int unflushDataHold = 5;
        int memCacheMsgSizeInMB = 6;
        int memCacheMsgCntInK = 7;
        int memCacheFlushIntvl = 8;
        boolean acceptPublish = true;
        boolean acceptSubscribe = false;
        String deletePolicy = "delete,12h";
        int dataStoreType = 9;
        String dataPath = "test\\test";
        TopicPropGroup propsCase2 =
                new TopicPropGroup(numTopicStores, numPartitions, unflushThreshold,
                        unflushInterval, unflushDataHold, memCacheMsgSizeInMB,
                        memCacheMsgCntInK, memCacheFlushIntvl, acceptPublish,
                        acceptSubscribe, deletePolicy, dataStoreType, dataPath);
        Assert.assertEquals(propsCase2.getNumTopicStores(), numTopicStores);
        Assert.assertEquals(propsCase2.getNumPartitions(), numPartitions);
        Assert.assertEquals(propsCase2.getUnflushThreshold(), unflushThreshold);
        Assert.assertEquals(propsCase2.getUnflushInterval(), unflushInterval);
        Assert.assertEquals(propsCase2.getUnflushDataHold(), unflushDataHold);
        Assert.assertEquals(propsCase2.getMemCacheMsgSizeInMB(), memCacheMsgSizeInMB);
        Assert.assertEquals(propsCase2.getMemCacheFlushIntvl(), memCacheFlushIntvl);
        Assert.assertEquals(propsCase2.getMemCacheMsgCntInK(), memCacheMsgCntInK);
        Assert.assertEquals(propsCase2.getAcceptPublish(), acceptPublish);
        Assert.assertEquals(propsCase2.getAcceptSubscribe(), acceptSubscribe);
        Assert.assertEquals(propsCase2.getDataStoreType(), dataStoreType);
        Assert.assertEquals(propsCase2.getDataPath(), dataPath);
        Assert.assertEquals(propsCase2.getDeletePolicy(), deletePolicy);
        Assert.assertEquals(propsCase2.getCleanPolicyType(), CleanPolType.CLEAN_POL_DELETE);
        Assert.assertEquals(propsCase2.getRetPeriodInMs(), 12 * 3600 * 1000);
        // case 3
        int newNumTopicStores = 101;
        final int newNumPartitions = 102;
        final int newUnflushThreshold = 103;
        final int newUnflushInterval = 104;
        final int newUnflushDataHold = 105;
        final int newMemCacheMsgSizeInMB = 106;
        final int newMemCacheMsgCntInK = 107;
        final int newMemCacheFlushIntvl = 108;
        final boolean newAcceptPublish = false;
        final boolean newAcceptSubscribe = true;
        final String newDeletePolicy = "delete,10h";
        final int newDataStoreType = 109;
        final String newDataPath = "newnew";
        TopicPropGroup propsCase3 = propsCase2.clone();
        Assert.assertTrue(propsCase3.isMatched(propsCase2));
        Assert.assertFalse(propsCase2.updModifyInfo(propsCase3));
        propsCase3.setNumTopicStores(newNumTopicStores);
        Assert.assertEquals(propsCase3.getNumTopicStores(), newNumTopicStores);
        Assert.assertNotEquals(propsCase3.getNumTopicStores(), numTopicStores);
        Assert.assertNotEquals(propsCase3.getNumTopicStores(), propsCase2.getNumTopicStores());
        propsCase3.setNumPartitions(newNumPartitions);
        Assert.assertEquals(propsCase3.getNumPartitions(), newNumPartitions);
        Assert.assertNotEquals(propsCase3.getNumPartitions(), numPartitions);
        Assert.assertNotEquals(propsCase3.getNumPartitions(), propsCase2.getNumPartitions());
        propsCase3.setUnflushThreshold(newUnflushThreshold);
        Assert.assertEquals(propsCase3.getUnflushThreshold(), newUnflushThreshold);
        Assert.assertNotEquals(propsCase3.getUnflushThreshold(), unflushThreshold);
        Assert.assertNotEquals(propsCase3.getUnflushThreshold(), propsCase2.getUnflushThreshold());
        propsCase3.setUnflushInterval(newUnflushInterval);
        Assert.assertEquals(propsCase3.getUnflushInterval(), newUnflushInterval);
        Assert.assertNotEquals(propsCase3.getUnflushInterval(), unflushInterval);
        Assert.assertNotEquals(propsCase3.getUnflushInterval(), propsCase2.getUnflushInterval());
        propsCase3.setUnflushDataHold(newUnflushDataHold);
        Assert.assertEquals(propsCase3.getUnflushDataHold(), newUnflushDataHold);
        Assert.assertNotEquals(propsCase3.getUnflushDataHold(), unflushDataHold);
        Assert.assertNotEquals(propsCase3.getUnflushDataHold(), propsCase2.getUnflushDataHold());
        propsCase3.setMemCacheMsgSizeInMB(newMemCacheMsgSizeInMB);
        Assert.assertEquals(propsCase3.getMemCacheMsgSizeInMB(), newMemCacheMsgSizeInMB);
        Assert.assertNotEquals(propsCase3.getMemCacheMsgSizeInMB(), memCacheMsgSizeInMB);
        Assert.assertNotEquals(propsCase3.getMemCacheMsgSizeInMB(), propsCase2.getMemCacheMsgSizeInMB());
        propsCase3.setMemCacheMsgCntInK(newMemCacheMsgCntInK);
        Assert.assertEquals(propsCase3.getMemCacheMsgCntInK(), newMemCacheMsgCntInK);
        Assert.assertNotEquals(propsCase3.getMemCacheMsgCntInK(), memCacheMsgCntInK);
        Assert.assertNotEquals(propsCase3.getMemCacheMsgCntInK(), propsCase2.getMemCacheMsgCntInK());
        propsCase3.setMemCacheFlushIntvl(newMemCacheFlushIntvl);
        Assert.assertEquals(propsCase3.getMemCacheFlushIntvl(), newMemCacheFlushIntvl);
        Assert.assertNotEquals(propsCase3.getMemCacheFlushIntvl(), memCacheFlushIntvl);
        Assert.assertNotEquals(propsCase3.getMemCacheFlushIntvl(), propsCase2.getMemCacheFlushIntvl());
        propsCase3.setAcceptPublish(newAcceptPublish);
        Assert.assertEquals(propsCase3.getAcceptPublish(), newAcceptPublish);
        Assert.assertNotEquals(propsCase3.getAcceptPublish(), acceptPublish);
        Assert.assertNotEquals(propsCase3.getAcceptPublish(), propsCase2.getAcceptPublish());
        propsCase3.setAcceptSubscribe(newAcceptSubscribe);
        Assert.assertEquals(propsCase3.getAcceptSubscribe(), newAcceptSubscribe);
        Assert.assertNotEquals(propsCase3.getAcceptSubscribe(), acceptSubscribe);
        Assert.assertNotEquals(propsCase3.getAcceptSubscribe(), propsCase2.getAcceptSubscribe());
        propsCase3.setDataStoreInfo(newDataStoreType, newDataPath);
        Assert.assertEquals(propsCase3.getDataStoreType(), newDataStoreType);
        Assert.assertNotEquals(propsCase3.getDataStoreType(), dataStoreType);
        Assert.assertNotEquals(propsCase3.getDataStoreType(), propsCase2.getDataStoreType());
        Assert.assertEquals(propsCase3.getDataPath(), newDataPath);
        Assert.assertNotEquals(propsCase3.getDataPath(), dataPath);
        Assert.assertNotEquals(propsCase3.getDataPath(), propsCase2.getDataPath());
        propsCase3.setDeletePolicy(newDeletePolicy);
        Assert.assertEquals(propsCase3.getDeletePolicy(), newDeletePolicy);
        Assert.assertNotEquals(propsCase3.getDeletePolicy(), deletePolicy);
        Assert.assertNotEquals(propsCase3.getDeletePolicy(), propsCase2.getDeletePolicy());
        Assert.assertEquals(propsCase3.getCleanPolicyType(), CleanPolType.CLEAN_POL_DELETE);
        Assert.assertEquals(propsCase3.getRetPeriodInMs(), 10 * 3600 * 1000);
        Assert.assertFalse(propsCase3.isMatched(propsCase2));
        Assert.assertTrue(propsCase2.updModifyInfo(propsCase3));
    }

    @Test
    public void getMapValuesTest() {
        Map<String, String> paramMap = new HashMap<>();
        // case 1
        TopicPropGroup entity1 = new TopicPropGroup();
        entity1.getConfigureInfo(paramMap, true);
        Assert.assertEquals(paramMap.size(), 0);
        paramMap.clear();
        // case 2
        TopicPropGroup entity2 = new TopicPropGroup(1, 2,
                3, 4, 5, 6,
                7, 8, true, false,
                "delete,22h", 9, "aaa");
        entity2.getConfigureInfo(paramMap, true);
        Assert.assertEquals(paramMap.size(), 13);
        Assert.assertEquals(paramMap.get("numTopicStores"), String.valueOf(1));
        Assert.assertEquals(paramMap.get("numPartitions"), String.valueOf(2));
        Assert.assertEquals(paramMap.get("unflushThreshold"), String.valueOf(3));
        Assert.assertEquals(paramMap.get("unflushInterval"), String.valueOf(4));
        Assert.assertEquals(paramMap.get("unflushDataHold"), String.valueOf(5));
        Assert.assertEquals(paramMap.get("memCacheMsgSizeInMB"), String.valueOf(6));
        Assert.assertEquals(paramMap.get("memCacheMsgCntInK"), String.valueOf(7));
        Assert.assertEquals(paramMap.get("memCacheFlushIntvl"), String.valueOf(8));
        Assert.assertEquals(paramMap.get("acceptPublish"), "true");
        Assert.assertEquals(paramMap.get("acceptSubscribe"), "false");
        Assert.assertEquals(paramMap.get("deletePolicy"), "delete,22h");
        Assert.assertEquals(paramMap.get("dataStoreType"), String.valueOf(9));
        Assert.assertEquals(paramMap.get("dataPath"), "aaa");
        paramMap.clear();
        // case 3
        entity2.getConfigureInfo(paramMap, false);
        Assert.assertEquals(paramMap.size(), 13);
        Assert.assertEquals(paramMap.get("numStore"), String.valueOf(1));
        Assert.assertEquals(paramMap.get("numPart"), String.valueOf(2));
        Assert.assertEquals(paramMap.get("unfDskMsgCnt"), String.valueOf(3));
        Assert.assertEquals(paramMap.get("unfDskInt"), String.valueOf(4));
        Assert.assertEquals(paramMap.get("unfDskDataSz"), String.valueOf(5));
        Assert.assertEquals(paramMap.get("cacheInMB"), String.valueOf(6));
        Assert.assertEquals(paramMap.get("unfMemMsgCnt"), String.valueOf(7));
        Assert.assertEquals(paramMap.get("unfMemInt"), String.valueOf(8));
        Assert.assertEquals(paramMap.get("accPub"), "true");
        Assert.assertEquals(paramMap.get("accSub"), "false");
        Assert.assertEquals(paramMap.get("delPol"), "delete,22h");
        Assert.assertEquals(paramMap.get("dStType"), String.valueOf(9));
        Assert.assertEquals(paramMap.get("dPath"), "aaa");
        paramMap.clear();
    }
}
