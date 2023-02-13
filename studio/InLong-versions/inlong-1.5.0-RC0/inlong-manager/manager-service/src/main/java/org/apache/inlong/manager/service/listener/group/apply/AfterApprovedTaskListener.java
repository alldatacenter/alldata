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
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.pojo.group.InlongGroupApproveRequest;
import org.apache.inlong.manager.pojo.workflow.form.task.InlongGroupApproveForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The listener for modifying InlongGroup info after the application InlongGroup process is approved.
 */
@Slf4j
@Component
public class AfterApprovedTaskListener implements TaskEventListener {

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

    /**
     * After approval, persist the modified info during the approval in DB.
     */
    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        InlongGroupApproveForm form = (InlongGroupApproveForm) context.getActionContext().getForm();
        InlongGroupApproveRequest approveInfo = form.getGroupApproveInfo();
        String groupId = approveInfo.getInlongGroupId();
        log.info("begin to execute AfterApprovedTaskListener for groupId={}", groupId);

        // save the inlong group and other info after approval
        groupService.updateAfterApprove(approveInfo, context.getOperator());
        streamService.updateAfterApprove(form.getStreamApproveInfoList(), context.getOperator());

        log.info("success to execute AfterApprovedTaskListener for groupId={}", groupId);
        return ListenerResult.success();
    }

}
