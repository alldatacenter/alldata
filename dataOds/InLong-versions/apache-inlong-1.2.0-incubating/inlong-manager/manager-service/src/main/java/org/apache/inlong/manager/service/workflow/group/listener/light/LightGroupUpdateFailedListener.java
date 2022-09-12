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

package org.apache.inlong.manager.service.workflow.group.listener.light;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.LightGroupResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Listener of light group update failed.
 */
@Slf4j
@Component
public class LightGroupUpdateFailedListener implements ProcessEventListener {

    @Autowired
    private InlongGroupService groupService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.FAIL;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        LightGroupResourceProcessForm form = (LightGroupResourceProcessForm) context.getProcessForm();
        InlongGroupInfo groupInfo = form.getGroupInfo();
        final String groupId = groupInfo.getInlongGroupId();
        final String applicant = context.getOperator();
        // Update inlong group status
        groupService.updateStatus(groupId, GroupStatus.CONFIG_FAILED.getCode(), applicant);
        groupService.update(groupInfo.genRequest(), applicant);
        return ListenerResult.success();
    }

    @Override
    public boolean async() {
        return false;
    }
}
