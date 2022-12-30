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

package org.apache.inlong.manager.service.sort;

import org.apache.inlong.common.pojo.sdk.SortSourceConfigResponse;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterResponse;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.mapper.DataNodeEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.SortService;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Sort service test for {@link SortService}
 */
@TestMethodOrder(OrderAnnotation.class)
public class SortServiceImplTest extends ServiceBaseTest {

    private static final String TEST_CLUSTER = "testCluster";
    private static final String TEST_TASK = "testTask";
    private static final String TEST_GROUP = "testGroup";
    private static final String TEST_TAG = "testTag";
    private static final String TEST_TOPIC = "testTopic";
    private static final String TEST_SINK_TYPE = "testSinkType";
    private static final String TEST_CREATOR = "testUser";

    @Autowired
    private InlongClusterEntityMapper clusterEntityMapper;
    @Autowired
    private StreamSinkEntityMapper streamSinkEntityMapper;
    @Autowired
    private InlongGroupEntityMapper inlongGroupEntityMapper;
    @Autowired
    private DataNodeEntityMapper dataNodeEntityMapper;
    @Autowired
    private SortService sortService;

    @Test
    @Order(1)
    @Transactional
    public void testSourceEmptyParams() {
        SortSourceConfigResponse response = sortService.getSourceConfig("", "", "");
        System.out.println(response.toString());
        Assertions.assertEquals(response.getCode(), -101);
        Assertions.assertNull(response.getMd5());
        Assertions.assertNull(response.getData());
        Assertions.assertNotNull(response.getMsg());
    }

    @Test
    @Order(2)
    @Transactional
    public void testSourceCorrectParams() {
        SortSourceConfigResponse response = sortService.getSourceConfig(TEST_CLUSTER, TEST_TASK, "");
        JSONObject jo = new JSONObject(response);
        System.out.println(jo);
        Assertions.assertEquals(0, response.getCode());
        Assertions.assertNotNull(response.getData());
        Assertions.assertNotNull(response.getMd5());
        Assertions.assertNotNull(response.getMsg());
    }

    @Test
    @Order(3)
    @Transactional
    public void testSourceSameMd5() {
        SortSourceConfigResponse response = sortService.getSourceConfig(TEST_CLUSTER, TEST_TASK, "");
        String md5 = response.getMd5();
        response = sortService.getSourceConfig(TEST_CLUSTER, TEST_TASK, md5);
        System.out.println(response);
        Assertions.assertEquals(1, response.getCode());
        Assertions.assertEquals(md5, response.getMd5());
        Assertions.assertNull(response.getData());
        Assertions.assertNotNull(response.getMsg());
    }

    @Test
    @Order(4)
    @Transactional
    public void testSourceErrorClusterName() {
        SortSourceConfigResponse response = sortService.getSourceConfig("errCluster", "errTask", "");
        System.out.println(response.toString());
        Assertions.assertEquals(0, response.getCode());
        Assertions.assertNull(response.getMd5());
        Assertions.assertNull(response.getData());
        Assertions.assertNotNull(response.getMsg());
    }

    @Test
    @Order(5)
    @Transactional
    public void testClusterEmptyParams() {
        SortClusterResponse response = sortService.getClusterConfig("", "");
        System.out.println(response.toString());
        Assertions.assertEquals(response.getCode(), -101);
        Assertions.assertNull(response.getMd5());
        Assertions.assertNull(response.getData());
        Assertions.assertNotNull(response.getMsg());

    }

    @Test
    @Order(6)
    @Transactional
    public void testClusterCorrectParams() {
        SortClusterResponse response = sortService.getClusterConfig(TEST_CLUSTER, "");
        JSONObject jo = new JSONObject(response);
        System.out.println(jo);
        Assertions.assertEquals(0, response.getCode());
        Assertions.assertNotNull(response.getData());
        Assertions.assertNotNull(response.getMd5());
        Assertions.assertNotNull(response.getMsg());
    }

    @Test
    @Order(7)
    @Transactional
    public void testClusterSameMd5() {
        SortClusterResponse response = sortService.getClusterConfig(TEST_CLUSTER, "");
        String md5 = response.getMd5();
        response = sortService.getClusterConfig(TEST_CLUSTER, md5);
        System.out.println(response);
        Assertions.assertEquals(1, response.getCode());
        Assertions.assertEquals(md5, response.getMd5());
        Assertions.assertNull(response.getData());
        Assertions.assertNotNull(response.getMsg());
    }

    @Test
    @Order(8)
    @Transactional
    public void testClusterErrorClusterName() {
        SortClusterResponse response = sortService.getClusterConfig("errCluster", "");
        System.out.println(response.toString());
        Assertions.assertEquals(0, response.getCode());
        Assertions.assertNull(response.getMd5());
        Assertions.assertNull(response.getData());
        Assertions.assertNotNull(response.getMsg());
    }

    @BeforeEach
    private void prepareAll() {
        this.prepareCluster(TEST_CLUSTER);
        this.prepareTask(TEST_TASK, TEST_GROUP, TEST_CLUSTER);
        this.prepareGroupId(TEST_GROUP);
        this.preparePulsar("testPulsar", true);
        this.prepareDataNode(TEST_TASK);
    }

    private void prepareDataNode(String taskName) {
        DataNodeEntity entity = new DataNodeEntity();
        entity.setName(taskName);
        entity.setType(TEST_SINK_TYPE);
        entity.setExtParams("{\"paramKey1\":\"paramValue1\"}");
        entity.setCreator(TEST_CREATOR);
        entity.setInCharges(TEST_CREATOR);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setIsDeleted(InlongConstants.UN_DELETED);
        entity.setVersion(InlongConstants.INITIAL_VERSION);
        dataNodeEntityMapper.insert(entity);
    }

    private void prepareGroupId(String groupId) {
        InlongGroupEntity entity = new InlongGroupEntity();
        entity.setInlongGroupId(groupId);
        entity.setInlongClusterTag(TEST_TAG);
        entity.setMqResource(TEST_TOPIC);
        entity.setMqType("PULSAR");
        entity.setName("testName");
        entity.setDescription("testDescription");
        entity.setCreator(TEST_CREATOR);
        entity.setInCharges(TEST_CREATOR);
        entity.setCreateTime(new Date());
        entity.setModifyTime(new Date());
        entity.setIsDeleted(InlongConstants.UN_DELETED);
        entity.setVersion(InlongConstants.INITIAL_VERSION);
        inlongGroupEntityMapper.insert(entity);
    }

    private void prepareCluster(String clusterName) {
        InlongClusterEntity entity = new InlongClusterEntity();
        entity.setName(clusterName);
        entity.setType(TEST_SINK_TYPE);
        entity.setExtParams("{}");
        entity.setCreator(TEST_CREATOR);
        entity.setInCharges(TEST_CREATOR);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setIsDeleted(InlongConstants.UN_DELETED);
        entity.setVersion(InlongConstants.INITIAL_VERSION);
        clusterEntityMapper.insert(entity);
    }

    private void preparePulsar(String pulsarName, boolean isConsumable) {
        InlongClusterEntity entity = new InlongClusterEntity();
        entity.setName(pulsarName);
        entity.setType("PULSAR");
        entity.setClusterTags(TEST_TAG);
        entity.setCreator(TEST_CREATOR);
        entity.setInCharges(TEST_CREATOR);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setIsDeleted(InlongConstants.UN_DELETED);
        entity.setVersion(InlongConstants.INITIAL_VERSION);
        String extTag = "zone=" + TEST_TAG
                + "&producer=true"
                + "&consumer=" + (isConsumable ? "true" : "false");
        entity.setExtTag(extTag);
        entity.setExtParams("{\"tenant\":\"testTenant\",\"namespace\":\"testNS\",\"serviceUrl\":\"testServiceUrl\","
                + "\"authentication\":\"testAuth\",\"adminUrl\":\"testAdmin\"}");
        clusterEntityMapper.insert(entity);
    }

    private void prepareTask(String taskName, String groupId, String clusterName) {
        StreamSinkEntity entity = new StreamSinkEntity();
        entity.setInlongGroupId(groupId);
        entity.setInlongStreamId("1");
        entity.setSinkType(TEST_SINK_TYPE);
        entity.setSinkName(taskName);
        entity.setInlongClusterName(clusterName);
        entity.setDataNodeName(taskName);
        entity.setSortTaskName(taskName);
        entity.setCreator(TEST_CREATOR);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setIsDeleted(InlongConstants.UN_DELETED);
        entity.setVersion(InlongConstants.INITIAL_VERSION);
        entity.setExtParams("{\"delimiter\":\"|\",\"dataType\":\"text\"}");
        streamSinkEntityMapper.insert(entity);
    }

}