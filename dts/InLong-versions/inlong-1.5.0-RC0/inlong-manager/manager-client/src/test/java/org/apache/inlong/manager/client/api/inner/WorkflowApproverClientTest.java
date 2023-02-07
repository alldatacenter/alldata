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
import org.apache.inlong.manager.client.api.inner.client.WorkflowApproverClient;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.workflow.ApproverPageRequest;
import org.apache.inlong.manager.pojo.workflow.ApproverRequest;
import org.apache.inlong.manager.pojo.workflow.ApproverResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

/**
 * Tests for {@link org.apache.inlong.manager.client.api.inner.client.WorkflowApproverClient}
 */
class WorkflowApproverClientTest extends ClientFactoryTest {

    private static final WorkflowApproverClient workflowApproverClient = clientFactory.getWorkflowApproverClient();

    @Test
    void testSave() {
        stubFor(
                post(urlMatching("/inlong/manager/api/workflow/approver/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(11)))));
        ApproverRequest approverRequest = ApproverRequest.builder()
                .processName("test")
                .approvers("user1,user2")
                .taskName("task1")
                .version(1)
                .build();

        Integer workflowApproverId = workflowApproverClient.save(approverRequest);
        Assertions.assertEquals(11, workflowApproverId);
    }

    @Test
    void testListByCondition() {
        stubFor(
                get(urlMatching("/inlong/manager/api/workflow/approver/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(
                                                new PageResult<>(
                                                        Lists.newArrayList(
                                                                ApproverResponse.builder()
                                                                        .taskName("task1")
                                                                        .approvers("user1,user2")
                                                                        .id(1)
                                                                        .build(),
                                                                ApproverResponse.builder()
                                                                        .taskName("task2")
                                                                        .approvers("user1,user2")
                                                                        .id(2)
                                                                        .build())))))));

        ApproverPageRequest request = ApproverPageRequest.builder()
                .processName("process1")
                .approver("user1")
                .build();
        request.setPageNum(1);
        request.setPageSize(10);

        List<ApproverResponse> approverResponseList = workflowApproverClient.listByCondition(request);

        Assertions.assertEquals(2, approverResponseList.size());
    }
}
