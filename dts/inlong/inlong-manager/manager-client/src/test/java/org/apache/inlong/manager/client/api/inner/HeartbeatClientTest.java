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

package org.apache.inlong.manager.client.api.inner;

import com.google.common.collect.Lists;
import org.apache.inlong.common.enums.ComponentTypeEnum;
import org.apache.inlong.manager.client.api.inner.client.HeartbeatClient;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.heartbeat.ComponentHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.GroupHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatPageRequest;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatQueryRequest;
import org.apache.inlong.manager.pojo.heartbeat.StreamHeartbeatResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

/**
 * Tests for {@link HeartbeatClient}
 */
public class HeartbeatClientTest extends ClientFactoryTest {

    private static final HeartbeatClient heartbeatClient = clientFactory.getHeartbeatClient();

    @Test
    void testGetComponent() {
        ComponentHeartbeatResponse response = ComponentHeartbeatResponse.builder()
                .component(ComponentTypeEnum.Agent.getType())
                .instance("127.0.0.1")
                .build();

        stubFor(
                post(urlMatching("/inlong/manager/api/heartbeat/component/get.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(response)))));

        HeartbeatQueryRequest request = new HeartbeatQueryRequest();
        request.setComponent(ComponentTypeEnum.Agent.getType());
        request.setInstance("127.0.0.1");
        ComponentHeartbeatResponse result = heartbeatClient.getComponentHeartbeat(request);
        Assertions.assertEquals(request.getComponent(), result.getComponent());
        Assertions.assertEquals(request.getInstance(), result.getInstance());
    }

    @Test
    void testGetGroup() {
        GroupHeartbeatResponse response = new GroupHeartbeatResponse();
        response.setComponent(ComponentTypeEnum.Agent.getType());
        response.setInstance("127.0.0.1");
        response.setInlongGroupId("test_group");

        stubFor(
                post(urlMatching("/inlong/manager/api/heartbeat/group/get.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(response)))));

        HeartbeatQueryRequest request = new HeartbeatQueryRequest();
        request.setComponent(ComponentTypeEnum.Agent.getType());
        request.setInstance("127.0.0.1");
        request.setInlongGroupId("test_group");
        GroupHeartbeatResponse result = heartbeatClient.getGroupHeartbeat(request);
        Assertions.assertEquals(request.getComponent(), result.getComponent());
        Assertions.assertEquals(request.getInstance(), result.getInstance());
        Assertions.assertEquals(request.getInlongGroupId(), result.getInlongGroupId());
    }

    @Test
    void testGetStream() {
        StreamHeartbeatResponse response = new StreamHeartbeatResponse();
        response.setComponent(ComponentTypeEnum.Agent.getType());
        response.setInstance("127.0.0.1");
        response.setInlongGroupId("test_group");
        response.setInlongStreamId("test_stream");

        stubFor(
                post(urlMatching("/inlong/manager/api/heartbeat/stream/get.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(response)))));

        HeartbeatQueryRequest request = new HeartbeatQueryRequest();
        request.setComponent(ComponentTypeEnum.Agent.getType());
        request.setInstance("127.0.0.1");
        request.setInlongGroupId("test_group");
        request.setInlongStreamId("test_stream");
        StreamHeartbeatResponse result = heartbeatClient.getStreamHeartbeat(request);
        Assertions.assertEquals(request.getComponent(), result.getComponent());
        Assertions.assertEquals(request.getInstance(), result.getInstance());
        Assertions.assertEquals(request.getInlongGroupId(), result.getInlongGroupId());
        Assertions.assertEquals(request.getInlongStreamId(), result.getInlongStreamId());
    }

    @Test
    void testListComponent() {
        List<ComponentHeartbeatResponse> responses = Lists.newArrayList(
                ComponentHeartbeatResponse.builder()
                        .component(ComponentTypeEnum.Agent.getType())
                        .instance("127.0.0.1")
                        .build());

        stubFor(
                post(urlMatching("/inlong/manager/api/heartbeat/component/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(new PageResult<>(responses))))));

        HeartbeatPageRequest request = new HeartbeatPageRequest();
        request.setComponent(ComponentTypeEnum.Agent.getType());
        PageResult<ComponentHeartbeatResponse> pageResult = heartbeatClient.listComponentHeartbeat(request);
        Assertions.assertEquals(JsonUtils.toJsonString(responses), JsonUtils.toJsonString(pageResult.getList()));
    }

    @Test
    void testListGroup() {
        List<GroupHeartbeatResponse> responses = Lists.newArrayList(
                GroupHeartbeatResponse.builder()
                        .component(ComponentTypeEnum.Agent.getType())
                        .instance("127.0.0.1")
                        .build());

        stubFor(
                post(urlMatching("/inlong/manager/api/heartbeat/group/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(new PageResult<>(responses))))));

        HeartbeatPageRequest request = new HeartbeatPageRequest();
        request.setComponent(ComponentTypeEnum.Agent.getType());
        PageResult<GroupHeartbeatResponse> pageResult = heartbeatClient.listGroupHeartbeat(request);
        Assertions.assertEquals(JsonUtils.toJsonString(responses), JsonUtils.toJsonString(pageResult.getList()));
    }

    @Test
    void testListStream() {
        List<StreamHeartbeatResponse> responses = Lists.newArrayList(
                StreamHeartbeatResponse.builder()
                        .component(ComponentTypeEnum.Agent.getType())
                        .inlongGroupId("test_group")
                        .inlongStreamId("test_stream")
                        .instance("127.0.0.1")
                        .build());

        stubFor(
                post(urlMatching("/inlong/manager/api/heartbeat/stream/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(new PageResult<>(responses))))));

        HeartbeatPageRequest request = new HeartbeatPageRequest();
        request.setComponent(ComponentTypeEnum.Agent.getType());
        request.setInlongGroupId("test_group");
        PageResult<StreamHeartbeatResponse> pageResult = heartbeatClient.listStreamHeartbeat(request);
        Assertions.assertEquals(JsonUtils.toJsonString(responses), JsonUtils.toJsonString(pageResult.getList()));
    }
}
