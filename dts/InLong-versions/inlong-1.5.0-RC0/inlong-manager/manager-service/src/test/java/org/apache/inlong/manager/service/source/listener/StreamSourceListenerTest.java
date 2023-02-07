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

package org.apache.inlong.manager.service.source.listener;

import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.mysql.MySQLBinlogSourceRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.core.ProcessService;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.apache.inlong.manager.workflow.util.WorkflowUtils;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class for operate StreamSource, such as frozen or restart.
 */
public class StreamSourceListenerTest extends ServiceBaseTest {

    private static final String GROUP_ID = "test-source-group-id";
    private static final String STREAM_ID = "test-source-stream-id";

    @Autowired
    private ProcessService processService;
    @Autowired
    private StreamSourceService sourceService;

    private InlongGroupInfo groupInfo;

    /**
     * There will be concurrency problems in the overall operation,This method temporarily fails the test
     */
    // @Test
    public void testAllOperate() {
        groupInfo = createInlongGroup(GROUP_ID, MQType.PULSAR);
        groupService.updateStatus(GROUP_ID, GroupStatus.CONFIG_ING.getCode(), GLOBAL_OPERATOR);
        groupService.updateStatus(GROUP_ID, GroupStatus.CONFIG_SUCCESSFUL.getCode(), GLOBAL_OPERATOR);
        groupService.update(groupInfo.genRequest(), GLOBAL_OPERATOR);

        Integer sourceId = this.createBinlogSource(groupInfo);
        testFrozenSource(sourceId);
        testRestartSource(sourceId);
    }

    private Integer createBinlogSource(InlongGroupInfo groupInfo) {
        InlongStreamInfo stream = createStreamInfo(groupInfo, STREAM_ID);
        MySQLBinlogSourceRequest sourceRequest = new MySQLBinlogSourceRequest();
        sourceRequest.setInlongGroupId(stream.getInlongGroupId());
        sourceRequest.setInlongStreamId(stream.getInlongStreamId());
        sourceRequest.setSourceName("binlog-collect");
        return sourceService.save(sourceRequest, GLOBAL_OPERATOR);
    }

    private void testFrozenSource(Integer sourceId) {
        sourceService.updateStatus(GROUP_ID, null, SourceStatus.SOURCE_NORMAL.getCode(), GLOBAL_OPERATOR);

        GroupResourceProcessForm form = new GroupResourceProcessForm();
        form.setGroupInfo(groupInfo);
        form.setGroupOperateType(GroupOperateType.SUSPEND);
        WorkflowContext context = processService.start(ProcessName.SUSPEND_GROUP_PROCESS.name(), GLOBAL_OPERATOR, form);
        WorkflowResult result = WorkflowUtils.getResult(context);
        ProcessResponse response = result.getProcessInfo();
        Assertions.assertSame(response.getStatus(), ProcessStatus.COMPLETED);

        WorkflowProcess process = context.getProcess();
        WorkflowTask task = process.getTaskByName("StopSource");
        Assertions.assertTrue(task instanceof ServiceTask);

        StreamSource streamSource = sourceService.get(sourceId);
        Assertions.assertSame(SourceStatus.forCode(streamSource.getStatus()), SourceStatus.TO_BE_ISSUED_FROZEN);
    }

    private void testRestartSource(Integer sourceId) {
        groupService.updateStatus(GROUP_ID, GroupStatus.SUSPENDING.getCode(), GLOBAL_OPERATOR);
        groupService.update(groupInfo.genRequest(), GLOBAL_OPERATOR);
        groupService.updateStatus(GROUP_ID, GroupStatus.SUSPENDED.getCode(), GLOBAL_OPERATOR);
        groupService.update(groupInfo.genRequest(), GLOBAL_OPERATOR);

        sourceService.updateStatus(GROUP_ID, null, SourceStatus.SOURCE_NORMAL.getCode(), GLOBAL_OPERATOR);

        GroupResourceProcessForm form = new GroupResourceProcessForm();
        form.setGroupInfo(groupInfo);
        form.setGroupOperateType(GroupOperateType.RESTART);
        WorkflowContext context = processService.start(ProcessName.RESTART_GROUP_PROCESS.name(), GLOBAL_OPERATOR, form);
        WorkflowResult result = WorkflowUtils.getResult(context);
        ProcessResponse response = result.getProcessInfo();
        Assertions.assertSame(response.getStatus(), ProcessStatus.COMPLETED);

        WorkflowProcess process = context.getProcess();
        WorkflowTask task = process.getTaskByName("RestartSource");
        Assertions.assertTrue(task instanceof ServiceTask);

        StreamSource streamSource = sourceService.get(sourceId);
        Assertions.assertSame(SourceStatus.forCode(streamSource.getStatus()), SourceStatus.TO_BE_ISSUED_RETRY);
    }

}
