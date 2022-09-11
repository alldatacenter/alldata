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

package org.apache.inlong.manager.service.sort.light;

import com.google.common.collect.Lists;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.transform.TransformResponse;
import org.apache.inlong.manager.common.pojo.workflow.form.LightGroupResourceProcessForm;
import org.apache.inlong.manager.common.settings.InlongGroupSettings;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.service.sort.util.ExtractNodeUtils;
import org.apache.inlong.manager.service.sort.util.LoadNodeUtils;
import org.apache.inlong.manager.service.sort.util.NodeRelationUtils;
import org.apache.inlong.manager.service.sort.util.TransformNodeUtils;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.service.transform.StreamTransformService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Event listener of creat light group sort config.
 */
@Component
public class LightGroupSortListener implements SortOperateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LightGroupSortListener.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private StreamSourceService sourceService;
    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private StreamTransformService transformService;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws WorkflowListenerException {
        LOGGER.info("create sort config for light group, context={}", context);
        try {
            LightGroupResourceProcessForm processForm = (LightGroupResourceProcessForm) context.getProcessForm();
            InlongGroupInfo groupInfo = processForm.getGroupInfo();
            List<InlongStreamInfo> streamInfos = processForm.getStreamInfos();
            final String groupId = groupInfo.getInlongGroupId();
            GroupInfo configInfo = this.createGroupInfo(groupInfo, streamInfos);
            String dataFlows = OBJECT_MAPPER.writeValueAsString(configInfo);
            InlongGroupExtInfo extInfo = new InlongGroupExtInfo();
            extInfo.setInlongGroupId(groupId);
            extInfo.setKeyName(InlongGroupSettings.DATA_FLOW);
            extInfo.setKeyValue(dataFlows);
            if (groupInfo.getExtList() == null) {
                groupInfo.setExtList(Lists.newArrayList());
            }
            upsertDataFlow(groupInfo, extInfo);
            return ListenerResult.success();
        } catch (Throwable t) {
            LOGGER.error("create sort config error: ", t);
            throw new WorkflowListenerException("create sort config error: " + t.getMessage());
        }
    }

    private GroupInfo createGroupInfo(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfoList) {
        final String groupId = groupInfo.getInlongGroupId();
        List<StreamSource> sourceInfos = sourceService.listSource(groupId, null);
        Map<String, List<StreamSource>> sourceMap = sourceInfos.stream()
                .collect(Collectors.groupingBy(StreamSource::getInlongStreamId, HashMap::new,
                        Collectors.toCollection(ArrayList::new)));

        List<StreamSink> streamSinks = sinkService.listSink(groupId, null);
        Map<String, List<StreamSink>> sinkMap = streamSinks.stream()
                .collect(Collectors.groupingBy(StreamSink::getInlongStreamId, HashMap::new,
                        Collectors.toCollection(ArrayList::new)));

        List<TransformResponse> transformResponses = transformService.listTransform(groupId, null);
        Map<String, List<TransformResponse>> transformMap = transformResponses.stream()
                .collect(Collectors.groupingBy(TransformResponse::getInlongStreamId, HashMap::new,
                        Collectors.toCollection(ArrayList::new)));

        List<StreamInfo> streamInfos = new ArrayList<>();
        for (InlongStreamInfo stream : streamInfoList) {
            String streamId = stream.getInlongStreamId();
            List<Node> nodes = this.createNodesForStream(sourceMap.get(streamId),
                    transformMap.get(streamId), sinkMap.get(streamId));
            StreamInfo streamInfo = new StreamInfo(streamId, nodes,
                    NodeRelationUtils.createNodeRelationsForStream(stream));
            streamInfos.add(streamInfo);

            // Rebuild joinerNode relation
            List<TransformResponse> transformResponseList = transformMap.get(streamId);
            NodeRelationUtils.optimizeNodeRelation(streamInfo, transformResponseList);
        }

        return new GroupInfo(groupInfo.getInlongGroupId(), streamInfos);
    }

    private List<Node> createNodesForStream(
            List<StreamSource> sourceInfos,
            List<TransformResponse> transformResponses,
            List<StreamSink> streamSinks) {
        List<Node> nodes = Lists.newArrayList();
        nodes.addAll(ExtractNodeUtils.createExtractNodes(sourceInfos));
        nodes.addAll(TransformNodeUtils.createTransformNodes(transformResponses));
        nodes.addAll(LoadNodeUtils.createLoadNodes(streamSinks));
        return nodes;
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
