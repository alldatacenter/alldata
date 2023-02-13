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

package org.apache.inlong.manager.service.listener.sink;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.StreamResourceProcessForm;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperator;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperatorFactory;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.SinkOperateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Event listener of operate sink resources for one inlong stream,
 * such as create or update Hive table, Kafka topics, ES indices, etc.
 */
@Service
@Slf4j
public class StreamSinkResourceListener implements SinkOperateListener {

    @Autowired
    private StreamSinkEntityMapper sinkEntityMapper;
    @Autowired
    private SinkResourceOperatorFactory resourceOperatorFactory;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public boolean accept(WorkflowContext context) {
        ProcessForm processForm = context.getProcessForm();
        return processForm instanceof StreamResourceProcessForm;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) {
        StreamResourceProcessForm form = (StreamResourceProcessForm) context.getProcessForm();
        InlongStreamInfo streamInfo = form.getStreamInfo();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        log.info("begin to create sink resource for groupId={}, streamId={}", groupId, streamId);

        List<SinkInfo> sinkInfos = sinkEntityMapper.selectAllConfig(groupId, Lists.newArrayList(streamId));
        List<SinkInfo> needCreateResources = sinkInfos.stream()
                .filter(sinkInfo -> InlongConstants.ENABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(needCreateResources)) {
            log.info("sink resources have been created for group [" + groupId + "] and stream [" + streamId + "]");
            return ListenerResult.success();
        }

        for (SinkInfo sinkInfo : needCreateResources) {
            SinkResourceOperator resourceOperator = resourceOperatorFactory.getInstance(sinkInfo.getSinkType());
            resourceOperator.createSinkResource(sinkInfo);
        }
        log.info("success to create sink resources for group [" + groupId + "] and stream [" + streamId + "]");
        return ListenerResult.success();
    }

}
