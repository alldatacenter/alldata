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

package org.apache.inlong.manager.service.listener.stream;

import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.StreamResourceProcessForm;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The listener for the update InlongStream.
 */
@Service
public class UpdateStreamListener implements ProcessEventListener {

    @Autowired
    private InlongStreamService streamService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.CREATE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        StreamResourceProcessForm form = (StreamResourceProcessForm) context.getProcessForm();
        InlongStreamInfo streamInfo = form.getStreamInfo();
        final String operator = context.getOperator();
        final GroupOperateType operateType = form.getGroupOperateType();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        switch (operateType) {
            case SUSPEND:
                streamService.updateStatus(groupId, streamId, StreamStatus.SUSPENDING.getCode(), operator);
                break;
            case RESTART:
                streamService.updateStatus(groupId, streamId, StreamStatus.RESTARTING.getCode(), operator);
                break;
            case DELETE:
                streamService.updateStatus(groupId, streamId, StreamStatus.DELETING.getCode(), operator);
                break;
            default:
                break;
        }
        return ListenerResult.success();
    }

}
