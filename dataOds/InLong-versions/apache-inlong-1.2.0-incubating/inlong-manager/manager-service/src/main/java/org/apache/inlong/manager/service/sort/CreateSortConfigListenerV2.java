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
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.common.enums.DataTypeEnum;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.cluster.InlongClusterInfo;
import org.apache.inlong.manager.common.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.source.kafka.KafkaSource;
import org.apache.inlong.manager.common.pojo.source.pulsar.PulsarSource;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.common.settings.InlongGroupSettings;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.service.sort.util.ExtractNodeUtils;
import org.apache.inlong.manager.service.sort.util.LoadNodeUtils;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.SortOperateListener;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.enums.PulsarScanStartupMode;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CreateSortConfigListenerV2 implements SortOperateListener {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private StreamSourceService sourceService;
    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private InlongClusterService clusterService;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        log.info("Create sort config V2 for groupId={}", context.getProcessForm().getInlongGroupId());
        GroupResourceProcessForm form = (GroupResourceProcessForm) context.getProcessForm();
        GroupOperateType groupOperateType = form.getGroupOperateType();
        if (groupOperateType == GroupOperateType.SUSPEND || groupOperateType == GroupOperateType.DELETE) {
            return ListenerResult.success();
        }
        InlongGroupInfo groupInfo = form.getGroupInfo();
        List<InlongStreamInfo> streamInfos = form.getStreamInfos();
        final String groupId = groupInfo.getInlongGroupId();
        GroupInfo configInfo = createGroupInfo(groupInfo, streamInfos);
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
    }

    /**
     * TODO need support TubeMQ
     */
    private GroupInfo createGroupInfo(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfoList) {
        String groupId = groupInfo.getInlongGroupId();
        List<StreamSink> streamSinks = sinkService.listSink(groupId, null);
        Map<String, List<StreamSink>> sinkMap = streamSinks.stream()
                .collect(Collectors.groupingBy(StreamSink::getInlongStreamId, HashMap::new,
                        Collectors.toCollection(ArrayList::new)));
        Map<String, List<StreamSource>> sourceMap = createPulsarSources(groupInfo, streamInfoList);

        List<StreamInfo> streamInfos = new ArrayList<>();
        for (InlongStreamInfo inlongStream : streamInfoList) {
            String streamId = inlongStream.getInlongStreamId();
            StreamInfo streamInfo = new StreamInfo(streamId,
                    createNodesForStream(sourceMap.get(streamId), sinkMap.get(streamId)),
                    createNodeRelationsForStream(sourceMap.get(streamId), sinkMap.get(streamId)));
            streamInfos.add(streamInfo);
        }

        return new GroupInfo(groupId, streamInfos);
    }

    private Map<String, List<StreamSource>> createPulsarSources(
            InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfoList) {

        if (!MQType.MQ_PULSAR.equals(groupInfo.getMqType())) {
            String errMsg = String.format("Unsupported MQ type %s", groupInfo.getMqType());
            log.error(errMsg);
            throw new WorkflowListenerException(errMsg);
        }

        Map<String, List<StreamSource>> sourceMap = Maps.newHashMap();
        InlongClusterInfo clusterInfo = clusterService.getOne(groupInfo.getInlongClusterTag(), null,
                ClusterType.CLS_PULSAR);

        PulsarClusterInfo pulsarCluster = (PulsarClusterInfo) clusterInfo;
        String adminUrl = pulsarCluster.getAdminUrl();
        String serviceUrl = pulsarCluster.getUrl();
        String tenant = StringUtils.isEmpty(pulsarCluster.getTenant()) ? InlongGroupSettings.DEFAULT_PULSAR_TENANT
                : pulsarCluster.getTenant();
        streamInfoList.forEach(streamInfo -> {
            PulsarSource pulsarSource = new PulsarSource();
            String streamId = streamInfo.getInlongStreamId();
            pulsarSource.setTenant(tenant);
            pulsarSource.setSourceName(streamId);
            pulsarSource.setNamespace(groupInfo.getMqResource());
            pulsarSource.setTopic(streamInfo.getMqResource());
            pulsarSource.setAdminUrl(adminUrl);
            pulsarSource.setServiceUrl(serviceUrl);
            pulsarSource.setInlongComponent(true);

            List<StreamSource> sourceInfos = sourceService.listSource(groupInfo.getInlongGroupId(), streamId);
            for (StreamSource sourceInfo : sourceInfos) {
                if (StringUtils.isEmpty(pulsarSource.getSerializationType())
                        && StringUtils.isNotEmpty(sourceInfo.getSerializationType())) {
                    pulsarSource.setSerializationType(sourceInfo.getSerializationType());
                }
                if (SourceType.forType(sourceInfo.getSourceType()) == SourceType.KAFKA) {
                    pulsarSource.setPrimaryKey(((KafkaSource) sourceInfo).getPrimaryKey());
                }
            }
            if (StringUtils.isEmpty(pulsarSource.getSerializationType())) {
                pulsarSource.setSerializationType(DataTypeEnum.CSV.getName());
            }
            pulsarSource.setScanStartupMode(PulsarScanStartupMode.EARLIEST.getValue());
            pulsarSource.setFieldList(streamInfo.getFieldList());
            sourceMap.computeIfAbsent(streamId, key -> Lists.newArrayList()).add(pulsarSource);
        });
        return sourceMap;
    }

    private List<Node> createNodesForStream(List<StreamSource> sources, List<StreamSink> streamSinks) {
        List<Node> nodes = Lists.newArrayList();
        nodes.addAll(ExtractNodeUtils.createExtractNodes(sources));
        nodes.addAll(LoadNodeUtils.createLoadNodes(streamSinks));
        return nodes;
    }

    private List<NodeRelation> createNodeRelationsForStream(List<StreamSource> sources, List<StreamSink> streamSinks) {
        NodeRelation relation = new NodeRelation();
        List<String> inputs = sources.stream().map(StreamSource::getSourceName).collect(Collectors.toList());
        List<String> outputs = streamSinks.stream().map(StreamSink::getSinkName).collect(Collectors.toList());
        relation.setInputs(inputs);
        relation.setOutputs(outputs);
        return Lists.newArrayList(relation);
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
