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

package org.apache.inlong.manager.service.workflow.group.listener;

import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Update listener for inlong group
 */
@Service
public class GroupUpdateListener implements ProcessEventListener {

    @Autowired
    private InlongGroupService groupService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.CREATE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        InlongGroupInfo groupInfo = groupService.get(context.getProcessForm().getInlongGroupId());
        GroupOperateType groupOperateType = form.getGroupOperateType();
        String username = context.getOperator();
        if (groupInfo != null) {
            switch (groupOperateType) {
                case SUSPEND:
                    groupService.updateStatus(groupInfo.getInlongGroupId(), GroupStatus.SUSPENDING.getCode(), username);
                    break;
                case RESTART:
                    groupService.updateStatus(groupInfo.getInlongGroupId(), GroupStatus.RESTARTING.getCode(), username);
                    break;
                case DELETE:
                    groupService.updateStatus(groupInfo.getInlongGroupId(), GroupStatus.DELETING.getCode(), username);
                    break;
                default:
                    break;
            }
            form.setGroupInfo(groupInfo);
        } else {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        return ListenerResult.success();
    }

    @Override
    public boolean async() {
        return false;
    }

}
