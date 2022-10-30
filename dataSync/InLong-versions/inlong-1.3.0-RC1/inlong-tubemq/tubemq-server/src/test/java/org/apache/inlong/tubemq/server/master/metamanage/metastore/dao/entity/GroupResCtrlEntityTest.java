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
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbGroupFlowCtrlEntity;
import org.junit.Assert;
import org.junit.Test;

public class GroupResCtrlEntityTest {

    @Test
    public void groupResCtrlEntityTest() {
        // case 1
        String groupName = "test_group";
        GroupResCtrlEntity resEntry = new GroupResCtrlEntity();
        resEntry.setGroupName(groupName);
        resEntry.fillDefaultValue();
        Assert.assertEquals(resEntry.getGroupName(), groupName);
        Assert.assertEquals(resEntry.getResCheckStatus(), EnableStatus.STATUS_DISABLE);
        Assert.assertEquals(resEntry.getAllowedBrokerClientRate(), 0);
        Assert.assertEquals(resEntry.getQryPriorityId(), TServerConstants.QRY_PRIORITY_DEF_VALUE);
        Assert.assertEquals(resEntry.getFlowCtrlStatus(), EnableStatus.STATUS_DISABLE);
        Assert.assertEquals(resEntry.getFlowCtrlInfo(), TServerConstants.BLANK_FLOWCTRL_RULES);
        Assert.assertEquals(resEntry.getRuleCnt(), 0);
        // case 2
        long dataVerId = 55;
        String groupName2 = "group_2";
        int ruleCnt = 4;
        String flowCtrlInfo = "[{},{},{},{}]";
        int statusId = 1;
        int qryPriorityId = 203;
        String attributes = "key=val&key2=va2";
        String createUser = "create";
        Date createDate = new Date();
        BdbGroupFlowCtrlEntity bdbEntity2 =
                new BdbGroupFlowCtrlEntity(dataVerId, groupName2,
                        flowCtrlInfo, statusId, ruleCnt, qryPriorityId,
                        attributes, createUser, createDate);
        Assert.assertEquals(bdbEntity2.getGroupName(), groupName2);
        Assert.assertEquals(bdbEntity2.getResCheckStatus(), EnableStatus.STATUS_UNDEFINE);
        Assert.assertEquals(bdbEntity2.getAllowedBrokerClientRate(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(bdbEntity2.getQryPriorityId(), 203);
        Assert.assertEquals(bdbEntity2.getStatusId(), 1);
        Assert.assertEquals(bdbEntity2.getFlowCtrlInfo(), flowCtrlInfo);
        Assert.assertEquals(bdbEntity2.getRuleCnt(), ruleCnt);
        Assert.assertEquals(bdbEntity2.getModifyUser(), createUser);
        Assert.assertEquals(bdbEntity2.getModifyDate(), createDate);
        Assert.assertEquals(bdbEntity2.getSerialId(), dataVerId);
        bdbEntity2.setResCheckStatus(EnableStatus.STATUS_ENABLE);
        // case 3
        GroupResCtrlEntity resEntry3 = new GroupResCtrlEntity(bdbEntity2);
        Assert.assertEquals(bdbEntity2.getGroupName(), resEntry3.getGroupName());
        Assert.assertEquals(bdbEntity2.getResCheckStatus(), resEntry3.getResCheckStatus());
        Assert.assertEquals(bdbEntity2.getAllowedBrokerClientRate(),
                resEntry3.getAllowedBrokerClientRate());
        Assert.assertEquals(bdbEntity2.getQryPriorityId(), resEntry3.getQryPriorityId());
        Assert.assertTrue(resEntry3.getFlowCtrlStatus().isEnable());
        Assert.assertEquals(bdbEntity2.getFlowCtrlInfo(), resEntry3.getFlowCtrlInfo());
        Assert.assertEquals(bdbEntity2.getRuleCnt(), resEntry3.getRuleCnt());
        Assert.assertEquals(bdbEntity2.getModifyUser(), resEntry3.getModifyUser());
        Assert.assertEquals(bdbEntity2.getModifyDate(), resEntry3.getModifyDate());
        Assert.assertEquals(bdbEntity2.getModifyUser(), resEntry3.getCreateUser());
        Assert.assertEquals(bdbEntity2.getModifyDate(), resEntry3.getCreateDate());
        Assert.assertEquals(bdbEntity2.getSerialId(), resEntry3.getDataVerId());
        // case 4
        long newDataVerId = 99;
        boolean resChkEnable = true;
        int newAllowedB2CRate = 5;
        int newQryPriorityId = 2;
        boolean newFlowCtrlEnable =  false;
        int newFlowRuleCnt = 2;
        String newFlowCtrlRuleInfo = "[{},{}]";
        GroupResCtrlEntity resEntry4 = resEntry3.clone();
        Assert.assertTrue(resEntry4.isMatched(resEntry3));
        Assert.assertTrue(resEntry4.updModifyInfo(newDataVerId, resChkEnable, newAllowedB2CRate,
                newQryPriorityId, newFlowCtrlEnable, newFlowRuleCnt, newFlowCtrlRuleInfo));
        Assert.assertEquals(resEntry4.getDataVerId(), newDataVerId);
        Assert.assertEquals(resEntry4.getResCheckStatus().isEnable(), resChkEnable);
        Assert.assertEquals(resEntry4.getAllowedBrokerClientRate(), newAllowedB2CRate);
        Assert.assertEquals(resEntry4.getQryPriorityId(), newQryPriorityId);
        Assert.assertEquals(resEntry4.getFlowCtrlStatus().isEnable(), newFlowCtrlEnable);
        Assert.assertEquals(resEntry4.getRuleCnt(), newFlowRuleCnt);
        Assert.assertEquals(resEntry4.getFlowCtrlInfo(), newFlowCtrlRuleInfo);
        Assert.assertEquals(resEntry4.getGroupName(), resEntry3.getGroupName());
        Assert.assertEquals(resEntry4.getResCheckStatus(), resEntry3.getResCheckStatus());
        Assert.assertNotEquals(resEntry4.getAllowedBrokerClientRate(),
                resEntry3.getAllowedBrokerClientRate());
        Assert.assertNotEquals(resEntry4.getQryPriorityId(), resEntry3.getQryPriorityId());
        Assert.assertNotEquals(resEntry4.getFlowCtrlStatus(), resEntry3.getFlowCtrlStatus());
        Assert.assertNotEquals(resEntry4.getFlowCtrlInfo(), resEntry3.getFlowCtrlInfo());
        Assert.assertNotEquals(resEntry4.getRuleCnt(), resEntry3.getRuleCnt());
        Assert.assertEquals(resEntry4.getCreateUser(), resEntry3.getCreateUser());
        Assert.assertEquals(resEntry4.getCreateDate(), resEntry3.getCreateDate());
        Assert.assertEquals(resEntry4.getModifyUser(), resEntry3.getModifyUser());
        Assert.assertEquals(resEntry4.getModifyDate(), resEntry3.getModifyDate());
        Assert.assertNotEquals(resEntry4.getDataVerId(), resEntry3.getDataVerId());
        // case 5
        BdbGroupFlowCtrlEntity bdbEntity5 = resEntry4.buildBdbGroupFlowCtrlEntity();
        Assert.assertEquals(bdbEntity5.getSerialId(), newDataVerId);
        Assert.assertEquals(bdbEntity5.getResCheckStatus().isEnable(), resChkEnable);
        Assert.assertEquals(bdbEntity5.getAllowedBrokerClientRate(), newAllowedB2CRate);
        Assert.assertEquals(bdbEntity5.getQryPriorityId(), newQryPriorityId);
        Assert.assertEquals(bdbEntity5.getStatusId(), 0);
        Assert.assertEquals(bdbEntity5.getRuleCnt(), newFlowRuleCnt);
        Assert.assertEquals(bdbEntity5.getFlowCtrlInfo(), newFlowCtrlRuleInfo);
        Assert.assertEquals(bdbEntity5.getGroupName(), bdbEntity5.getGroupName());
        Assert.assertEquals(resEntry4.getCreateUser(), bdbEntity5.getCreateUser());
        Assert.assertEquals(resEntry4.getCreateDateStr(), bdbEntity5.getStrCreateDate());
        Assert.assertEquals(resEntry4.getModifyUser(), bdbEntity5.getModifyUser());
        Assert.assertEquals(resEntry4.getModifyDateStr(), bdbEntity5.getStrModifyDate());

    }

}
