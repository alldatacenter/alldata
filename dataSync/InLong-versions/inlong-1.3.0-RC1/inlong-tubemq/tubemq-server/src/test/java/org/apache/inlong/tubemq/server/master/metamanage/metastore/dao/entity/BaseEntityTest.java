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
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.junit.Assert;
import org.junit.Test;

public class BaseEntityTest {

    @Test
    public void baseEntityTest() {
        // case 1
        BaseEntity baseEntity1 = new BaseEntity();
        Assert.assertEquals(baseEntity1.getDataVerId(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(baseEntity1.getSerialId(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(baseEntity1.getCreateUser(), "");
        Assert.assertNull(baseEntity1.getCreateDate());
        Assert.assertEquals(baseEntity1.getModifyUser(), "");
        Assert.assertNull(baseEntity1.getModifyDate());
        Assert.assertEquals(baseEntity1.getAttributes(), "");
        Assert.assertEquals(baseEntity1.getCreateDateStr(), "");
        Assert.assertEquals(baseEntity1.getModifyDateStr(), "");
        // case 2
        String createUser = "test";
        Date createDate = new Date();
        String createDataStr = DateTimeConvertUtils.date2yyyyMMddHHmmss(createDate);
        BaseEntity baseEntity2 = new BaseEntity(createUser, createDate);
        Assert.assertEquals(baseEntity2.getDataVerId(), TServerConstants.DEFAULT_DATA_VERSION);
        Assert.assertNotEquals(baseEntity2.getSerialId(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(baseEntity2.getCreateUser(), createUser);
        Assert.assertEquals(baseEntity2.getCreateDate(), createDate);
        Assert.assertEquals(baseEntity2.getModifyUser(), createUser);
        Assert.assertEquals(baseEntity2.getModifyDate(), createDate);
        Assert.assertEquals(baseEntity2.getAttributes(), "");
        Assert.assertEquals(baseEntity2.getCreateDateStr(), createDataStr);
        Assert.assertEquals(baseEntity2.getModifyDateStr(), createDataStr);
        // case 3
        long dataVersionId = 5;
        BaseEntity baseEntity3 = new BaseEntity(dataVersionId, createUser, createDate);
        Assert.assertEquals(baseEntity3.getDataVerId(), dataVersionId);
        Assert.assertNotEquals(baseEntity3.getSerialId(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(baseEntity3.getCreateUser(), createUser);
        Assert.assertEquals(baseEntity3.getCreateDate(), createDate);
        Assert.assertEquals(baseEntity3.getModifyUser(), createUser);
        Assert.assertEquals(baseEntity3.getModifyDate(), createDate);
        Assert.assertEquals(baseEntity3.getAttributes(), "");
        Assert.assertEquals(baseEntity3.getCreateDateStr(), createDataStr);
        Assert.assertEquals(baseEntity3.getModifyDateStr(), createDataStr);
        // case 4
        String modifyUser = "modifyUser";
        Date modifyDate = new Date();
        String modifyDateStr = DateTimeConvertUtils.date2yyyyMMddHHmmss(modifyDate);
        BaseEntity baseEntity4 =
                new BaseEntity(createUser, createDate, modifyUser, modifyDate);
        Assert.assertEquals(baseEntity4.getDataVerId(), TServerConstants.DEFAULT_DATA_VERSION);
        Assert.assertNotEquals(baseEntity4.getSerialId(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(baseEntity4.getCreateUser(), createUser);
        Assert.assertEquals(baseEntity4.getCreateDate(), createDate);
        Assert.assertEquals(baseEntity4.getModifyUser(), modifyUser);
        Assert.assertEquals(baseEntity4.getModifyDate(), modifyDate);
        Assert.assertEquals(baseEntity4.getAttributes(), "");
        Assert.assertEquals(baseEntity4.getCreateDateStr(), createDataStr);
        Assert.assertEquals(baseEntity4.getModifyDateStr(), modifyDateStr);
        // case 5
        dataVersionId = 10;
        BaseEntity baseEntity5 = new BaseEntity(dataVersionId,
                createUser, createDate, modifyUser, modifyDate);
        Assert.assertEquals(baseEntity5.getDataVerId(), dataVersionId);
        Assert.assertNotEquals(baseEntity5.getSerialId(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(baseEntity5.getCreateUser(), createUser);
        Assert.assertEquals(baseEntity5.getCreateDate(), createDate);
        Assert.assertEquals(baseEntity5.getModifyUser(), modifyUser);
        Assert.assertEquals(baseEntity5.getModifyDate(), modifyDate);
        Assert.assertEquals(baseEntity5.getAttributes(), "");
        Assert.assertEquals(baseEntity5.getCreateDateStr(), createDataStr);
        Assert.assertEquals(baseEntity5.getModifyDateStr(), modifyDateStr);
        // case 6
        baseEntity5.setKeyAndVal("aaa", "bbb");
        BaseEntity baseEntity6 = new BaseEntity(baseEntity5);
        Assert.assertEquals(baseEntity6.getDataVerId(), baseEntity5.getDataVerId());
        Assert.assertEquals(baseEntity6.getSerialId(), baseEntity5.getSerialId());
        Assert.assertEquals(baseEntity6.getCreateUser(), baseEntity5.getCreateUser());
        Assert.assertEquals(baseEntity6.getCreateDate(), baseEntity5.getCreateDate());
        Assert.assertEquals(baseEntity6.getModifyUser(), baseEntity5.getModifyUser());
        Assert.assertEquals(baseEntity6.getModifyDate(), baseEntity5.getModifyDate());
        Assert.assertEquals(baseEntity6.getAttributes(), baseEntity5.getAttributes());
        Assert.assertEquals(baseEntity6.getCreateDateStr(), baseEntity5.getCreateDateStr());
        Assert.assertEquals(baseEntity6.getModifyDateStr(), baseEntity5.getModifyDateStr());
        // case 7
        BaseEntity baseEntity7 = new BaseEntity();
        baseEntity7.updBaseModifyInfo(baseEntity6);
        Assert.assertNotEquals(baseEntity6.getDataVerId(), baseEntity7.getDataVerId());
        Assert.assertNotEquals(baseEntity6.getSerialId(), baseEntity7.getSerialId());
        Assert.assertNotEquals(baseEntity6.getCreateUser(), baseEntity7.getCreateUser());
        Assert.assertNotEquals(baseEntity6.getCreateDate(), baseEntity7.getCreateDate());
        Assert.assertNotEquals(baseEntity6.getCreateDateStr(), baseEntity7.getCreateDateStr());
        Assert.assertEquals(baseEntity6.getModifyUser(), baseEntity7.getModifyUser());
        Assert.assertEquals(baseEntity6.getModifyDate(), baseEntity7.getModifyDate());
        Assert.assertEquals(baseEntity6.getAttributes(), baseEntity7.getAttributes());
        Assert.assertEquals(baseEntity6.getModifyDateStr(), baseEntity7.getModifyDateStr());
        // case 8
        long newDataVerId = 999;
        String newCreateUser = "queryCreate";
        String newModifyUser = "queryModify";
        BaseEntity baseEntity8 = new BaseEntity();
        baseEntity8.updQueryKeyInfo(newDataVerId, newCreateUser, newModifyUser);
        Assert.assertEquals(baseEntity8.getDataVerId(), newDataVerId);
        Assert.assertEquals(baseEntity8.getSerialId(), TBaseConstants.META_VALUE_UNDEFINED);
        Assert.assertEquals(baseEntity8.getCreateUser(), newCreateUser);
        Assert.assertNull(baseEntity8.getCreateDate());
        Assert.assertEquals(baseEntity8.getModifyUser(), newModifyUser);
        Assert.assertNull(baseEntity8.getModifyDate());
        Assert.assertEquals(baseEntity8.getAttributes(), "");
        Assert.assertEquals(baseEntity8.getCreateDateStr(), "");
        Assert.assertEquals(baseEntity8.getModifyDateStr(), "");
        // case 9
        BaseEntity baseEntity9 =  baseEntity6.clone();
        Assert.assertEquals(baseEntity9, baseEntity6);
        baseEntity9.updSerialId();
        baseEntity9.setDataVersionId(222223333);
        baseEntity9.setAttributes("aaaaabbbbccccddd");
        baseEntity9.updQueryKeyInfo(newDataVerId, newCreateUser, newModifyUser);
        Assert.assertNotEquals(baseEntity6.getDataVerId(), baseEntity9.getDataVerId());
        Assert.assertNotEquals(baseEntity6.getSerialId(), baseEntity9.getSerialId());
        Assert.assertNotEquals(baseEntity6.getCreateUser(), baseEntity9.getCreateUser());
        Assert.assertEquals(baseEntity6.getCreateDate(), baseEntity9.getCreateDate());
        Assert.assertNotEquals(baseEntity6.getModifyUser(), baseEntity9.getModifyUser());
        Assert.assertEquals(baseEntity6.getModifyDate(), baseEntity9.getModifyDate());
        Assert.assertNotEquals(baseEntity6.getAttributes(), baseEntity9.getAttributes());
        Assert.assertEquals(baseEntity6.getCreateDateStr(), baseEntity9.getCreateDateStr());
        Assert.assertEquals(baseEntity6.getModifyDateStr(), baseEntity9.getModifyDateStr());
        // case 10
        Assert.assertTrue(baseEntity8.isMatched(baseEntity9));
    }

    @Test
    public void getMapValuesTest() {
        Map<String, String> paramMap = new HashMap<>();
        // case 1
        BaseEntity entity1 = new BaseEntity();
        entity1.getConfigureInfo(paramMap, true);
        Assert.assertEquals(paramMap.size(), 0);
        paramMap.clear();
        // case 2
        String createUser = "creater";
        String dateStr1 = "20220302110925";
        Date createDate = DateTimeConvertUtils.yyyyMMddHHmmss2date(dateStr1);
        BaseEntity entity2 = new BaseEntity(createUser, createDate);
        entity2.getConfigureInfo(paramMap, true);
        Assert.assertEquals(paramMap.size(), 6);
        Assert.assertEquals(paramMap.get("dataVersionId"),
                String.valueOf(entity2.getDataVerId()));
        Assert.assertEquals(paramMap.get("serialId"),
                String.valueOf(entity2.getSerialId()));
        Assert.assertEquals(paramMap.get("createUser"), createUser);
        Assert.assertEquals(paramMap.get("createDate"), entity2.getCreateDateStr());
        Assert.assertEquals(paramMap.get("modifyUser"), createUser);
        Assert.assertEquals(paramMap.get("modifyDate"), entity2.getModifyDateStr());
        paramMap.clear();
        // case 3
        String modifyUser = "modifyUser";
        String dateStr2 = "20220302111925";
        Date modifyDate = DateTimeConvertUtils.yyyyMMddHHmmss2date(dateStr2);
        BaseEntity entity3 = new BaseEntity(55,
                createUser, createDate, modifyUser, modifyDate);
        entity3.getConfigureInfo(paramMap, false);
        Assert.assertEquals(paramMap.size(), 6);
        Assert.assertEquals(paramMap.get("dVerId"),
                String.valueOf(entity3.getDataVerId()));
        Assert.assertEquals(paramMap.get("serialId"),
                String.valueOf(entity3.getSerialId()));
        Assert.assertEquals(paramMap.get("cur"), createUser);
        Assert.assertEquals(paramMap.get("cDate"), entity3.getCreateDateStr());
        Assert.assertEquals(paramMap.get("mur"), entity3.getModifyUser());
        Assert.assertEquals(paramMap.get("mDate"), entity3.getModifyDateStr());
        paramMap.clear();
    }

}
