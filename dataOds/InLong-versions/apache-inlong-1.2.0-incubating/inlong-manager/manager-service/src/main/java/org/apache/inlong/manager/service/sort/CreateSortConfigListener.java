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

package org.apache.inlong.manager.service.sort;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.common.settings.InlongGroupSettings;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.service.sort.util.DataFlowUtils;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Create sort config when disable the ZooKeeper
 */
@Deprecated
@Component
public class CreateSortConfigListener implements SortOperateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateSortConfigListener.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // thread safe

    @Autowired
    private StreamSinkService streamSinkService;
    @Autowired
    private DataFlowUtils dataFlowUtils;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        LOGGER.info("Create sort config for groupId={}", context.getProcessForm().getInlongGroupId());
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        GroupOperateType groupOperateType = form.getGroupOperateType();
        if (groupOperateType == GroupOperateType.SUSPEND || groupOperateType == GroupOperateType.DELETE) {
            return ListenerResult.success();
        }
        InlongGroupInfo groupInfo = form.getGroupInfo();
        String groupId = groupInfo.getInlongGroupId();
        if (StringUtils.isEmpty(groupId)) {
            LOGGER.warn("GroupId is null for context={}", context);
            return ListenerResult.success();
        }

        List<StreamSink> streamSinks = streamSinkService.listSink(groupId, null);
        if (CollectionUtils.isEmpty(streamSinks)) {
            LOGGER.warn("Sink not found by groupId={}", groupId);
            return ListenerResult.success();
        }

        try {
            // TODO Support more than one sinks under a stream
            // Map<String, DataFlowInfo> dataFlowInfoMap = streamSinks.stream().map(sink -> {
            //             DataFlowInfo flowInfo = dataFlowUtils.createDataFlow(groupInfo, sink);
            //             return Pair.of(sink.getInlongStreamId(), flowInfo);
            //         }
            // ).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

            // String dataFlows = OBJECT_MAPPER.writeValueAsString(dataFlowInfoMap);
            InlongGroupExtInfo extInfo = new InlongGroupExtInfo();
            extInfo.setInlongGroupId(groupId);
            extInfo.setKeyName(InlongGroupSettings.DATA_FLOW);
            // extInfo.setKeyValue(dataFlows);
            if (groupInfo.getExtList() == null) {
                groupInfo.setExtList(Lists.newArrayList());
            }
            upsertDataFlow(groupInfo, extInfo);
        } catch (Exception e) {
            LOGGER.error("create sort config failed for sink list={} ", streamSinks, e);
            throw new WorkflowListenerException("create sort config failed: " + e.getMessage());
        }
        return ListenerResult.success();
    }

    private void upsertDataFlow(InlongGroupInfo groupInfo, InlongGroupExtInfo extInfo) {
        groupInfo.getExtList().removeIf(ext -> InlongGroupSettings.DATA_FLOW.equals(ext.getKeyName()));
        groupInfo.getExtList().add(extInfo);
    }

    @Override
    public boolean async() {
        return false;
    }
}
