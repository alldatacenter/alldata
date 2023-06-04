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

package org.apache.inlong.manager.service.listener.group.apply;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The listener that approves to apply for InlongGroup.
 * <p/>
 * After approval, start other follow-up processes.
 */
@Slf4j
@Component
public class ApproveApplyProcessListener implements ProcessEventListener {

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private WorkflowService workflowService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        ApplyGroupProcessForm form = (ApplyGroupProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        log.info("begin to execute ApproveApplyProcessListener for groupId={}", groupId);

        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupResourceProcessForm processForm = new GroupResourceProcessForm();
        processForm.setGroupInfo(groupInfo);
        String username = context.getOperator();
        List<InlongStreamInfo> streamList = streamService.list(groupId);
        processForm.setStreamInfos(streamList);

        // may run for long time, make it async processing
        EXECUTOR_SERVICE.execute(() -> workflowService.start(ProcessName.CREATE_GROUP_RESOURCE, username, processForm));
        log.info("success to execute ApproveApplyProcessListener for groupId={}", groupId);
        return ListenerResult.success();
    }

}
