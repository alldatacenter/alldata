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

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamExtInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.transform.TransformResponse;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.pojo.sort.util.ExtractNodeUtils;
import org.apache.inlong.manager.pojo.sort.util.LoadNodeUtils;
import org.apache.inlong.manager.pojo.sort.util.NodeRelationUtils;
import org.apache.inlong.manager.pojo.sort.util.TransformNodeUtils;
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
import java.util.List;
import java.util.Map;
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
        if (groupInfo == null || CollectionUtils.isEmpty(streamInfos)) {
            LOGGER.warn("group info is null or stream infos is empty, no need to build sort config for disable zk");
            return;
        }

        GroupInfo configInfo;
        // if the mode of inlong group is LIGHTWEIGHT, means not using any MQ as a cached source
        if (InlongConstants.LIGHTWEIGHT_MODE.equals(groupInfo.getLightweight())) {
            configInfo = this.getLightweightGroupInfo(groupInfo, streamInfos);
        } else {
            configInfo = this.getStandardGroupInfo(groupInfo, streamInfos);
        }

        String dataflow = OBJECT_MAPPER.writeValueAsString(configInfo);
        if (isStream) {
            this.addToStreamExt(streamInfos, dataflow);
        } else {
            this.addToGroupExt(groupInfo, dataflow);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("success to build sort config, isStream={}, dataflow={}", isStream, dataflow);
        }
    }

    private GroupInfo getLightweightGroupInfo(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfoList) {
        // get source info
        Map<String, List<StreamSource>> sourceMap = sourceService.getSourcesMap(groupInfo, streamInfoList);
        // get sink info
        Map<String, List<StreamSink>> sinkMap = sinkService.getSinksMap(groupInfo, streamInfoList);

        List<TransformResponse> transformResponses = transformService.listTransform(groupInfo.getInlongGroupId(), null);
        Map<String, List<TransformResponse>> transformMap = transformResponses.stream()
                .collect(Collectors.groupingBy(TransformResponse::getInlongStreamId, HashMap::new,
                        Collectors.toCollection(ArrayList::new)));

        List<StreamInfo> sortStreamInfos = new ArrayList<>();
        for (InlongStreamInfo inlongStream : streamInfoList) {
            String streamId = inlongStream.getInlongStreamId();
            Map<String, StreamField> fieldMap = new HashMap<>();
            inlongStream.getSourceList().forEach(
                    source -> parseConstantFieldMap(source.getSourceName(), source.getFieldList(), fieldMap));
            List<TransformResponse> transformResponseList = transformMap.get(streamId);
            if (CollectionUtils.isNotEmpty(transformResponseList)) {
                transformResponseList.forEach(
                        trans -> parseConstantFieldMap(trans.getTransformName(), trans.getFieldList(), fieldMap));
            }

            // build a stream info from the nodes and relations
            List<Node> nodes = this.createNodesWithTransform(sourceMap.get(streamId),
                    transformResponseList, sinkMap.get(streamId), fieldMap);
            List<NodeRelation> relations = NodeRelationUtils.createNodeRelations(inlongStream);
            StreamInfo streamInfo = new StreamInfo(streamId, nodes, relations);
            sortStreamInfos.add(streamInfo);

            // rebuild joinerNode relation
            NodeRelationUtils.optimizeNodeRelation(streamInfo, transformResponseList);
        }

        return new GroupInfo(groupInfo.getInlongGroupId(), sortStreamInfos);
    }

    /**
     * Get Sort GroupInfo of STANDARD inlong group.
     *
     * @see org.apache.inlong.sort.protocol.GroupInfo
     */
    private GroupInfo getStandardGroupInfo(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfoList) {
        // get source info
        Map<String, List<StreamSource>> sourceMap = sourceService.getSourcesMap(groupInfo, streamInfoList);
        // get sink info
        Map<String, List<StreamSink>> sinkMap = sinkService.getSinksMap(groupInfo, streamInfoList);

        // create StreamInfo for Sort protocol
        List<StreamInfo> sortStreamInfos = new ArrayList<>();
        for (InlongStreamInfo inlongStream : streamInfoList) {
            String streamId = inlongStream.getInlongStreamId();

            Map<String, StreamField> fieldMap = new HashMap<>();
            inlongStream.getSourceList().forEach(
                    source -> parseConstantFieldMap(source.getSourceName(), source.getFieldList(), fieldMap));

            List<StreamSource> sources = sourceMap.get(streamId);
            List<StreamSink> sinks = sinkMap.get(streamId);
            StreamInfo sortStream = new StreamInfo(streamId,
                    this.createNodesWithoutTransform(sources, sinks, fieldMap),
                    NodeRelationUtils.createNodeRelations(sources, sinks));
            sortStreamInfos.add(sortStream);
        }

        return new GroupInfo(groupInfo.getInlongGroupId(), sortStreamInfos);
    }

    private List<Node> createNodesWithoutTransform(List<StreamSource> sources, List<StreamSink> sinks,
            Map<String, StreamField> constantFieldMap) {
        List<Node> nodes = Lists.newArrayList();
        nodes.addAll(ExtractNodeUtils.createExtractNodes(sources));
        nodes.addAll(LoadNodeUtils.createLoadNodes(sinks, constantFieldMap));
        return nodes;
    }

    private List<Node> createNodesWithTransform(List<StreamSource> sources, List<TransformResponse> transformResponses,
            List<StreamSink> sinks, Map<String, StreamField> constantFieldMap) {
        List<Node> nodes = Lists.newArrayList();
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
            groupInfo.setExtList(Lists.newArrayList());
        }

        InlongGroupExtInfo extInfo = new InlongGroupExtInfo();
        extInfo.setInlongGroupId(groupInfo.getInlongGroupId());
        extInfo.setKeyName(InlongConstants.DATAFLOW);
        extInfo.setKeyValue(value);

        groupInfo.getExtList().removeIf(ext -> extInfo.getKeyName().equals(ext.getKeyName()));
        groupInfo.getExtList().add(extInfo);
    }

    /**
     * Add config into inlong stream ext info
     */
    private void addToStreamExt(List<InlongStreamInfo> streamInfos, String value) {
        streamInfos.forEach(streamInfo -> {
            if (streamInfo.getExtList() == null) {
                streamInfo.setExtList(Lists.newArrayList());
            }

            InlongStreamExtInfo extInfo = new InlongStreamExtInfo();
            extInfo.setInlongGroupId(streamInfo.getInlongGroupId());
            extInfo.setInlongStreamId(streamInfo.getInlongStreamId());
            extInfo.setKeyName(InlongConstants.DATAFLOW);
            extInfo.setKeyValue(value);

            streamInfo.getExtList().removeIf(ext -> extInfo.getKeyName().equals(ext.getKeyName()));
            streamInfo.getExtList().add(extInfo);
        });
    }

}
