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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.exceptions.WorkflowListenerException;
import org.apache.inlong.manager.common.pojo.cluster.InlongClusterInfo;
import org.apache.inlong.manager.common.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.source.kafka.KafkaSource;
import org.apache.inlong.manager.common.pojo.source.pulsar.PulsarSource;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamExtInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.StreamResourceProcessForm;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Create sort config for one stream if Zookeeper is disabled.
 */
@Slf4j
@Component
public class CreateStreamSortConfigListener implements SortOperateListener {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // thread safe

    @Autowired
    private StreamSourceService streamSourceService;
    @Autowired
    private StreamSinkService streamSinkService;
    @Autowired
    private InlongClusterService clusterService;

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) throws Exception {
        StreamResourceProcessForm form = (StreamResourceProcessForm) context.getProcessForm();
        GroupOperateType groupOperateType = form.getGroupOperateType();
        if (groupOperateType == GroupOperateType.SUSPEND || groupOperateType == GroupOperateType.DELETE) {
            return ListenerResult.success();
        }
        InlongGroupInfo groupInfo = form.getGroupInfo();
        InlongStreamInfo streamInfo = form.getStreamInfo();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        List<StreamSink> streamSinks = streamSinkService.listSink(groupId, streamId);
        if (CollectionUtils.isEmpty(streamSinks)) {
            log.warn("Sink not found by groupId={}", groupId);
            return ListenerResult.success();
        }
        try {
            List<StreamSource> sources = createPulsarSources(groupInfo, streamInfo);
            List<Node> nodes = createNodesForStream(sources, streamSinks);
            List<NodeRelation> nodeRelations = createNodeRelationsForStream(sources, streamSinks);
            StreamInfo sortStreamInfo = new StreamInfo(streamId, nodes, nodeRelations);
            GroupInfo sortGroupInfo = new GroupInfo(groupId, Lists.newArrayList(sortStreamInfo));
            String dataFlows = OBJECT_MAPPER.writeValueAsString(sortGroupInfo);
            InlongStreamExtInfo extInfo = new InlongStreamExtInfo();
            extInfo.setInlongGroupId(groupId);
            extInfo.setInlongStreamId(streamId);
            String keyName = InlongGroupSettings.DATA_FLOW;
            extInfo.setKeyName(keyName);
            extInfo.setKeyValue(dataFlows);
            if (streamInfo.getExtList() == null) {
                groupInfo.setExtList(Lists.newArrayList());
            }
            upsertDataFlow(streamInfo, extInfo, keyName);
        } catch (Exception e) {
            log.error("create sort config failed for sink list={} of groupId={}, streamId={}", streamSinks, groupId,
                    streamId, e);
            throw new WorkflowListenerException("create sort config failed: " + e.getMessage());
        }
        return ListenerResult.success();
    }

    private List<StreamSource> createPulsarSources(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo) {
        if (!MQType.MQ_PULSAR.equals(groupInfo.getMqType())) {
            String errMsg = String.format("Unsupported MQ type %s", groupInfo.getMqType());
            log.error(errMsg);
            throw new WorkflowListenerException(errMsg);
        }

        PulsarSource pulsarSource = new PulsarSource();
        String streamId = streamInfo.getInlongStreamId();
        pulsarSource.setSourceName(streamId);
        pulsarSource.setNamespace(groupInfo.getMqResource());
        pulsarSource.setTopic(streamInfo.getMqResource());

        InlongClusterInfo clusterInfo = clusterService.getOne(groupInfo.getInlongClusterTag(), null,
                ClusterType.CLS_PULSAR);
        PulsarClusterInfo pulsarCluster = (PulsarClusterInfo) clusterInfo;
        String adminUrl = pulsarCluster.getAdminUrl();
        String serviceUrl = pulsarCluster.getUrl();

        pulsarSource.setAdminUrl(adminUrl);
        pulsarSource.setServiceUrl(serviceUrl);
        pulsarSource.setInlongComponent(true);
        List<StreamSource> sources = streamSourceService.listSource(groupInfo.getInlongGroupId(), streamId);
        for (StreamSource source : sources) {
            if (StringUtils.isEmpty(pulsarSource.getSerializationType())
                    && StringUtils.isNotEmpty(source.getSerializationType())) {
                pulsarSource.setSerializationType(source.getSerializationType());
            }
            if (SourceType.forType(source.getSourceType()) == SourceType.KAFKA) {
                pulsarSource.setPrimaryKey(((KafkaSource) source).getPrimaryKey());
            }
        }
        pulsarSource.setScanStartupMode(PulsarScanStartupMode.EARLIEST.getValue());
        pulsarSource.setFieldList(streamInfo.getFieldList());
        return Lists.newArrayList(pulsarSource);
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

    private void upsertDataFlow(InlongStreamInfo streamInfo, InlongStreamExtInfo extInfo, String keyName) {
        streamInfo.getExtList().removeIf(ext -> keyName.equals(ext.getKeyName()));
        streamInfo.getExtList().add(extInfo);
    }

    @Override
    public boolean async() {
        return false;
    }
}
