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

package org.apache.inlong.manager.service.core.impl;

import org.apache.inlong.common.pojo.sdk.SortSourceConfigResponse;
import org.apache.inlong.manager.dao.entity.SortSourceConfigEntity;
import org.apache.inlong.manager.dao.mapper.SortSourceConfigEntityMapper;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.SortService;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Sort service test for {@link SortService}
 */
public class SortServiceImplTest extends ServiceBaseTest {

    private static final String TEST_CLUSTER = "testCluster";
    private static final String TEST_TASK = "testTask";
    @Autowired
    SortSourceConfigEntityMapper sourceMapper;
    @Autowired
    private SortService sortService;

    @Test
    @Transactional
    public void testSourceEmptyParams() {
        SortSourceConfigResponse response = sortService.getSourceConfig("", "", "");
        System.out.println(response.toString());
        Assert.assertEquals(response.getCode(), -101);
        Assert.assertNull(response.getMd5());
        Assert.assertNull(response.getData());
        Assert.assertNotNull(response.getMsg());
    }

    @Test
    @Transactional
    public void testSourceCorrectParams() {
        SortSourceConfigResponse response = sortService.getSourceConfig(TEST_CLUSTER, TEST_TASK, "");
        JSONObject jo = new JSONObject(response);
        System.out.println(jo);
        Assert.assertEquals(0, response.getCode());
        Assert.assertNotNull(response.getData());
        Assert.assertNotNull(response.getMd5());
        Assert.assertNotNull(response.getMsg());
    }

    @Test
    @Transactional
    public void testSourceSameMd5() {
        SortSourceConfigResponse response = sortService.getSourceConfig(TEST_CLUSTER, TEST_TASK, "");
        String md5 = response.getMd5();
        response = sortService.getSourceConfig(TEST_CLUSTER, TEST_TASK, md5);
        System.out.println(response);
        Assert.assertEquals(1, response.getCode());
        Assert.assertEquals(md5, response.getMd5());
        Assert.assertNull(response.getData());
        Assert.assertNotNull(response.getMsg());
    }

    @Test
    @Transactional
    public void testSourceErrorClusterName() {
        SortSourceConfigResponse response = sortService.getSourceConfig("errCluster", "errTask", "");
        System.out.println(response.toString());
        Assert.assertEquals(response.getCode(), -101);
        Assert.assertNull(response.getMd5());
        Assert.assertNull(response.getData());
        Assert.assertNotNull(response.getMsg());
    }

    @Test
    @Transactional
    public void testSourceDuplicatedZoneParam() {
        sourceMapper.insertSelective(prepareSourceEntity(TEST_CLUSTER, TEST_TASK, "testZone1", null));
        SortSourceConfigResponse response = sortService.getSourceConfig(TEST_CLUSTER, TEST_TASK, "");
        System.out.println(response);
        Assert.assertEquals(-1, response.getCode());
        Assert.assertNull(response.getData());
        Assert.assertNull(response.getMd5());
        Assert.assertNotNull(response.getMsg());
    }

    @Before
    public void prepareSourceProperties() {
        String testZone = "testZone";
        String testTopic = "testTopic";
        sourceMapper.insertSelective(prepareSourceEntity(TEST_CLUSTER, TEST_TASK, "testZone1", "topic1"));
        sourceMapper.insertSelective(prepareSourceEntity(TEST_CLUSTER, TEST_TASK, "testZone1", "topic2"));
        sourceMapper.insertSelective(prepareSourceEntity(TEST_CLUSTER, TEST_TASK, "testZone1", null));
        sourceMapper.insertSelective(prepareSourceEntity(TEST_CLUSTER, TEST_TASK, "testZone2", "topic1"));
        sourceMapper.insertSelective(prepareSourceEntity(TEST_CLUSTER, TEST_TASK, "testZone2", "topic2"));
        sourceMapper.insertSelective(prepareSourceEntity(TEST_CLUSTER, TEST_TASK, "testZone2", null));
    }

    private SortSourceConfigEntity prepareSourceEntity(
            String clusterName,
            String taskName,
            String zone,
            String topic) {

        Map<String, String> extParamMap = new HashMap<>();
        extParamMap.put("param key of " + zone + topic, "param value of " + zone + topic);

        if (topic == null) {
            extParamMap.put("zoneName", zone);
            extParamMap.put("serviceUrl", "testUrl");
            extParamMap.put("authentication", "testAuth");
            extParamMap.put("zoneType", "testZoneType");
        } else {
            extParamMap.put("partitionCnt", "123");
            extParamMap.put("topic", "testTopic");
        }

        JSONObject jo = new JSONObject(extParamMap);
        return SortSourceConfigEntity.builder()
                .clusterName(clusterName)
                .taskName(taskName)
                .zoneName(zone)
                .topic(topic)
                .extParams(jo.toString())
                .build();
    }

}