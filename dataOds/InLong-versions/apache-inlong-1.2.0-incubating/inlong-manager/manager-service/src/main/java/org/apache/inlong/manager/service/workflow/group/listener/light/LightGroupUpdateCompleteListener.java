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
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.LightGroupResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Listener of light group update complete.
 */
@Slf4j
@Component
public class LightGroupUpdateCompleteListener implements ProcessEventListener {

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

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        LightGroupResourceProcessForm form = (LightGroupResourceProcessForm) context.getProcessForm();
        final String groupId = form.getInlongGroupId();
        final String applicant = context.getOperator();
        GroupOperateType groupOperateType = form.getGroupOperateType();
        switch (groupOperateType) {
            case SUSPEND:
                groupService.updateStatus(groupId, GroupStatus.SUSPENDED.getCode(), applicant);
                sourceService.updateStatus(groupId, null, SourceStatus.SOURCE_FROZEN.getCode(), applicant);
                break;
            case RESTART:
                groupService.updateStatus(groupId, GroupStatus.RESTARTED.getCode(), applicant);
                sourceService.updateStatus(groupId, null, SourceStatus.SOURCE_NORMAL.getCode(), applicant);
                break;
            case DELETE:
                groupService.updateStatus(groupId, GroupStatus.DELETED.getCode(), applicant);
                sourceService.logicDeleteAll(groupId, null, applicant);
                break;
            default:
                break;
        }

        if (CollectionUtils.isEmpty(form.getStreamInfos())) {
            List<InlongStreamInfo> streamInfos = streamService.list(groupId);
            form.setStreamInfos(streamInfos);
        }
        return ListenerResult.success();
    }

    @Override
    public boolean async() {
        return false;
    }
}
