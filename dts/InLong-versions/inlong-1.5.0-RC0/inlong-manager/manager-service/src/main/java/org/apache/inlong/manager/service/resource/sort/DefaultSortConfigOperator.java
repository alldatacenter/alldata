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

package org.apache.inlong.manager.service.resource.sort;

import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sort.util.ExtractNodeUtils;
import org.apache.inlong.manager.pojo.sort.util.LoadNodeUtils;
import org.apache.inlong.manager.pojo.sort.util.NodeRelationUtils;
import org.apache.inlong.manager.pojo.sort.util.TransformNodeUtils;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.transform.TransformResponse;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.service.transform.StreamTransformService;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default Sort config operator, used to create a Sort config for the InlongGroup with ZK disabled.
 */
@Service
public class DefaultSortConfigOperator implements SortConfigOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSortConfigOperator.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private StreamSourceService sourceService;
    @Autowired
    private StreamTransformService transformService;
    @Autowired
    private StreamSinkService sinkService;

    @Override
    public Boolean accept(Integer enableZk) {
        return InlongConstants.DISABLE_ZK.equals(enableZk);
    }

    @Override
    public void buildConfig(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfos, boolean isStream)
            throws Exception {
        if (isStream) {
            LOGGER.warn("no need to build sort config for stream process when disable zk");
            return;
        }
        if (groupInfo == null || CollectionUtils.isEmpty(streamInfos)) {
            LOGGER.warn("no need to build sort config as the group is null or streams is empty when disable zk");
            return;
        }

        GroupInfo sortConfigInfo = this.getGroupInfo(groupInfo, streamInfos);
        String dataflow = OBJECT_MAPPER.writeValueAsString(sortConfigInfo);
        this.addToGroupExt(groupInfo, dataflow);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("success to build sort config, isStream={}, dataflow={}", isStream, dataflow);
        }
    }

    private GroupInfo getGroupInfo(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfoList) {
        // get source info
        Map<String, List<StreamSource>> sourceMap = sourceService.getSourcesMap(groupInfo, streamInfoList);
        // get sink info
        Map<String, List<StreamSink>> sinkMap = sinkService.getSinksMap(groupInfo, streamInfoList);

        List<TransformResponse> transformList = transformService.listTransform(groupInfo.getInlongGroupId(), null);
        Map<String, List<TransformResponse>> transformMap = transformList.stream()
                .collect(Collectors.groupingBy(TransformResponse::getInlongStreamId, HashMap::new,
                        Collectors.toCollection(ArrayList::new)));

        List<StreamInfo> sortStreamInfos = new ArrayList<>();
        for (InlongStreamInfo inlongStream : streamInfoList) {
            String streamId = inlongStream.getInlongStreamId();
            Map<String, StreamField> fieldMap = new HashMap<>();
            inlongStream.getSourceList().forEach(
                    source -> parseConstantFieldMap(source.getSourceName(), source.getFieldList(), fieldMap));

            List<TransformResponse> transformResponses = transformMap.get(streamId);
            if (CollectionUtils.isNotEmpty(transformResponses)) {
                transformResponses.forEach(
                        trans -> parseConstantFieldMap(trans.getTransformName(), trans.getFieldList(), fieldMap));
            }

            // build a stream info from the nodes and relations
            List<StreamSource> sources = sourceMap.get(streamId);
            List<StreamSink> sinks = sinkMap.get(streamId);
            List<NodeRelation> relations;

            if (InlongConstants.STANDARD_MODE.equals(groupInfo.getLightweight())) {
                if (CollectionUtils.isNotEmpty(transformResponses)) {
                    relations = NodeRelationUtils.createNodeRelations(inlongStream);
                    // in standard mode, replace upstream source node and transform input fields node
                    // to MQ node (which is inlong stream id)
                    String mqNodeName = sources.get(0).getSourceName();
                    Set<String> nodeNameSet = getInputNodeNames(sources, transformResponses);
                    adjustTransformField(transformResponses, nodeNameSet, mqNodeName);
                    adjustNodeRelations(relations, nodeNameSet, mqNodeName);
                } else {
                    relations = NodeRelationUtils.createNodeRelations(sources, sinks);
                }
            } else {
                relations = NodeRelationUtils.createNodeRelations(inlongStream);
            }

            // create extract-transform-load nodes
            List<Node> nodes = this.createNodes(sources, transformResponses, sinks, fieldMap);

            StreamInfo streamInfo = new StreamInfo(streamId, nodes, relations);
            sortStreamInfos.add(streamInfo);

            // rebuild joinerNode relation
            NodeRelationUtils.optimizeNodeRelation(streamInfo, transformResponses);
        }

        return new GroupInfo(groupInfo.getInlongGroupId(), sortStreamInfos);
    }

    /**
     * Deduplicate to get the node names of Source and Transform.
     */
    private Set<String> getInputNodeNames(List<StreamSource> sources, List<TransformResponse> transforms) {
        Set<String> result = new HashSet<>();
        if (CollectionUtils.isNotEmpty(sources)) {
            result.addAll(sources.stream().map(StreamSource::getSourceName).collect(Collectors.toSet()));
        }
        if (CollectionUtils.isNotEmpty(transforms)) {
            result.addAll(transforms.stream().map(TransformResponse::getTransformName).collect(Collectors.toSet()));
        }
        return result;
    }

    /**
     * Set origin node to mq node for transform fields if necessary.
     *
     * In standard mode for InlongGroup, transform input node must either be
     * mq source node or transform node, otherwise replace it with mq node name.
     */
    private void adjustTransformField(List<TransformResponse> transforms, Set<String> nodeNameSet, String mqNodeName) {
        for (TransformResponse transform : transforms) {
            for (StreamField field : transform.getFieldList()) {
                if (!nodeNameSet.contains(field.getOriginNodeName())) {
                    field.setOriginNodeName(mqNodeName);
                }
            }
        }
    }

    /**
     * Set the input node to MQ node for NodeRelations
     */
    private void adjustNodeRelations(List<NodeRelation> relations, Set<String> nodeNameSet, String mqNodeName) {
        for (NodeRelation relation : relations) {
            ListIterator<String> iterator = relation.getInputs().listIterator();
            while (iterator.hasNext()) {
                if (!nodeNameSet.contains(iterator.next())) {
                    iterator.set(mqNodeName);
                }
            }
        }
    }

    private List<Node> createNodes(List<StreamSource> sources, List<TransformResponse> transformResponses,
            List<StreamSink> sinks, Map<String, StreamField> constantFieldMap) {
        List<Node> nodes = new ArrayList<>();
        nodes.addAll(ExtractNodeUtils.createExtractNodes(sources));
        nodes.addAll(TransformNodeUtils.createTransformNodes(transformResponses, constantFieldMap));
        nodes.addAll(LoadNodeUtils.createLoadNodes(sinks, constantFieldMap));
        return nodes;
    }

    /**
     * Get constant field from stream fields
     *
     * @param nodeId node id
     * @param fields stream fields
     * @param constantFieldMap constant field map
     */
    private void parseConstantFieldMap(String nodeId, List<StreamField> fields,
            Map<String, StreamField> constantFieldMap) {
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }
        for (StreamField field : fields) {
            if (field.getFieldValue() != null) {
                constantFieldMap.put(String.format("%s-%s", nodeId, field.getFieldName()), field);
            }
        }
    }

    /**
     * Add config into inlong group ext info
     */
    private void addToGroupExt(InlongGroupInfo groupInfo, String value) {
        if (groupInfo.getExtList() == null) {
            groupInfo.setExtList(new ArrayList<>());
        }

        InlongGroupExtInfo extInfo = new InlongGroupExtInfo();
        extInfo.setInlongGroupId(groupInfo.getInlongGroupId());
        extInfo.setKeyName(InlongConstants.DATAFLOW);
        extInfo.setKeyValue(value);

        groupInfo.getExtList().removeIf(ext -> extInfo.getKeyName().equals(ext.getKeyName()));
        groupInfo.getExtList().add(extInfo);
    }

}
