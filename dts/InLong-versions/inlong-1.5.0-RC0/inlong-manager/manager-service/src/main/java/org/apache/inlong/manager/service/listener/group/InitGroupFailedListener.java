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
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The listener of InlongGroup when created resources failed.
 */
@Slf4j
@Component
public class InitGroupFailedListener implements ProcessEventListener {

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongStreamService streamService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.FAIL;
    }

    /**
     * After the process of creating InlongGroup resources is completed,
     * modify the status of related InlongGroup and all InlongStream to [Failed]
     * <p/>
     * {@link InitGroupCompleteListener#listen}
     */
    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        log.info("begin to execute InitGroupFailedListener for groupId={}", groupId);

        // update inlong group status
        String operator = context.getOperator();
        groupService.updateStatus(groupId, GroupStatus.CONFIG_FAILED.getCode(), operator);

        // update inlong stream status
        streamService.updateStatus(groupId, null, StreamStatus.CONFIG_FAILED.getCode(), operator);

        log.info("success to execute InitGroupFailedListener for groupId={}", groupId);
        return ListenerResult.success();
    }

}
