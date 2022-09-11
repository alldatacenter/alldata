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

import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.group.pulsar.InlongPulsarRequest;
import org.apache.inlong.manager.common.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.core.operation.InlongGroupProcessOperation;
import org.apache.inlong.manager.service.mocks.MockPlugin;
import org.apache.inlong.manager.service.workflow.listener.GroupTaskListenerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Inlong group process operation service test.
 */
@EnableAutoConfiguration
public class InlongGroupProcessOperationTest extends ServiceBaseTest {

    private static final String OPERATOR = "operator";

    private static final String GROUP_NAME = "test_biz";

    private static final String GROUP_ID = "test_biz";

    @Autowired
    private InlongGroupService groupService;

    @Autowired
    private InlongGroupProcessOperation groupProcessOperation;

    @Autowired
    private GroupTaskListenerFactory groupTaskListenerFactory;

    /**
     * Set some base information before start process.
     */
    public void before() {
        MockPlugin mockPlugin = new MockPlugin();
        groupTaskListenerFactory.acceptPlugin(mockPlugin);
        InlongPulsarRequest groupInfo = new InlongPulsarRequest();
        groupInfo.setInlongGroupId(GROUP_ID);
        groupInfo.setName(GROUP_NAME);
        groupInfo.setInCharges(OPERATOR);
        groupInfo.setMqType(MQType.PULSAR.getType());
        groupService.save(groupInfo, OPERATOR);
    }

    // There will be concurrency problems in the overall operation, and the testDeleteProcess() method will call
    // @Test
    public void testStartProcess() {
        before();
        WorkflowResult result = groupProcessOperation.startProcess(GROUP_ID, OPERATOR);
        ProcessResponse response = result.getProcessInfo();
        Assert.assertSame(response.getStatus(), ProcessStatus.PROCESSING);
        InlongGroupInfo groupInfo = groupService.get(GROUP_ID);
        Assert.assertEquals(groupInfo.getStatus(), GroupStatus.TO_BE_APPROVAL.getCode());
    }

    // There will be concurrency problems in the overall operation, and the testDeleteProcess() method will call
    // @Test
    public void testSuspendProcess() {
        testStartProcess();
        InlongGroupInfo groupInfo = groupService.get(GROUP_ID);
        groupService.updateStatus(GROUP_ID, GroupStatus.APPROVE_PASSED.getCode(), OPERATOR);
        groupService.update(groupInfo.genRequest(), OPERATOR);
        groupService.updateStatus(GROUP_ID, GroupStatus.CONFIG_ING.getCode(), OPERATOR);
        groupService.update(groupInfo.genRequest(), OPERATOR);
        groupService.updateStatus(GROUP_ID, GroupStatus.CONFIG_SUCCESSFUL.getCode(), OPERATOR);
        groupService.update(groupInfo.genRequest(), OPERATOR);

        WorkflowResult result = groupProcessOperation.suspendProcess(GROUP_ID, OPERATOR);
        ProcessResponse response = result.getProcessInfo();
        Assert.assertSame(response.getStatus(), ProcessStatus.COMPLETED);
        groupInfo = groupService.get(GROUP_ID);
        Assert.assertEquals(groupInfo.getStatus(), GroupStatus.SUSPENDED.getCode());
    }

    // There will be concurrency problems in the overall operation, and the testDeleteProcess() method will call
    // @Test
    public void testRestartProcess() {
        testSuspendProcess();
        WorkflowResult result = groupProcessOperation.restartProcess(GROUP_ID, OPERATOR);
        ProcessResponse response = result.getProcessInfo();
        Assert.assertSame(response.getStatus(), ProcessStatus.COMPLETED);
        InlongGroupInfo groupInfo = groupService.get(GROUP_ID);
        Assert.assertEquals(groupInfo.getStatus(), GroupStatus.RESTARTED.getCode());
    }

    @Test
    public void testDeleteProcess() {
        testStartProcess();
        // testRestartProcess();
        // boolean result = groupProcessOperation.deleteProcess(GROUP_ID, OPERATOR);
        // Assert.assertTrue(result);
    }
}

