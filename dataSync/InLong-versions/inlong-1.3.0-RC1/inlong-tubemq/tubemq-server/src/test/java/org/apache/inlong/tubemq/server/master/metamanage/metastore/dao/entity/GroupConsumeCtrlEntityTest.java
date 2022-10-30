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

import java.util.Calendar;
import java.util.Date;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbGroupFilterCondEntity;
import org.junit.Assert;
import org.junit.Test;

public class GroupConsumeCtrlEntityTest {

    @Test
    public void groupConsumeCtrlEntityTest() {
        // case 1
        String topicName = "test_1";
        String groupName = "group_1";
        int controlStatus = 2;
        String filterCondStr = "[1,2,3,4]";
        String attributes = "key=val&ke2=val3";
        String createUser = "creater";
        Date createDate = new Date();
        BdbGroupFilterCondEntity bdbEntity1 =
                new BdbGroupFilterCondEntity(topicName, groupName, controlStatus,
                        filterCondStr, attributes, createUser, createDate);

        GroupConsumeCtrlEntity ctrlEntry1 = new GroupConsumeCtrlEntity(bdbEntity1);
        Assert.assertEquals(ctrlEntry1.getGroupName(), bdbEntity1.getConsumerGroupName());
        Assert.assertEquals(ctrlEntry1.getTopicName(), bdbEntity1.getTopicName());
        Assert.assertEquals(ctrlEntry1.getFilterCondStr(), bdbEntity1.getFilterCondStr());
        Assert.assertEquals(bdbEntity1.getControlStatus(), 2);
        Assert.assertTrue(ctrlEntry1.getFilterEnable().isEnable());
        Assert.assertEquals(ctrlEntry1.getConsumeEnable(), bdbEntity1.getConsumeEnable());
        Assert.assertEquals(ctrlEntry1.getCreateUser(), bdbEntity1.getModifyUser());
        Assert.assertEquals(ctrlEntry1.getCreateDate(), bdbEntity1.getModifyDate());
        Assert.assertEquals(ctrlEntry1.getAttributes(), bdbEntity1.getAttributes());
        Assert.assertEquals(ctrlEntry1.getDisableReason(), bdbEntity1.getDisableConsumeReason());
        Assert.assertEquals(ctrlEntry1.getRecordKey(), bdbEntity1.getRecordKey());
        // case 2
        final long newDataVerId = 5555;
        final boolean consumeEnable = true;
        final String disableRsn = "disable";
        final boolean filterEnable = true;
        final String newFilterCondStr = "[1,2,4]";
        Date newDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(newDate);
        c.add(Calendar.DAY_OF_MONTH, 1);
        newDate = c.getTime();
        BaseEntity opInfoEntity =
                new BaseEntity(newDataVerId, "modify", newDate);
        GroupConsumeCtrlEntity ctrlEntry2 = ctrlEntry1.clone();
        Assert.assertTrue(ctrlEntry2.isMatched(ctrlEntry1));
        ctrlEntry2.updBaseModifyInfo(opInfoEntity);
        Assert.assertTrue(ctrlEntry2.updModifyInfo(opInfoEntity.getDataVerId(),
                consumeEnable, disableRsn, filterEnable, newFilterCondStr));
        // case 3
        BdbGroupFilterCondEntity bdbEntity3 = ctrlEntry2.buildBdbGroupFilterCondEntity();
        Assert.assertEquals(ctrlEntry2.getGroupName(), bdbEntity3.getConsumerGroupName());
        Assert.assertEquals(ctrlEntry2.getTopicName(), bdbEntity3.getTopicName());
        Assert.assertEquals(ctrlEntry2.getFilterCondStr(), bdbEntity3.getFilterCondStr());
        Assert.assertEquals(bdbEntity3.getControlStatus(), 2);
        Assert.assertTrue(ctrlEntry2.getFilterEnable().isEnable());
        Assert.assertEquals(ctrlEntry2.getConsumeEnable(), bdbEntity3.getConsumeEnable());
        Assert.assertEquals(opInfoEntity.getCreateUser(), bdbEntity3.getModifyUser());
        Assert.assertEquals(opInfoEntity.getCreateDate(), bdbEntity3.getModifyDate());
        Assert.assertNotEquals(ctrlEntry2.getCreateDate(), bdbEntity3.getModifyDate());
        Assert.assertNotEquals(ctrlEntry2.getCreateUser(), bdbEntity3.getModifyUser());
        Assert.assertEquals(ctrlEntry2.getAttributes(), bdbEntity3.getAttributes());
        Assert.assertEquals(ctrlEntry2.getDisableReason(), bdbEntity3.getDisableConsumeReason());
        Assert.assertEquals(ctrlEntry2.getRecordKey(), bdbEntity3.getRecordKey());

    }

}
