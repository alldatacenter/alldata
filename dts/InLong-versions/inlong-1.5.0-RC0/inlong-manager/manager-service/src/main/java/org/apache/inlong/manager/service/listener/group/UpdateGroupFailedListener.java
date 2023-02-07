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
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The listener of InlongGroup when update operates failed.
 */
@Slf4j
@Component
public class UpdateGroupFailedListener implements ProcessEventListener {

    @Autowired
    private InlongGroupService groupService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.FAIL;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        log.info("begin to execute UpdateGroupFailedListener for groupId={}", groupId);

        // update inlong group status and other info
        String operator = context.getOperator();
        // delete process failed, then change the group status to [CONFIG_FAILED]
        groupService.updateStatus(groupId, GroupStatus.CONFIG_FAILED.getCode(), operator);
        groupService.update(form.getGroupInfo().genRequest(), operator);

        log.info("success to execute UpdateGroupFailedListener for groupId={}", groupId);
        return ListenerResult.success();
    }

}
