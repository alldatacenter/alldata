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

package org.apache.inlong.manager.service.group;

import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarRequest;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.listener.GroupTaskListenerFactory;
import org.apache.inlong.manager.service.mocks.MockPlugin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Inlong group process operation service test.
 */
public class InlongGroupProcessServiceTest extends ServiceBaseTest {

    private static final String GROUP_ID = "test_group_process";
    private static final String OPERATOR = "operator";

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongGroupProcessService groupProcessOperation;
    @Autowired
    private GroupTaskListenerFactory groupTaskListenerFactory;

    @Test
    public void testAllProcess() {
        before();
        testStartProcess();
        /*
         * TODO: https://github.com/apache/inlong/issues/6631 In order to avoid affecting the integration of other
         * features, temporarily comment out the execution content of this function, and add this test case after the
         * problem is identified
         */
        // testSuspendProcess();
        // testRestartProcess();

        // delete the process, will delete the Pulsar resource
        // TODO Mock the cluster related operate
        // boolean result = groupProcessOperation.deleteProcess(GROUP_ID, OPERATOR);
        // Assertions.assertTrue(result);
    }

    private void before() {
        MockPlugin mockPlugin = new MockPlugin();
        groupTaskListenerFactory.acceptPlugin(mockPlugin);
        InlongPulsarRequest groupInfo = new InlongPulsarRequest();
        groupInfo.setInlongGroupId(GROUP_ID);
        groupInfo.setInCharges(OPERATOR);
        groupInfo.setMqType(MQType.PULSAR);
        groupService.save(groupInfo, OPERATOR);
    }

    private void testStartProcess() {
        InlongGroupInfo groupInfo = groupService.get(GROUP_ID);
        groupInfo.setInlongClusterTag("default_cluster");
        groupService.update(groupInfo.genRequest(), OPERATOR);

        WorkflowResult result = groupProcessOperation.startProcess(GROUP_ID, OPERATOR);
        ProcessResponse response = result.getProcessInfo();
        Assertions.assertSame(response.getStatus(), ProcessStatus.PROCESSING);
        groupInfo = groupService.get(GROUP_ID);
        Assertions.assertEquals(groupInfo.getStatus(), GroupStatus.TO_BE_APPROVAL.getCode());
    }

    private void testSuspendProcess() {
        groupService.updateStatus(GROUP_ID, GroupStatus.APPROVE_PASSED.getCode(), OPERATOR);
        groupService.updateStatus(GROUP_ID, GroupStatus.CONFIG_ING.getCode(), OPERATOR);
        groupService.updateStatus(GROUP_ID, GroupStatus.CONFIG_SUCCESSFUL.getCode(), OPERATOR);

        WorkflowResult result = groupProcessOperation.suspendProcess(GROUP_ID, OPERATOR);
        ProcessResponse response = result.getProcessInfo();
        Assertions.assertSame(response.getStatus(), ProcessStatus.COMPLETED);
        InlongGroupInfo groupInfo = groupService.get(GROUP_ID);
        Assertions.assertEquals(groupInfo.getStatus(), GroupStatus.SUSPENDED.getCode());
    }

    private void testRestartProcess() {
        WorkflowResult result = groupProcessOperation.restartProcess(GROUP_ID, OPERATOR);
        ProcessResponse response = result.getProcessInfo();
        Assertions.assertSame(response.getStatus(), ProcessStatus.COMPLETED);
        InlongGroupInfo groupInfo = groupService.get(GROUP_ID);
        Assertions.assertEquals(groupInfo.getStatus(), GroupStatus.RESTARTED.getCode());
    }

}
