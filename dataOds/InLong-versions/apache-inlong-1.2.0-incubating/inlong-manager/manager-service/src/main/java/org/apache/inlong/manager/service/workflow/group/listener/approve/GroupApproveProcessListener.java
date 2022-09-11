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

package org.apache.inlong.manager.service.workflow.group.listener.approve;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupMode;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.LightGroupResourceProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.NewGroupProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.workflow.ProcessName;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * After the new inlong group is approved, initiate a listener for other processes
 */
@Slf4j
@Component
public class GroupApproveProcessListener implements ProcessEventListener {

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private InlongStreamService streamService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.COMPLETE;
    }

    /**
     * Initiate the process of creating inlong group resources after new inlong group access approved
     */
    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        NewGroupProcessForm form = (NewGroupProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupMode mode = GroupMode.parseGroupMode(groupInfo);
        switch (mode) {
            case NORMAL:
                createGroupResource(context, groupInfo);
                break;
            case LIGHT:
                createLightGroupResource(context, groupInfo);
                break;
            default:
                throw new WorkflowListenerException(ErrorCodeEnum.GROUP_MODE_UNSUPPORTED.getMessage());
        }

        return ListenerResult.success();
    }

    private void createGroupResource(WorkflowContext context, InlongGroupInfo groupInfo) {
        GroupResourceProcessForm processForm = new GroupResourceProcessForm();
        processForm.setGroupInfo(groupInfo);
        String username = context.getOperator();
        String groupId = groupInfo.getInlongGroupId();
        List<InlongStreamInfo> streamList = streamService.list(groupId);
        processForm.setStreamInfos(streamList);
        workflowService.start(ProcessName.CREATE_GROUP_RESOURCE, username, processForm);
    }

    private void createLightGroupResource(WorkflowContext context, InlongGroupInfo groupInfo) {
        LightGroupResourceProcessForm processForm = new LightGroupResourceProcessForm();
        processForm.setGroupInfo(groupInfo);
        String username = context.getOperator();
        String groupId = groupInfo.getInlongGroupId();
        List<InlongStreamInfo> streamList = streamService.list(groupId);
        processForm.setStreamInfos(streamList);
        workflowService.start(ProcessName.CREATE_LIGHT_GROUP_PROCESS, username, processForm);
    }

    @Override
    public boolean async() {
        return true;
    }

}
