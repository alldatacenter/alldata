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

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Create group resources [process completion] event listener
 */
@Slf4j
@Component
public class GroupCompleteProcessListener implements ProcessEventListener {

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private StreamSourceService sourceService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.COMPLETE;
    }

    /**
     * After the process of creating inlong group resources is completed, modify the status of related
     * inlong group and all inlong stream to [Configuration Successful] [Configuration Failed]
     * <p/>{@link GroupFailedProcessListener#listen}
     */
    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        String applicant = context.getOperator();
        // Update inlong group status and other info
        groupService.updateStatus(groupId, GroupStatus.CONFIG_SUCCESSFUL.getCode(), applicant);
        groupService.update(form.getGroupInfo().genRequest(), applicant);

        // Update status of other related configs
        streamService.updateStatus(groupId, null, StreamStatus.CONFIG_SUCCESSFUL.getCode(), applicant);
        sourceService.updateStatus(groupId, null, SourceStatus.TO_BE_ISSUED_ADD.getCode(), applicant);

        return ListenerResult.success();
    }

    @Override
    public boolean async() {
        return false;
    }

}
