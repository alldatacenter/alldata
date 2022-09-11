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

package org.apache.inlong.manager.service.resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.pojo.sink.SinkInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.SinkOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Event listener of operate sink resources, such as create or update Hive table, Kafka topics, ES indices, etc.
 */
@Slf4j
@Service
public class SinkResourceListener implements SinkOperateListener {

    @Autowired
    private StreamSinkEntityMapper sinkMapper;
    @Autowired
    private SinkResourceOperatorFactory resourceOperatorFactory;

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
                .filter(sinkInfo -> GlobalConstants.ENABLE_CREATE_RESOURCE.equals(sinkInfo.getEnableCreateResource()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(needCreateList)) {
            String result = "sink resources have been created for group [" + groupId + "] and stream " + streamIdList;
            log.info(result);
            return ListenerResult.success(result);
        }

        for (SinkInfo sinkConfig : needCreateList) {
            String sinkType = sinkConfig.getSinkType();
            SinkResourceOperator resourceOperator = resourceOperatorFactory.getInstance(SinkType.forType(sinkType));
            resourceOperator.createSinkResource(sinkConfig);
        }
        String result = "success to create sink resources for group [" + groupId + "] and stream " + streamIdList;
        log.info(result);
        return ListenerResult.success(result);
    }

    @Override
    public boolean async() {
        return false;
    }

}
