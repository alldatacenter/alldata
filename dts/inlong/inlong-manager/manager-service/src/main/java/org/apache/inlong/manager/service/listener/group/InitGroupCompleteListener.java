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
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupUtils;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The listener of InlongGroup when created resources successfully.
 */
@Slf4j
@Component
public class InitGroupCompleteListener implements ProcessEventListener {

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private StreamSourceService sourceService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.COMPLETE;
    }

    /**
     * After the process of creating InlongGroup resources is completed,
     * modify the status of related InlongGroup to [Successful]
     * <p/>
     * {@link InitGroupFailedListener#listen}
     */
    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        log.info("begin to execute InitGroupCompleteListener for groupId={}", groupId);

        try {
            // update inlong group status and other info
            InlongGroupInfo groupInfo = form.getGroupInfo();
            String operator = context.getOperator();
            Integer nextStatus = InlongGroupUtils.isBatchTask(form.getGroupInfo())
                    ? GroupStatus.FINISH.getCode()
                    : GroupStatus.CONFIG_SUCCESSFUL.getCode();
            groupService.updateStatus(groupId, nextStatus, operator);

            InlongGroupEntity existGroup = groupMapper.selectByGroupId(groupId);
            InlongGroupRequest updateGroupRequest = groupInfo.genRequest();
            updateGroupRequest.setVersion(existGroup.getVersion());
            groupService.update(updateGroupRequest, operator);
            streamService.updateStatus(groupId, null, StreamStatus.CONFIG_SUCCESSFUL.getCode(), operator);

            // update status of other related configs
            if (InlongConstants.DISABLE_CREATE_RESOURCE.equals(groupInfo.getEnableCreateResource())) {
                if (InlongConstants.LIGHTWEIGHT_MODE.equals(groupInfo.getLightweight())) {
                    sourceService.updateStatus(groupId, null, SourceStatus.SOURCE_NORMAL.getCode(), operator);
                } else {
                    sourceService.updateStatus(groupId, null, SourceStatus.TO_BE_ISSUED_ADD.getCode(), operator);
                }
            }

            log.info("success to execute InitGroupCompleteListener for groupId={}", groupId);
            return ListenerResult.success();
        } catch (Exception e) {
            throw new WorkflowListenerException(e);
        }
    }

}
