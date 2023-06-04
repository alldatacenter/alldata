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

import org.apache.inlong.common.constant.ClusterSwitch;
import org.apache.inlong.common.pojo.sdk.CacheZone;
import org.apache.inlong.common.pojo.sdk.CacheZoneConfig;
import org.apache.inlong.common.pojo.sdk.SortSourceConfigResponse;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterResponse;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.mapper.DataNodeEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarRequest;
import org.apache.inlong.manager.pojo.node.hive.HiveDataNodeRequest;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.hive.HiveSinkRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamExtInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.core.SortService;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.node.DataNodeService;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sort service test for {@link SortService}
 */
@TestMethodOrder(OrderAnnotation.class)
public class SortServiceImplTest extends ServiceBaseTest {

    private static final String TEST_GROUP = "test-group";
    private static final String TEST_CLUSTER_1 = "test-cluster-1";
    private static final String TEST_CLUSTER_2 = "test-cluster-2";
    private static final String TEST_CLUSTER_3 = "test-cluster-3";
    private static final String TEST_TASK_1 = "test-task-1";
    private static final String TEST_TASK_2 = "test-task-2";
    private static final String TEST_TASK_3 = "test-task-3";
    private static final String TEST_STREAM_1 = "1";
    private static final String TEST_STREAM_2 = "2";
    private static final String TEST_TAG = "testTag";
    private static final String BACK_UP_TAG = "testBackupTag";
    private static final String TEST_TOPIC_1 = "testTopic";
    private static final String TEST_TOPIC_2 = "testTopic2";
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
    @Autowired
    private InlongClusterService clusterService;
    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private DataNodeService dataNodeService;
    @Autowired
    private StreamSinkService streamSinkService;

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
    public void testSourceCorrectParamsOfNoTagSortCluster() {
        SortSourceConfigResponse response = sortService.getSourceConfig(TEST_CLUSTER_1, TEST_TASK_1, "");
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
        SortSourceConfigResponse response = sortService.getSourceConfig(TEST_CLUSTER_1, TEST_TASK_1, "");
        String md5 = response.getMd5();
        response = sortService.getSourceConfig(TEST_CLUSTER_1, TEST_TASK_1, md5);
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
        SortClusterResponse response = sortService.getClusterConfig(TEST_CLUSTER_1, "");
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
        SortClusterResponse response = sortService.getClusterConfig(TEST_CLUSTER_1, "");
        String md5 = response.getMd5();
        response = sortService.getClusterConfig(TEST_CLUSTER_1, md5);
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

    @Test
    @Order(9)
    @Transactional
    public void testSourceCorrectParamsOfTaggedSortCluster() {
        SortSourceConfigResponse response = sortService.getSourceConfig(TEST_CLUSTER_2, TEST_TASK_2, "");
        JSONObject jo = new JSONObject(response);
        System.out.println(jo);
        Assertions.assertEquals(0, response.getCode());
        Assertions.assertNotNull(response.getMd5());
        Assertions.assertNotNull(response.getMsg());
        CacheZoneConfig config = response.getData();
        Assertions.assertNotNull(config);
        Assertions.assertEquals(TEST_CLUSTER_2, config.getSortClusterName());
        Assertions.assertEquals(TEST_TASK_2, config.getSortTaskId());
        Assertions.assertEquals(1, config.getCacheZones().size());
        CacheZone zone = config.getCacheZones().get("testPulsar");
        Assertions.assertNotNull(zone);
        Assertions.assertEquals("testPulsar", zone.getZoneName());
        Assertions.assertEquals(1, zone.getTopics().size());

        response = sortService.getSourceConfig(TEST_CLUSTER_3, TEST_TASK_3, "");
        jo = new JSONObject(response);
        System.out.println(jo);
        Assertions.assertEquals(0, response.getCode());
        Assertions.assertNotNull(response.getMd5());
        Assertions.assertNotNull(response.getMsg());
        config = response.getData();
        Assertions.assertNotNull(config);
        Assertions.assertEquals(TEST_CLUSTER_3, config.getSortClusterName());
        Assertions.assertEquals(TEST_TASK_3, config.getSortTaskId());
        Assertions.assertEquals(1, config.getCacheZones().size());
        zone = config.getCacheZones().get("backupPulsar");
        Assertions.assertNotNull(zone);
        Assertions.assertEquals("backupPulsar", zone.getZoneName());
        Assertions.assertEquals(1, zone.getTopics().size());
    }

    @BeforeEach
    private void prepareAll() {
        this.prepareCluster(TEST_CLUSTER_1, null);
        this.prepareCluster(TEST_CLUSTER_2, TEST_TAG);
        this.prepareCluster(TEST_CLUSTER_3, BACK_UP_TAG);
        this.preparePulsar("testPulsar", true, TEST_TAG);
        this.preparePulsar("backupPulsar", true, BACK_UP_TAG);
        this.prepareDataNode(TEST_TASK_1);
        this.prepareDataNode(TEST_TASK_2);
        this.prepareDataNode(TEST_TASK_3);
        this.prepareGroupId(TEST_GROUP);
        this.prepareStreamId(TEST_GROUP, TEST_STREAM_1, TEST_TOPIC_1);
        this.prepareStreamId(TEST_GROUP, TEST_STREAM_2, TEST_TOPIC_2);
        this.prepareTask(TEST_TASK_1, TEST_GROUP, TEST_CLUSTER_1, TEST_STREAM_1);
        this.prepareTask(TEST_TASK_1, TEST_GROUP, TEST_CLUSTER_1, TEST_STREAM_2);
        this.prepareTask(TEST_TASK_2, TEST_GROUP, TEST_CLUSTER_2, TEST_STREAM_1);
        this.prepareTask(TEST_TASK_3, TEST_GROUP, TEST_CLUSTER_3, TEST_STREAM_2);
    }

    private void prepareDataNode(String taskName) {
        HiveDataNodeRequest request = new HiveDataNodeRequest();
        request.setUrl("test_hive_url");
        request.setName(taskName);
        request.setExtParams("{\"paramKey1\":\"paramValue1\",\"hdfsUgi\":\"test_hdfsUgi\"}");
        request.setDataPath("testPath");
        request.setHiveConfDir("testDir");
        request.setWarehouse("testWareHouse");
        request.setHdfsUgi("testUgi");
        request.setInCharges(TEST_CREATOR);
        request.setUsername("test_hive_user");
        request.setToken("test_hive_token");
        dataNodeService.save(request, TEST_CREATOR);
    }

    private void prepareGroupId(String groupId) {
        InlongPulsarRequest request = new InlongPulsarRequest();
        request.setInlongGroupId(groupId);
        request.setMqResource("test_namespace");
        request.setInlongClusterTag(TEST_TAG);
        request.setVersion(InlongConstants.INITIAL_VERSION);
        request.setName("test_group_name");
        request.setMqType(ClusterType.PULSAR);
        request.setInCharges(TEST_CREATOR);
        List<InlongGroupExtInfo> extList = new ArrayList<>();
        InlongGroupExtInfo ext1 = InlongGroupExtInfo
                .builder()
                .inlongGroupId(groupId)
                .keyName(ClusterSwitch.BACKUP_CLUSTER_TAG)
                .keyValue(BACK_UP_TAG)
                .build();
        InlongGroupExtInfo ext2 = InlongGroupExtInfo
                .builder()
                .inlongGroupId(groupId)
                .keyName(ClusterSwitch.BACKUP_MQ_RESOURCE)
                .keyValue("backup_name")
                .build();

        extList.add(ext1);
        extList.add(ext2);
        request.setExtList(extList);
        groupService.save(request, "test operator");
    }

    private void prepareStreamId(String groupId, String streamId, String topic) {
        InlongStreamRequest request = new InlongStreamRequest();
        request.setInlongGroupId(groupId);
        request.setInlongStreamId(streamId);
        request.setName("test_stream_name");
        request.setMqResource(topic);
        request.setVersion(InlongConstants.INITIAL_VERSION);
        List<InlongStreamExtInfo> extInfos = new ArrayList<>();
        InlongStreamExtInfo ext = new InlongStreamExtInfo();
        extInfos.add(ext);
        ext.setInlongStreamId(streamId);
        ext.setInlongGroupId(groupId);
        ext.setKeyName(ClusterSwitch.BACKUP_MQ_RESOURCE);
        ext.setKeyValue("backup_" + topic);
        request.setExtList(extInfos);
        streamService.save(request, "test_operator");
    }

    private void prepareCluster(String clusterName, String clusterTag) {
        InlongClusterEntity entity = new InlongClusterEntity();
        entity.setName(clusterName);
        entity.setType(DataNodeType.HIVE);
        entity.setExtParams("{}");
        entity.setCreator(TEST_CREATOR);
        entity.setInCharges(TEST_CREATOR);
        entity.setClusterTags(clusterTag);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setIsDeleted(InlongConstants.UN_DELETED);
        entity.setVersion(InlongConstants.INITIAL_VERSION);
        clusterEntityMapper.insert(entity);
    }

    private void preparePulsar(String pulsarName, boolean isConsumable, String tag) {
        PulsarClusterRequest request = new PulsarClusterRequest();
        request.setUrl("testServiceUrl");
        request.setName(pulsarName);
        request.setType(ClusterType.PULSAR);
        request.setClusterTags(tag);
        request.setInCharges(TEST_CREATOR);
        request.setVersion(InlongConstants.INITIAL_VERSION);
        String extTag = "zone=" + TEST_TAG
                + "&producer=true"
                + "&consumer=" + (isConsumable ? "true" : "false");
        request.setExtTag(extTag);
        request.setExtParams("{\"tenant\":\"testTenant\","
                + "\"authentication\":\"testAuth\",\"adminUrl\":\"testAdmin\"}");
        clusterService.save(request, "test operator");
    }

    private void prepareTask(String taskName, String groupId, String clusterName, String streamId) {
        SinkRequest request = new HiveSinkRequest();
        request.setDataNodeName(taskName);
        request.setSinkType(SinkType.HIVE);
        request.setInlongClusterName(clusterName);
        request.setSinkName(taskName);
        request.setSortTaskName(taskName);
        request.setInlongGroupId(groupId);
        request.setInlongStreamId(streamId);
        Map<String, Object> properties = new HashMap<>();
        properties.put("delimiter", "|");
        properties.put("dataType", "text");
        request.setProperties(properties);
        streamSinkService.save(request, TEST_CREATOR);
    }

}
