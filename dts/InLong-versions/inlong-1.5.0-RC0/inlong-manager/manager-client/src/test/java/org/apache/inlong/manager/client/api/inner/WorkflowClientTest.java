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
import org.apache.inlong.manager.client.api.inner.client.WorkflowClient;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.pojo.workflow.ProcessRequest;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowOperationRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

public class WorkflowClientTest extends ClientFactoryTest {

    private static final WorkflowClient workflowClient = clientFactory.getWorkflowClient();

    @Test
    void testWorkflowStart() {
        WorkflowResult workflowResult = WorkflowResult.builder()
                .processInfo(ProcessResponse.builder()
                        .id(1)
                        .name(ProcessName.APPLY_GROUP_PROCESS.getDisplayName())
                        .applicant("test_user").build())
                .build();

        stubFor(
                post(urlMatching("/inlong/manager/api/workflow/start.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(workflowResult)))));
        WorkflowOperationRequest request = new WorkflowOperationRequest();
        request.setName(ProcessName.APPLY_GROUP_PROCESS);
        request.setApplicant("test_user");
        ApplyGroupProcessForm form = new ApplyGroupProcessForm();
        form.setGroupInfo(new InlongPulsarInfo());
        request.setForm(form);

        WorkflowResult workflowInfo = workflowClient.start(request);
        Assertions.assertEquals(request.getName().getDisplayName(), workflowInfo.getProcessInfo().getName());
        Assertions.assertEquals(request.getApplicant(), workflowInfo.getProcessInfo().getApplicant());
    }

    @Test
    void testListProcess() {
        List<ProcessResponse> responses = Lists.newArrayList(
                ProcessResponse.builder()
                        .id(1)
                        .name("test_process")
                        .build());

        stubFor(
                get(urlMatching("/inlong/manager/api/workflow/listProcess.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(new PageResult<>(responses))))));

        ProcessRequest request = new ProcessRequest();
        request.setId(1);
        PageResult<ProcessResponse> pageInfo = workflowClient.listProcess(request);
        Assertions.assertEquals(JsonUtils.toJsonString(pageInfo.getList()), JsonUtils.toJsonString(responses));
    }
}
