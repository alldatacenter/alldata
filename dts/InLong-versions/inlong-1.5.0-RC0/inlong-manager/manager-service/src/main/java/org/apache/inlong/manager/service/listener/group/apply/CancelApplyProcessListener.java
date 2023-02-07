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
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * The listener of InlongGroup when cancels the application.
 */
@Slf4j
@Component
public class CancelApplyProcessListener implements ProcessEventListener {

    @Autowired
    private InlongGroupEntityMapper groupMapper;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.CANCEL;
    }

    /**
     * After canceling the approval, the status becomes [ToBeSubmit]
     */
    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        ApplyGroupProcessForm form = (ApplyGroupProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        log.info("begin to execute CancelApplyProcessListener for groupId={}", groupId);

        // only the [ToBeApproval] status allowed the canceling operation
        InlongGroupEntity entity = groupMapper.selectByGroupId(groupId);
        if (entity == null) {
            throw new WorkflowListenerException("InlongGroup not found with groupId=" + groupId);
        }
        if (!Objects.equals(GroupStatus.TO_BE_APPROVAL.getCode(), entity.getStatus())) {
            throw new WorkflowListenerException(String.format("Current status [%s] was not allowed to cancel",
                    GroupStatus.forCode(entity.getStatus())));
        }
        String operator = context.getOperator();
        groupMapper.updateStatus(groupId, GroupStatus.TO_BE_SUBMIT.getCode(), operator);

        log.info("success to execute CancelApplyProcessListener for groupId={}", groupId);
        return ListenerResult.success();
    }

}
