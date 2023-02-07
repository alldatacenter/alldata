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

package org.apache.inlong.manager.service.listener.queue;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.StreamResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.resource.queue.QueueResourceOperator;
import org.apache.inlong.manager.service.resource.queue.QueueResourceOperatorFactory;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.QueueOperateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Create task listener for Pulsar Topic
 */
@Slf4j
@Component
public class StreamQueueResourceListener implements QueueOperateListener {

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private QueueResourceOperatorFactory queueOperatorFactory;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public boolean accept(WorkflowContext context) {
        ProcessForm processForm = context.getProcessForm();
        if (!(processForm instanceof StreamResourceProcessForm)) {
            return false;
        }
        StreamResourceProcessForm streamProcessForm = (StreamResourceProcessForm) processForm;
        return InlongConstants.STANDARD_MODE.equals(streamProcessForm.getGroupInfo().getLightweight());
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        StreamResourceProcessForm streamProcessForm = (StreamResourceProcessForm) context.getProcessForm();
        final String groupId = streamProcessForm.getInlongGroupId();
        InlongStreamInfo streamInfo = streamProcessForm.getStreamInfo();
        if (streamInfo == null) {
            String msg = "inlong stream cannot be null in StreamResourceProcessForm with groupId=" + groupId;
            log.error(msg);
            throw new WorkflowListenerException(msg);
        }

        // ensure the inlong group exists
        InlongGroupInfo groupInfo = groupService.get(groupId);
        if (groupInfo == null) {
            String msg = "inlong group not found with groupId=" + groupId;
            log.error(msg);
            throw new WorkflowListenerException(msg);
        }

        if (InlongConstants.DISABLE_CREATE_RESOURCE.equals(groupInfo.getEnableCreateResource())) {
            log.warn("skip to execute StreamQueueResourceListener as disable create resource for groupId={}", groupId);
            return ListenerResult.success("skip - disable create resource");
        }

        QueueResourceOperator queueOperator = queueOperatorFactory.getInstance(groupInfo.getMqType());
        GroupOperateType operateType = streamProcessForm.getGroupOperateType();
        String operator = context.getOperator();
        switch (operateType) {
            case INIT:
                queueOperator.createQueueForStream(groupInfo, streamInfo, operator);
                break;
            case DELETE:
                queueOperator.deleteQueueForStream(groupInfo, streamInfo, operator);
                break;
            default:
                log.warn("unsupported operate={} for inlong group", operateType);
                break;
        }

        log.info("success to execute StreamQueueResourceListener for groupId={}, operateType={}", groupId, operateType);
        return ListenerResult.success("success");
    }

}
