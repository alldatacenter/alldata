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
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupApproveRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamApproveRequest;
import org.apache.inlong.manager.common.pojo.workflow.form.InlongGroupApproveForm;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Approve pass listener for new inlong group
 */
@Slf4j
@Component
public class GroupAfterApprovedListener implements TaskEventListener {

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongStreamService streamService;

    @Override
    public TaskEvent event() {
        return TaskEvent.APPROVE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        // Save the data format selected at the time of approval and the cluster information of the inlong stream
        InlongGroupApproveForm form = (InlongGroupApproveForm) context.getActionContext().getForm();

        InlongGroupApproveRequest approveInfo = form.getGroupApproveInfo();
        // Only the [Wait approval] status allowed the passing operation
        String groupId = approveInfo.getInlongGroupId();
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity == null) {
            throw new WorkflowListenerException("inlong group not found with group id=" + groupId);
        }
        if (!Objects.equals(GroupStatus.TO_BE_APPROVAL.getCode(), entity.getStatus())) {
            throw new WorkflowListenerException("inlong group status is [wait_approval], not allowed to approve again");
        }

        // Save the inlong group information after approval
        groupService.updateAfterApprove(approveInfo, context.getOperator());

        // Save inlong stream information after approval
        List<InlongStreamApproveRequest> streamApproveInfoList = form.getStreamApproveInfoList();
        streamService.updateAfterApprove(streamApproveInfoList, context.getOperator());
        return ListenerResult.success();
    }

    @Override
    public boolean async() {
        return false;
    }

}
