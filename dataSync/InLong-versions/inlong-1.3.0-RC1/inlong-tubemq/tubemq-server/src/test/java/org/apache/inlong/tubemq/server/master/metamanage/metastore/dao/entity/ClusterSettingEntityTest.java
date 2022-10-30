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
import java.util.HashMap;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.SettingValidUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbClusterSettingEntity;
import org.junit.Assert;
import org.junit.Test;

public class ClusterSettingEntityTest {

    @Test
    public void clusterSettingEntityTest() {
        // case 1
        ClusterSettingEntity setting1 = new ClusterSettingEntity();
        setting1.fillDefaultValue();
        Assert.assertEquals(setting1.getBrokerPort(), TBaseConstants.META_DEFAULT_BROKER_PORT);
        Assert.assertEquals(setting1.getBrokerTLSPort(), TBaseConstants.META_DEFAULT_BROKER_TLS_PORT);
        Assert.assertEquals(setting1.getBrokerWebPort(), TBaseConstants.META_DEFAULT_BROKER_WEB_PORT);
        Assert.assertEquals(setting1.getMaxMsgSizeInMB(), TBaseConstants.META_MIN_ALLOWED_MESSAGE_SIZE_MB);
        Assert.assertEquals(setting1.getMaxMsgSizeInB(),
                SettingValidUtils.validAndXfeMaxMsgSizeFromMBtoB(
                        TBaseConstants.META_MIN_ALLOWED_MESSAGE_SIZE_MB));

        Assert.assertEquals(setting1.getQryPriorityId(), TServerConstants.QRY_PRIORITY_DEF_VALUE);
        Assert.assertEquals(setting1.getGloFlowCtrlStatus(), EnableStatus.STATUS_DISABLE);
        Assert.assertEquals(setting1.getGloFlowCtrlRuleCnt(), 0);
        Assert.assertEquals(setting1.getGloFlowCtrlRuleInfo(), TServerConstants.BLANK_FLOWCTRL_RULES);
        TopicPropGroup defProps = new TopicPropGroup();
        defProps.fillDefaultValue();
        Assert.assertEquals(setting1.getClsDefTopicProps(), defProps);
        // case 2
        String recordKey = "test_key";
        long configId = 2223335555L;
        int brokerPort = 8888;
        int brokerTLSPort = 9999;
        int brokerWebPort = 7777;
        int numTopicStores = 9;
        int numPartitions = 10;
        int unflushThreshold = 20;
        int unflushInterval = 25;
        int unflushDataHold = 30;
        int memCacheMsgCntInK = 44;
        int memCacheFlushIntvl = 50;
        int memCacheMsgSizeInMB = 33;
        boolean acceptPublish = false;
        boolean acceptSubscribe = true;
        String deletePolicy = "delete,5h";
        int qryPriorityId = 202;
        int maxMsgSizeInB = 1024 * 1024;
        String attributes = "";
        String modifyUser = "modifyUser";
        Date modifyDate = new Date();
        BdbClusterSettingEntity bdbEntity =
                new BdbClusterSettingEntity(recordKey, configId, brokerPort, brokerTLSPort,
                        brokerWebPort, numTopicStores, numPartitions, unflushThreshold,
                        unflushInterval, unflushDataHold, memCacheMsgCntInK, memCacheFlushIntvl,
                        memCacheMsgSizeInMB, acceptPublish, acceptSubscribe, deletePolicy,
                        qryPriorityId, maxMsgSizeInB, attributes, modifyUser, modifyDate);
        ClusterSettingEntity setting2 = new ClusterSettingEntity(bdbEntity);
        Assert.assertEquals(setting2.getBrokerPort(), bdbEntity.getBrokerPort());
        Assert.assertEquals(setting2.getBrokerTLSPort(), bdbEntity.getBrokerTLSPort());
        Assert.assertEquals(setting2.getBrokerWebPort(), bdbEntity.getBrokerWebPort());
        Assert.assertEquals(setting2.getMaxMsgSizeInB(), bdbEntity.getMaxMsgSizeInB());
        Assert.assertEquals(setting2.getQryPriorityId(), bdbEntity.getQryPriorityId());
        Assert.assertFalse(setting2.getGloFlowCtrlStatus().isEnable());
        Assert.assertNull(bdbEntity.getEnableGloFlowCtrl());
        Assert.assertEquals(setting2.getGloFlowCtrlRuleCnt(), 0);
        Assert.assertEquals(bdbEntity.getGloFlowCtrlCnt(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(setting2.getGloFlowCtrlRuleInfo(), TServerConstants.BLANK_FLOWCTRL_RULES);
        Assert.assertNull(bdbEntity.getGloFlowCtrlInfo());
        TopicPropGroup defProps2 = setting2.getClsDefTopicProps();
        Assert.assertEquals(defProps2.getNumTopicStores(), bdbEntity.getNumTopicStores());
        Assert.assertEquals(defProps2.getNumPartitions(), bdbEntity.getNumPartitions());
        Assert.assertEquals(defProps2.getUnflushThreshold(), bdbEntity.getUnflushThreshold());
        Assert.assertEquals(defProps2.getUnflushInterval(), bdbEntity.getUnflushInterval());
        Assert.assertEquals(defProps2.getUnflushDataHold(), bdbEntity.getUnflushDataHold());
        Assert.assertEquals(defProps2.getMemCacheMsgSizeInMB(), bdbEntity.getMemCacheMsgSizeInMB());
        Assert.assertEquals(defProps2.getMemCacheFlushIntvl(), bdbEntity.getMemCacheFlushIntvl());
        Assert.assertEquals(defProps2.getMemCacheMsgCntInK(), bdbEntity.getMemCacheMsgCntInK());
        Assert.assertEquals(defProps2.getAcceptPublish(), bdbEntity.isAcceptPublish());
        Assert.assertEquals(defProps2.getAcceptSubscribe(), bdbEntity.isAcceptSubscribe());
        Assert.assertEquals(defProps2.getDataStoreType(), bdbEntity.getDefDataType());
        Assert.assertEquals(defProps2.getDataPath(), "");
        Assert.assertNull(bdbEntity.getDefDataPath());
        Assert.assertEquals(defProps2.getDeletePolicy(), bdbEntity.getDeletePolicy());
        // case 3
        BdbClusterSettingEntity bdbSetting = setting2.buildBdbClsDefSettingEntity();
        Assert.assertEquals(setting2.getBrokerPort(), bdbSetting.getBrokerPort());
        Assert.assertEquals(setting2.getBrokerTLSPort(), bdbSetting.getBrokerTLSPort());
        Assert.assertEquals(setting2.getBrokerWebPort(), bdbSetting.getBrokerWebPort());
        Assert.assertEquals(setting2.getMaxMsgSizeInB(), bdbSetting.getMaxMsgSizeInB());
        Assert.assertEquals(setting2.getQryPriorityId(), bdbSetting.getQryPriorityId());
        Assert.assertEquals(setting2.getGloFlowCtrlStatus().isEnable(), bdbSetting.getEnableGloFlowCtrl());
        Assert.assertEquals(setting2.getGloFlowCtrlRuleCnt(), bdbSetting.getGloFlowCtrlCnt());
        Assert.assertEquals(setting2.getGloFlowCtrlRuleInfo(), bdbSetting.getGloFlowCtrlInfo());
        TopicPropGroup defProps3 = setting2.getClsDefTopicProps();
        Assert.assertEquals(defProps3.getNumTopicStores(), bdbSetting.getNumTopicStores());
        Assert.assertEquals(defProps3.getNumPartitions(), bdbSetting.getNumPartitions());
        Assert.assertEquals(defProps3.getUnflushThreshold(), bdbSetting.getUnflushThreshold());
        Assert.assertEquals(defProps3.getUnflushInterval(), bdbSetting.getUnflushInterval());
        Assert.assertEquals(defProps3.getUnflushDataHold(), bdbSetting.getUnflushDataHold());
        Assert.assertEquals(defProps3.getMemCacheMsgSizeInMB(), bdbSetting.getMemCacheMsgSizeInMB());
        Assert.assertEquals(defProps3.getMemCacheFlushIntvl(), bdbSetting.getMemCacheFlushIntvl());
        Assert.assertEquals(defProps3.getMemCacheMsgCntInK(), bdbSetting.getMemCacheMsgCntInK());
        Assert.assertEquals(defProps3.getAcceptPublish(), bdbSetting.isAcceptPublish());
        Assert.assertEquals(defProps3.getAcceptSubscribe(), bdbSetting.isAcceptSubscribe());
        Assert.assertEquals(defProps3.getDataStoreType(), bdbSetting.getDefDataType());
        Assert.assertEquals(defProps3.getDataPath(), "");
        Assert.assertNull(bdbSetting.getDefDataPath());
        Assert.assertEquals(defProps3.getDeletePolicy(), bdbSetting.getDeletePolicy());
        // case 4
        ClusterSettingEntity setting4 = new ClusterSettingEntity(bdbSetting);
        Assert.assertEquals(setting4.getBrokerPort(), bdbSetting.getBrokerPort());
        Assert.assertEquals(setting4.getBrokerTLSPort(), bdbSetting.getBrokerTLSPort());
        Assert.assertEquals(setting4.getBrokerWebPort(), bdbSetting.getBrokerWebPort());
        Assert.assertEquals(setting4.getMaxMsgSizeInB(), bdbSetting.getMaxMsgSizeInB());
        Assert.assertEquals(setting4.getQryPriorityId(), bdbSetting.getQryPriorityId());
        Assert.assertEquals(setting4.getGloFlowCtrlStatus().isEnable(), bdbSetting.getEnableGloFlowCtrl());
        Assert.assertEquals(setting4.getGloFlowCtrlRuleCnt(), bdbSetting.getGloFlowCtrlCnt());
        Assert.assertEquals(setting4.getGloFlowCtrlRuleInfo(), bdbSetting.getGloFlowCtrlInfo());
        TopicPropGroup defProps4 = setting4.getClsDefTopicProps();
        Assert.assertEquals(defProps4.getNumTopicStores(), bdbSetting.getNumTopicStores());
        Assert.assertEquals(defProps4.getNumPartitions(), bdbSetting.getNumPartitions());
        Assert.assertEquals(defProps4.getUnflushThreshold(), bdbSetting.getUnflushThreshold());
        Assert.assertEquals(defProps4.getUnflushInterval(), bdbSetting.getUnflushInterval());
        Assert.assertEquals(defProps4.getUnflushDataHold(), bdbSetting.getUnflushDataHold());
        Assert.assertEquals(defProps4.getMemCacheMsgSizeInMB(), bdbSetting.getMemCacheMsgSizeInMB());
        Assert.assertEquals(defProps4.getMemCacheFlushIntvl(), bdbSetting.getMemCacheFlushIntvl());
        Assert.assertEquals(defProps4.getMemCacheMsgCntInK(), bdbSetting.getMemCacheMsgCntInK());
        Assert.assertEquals(defProps4.getAcceptPublish(), bdbSetting.isAcceptPublish());
        Assert.assertEquals(defProps4.getAcceptSubscribe(), bdbSetting.isAcceptSubscribe());
        Assert.assertEquals(defProps4.getDataStoreType(), bdbSetting.getDefDataType());
        Assert.assertEquals(defProps4.getDataPath(), "");
        Assert.assertNull(bdbSetting.getDefDataPath());
        Assert.assertEquals(defProps4.getDeletePolicy(), bdbSetting.getDeletePolicy());
        // case 5
        long newDataVerId = 99;
        int newBrokerPort = 52;
        int newBrokerTLSPort = 22;
        int newBrokerWebPort = 32;
        int newMaxMsgSizeMB = 2;
        int newQryPriorityId = 101;
        Boolean newFlowCtrlEnable = true;
        int newFlowRuleCnt = 5;
        String newFlowCtrlRuleInfo = "[{},{},{},{},{}]";
        TopicPropGroup newProps = new TopicPropGroup();
        newProps.fillDefaultValue();
        ClusterSettingEntity setting5 = setting2.clone();
        Assert.assertTrue(setting5.updModifyInfo(newDataVerId, newBrokerPort, newBrokerTLSPort,
                newBrokerWebPort, newMaxMsgSizeMB, newQryPriorityId, newFlowCtrlEnable,
                newFlowRuleCnt, newFlowCtrlRuleInfo, newProps));
        Assert.assertNotEquals(setting5.getBrokerPort(), setting2.getBrokerPort());
        Assert.assertNotEquals(setting5.getBrokerTLSPort(), setting2.getBrokerTLSPort());
        Assert.assertNotEquals(setting5.getBrokerWebPort(), setting2.getBrokerWebPort());
        Assert.assertNotEquals(setting5.getMaxMsgSizeInB(), setting2.getMaxMsgSizeInB());
        Assert.assertNotEquals(setting5.getQryPriorityId(), setting2.getQryPriorityId());
        Assert.assertNotEquals(setting5.getGloFlowCtrlStatus(), setting2.getGloFlowCtrlStatus());
        Assert.assertNotEquals(setting5.getGloFlowCtrlRuleCnt(), setting2.getGloFlowCtrlRuleCnt());
        Assert.assertNotEquals(setting5.getGloFlowCtrlRuleInfo(), setting2.getGloFlowCtrlRuleInfo());
        Assert.assertNotEquals(setting5.getClsDefTopicProps(), setting2.getClsDefTopicProps());
    }

    @Test
    public void getMapValuesTest() {
        Map<String, String> paramMap = new HashMap<>();
        // case 1
        ClusterSettingEntity entity1 = new ClusterSettingEntity();
        entity1.getConfigureInfo(paramMap, true);
        Assert.assertEquals(paramMap.size(), 0);
        paramMap.clear();
        // case 2
        String createUser = "creater";
        String dateStr1 = "20220302110925";
        Date createDate = DateTimeConvertUtils.yyyyMMddHHmmss2date(dateStr1);
        String modifyUser = "modifyUser";
        String dateStr2 = "20220302111925";
        Date modifyDate = DateTimeConvertUtils.yyyyMMddHHmmss2date(dateStr2);
        BaseEntity baseEntity = new BaseEntity(30,
                createUser, createDate, modifyUser, modifyDate);
        ClusterSettingEntity entity2 = new ClusterSettingEntity(baseEntity);
        entity2.getConfigureInfo(paramMap, true);
        Assert.assertEquals(paramMap.size(), 6);
        Assert.assertEquals(paramMap.get("dataVersionId"),
                String.valueOf(30));
        Assert.assertEquals(paramMap.get("serialId"),
                String.valueOf(entity2.getSerialId()));
        Assert.assertEquals(paramMap.get("createUser"), createUser);
        Assert.assertEquals(paramMap.get("createDate"), dateStr1);
        Assert.assertEquals(paramMap.get("modifyUser"), modifyUser);
        Assert.assertEquals(paramMap.get("modifyDate"), dateStr2);
        paramMap.clear();
        // case 3
        entity2.updModifyInfo(5, 6, 7,
                8, 9, 101, false,
                0, null, null);
        entity2.getConfigureInfo(paramMap, true);
        Assert.assertEquals(paramMap.size(), 12);
        Assert.assertEquals(paramMap.get("dataVersionId"),
                String.valueOf(5));
        Assert.assertEquals(paramMap.get("serialId"),
                String.valueOf(entity2.getSerialId()));
        Assert.assertEquals(paramMap.get("createUser"), createUser);
        Assert.assertEquals(paramMap.get("createDate"), dateStr1);
        Assert.assertEquals(paramMap.get("modifyUser"), modifyUser);
        Assert.assertEquals(paramMap.get("modifyDate"), dateStr2);
        Assert.assertEquals(paramMap.get("brokerPort"), String.valueOf(6));
        Assert.assertEquals(paramMap.get("brokerTLSPort"), String.valueOf(7));
        Assert.assertEquals(paramMap.get("brokerWebPort"), String.valueOf(8));
        Assert.assertEquals(paramMap.get("maxMsgSizeInMB"), String.valueOf(9));
        Assert.assertEquals(paramMap.get("qryPriorityId"), String.valueOf(101));
        Assert.assertEquals(paramMap.get("flowCtrlEnable"), "false");
        paramMap.clear();
        // case 4
        entity2.updModifyInfo(9, 10, 11,
                12, 13, 301, true,
                2, "[{\"type\":1},{\"type\":2}]", null);
        entity2.getConfigureInfo(paramMap, false);
        Assert.assertEquals(paramMap.size(), 14);
        Assert.assertEquals(paramMap.get("dVerId"),
                String.valueOf(9));
        Assert.assertEquals(paramMap.get("serialId"),
                String.valueOf(entity2.getSerialId()));
        Assert.assertEquals(paramMap.get("cur"), createUser);
        Assert.assertEquals(paramMap.get("cDate"), dateStr1);
        Assert.assertEquals(paramMap.get("mur"), modifyUser);
        Assert.assertEquals(paramMap.get("mDate"), dateStr2);

        Assert.assertEquals(paramMap.get("bPort"), String.valueOf(10));
        Assert.assertEquals(paramMap.get("bTlsPort"), String.valueOf(11));
        Assert.assertEquals(paramMap.get("bWebPort"), String.valueOf(12));
        Assert.assertEquals(paramMap.get("mxMsgInMB"), String.valueOf(13));
        Assert.assertEquals(paramMap.get("qryPriId"), String.valueOf(301));
        Assert.assertEquals(paramMap.get("fCtrlEn"), "true");
        Assert.assertEquals(paramMap.get("fCtrlCnt"), String.valueOf(2));
        Assert.assertEquals(paramMap.get("fCtrlInfo"), "[{\"type\":1},{\"type\":2}]");
        paramMap.clear();
    }
}
