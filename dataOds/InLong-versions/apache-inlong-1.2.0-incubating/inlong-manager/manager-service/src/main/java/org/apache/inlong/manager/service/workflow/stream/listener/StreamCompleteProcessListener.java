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

package org.apache.inlong.manager.service.workflow.stream.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.StreamResourceProcessForm;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.process.ProcessEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Event listener for completed creation of inlong stream resource
 */
@Slf4j
@Component
public class StreamCompleteProcessListener implements ProcessEventListener {

    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private StreamSourceService sourceService;

    @Override
    public ProcessEvent event() {
        return ProcessEvent.COMPLETE;
    }

    /**
     * The creation process ends normally, modify the status of inlong group and other related info.
     */
    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        StreamResourceProcessForm form = (StreamResourceProcessForm) context.getProcessForm();
        InlongStreamInfo streamInfo = form.getStreamInfo();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        final String operator = context.getOperator();

        // Update status of other related configs
        streamService.updateStatus(groupId, streamId, StreamStatus.CONFIG_SUCCESSFUL.getCode(), operator);
        streamService.update(streamInfo.genRequest(), operator);
        sourceService.updateStatus(groupId, streamId, SourceStatus.TO_BE_ISSUED_ADD.getCode(), operator);

        return ListenerResult.success();
    }

    @Override
    public boolean async() {
        return false;
    }

}
