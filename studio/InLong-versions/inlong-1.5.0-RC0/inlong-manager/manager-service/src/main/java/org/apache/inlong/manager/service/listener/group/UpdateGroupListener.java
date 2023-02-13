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

package org.apache.inlong.manager.service.listener.group;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The listener for the update InlongGroup.
 */
@Slf4j
@Component
public class UpdateGroupListener implements ProcessEventListener {

    @Autowired
    private InlongGroupService groupService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.CREATE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        log.info("begin to execute UpdateGroupListener for groupId={}", groupId);

        InlongGroupInfo groupInfo = form.getGroupInfo();
        if (groupInfo == null) {
            throw new BusinessException("InlongGroupInfo cannot be null for update group process");
        }

        GroupOperateType operateType = form.getGroupOperateType();
        String operator = context.getOperator();
        switch (operateType) {
            case SUSPEND:
                groupService.updateStatus(groupId, GroupStatus.SUSPENDING.getCode(), operator);
                break;
            case RESTART:
                groupService.updateStatus(groupId, GroupStatus.RESTARTING.getCode(), operator);
                break;
            case DELETE:
                groupService.updateStatus(groupId, GroupStatus.DELETING.getCode(), operator);
                break;
            default:
                break;
        }

        log.info("success to execute UpdateGroupListener for groupId={}, operateType={}", groupId, operateType);
        return ListenerResult.success();
    }

}
