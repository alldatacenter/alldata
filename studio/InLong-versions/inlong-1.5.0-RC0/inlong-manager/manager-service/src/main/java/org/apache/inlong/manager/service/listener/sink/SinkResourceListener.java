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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperator;
import org.apache.inlong.manager.service.resource.sink.SinkResourceOperatorFactory;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.SinkOperateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Event listener of operate sink resources,
 * such as create or update Hive table, Kafka topics, ES indices, etc.
 */
@Slf4j
@Service
public class SinkResourceListener implements SinkOperateListener {

    @Autowired
    private StreamSinkEntityMapper sinkMapper;
    @Autowired
    private SinkResourceOperatorFactory sinkOperatorFactory;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) {
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        String groupId = form.getInlongGroupId();
        log.info("begin to create sink resources for groupId={}", groupId);

        List<String> streamIdList = new ArrayList<>();
        List<InlongStreamInfo> streamList = form.getStreamInfos();
        if (CollectionUtils.isNotEmpty(streamList)) {
            streamIdList = streamList.stream().map(InlongStreamInfo::getInlongStreamId).collect(Collectors.toList());
        }
        List<SinkInfo> configList = sinkMapper.selectAllConfig(groupId, streamIdList);
        List<SinkInfo> needCreateList = configList.stream()
                .filter(sinkInfo -> InlongConstants.ENABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(needCreateList)) {
            log.info("all sink resources have been created for group [" + groupId + "] and stream " + streamIdList);
            return ListenerResult.success();
        }

        for (SinkInfo sinkInfo : needCreateList) {
            SinkResourceOperator resourceOperator = sinkOperatorFactory.getInstance(sinkInfo.getSinkType());
            resourceOperator.createSinkResource(sinkInfo);
        }
        log.info("success to create sink resources for group [" + groupId + "] and stream " + streamIdList);
        return ListenerResult.success();
    }

}
