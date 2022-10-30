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
import org.apache.inlong.tubemq.corebase.utils.SettingValidUtils;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbTopicAuthControlEntity;
import org.junit.Assert;
import org.junit.Test;

public class TopicCtrlEntityTest {

    @Test
    public void topicCtrlEntityTest() {
        // case 1
        String topicName = "test_1";
        boolean enableAuthControl = false;
        String attributes = "";
        String createUser = "creater";
        Date createDate = new Date();
        final int maxMsgSizeInB = 12222;
        BdbTopicAuthControlEntity bdbEntity1 =
                new BdbTopicAuthControlEntity(topicName,
                        enableAuthControl, attributes, createUser, createDate);
        final TopicCtrlEntity ctrlEntity1 = new TopicCtrlEntity(bdbEntity1);
        // check bdbEntity1
        Assert.assertEquals(bdbEntity1.getTopicName(), topicName);
        Assert.assertEquals(bdbEntity1.getTopicId(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(bdbEntity1.isEnableAuthControl(), enableAuthControl);
        Assert.assertEquals(bdbEntity1.getMaxMsgSize(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(bdbEntity1.getModifyUser(), createUser);
        Assert.assertEquals(bdbEntity1.getModifyDate(), createDate);
        Assert.assertEquals(bdbEntity1.getDataVerId(), TBaseConstants.META_VALUE_UNDEFINED);
        bdbEntity1.setMaxMsgSize(maxMsgSizeInB);
        Assert.assertEquals(bdbEntity1.getMaxMsgSize(), maxMsgSizeInB);
        // check ctrlEntity1
        Assert.assertEquals(ctrlEntity1.getTopicName(), topicName);
        Assert.assertEquals(ctrlEntity1.getTopicId(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(ctrlEntity1.getAuthCtrlStatus(), EnableStatus.STATUS_DISABLE);
        Assert.assertEquals(ctrlEntity1.getMaxMsgSizeInB(),
                TBaseConstants.META_MAX_MESSAGE_DATA_SIZE);
        Assert.assertEquals(ctrlEntity1.getMaxMsgSizeInMB(),
                TBaseConstants.META_MIN_ALLOWED_MESSAGE_SIZE_MB);
        Assert.assertEquals(ctrlEntity1.getCreateUser(), createUser);
        Assert.assertEquals(ctrlEntity1.getCreateDate(), createDate);
        Assert.assertEquals(ctrlEntity1.getModifyUser(), createUser);
        Assert.assertEquals(ctrlEntity1.getModifyDate(), createDate);
        Assert.assertEquals(ctrlEntity1.getDataVerId(), TBaseConstants.META_VALUE_UNDEFINED);
        // case 2
        long dataVerId2 = 555;
        int topicId2 = 222;
        String topicName2 = "test_1";
        boolean enableAuthControl2 = true;
        String attributes2 = "";
        String createUser2 = "creater2";
        Date createDate2 = new Date();
        int maxMsgSizeInB2 = 14;
        TopicCtrlEntity ctrlEntity2 = ctrlEntity1.clone();
        Assert.assertTrue(ctrlEntity2.isDataEquals(ctrlEntity1));
        BaseEntity opInfoEntry = new BaseEntity(dataVerId2, createUser2, createDate2);
        Assert.assertTrue(ctrlEntity2.updBaseModifyInfo(opInfoEntry));
        Assert.assertTrue(ctrlEntity2.updModifyInfo(opInfoEntry.getDataVerId(),
                topicId2, maxMsgSizeInB2, enableAuthControl2));
        Assert.assertFalse(ctrlEntity2.isDataEquals(ctrlEntity1));
        Assert.assertFalse(ctrlEntity2.isMatched(ctrlEntity1, true));
        // check ctrlEntity2
        Assert.assertEquals(ctrlEntity2.getTopicName(), topicName);
        Assert.assertEquals(ctrlEntity2.getTopicId(), topicId2);
        Assert.assertEquals(ctrlEntity2.getAuthCtrlStatus(), EnableStatus.STATUS_ENABLE);
        Assert.assertEquals(ctrlEntity2.getMaxMsgSizeInB(),
                SettingValidUtils.validAndXfeMaxMsgSizeFromMBtoB(maxMsgSizeInB2));
        Assert.assertEquals(ctrlEntity2.getMaxMsgSizeInMB(), maxMsgSizeInB2);
        Assert.assertEquals(ctrlEntity2.getCreateUser(), createUser);
        Assert.assertEquals(ctrlEntity2.getCreateDate(), createDate);
        Assert.assertEquals(ctrlEntity2.getModifyUser(), createUser2);
        Assert.assertEquals(ctrlEntity2.getModifyDate(), createDate2);
        Assert.assertEquals(ctrlEntity2.getDataVerId(), dataVerId2);
        // case 3
        BdbTopicAuthControlEntity bdbEntity3 =
                ctrlEntity2.buildBdbTopicAuthControlEntity();
        Assert.assertEquals(bdbEntity3.getTopicName(), ctrlEntity2.getTopicName());
        Assert.assertEquals(bdbEntity3.getTopicId(), ctrlEntity2.getTopicId());
        Assert.assertEquals(bdbEntity3.isEnableAuthControl(),
                ctrlEntity2.getAuthCtrlStatus().isEnable());
        Assert.assertEquals(bdbEntity3.getMaxMsgSize(), ctrlEntity2.getMaxMsgSizeInB());
        Assert.assertEquals(bdbEntity3.getModifyUser(), ctrlEntity2.getModifyUser());
        Assert.assertEquals(bdbEntity3.getStrModifyDate(), ctrlEntity2.getModifyDateStr());
        Assert.assertEquals(bdbEntity3.getCreateUser(), ctrlEntity2.getCreateUser());
        Assert.assertEquals(bdbEntity3.getStrCreateDate(), ctrlEntity2.getCreateDateStr());
        Assert.assertEquals(bdbEntity3.getDataVerId(), ctrlEntity2.getDataVerId());
        // case 4
        TopicCtrlEntity ctrlEntity4 = new TopicCtrlEntity(bdbEntity3);
        // check ctrlEntity4
        Assert.assertTrue(ctrlEntity4.isDataEquals(ctrlEntity2));
        Assert.assertEquals(ctrlEntity4.getTopicName(), ctrlEntity2.getTopicName());
        Assert.assertEquals(ctrlEntity4.getTopicId(), ctrlEntity2.getTopicId());
        Assert.assertEquals(ctrlEntity4.getAuthCtrlStatus(), ctrlEntity2.getAuthCtrlStatus());
        Assert.assertEquals(ctrlEntity4.getMaxMsgSizeInB(), ctrlEntity2.getMaxMsgSizeInB());
        Assert.assertEquals(ctrlEntity4.getCreateUser(), ctrlEntity2.getCreateUser());
        Assert.assertEquals(ctrlEntity4.getCreateDateStr(), ctrlEntity2.getCreateDateStr());
        Assert.assertEquals(ctrlEntity4.getModifyUser(), ctrlEntity2.getModifyUser());
        Assert.assertEquals(ctrlEntity4.getModifyDateStr(), ctrlEntity2.getModifyDateStr());
        Assert.assertEquals(ctrlEntity4.getDataVerId(), ctrlEntity2.getDataVerId());
    }

}
