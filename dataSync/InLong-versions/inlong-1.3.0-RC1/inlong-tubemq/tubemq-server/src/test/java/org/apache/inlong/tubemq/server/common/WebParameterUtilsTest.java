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

package org.apache.inlong.tubemq.server.common;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicPropGroup;
import org.junit.Assert;
import org.junit.Test;

public class WebParameterUtilsTest {

    @Test
    public void getStringParamValueTest() {
        boolean retValue;
        String initialValue;
        StringBuilder sBuffer = new StringBuilder(512);
        ProcessResult result = new ProcessResult();
        Map<String, String> paramCntrMap = new HashMap<>();
        // case 1
        initialValue = null;
        retValue = WebParameterUtils.getStringParamValue(paramCntrMap,
                WebFieldDef.COMPSGROUPNAME, false, initialValue, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        Set<String> paraDataSet = (Set<String>) result.getRetData();
        Assert.assertEquals(paraDataSet, Collections.EMPTY_SET);
        // case 2
        paramCntrMap.clear();
        retValue = WebParameterUtils.getStringParamValue(paramCntrMap,
                WebFieldDef.COMPSGROUPNAME, true, initialValue, sBuffer, result);
        Assert.assertFalse(retValue);
        Assert.assertFalse(result.isSuccess());
        // case 3
        paramCntrMap.clear();
        paramCntrMap.put(WebFieldDef.COMPSGROUPNAME.shortName, "");
        retValue = WebParameterUtils.getStringParamValue(paramCntrMap,
                WebFieldDef.COMPSGROUPNAME, true, initialValue, sBuffer, result);
        Assert.assertFalse(retValue);
        Assert.assertFalse(result.isSuccess());
        // case 4
        paramCntrMap.clear();
        Set<String> exceptedValSet = new TreeSet<>();
        exceptedValSet.add("group1");
        exceptedValSet.add("group2");
        exceptedValSet.add("group3");
        paramCntrMap.put(WebFieldDef.COMPSGROUPNAME.name, "group3,group1,group2");
        retValue = WebParameterUtils.getStringParamValue(paramCntrMap,
                WebFieldDef.COMPSGROUPNAME, true, initialValue, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        paraDataSet = (Set<String>) result.getRetData();
        Assert.assertEquals(paraDataSet, exceptedValSet);
        // case 5
        paramCntrMap.clear();
        paramCntrMap.put(WebFieldDef.GROUPNAME.name, "test2,test1,test3");
        retValue = WebParameterUtils.getStringParamValue(paramCntrMap,
                WebFieldDef.GROUPNAME, true, initialValue, sBuffer, result);
        Assert.assertFalse(retValue);
        Assert.assertFalse(result.isSuccess());
        // case 6
        paramCntrMap.clear();
        paramCntrMap.put(WebFieldDef.GROUPNAME.name, "test2");
        retValue = WebParameterUtils.getStringParamValue(paramCntrMap,
                WebFieldDef.GROUPNAME, true, initialValue, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        String paraDataStr = (String) result.getRetData();
        Assert.assertEquals(paraDataStr, "test2");
        // case 7
        paramCntrMap.clear();
        paramCntrMap.put(WebFieldDef.GROUPNAME.name, "");
        retValue = WebParameterUtils.getStringParamValue(paramCntrMap,
                WebFieldDef.GROUPNAME, false, initialValue, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        paraDataStr = (String) result.getRetData();
        Assert.assertEquals(paraDataStr, initialValue);
        // case 8
        paramCntrMap.clear();
        initialValue = "initial value";
        paramCntrMap.put(WebFieldDef.GROUPNAME.name, "");
        retValue = WebParameterUtils.getStringParamValue(paramCntrMap,
                WebFieldDef.GROUPNAME, false, initialValue, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        paraDataStr = (String) result.getRetData();
        Assert.assertEquals(paraDataStr, initialValue);
        // case 9
        paramCntrMap.clear();
        paramCntrMap.put(WebFieldDef.COMPSGROUPNAME.name, "\"test1,test1,test3\"");
        retValue = WebParameterUtils.getStringParamValue(paramCntrMap,
                WebFieldDef.COMPSGROUPNAME, false, initialValue, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        paraDataSet = (Set<String>) result.getRetData();
        Assert.assertEquals(paraDataSet.size(), 2);
    }

    @Test
    public void getBooleanParamValueTest() {
        boolean retValue;
        StringBuilder sBuffer = new StringBuilder(512);
        ProcessResult result = new ProcessResult();
        Map<String, String> paramCntrMap = new HashMap<>();
        // case 1
        retValue = WebParameterUtils.getBooleanParamValue(paramCntrMap,
                WebFieldDef.WITHIP, true, null, sBuffer, result);
        Assert.assertFalse(retValue);
        Assert.assertFalse(result.isSuccess());
        // case 2
        paramCntrMap.clear();
        paramCntrMap.put(WebFieldDef.WITHIP.name, "1");
        retValue = WebParameterUtils.getBooleanParamValue(paramCntrMap,
                WebFieldDef.WITHIP, true, null, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        Boolean paraDataObj = (Boolean) result.getRetData();
        Assert.assertEquals(paraDataObj, Boolean.TRUE);
        // case 3
        paramCntrMap.clear();
        paramCntrMap.put(WebFieldDef.WITHIP.name, "false");
        retValue = WebParameterUtils.getBooleanParamValue(paramCntrMap,
                WebFieldDef.WITHIP, true, null, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        paraDataObj = (Boolean) result.getRetData();
        Assert.assertEquals(paraDataObj, Boolean.FALSE);
    }

    @Test
    public void getAUDBaseInfoTest() {
        boolean retValue;
        StringBuilder sBuffer = new StringBuilder(512);
        ProcessResult result = new ProcessResult();
        Map<String, String> paramCntrMap = new HashMap<>();
        // case 1
        retValue = WebParameterUtils.getAUDBaseInfo(paramCntrMap,
                true, null, sBuffer, result);
        Assert.assertFalse(retValue);
        Assert.assertFalse(result.isSuccess());
        // case 2
        paramCntrMap.clear();
        retValue = WebParameterUtils.getAUDBaseInfo(paramCntrMap,
                false, null, sBuffer, result);
        Assert.assertFalse(retValue);
        Assert.assertFalse(result.isSuccess());
        // case 3
        paramCntrMap.clear();
        BaseEntity defOpEntity = new BaseEntity();
        defOpEntity.updQueryKeyInfo(-2,
                "testCreate", "testModify");
        retValue = WebParameterUtils.getAUDBaseInfo(paramCntrMap,
                true, defOpEntity, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        BaseEntity retEntry = (BaseEntity) result.getRetData();
        Assert.assertEquals(defOpEntity.getCreateUser(), retEntry.getCreateUser());
        Assert.assertEquals(defOpEntity.getModifyUser(), retEntry.getModifyUser());
        // case 4
        paramCntrMap.clear();
        defOpEntity = null;
        paramCntrMap.put(WebFieldDef.DATAVERSIONID.name, "1");
        paramCntrMap.put(WebFieldDef.CREATEUSER.name, "test4");
        paramCntrMap.put(WebFieldDef.CREATEDATE.name, "20210519082350");
        retValue = WebParameterUtils.getAUDBaseInfo(paramCntrMap,
                true, defOpEntity, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        retEntry = (BaseEntity) result.getRetData();
        Assert.assertEquals(String.valueOf(retEntry.getDataVerId()),
                paramCntrMap.get(WebFieldDef.DATAVERSIONID.name));
        Assert.assertEquals(retEntry.getCreateUser(),
                paramCntrMap.get(WebFieldDef.CREATEUSER.name));
        Assert.assertEquals(DateTimeConvertUtils.date2yyyyMMddHHmmss(retEntry.getCreateDate()),
                paramCntrMap.get(WebFieldDef.CREATEDATE.name));
        Assert.assertEquals(retEntry.getModifyUser(),
                paramCntrMap.get(WebFieldDef.CREATEUSER.name));
        Assert.assertEquals(DateTimeConvertUtils.date2yyyyMMddHHmmss(retEntry.getModifyDate()),
                paramCntrMap.get(WebFieldDef.CREATEDATE.name));
        // case 5
        paramCntrMap.clear();
        defOpEntity = new BaseEntity(1, "aa",
                new Date(), "modify", new Date());
        retValue = WebParameterUtils.getAUDBaseInfo(paramCntrMap,
                true, defOpEntity, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        retEntry = (BaseEntity) result.getRetData();
        Assert.assertEquals(retEntry.getDataVerId(),
                defOpEntity.getDataVerId());
        Assert.assertEquals(retEntry.getCreateUser(),
                defOpEntity.getCreateUser());
        Assert.assertEquals(retEntry.getCreateDate(),
                defOpEntity.getCreateDate());
        Assert.assertEquals(retEntry.getModifyUser(),
                defOpEntity.getModifyUser());
        Assert.assertEquals(retEntry.getModifyDate(),
                defOpEntity.getModifyDate());
        // case 4
        paramCntrMap.clear();
        defOpEntity = null;
        paramCntrMap.put(WebFieldDef.DATAVERSIONID.name, "1");
        paramCntrMap.put(WebFieldDef.MODIFYUSER.name, "test4");
        paramCntrMap.put(WebFieldDef.MODIFYDATE.name, "20210519082350");
        retValue = WebParameterUtils.getAUDBaseInfo(paramCntrMap,
                false, defOpEntity, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        retEntry = (BaseEntity) result.getRetData();
        Assert.assertEquals(String.valueOf(retEntry.getDataVerId()),
                paramCntrMap.get(WebFieldDef.DATAVERSIONID.name));
        Assert.assertTrue(TStringUtils.isBlank(retEntry.getCreateUser()));
        Assert.assertEquals(retEntry.getModifyUser(),
                paramCntrMap.get(WebFieldDef.MODIFYUSER.name));
        Assert.assertEquals(DateTimeConvertUtils.date2yyyyMMddHHmmss(retEntry.getModifyDate()),
                paramCntrMap.get(WebFieldDef.MODIFYDATE.name));
    }

    @Test
    public void getTopicPropInfoTest() {
        StringBuilder sBuffer = new StringBuilder(512);
        ProcessResult result = new ProcessResult();
        Map<String, String> paramCntrMap = new HashMap<>();
        // case 1
        paramCntrMap.put(WebFieldDef.NUMTOPICSTORES.name, "1");
        paramCntrMap.put(WebFieldDef.NUMPARTITIONS.name, "2");
        paramCntrMap.put(WebFieldDef.UNFLUSHTHRESHOLD.name, "3");
        paramCntrMap.put(WebFieldDef.UNFLUSHINTERVAL.name, "4");
        paramCntrMap.put(WebFieldDef.UNFLUSHDATAHOLD.name, "5");
        paramCntrMap.put(WebFieldDef.MCACHESIZEINMB.name, "2");
        paramCntrMap.put(WebFieldDef.UNFMCACHECNTINK.name, "7");
        paramCntrMap.put(WebFieldDef.UNFMCACHEINTERVAL.name, "4000");
        paramCntrMap.put(WebFieldDef.ACCEPTPUBLISH.name, "true");
        paramCntrMap.put(WebFieldDef.ACCEPTSUBSCRIBE.name, "false");
        paramCntrMap.put(WebFieldDef.DATASTORETYPE.name, "9");
        paramCntrMap.put(WebFieldDef.DATAPATH.name, "test");
        paramCntrMap.put(WebFieldDef.DELETEPOLICY.name, "delete,2h");
        boolean retValue = WebParameterUtils.getTopicPropInfo(paramCntrMap,
                null, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        TopicPropGroup retEntry = (TopicPropGroup) result.getRetData();
        Assert.assertEquals(retEntry.getNumTopicStores(),
                Integer.parseInt(paramCntrMap.get(WebFieldDef.NUMTOPICSTORES.name)));
        // case 2
        paramCntrMap.clear();
        TopicPropGroup defOpEntity = new TopicPropGroup();
        defOpEntity.fillDefaultValue();
        paramCntrMap.put(WebFieldDef.ACCEPTPUBLISH.name, "0");
        paramCntrMap.put(WebFieldDef.NUMTOPICSTORES.name, "9");
        paramCntrMap.put(WebFieldDef.UNFMCACHECNTINK.name, "100");
        retValue = WebParameterUtils.getTopicPropInfo(paramCntrMap,
                defOpEntity, sBuffer, result);
        Assert.assertTrue(retValue);
        Assert.assertTrue(result.isSuccess());
        retEntry = (TopicPropGroup) result.getRetData();

    }

}
