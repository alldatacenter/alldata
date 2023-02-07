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
import org.apache.inlong.manager.client.api.inner.client.InlongConsumeClient;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.consume.InlongConsumeBriefInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeCountInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumePageRequest;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;
import org.apache.inlong.manager.pojo.consume.pulsar.ConsumePulsarInfo;
import org.apache.inlong.manager.pojo.consume.pulsar.ConsumePulsarRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

/**
 * Tests for {@link InlongConsumeClient}
 */
public class InlongConsumeClientTest extends ClientFactoryTest {

    private final InlongConsumeClient consumeClient = clientFactory.getConsumeClient();

    @Test
    void testConsumeSave() {
        stubFor(
                post(urlMatching("/inlong/manager/api/consume/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(1)))));

        InlongConsumeRequest request = new ConsumePulsarRequest();
        request.setTopic("test_topic");
        request.setMqType(MQType.PULSAR);
        request.setConsumerGroup("test_consume_group");
        Integer consumeId = consumeClient.save(request);
        Assertions.assertEquals(1, consumeId);
    }

    @Test
    void testConsumeGet() {
        InlongConsumeInfo response = new ConsumePulsarInfo();
        response.setMqType(MQType.PULSAR);
        response.setId(1);

        stubFor(
                get(urlMatching("/inlong/manager/api/consume/get/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(response)))));

        InlongConsumeInfo consumeInfo = consumeClient.get(1);
        Assertions.assertEquals(1, consumeInfo.getId());
        Assertions.assertTrue(consumeInfo instanceof ConsumePulsarInfo);
    }

    @Test
    void testConsumeCountStatus() {
        InlongConsumeCountInfo response = new InlongConsumeCountInfo();
        response.setTotalCount(10);
        response.setRejectCount(2);
        response.setWaitApproveCount(5);
        response.setWaitAssignCount(3);

        stubFor(
                get(urlMatching("/inlong/manager/api/consume/countStatus.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(response)))));

        InlongConsumeCountInfo consumeCountInfo = consumeClient.countStatusByUser();
        Assertions.assertEquals(10, consumeCountInfo.getTotalCount());
    }

    @Test
    void testConsumeList() {
        List<InlongConsumeBriefInfo> responses = Lists.newArrayList(
                InlongConsumeBriefInfo.builder()
                        .id(1)
                        .mqType(MQType.PULSAR)
                        .inlongGroupId("test_group_id")
                        .consumerGroup("test_consume_group")
                        .build());

        stubFor(
                get(urlMatching("/inlong/manager/api/consume/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(new PageResult<>(responses))))));

        PageResult<InlongConsumeBriefInfo> briefInfoPageResult = consumeClient.list(new InlongConsumePageRequest());
        Assertions.assertEquals(JsonUtils.toJsonString(responses),
                JsonUtils.toJsonString(briefInfoPageResult.getList()));
    }

    @Test
    void testConsumeUpdate() {
        stubFor(
                post(urlMatching("/inlong/manager/api/consume/update.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(1)))));

        InlongConsumeRequest request = new ConsumePulsarRequest();
        request.setId(1);
        request.setMqType(MQType.PULSAR);
        Integer consumeId = consumeClient.update(request);
        Assertions.assertEquals(1, consumeId);
    }

    @Test
    void testConsumeDelete() {
        stubFor(
                delete(urlMatching("/inlong/manager/api/consume/delete/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(true)))));

        InlongConsumeRequest request = new ConsumePulsarRequest();
        request.setId(1);
        request.setMqType(MQType.PULSAR);
        Boolean delete = consumeClient.delete(1);
        Assertions.assertTrue(delete);
    }
}
