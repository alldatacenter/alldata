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

import com.google.common.collect.Maps;
import org.apache.inlong.common.enums.ComponentTypeEnum;
import org.apache.inlong.common.heartbeat.GroupHeartbeat;
import org.apache.inlong.common.heartbeat.StreamHeartbeat;
import org.apache.inlong.common.constant.ProtocolType;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatQueryRequest;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatReportRequest;
import org.apache.inlong.manager.pojo.heartbeat.StreamHeartbeatResponse;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.HeartbeatService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Heartbeat service test.
 */
@EnableAutoConfiguration
public class HeartbeatServiceTest extends ServiceBaseTest {

    @Autowired
    private HeartbeatService heartbeatService;

    @Test
    public void testReportHeartbeat() {
        HeartbeatReportRequest request = new HeartbeatReportRequest();
        request.setComponentType(ComponentTypeEnum.DataProxy.getType());
        request.setIp("127.0.0.1");
        request.setPort("56802");
        request.setClusterTag("default_cluster");
        request.setReportTime(Instant.now().toEpochMilli());
        request.setProtocolType(ProtocolType.HTTP);

        List<GroupHeartbeat> groupHeartbeats = new ArrayList<>();
        GroupHeartbeat groupHeartbeat = new GroupHeartbeat();
        groupHeartbeat.setInlongGroupId("group1");
        groupHeartbeat.setStatus("running");
        request.setGroupHeartbeats(groupHeartbeats);

        StreamHeartbeat streamHeartbeat = new StreamHeartbeat();
        Map<String, String> metrics = Maps.newHashMap();
        metrics.put("count", "10000");
        metrics.put("speed", "100/s");
        streamHeartbeat.setMetric(JsonUtils.toJsonString(metrics));
        streamHeartbeat.setStatus("running");
        streamHeartbeat.setInlongGroupId("group1");
        streamHeartbeat.setInlongStreamId("stream1");
        request.setStreamHeartbeats(Collections.singletonList(streamHeartbeat));

        Assertions.assertTrue(heartbeatService.reportHeartbeat(request));
    }

    @Test
    public void testGetStreamHeartbeat() {
        HeartbeatQueryRequest request = new HeartbeatQueryRequest();
        request.setComponent(ComponentTypeEnum.DataProxy.getType());
        request.setInstance("127.0.0.1");
        request.setInlongGroupId("group1");
        request.setInlongStreamId("stream1");

        StreamHeartbeatResponse response = heartbeatService.getStreamHeartbeat(request);
        Assertions.assertEquals("127.0.0.1", response.getInstance());
    }

}
