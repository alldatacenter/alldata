/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.web.controller;

import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.dao.mapper.DataNodeEntityMapper;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.node.hive.HiveDataNodeRequest;
import org.apache.inlong.manager.web.WebBaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import javax.annotation.Resource;

import java.util.Date;

class DataNodeControllerTest extends WebBaseTest {

    @Resource
    DataNodeEntityMapper dataNodeMapper;

    HiveDataNodeRequest getHiveDataNodeRequest() {
        HiveDataNodeRequest hiveDataNodeRequest = new HiveDataNodeRequest();
        hiveDataNodeRequest.setName("hiveNode1");
        hiveDataNodeRequest.setType(DataNodeType.HIVE);
        hiveDataNodeRequest.setUrl("127.0.0.1:8080");
        hiveDataNodeRequest.setUsername("admin");
        hiveDataNodeRequest.setToken("123");
        hiveDataNodeRequest.setInCharges("admin");
        return hiveDataNodeRequest;
    }

    @Test
    void testSaveAndGetAndDelete() throws Exception {
        // save
        MvcResult mvcResult = postForSuccessMvcResult("/api/node/save", getHiveDataNodeRequest());

        Integer dataNodeId = getResBodyObj(mvcResult, Integer.class);
        Assertions.assertNotNull(dataNodeId);

        // get
        MvcResult getResult = getForSuccessMvcResult("/api/node/get/{id}", dataNodeId);

        DataNodeInfo dataNode = getResBodyObj(getResult, DataNodeInfo.class);
        Assertions.assertNotNull(dataNode);
        Assertions.assertEquals(getHiveDataNodeRequest().getName(), dataNode.getName());

        // delete
        MvcResult deleteResult = deleteForSuccessMvcResult("/api/node/delete/{id}", dataNodeId);

        Boolean success = getResBodyObj(deleteResult, Boolean.class);
        Assertions.assertTrue(success);

        DataNodeEntity dataNodeEntity = dataNodeMapper.selectById(dataNodeId);
        Assertions.assertNull(dataNodeEntity);
    }

    @Test
    void testSaveAndGetAndDeleteByKey() throws Exception {
        HiveDataNodeRequest request = getHiveDataNodeRequest();
        // save
        MvcResult mvcResult = postForSuccessMvcResult("/api/node/save", request);

        Integer dataNodeId = getResBodyObj(mvcResult, Integer.class);
        Assertions.assertNotNull(dataNodeId);

        // get
        MvcResult getResult = getForSuccessMvcResult("/api/node/get/{id}", dataNodeId);

        DataNodeInfo dataNode = getResBodyObj(getResult, DataNodeInfo.class);
        Assertions.assertNotNull(dataNode);
        Assertions.assertEquals(getHiveDataNodeRequest().getName(), dataNode.getName());

        // delete
        MvcResult deleteResult = deleteForSuccessMvcResult("/api/node/deleteByKey?name=" + request.getName()
                + "&type=" + request.getType());

        Boolean success = getResBodyObj(deleteResult, Boolean.class);
        Assertions.assertTrue(success);

        DataNodeEntity dataNodeEntity = dataNodeMapper.selectById(dataNodeId);
        Assertions.assertNull(dataNodeEntity);
    }

    @Test
    void testUpdate() throws Exception {
        // insert the test data
        DataNodeEntity nodeEntity = new DataNodeEntity();
        nodeEntity.setName("test");
        nodeEntity.setType(DataNodeType.HIVE);
        nodeEntity.setIsDeleted(0);
        nodeEntity.setModifier("test");
        nodeEntity.setCreator("test");
        nodeEntity.setCreateTime(new Date());
        nodeEntity.setModifyTime(new Date());
        nodeEntity.setInCharges("test");
        nodeEntity.setVersion(InlongConstants.INITIAL_VERSION);

        dataNodeMapper.insert(nodeEntity);

        DataNodeRequest request = getHiveDataNodeRequest();
        request.setId(nodeEntity.getId());
        request.setName("test");
        request.setVersion(nodeEntity.getVersion());
        MvcResult mvcResult = postForSuccessMvcResult("/api/node/update", request);

        Boolean success = getResBodyObj(mvcResult, Boolean.class);
        Assertions.assertTrue(success);

        DataNodeEntity dataNodeEntity = dataNodeMapper.selectById(request.getId());
        Assertions.assertEquals(request.getName(), dataNodeEntity.getName());
    }

    @Test
    void testUpdateByKey() throws Exception {
        // insert the test data
        DataNodeEntity nodeEntity = new DataNodeEntity();
        nodeEntity.setName("hiveNode1");
        nodeEntity.setType(DataNodeType.HIVE);
        nodeEntity.setIsDeleted(0);
        nodeEntity.setModifier("test");
        nodeEntity.setCreator("test");
        nodeEntity.setUrl("old url");
        nodeEntity.setCreateTime(new Date());
        nodeEntity.setModifyTime(new Date());
        nodeEntity.setInCharges("test");
        nodeEntity.setVersion(InlongConstants.INITIAL_VERSION);

        dataNodeMapper.insert(nodeEntity);

        DataNodeRequest request = getHiveDataNodeRequest();
        request.setVersion(nodeEntity.getVersion());
        MvcResult mvcResult = postForSuccessMvcResult("/api/node/updateByKey", request);

        UpdateResult result = getResBodyObj(mvcResult, UpdateResult.class);
        Assertions.assertTrue(result.getSuccess());
        Assertions.assertEquals(request.getVersion() + 1, result.getVersion());

        DataNodeEntity dataNodeEntity = dataNodeMapper.selectByUniqueKey(request.getName(), request.getType());
        Assertions.assertEquals(request.getUrl(), dataNodeEntity.getUrl());
    }

    @Test
    void testUpdateFailByNoId() throws Exception {
        MvcResult mvcResult = postForSuccessMvcResult("/api/node/update", getHiveDataNodeRequest());

        Response<Boolean> response = getResBody(mvcResult, Boolean.class);
        Assertions.assertFalse(response.isSuccess());
    }

}
