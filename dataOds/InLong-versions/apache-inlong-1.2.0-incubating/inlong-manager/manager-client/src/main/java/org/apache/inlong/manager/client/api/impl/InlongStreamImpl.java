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

package org.apache.inlong.manager.client.api.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.manager.client.api.InlongStream;
import org.apache.inlong.manager.client.api.inner.InnerInlongManagerClient;
import org.apache.inlong.manager.client.api.util.StreamTransformTransfer;
import org.apache.inlong.manager.common.pojo.sink.SinkListResponse;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.stream.FullStreamResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.inlong.manager.common.pojo.stream.StreamNodeRelation;
import org.apache.inlong.manager.common.pojo.stream.StreamPipeline;
import org.apache.inlong.manager.common.pojo.stream.StreamTransform;
import org.apache.inlong.manager.common.pojo.transform.TransformRequest;
import org.apache.inlong.manager.common.pojo.transform.TransformResponse;
import org.apache.inlong.manager.common.util.AssertUtils;
import org.apache.inlong.manager.common.util.JsonUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Inlong stream service implementation.
 */
@Data
public class InlongStreamImpl implements InlongStream {

    private InnerInlongManagerClient managerClient;

    private String inlongGroupId;

    private String inlongStreamId;

    private Map<String, StreamSource> streamSources = Maps.newHashMap();

    private Map<String, StreamSink> streamSinks = Maps.newHashMap();

    private Map<String, StreamTransform> streamTransforms = Maps.newHashMap();

    private List<StreamField> streamFields = Lists.newArrayList();

    /**
     * Constructor of InlongStreamImpl.
     */
    public InlongStreamImpl(FullStreamResponse streamResponse, InnerInlongManagerClient managerClient) {
        InlongStreamInfo streamInfo = streamResponse.getStreamInfo();
        this.managerClient = managerClient;
        this.inlongGroupId = streamInfo.getInlongGroupId();
        this.inlongStreamId = streamInfo.getInlongStreamId();
        List<StreamField> streamFields = streamInfo.getFieldList();
        if (CollectionUtils.isNotEmpty(streamFields)) {
            this.streamFields = streamFields.stream()
                    .map(fieldInfo -> new StreamField(
                                    fieldInfo.getId(),
                                    fieldInfo.getFieldType(),
                                    fieldInfo.getFieldName(),
                                    fieldInfo.getFieldComment(),
                                    fieldInfo.getFieldValue(),
                                    fieldInfo.getIsMetaField(),
                                    fieldInfo.getMetaFieldName(),
                                    fieldInfo.getOriginNodeName()
                            )
                    ).collect(Collectors.toList());
        }
        List<StreamSink> responseList = streamResponse.getSinkInfo();
        if (CollectionUtils.isNotEmpty(responseList)) {
            this.streamSinks = responseList.stream()
                    .collect(Collectors.toMap(StreamSink::getSinkName, streamSink -> streamSink,
                            (sink1, sink2) -> {
                                throw new RuntimeException(String.format("duplicate sinkName:%s in stream:%s",
                                        sink1.getSinkName(), this.inlongStreamId));
                            }));
        }
        List<StreamSource> sourceList = streamResponse.getSourceInfo();
        if (CollectionUtils.isNotEmpty(sourceList)) {
            this.streamSources = sourceList.stream()
                    .collect(Collectors.toMap(StreamSource::getSourceName, streamSource -> streamSource,
                            (source1, source2) -> {
                                throw new RuntimeException(String.format("duplicate sourceName: %s in streamId: %s",
                                        source1.getSourceName(), this.inlongStreamId));
                            }
                    ));
        }
    }

    public InlongStreamImpl(String groupId, String streamId, InnerInlongManagerClient managerClient) {
        this.managerClient = managerClient;
        this.inlongGroupId = groupId;
        this.inlongStreamId = streamId;
    }

    @Override
    public List<StreamField> getStreamFields() {
        return this.streamFields;
    }

    @Override
    public Map<String, StreamSource> getSources() {
        return this.streamSources;
    }

    @Override
    public Map<String, StreamSink> getSinks() {
        return this.streamSinks;
    }

    @Override
    public Map<String, StreamTransform> getTransforms() {
        return this.streamTransforms;
    }

    @Override
    public InlongStream addSource(StreamSource source) {
        AssertUtils.notNull(source.getSourceName(), "Source name should not be empty");
        String sourceName = source.getSourceName();
        if (streamSources.get(sourceName) != null) {
            throw new IllegalArgumentException(String.format("StreamSource=%s has already be set", source));
        }
        streamSources.put(sourceName, source);
        return this;
    }

    @Override
    public InlongStream addSink(StreamSink streamSink) {
        AssertUtils.notNull(streamSink.getSinkName(), "Sink name should not be empty");
        String sinkName = streamSink.getSinkName();
        if (streamSinks.get(sinkName) != null) {
            throw new IllegalArgumentException(String.format("StreamSink=%s has already be set", streamSink));
        }
        streamSinks.put(sinkName, streamSink);
        return this;
    }

    @Override
    public InlongStream addTransform(StreamTransform transform) {
        AssertUtils.notNull(transform.getTransformName(), "Transform name should not be empty");
        String transformName = transform.getTransformName();
        if (streamTransforms.get(transformName) != null) {
            throw new IllegalArgumentException(String.format("TransformName=%s has already be set", transform));
        }
        streamTransforms.put(transformName, transform);
        return this;
    }

    @Override
    public InlongStream deleteSource(String sourceName) {
        streamSources.remove(sourceName);
        return this;
    }

    @Override
    public InlongStream deleteSink(String sinkName) {
        streamSinks.remove(sinkName);
        return this;
    }

    @Override
    public InlongStream deleteTransform(String transformName) {
        streamTransforms.remove(transformName);
        return this;
    }

    @Override
    public InlongStream updateSource(StreamSource source) {
        AssertUtils.notNull(source.getSourceName(), "Source name should not be empty");
        streamSources.put(source.getSourceName(), source);
        return this;
    }

    @Override
    public InlongStream updateSink(StreamSink streamSink) {
        AssertUtils.notNull(streamSink.getSinkName(), "Sink name should not be empty");
        streamSinks.put(streamSink.getSinkName(), streamSink);
        return this;
    }

    @Override
    public InlongStream updateTransform(StreamTransform transform) {
        AssertUtils.notNull(transform.getTransformName(), "Transform name should not be empty");
        streamTransforms.put(transform.getTransformName(), transform);
        return this;
    }

    @Override
    public StreamPipeline createPipeline() {
        StreamPipeline streamPipeline = new StreamPipeline();
        if (MapUtils.isEmpty(streamTransforms)) {
            StreamNodeRelation relation = new StreamNodeRelation();
            relation.setInputNodes(streamSources.keySet());
            relation.setOutputNodes(streamSinks.keySet());
            streamPipeline.setPipeline(Lists.newArrayList(relation));
            return streamPipeline;
        }

        Map<Set<String>, List<StreamNodeRelation>> relationMap = Maps.newHashMap();
        // Check preNodes
        for (StreamTransform streamTransform : streamTransforms.values()) {
            String transformName = streamTransform.getTransformName();
            Set<String> preNodes = streamTransform.getPreNodes();
            StreamNodeRelation relation = new StreamNodeRelation();
            relation.setInputNodes(preNodes);
            relation.setOutputNodes(Sets.newHashSet(transformName));
            for (String preNode : preNodes) {
                StreamTransform transform = streamTransforms.get(preNode);
                if (transform != null) {
                    transform.addPost(transformName);
                }
            }
            relationMap.computeIfAbsent(preNodes, key -> Lists.newArrayList()).add(relation);
        }
        // Check postNodes
        for (StreamTransform streamTransform : streamTransforms.values()) {
            String transformName = streamTransform.getTransformName();
            Set<String> postNodes = streamTransform.getPostNodes();
            Set<String> sinkSet = Sets.newHashSet();
            for (String postNode : postNodes) {
                StreamSink sink = streamSinks.get(postNode);
                if (sink != null) {
                    sinkSet.add(sink.getSinkName());
                }
            }
            if (CollectionUtils.isNotEmpty(sinkSet)) {
                StreamNodeRelation relation = new StreamNodeRelation();
                Set<String> preNodes = Sets.newHashSet(transformName);
                relation.setInputNodes(preNodes);
                relation.setOutputNodes(sinkSet);
                relationMap.computeIfAbsent(preNodes, key -> Lists.newArrayList()).add(relation);
            }
        }
        List<StreamNodeRelation> relations = Lists.newArrayList();
        // Merge StreamNodeRelation with same preNodes
        for (Map.Entry<Set<String>, List<StreamNodeRelation>> entry : relationMap.entrySet()) {
            List<StreamNodeRelation> unmergedRelations = entry.getValue();
            if (unmergedRelations.size() == 1) {
                relations.add(unmergedRelations.get(0));
            } else {
                StreamNodeRelation mergedRelation = unmergedRelations.get(0);
                for (int index = 1; index < unmergedRelations.size(); index++) {
                    StreamNodeRelation unmergedRelation = unmergedRelations.get(index);
                    unmergedRelation.getOutputNodes().forEach(mergedRelation::addOutputNode);
                }
                relations.add(mergedRelation);
            }
        }
        streamPipeline.setPipeline(relations);
        Pair<Boolean, Pair<String, String>> circleState = streamPipeline.hasCircle();
        if (circleState.getLeft()) {
            Pair<String, String> circleNodes = circleState.getRight();
            throw new IllegalStateException(
                    String.format("There is circle dependency in streamPipeline for node=%s and node=%s",
                            circleNodes.getLeft(), circleNodes.getRight()));
        }
        return streamPipeline;
    }

    @Override
    public InlongStream update() {
        InlongStreamInfo streamInfo = managerClient.getStreamInfo(inlongGroupId, inlongStreamId);
        if (streamInfo == null) {
            throw new IllegalArgumentException(
                    String.format("Stream is not exists for group=%s and stream=%s", inlongGroupId, inlongStreamId));
        }

        streamInfo.setFieldList(this.streamFields);
        StreamPipeline streamPipeline = createPipeline();
        streamInfo.setExtParams(JsonUtils.toJsonString(streamPipeline));
        Pair<Boolean, String> updateMsg = managerClient.updateStreamInfo(streamInfo);
        if (!updateMsg.getKey()) {
            throw new RuntimeException(String.format("Update data stream failed: %s", updateMsg.getValue()));
        }
        initOrUpdateTransform(streamInfo);
        initOrUpdateSource(streamInfo);
        initOrUpdateSink(streamInfo);
        return this;
    }

    private void initOrUpdateTransform(InlongStreamInfo streamInfo) {
        List<TransformResponse> transformResponses = managerClient.listTransform(inlongGroupId, inlongStreamId);
        List<String> updateTransformNames = Lists.newArrayList();
        for (TransformResponse transformResponse : transformResponses) {
            StreamTransform transform = StreamTransformTransfer.parseStreamTransform(transformResponse);
            final String transformName = transform.getTransformName();
            final int id = transformResponse.getId();
            if (this.streamTransforms.get(transformName) == null) {
                TransformRequest transformRequest = StreamTransformTransfer.createTransformRequest(transform,
                        streamInfo);
                boolean isDelete = managerClient.deleteTransform(transformRequest);
                if (!isDelete) {
                    throw new RuntimeException(String.format("Delete transform=%s failed", transformRequest));
                }
            } else {
                StreamTransform newTransform = this.streamTransforms.get(transformName);
                TransformRequest transformRequest = StreamTransformTransfer.createTransformRequest(newTransform,
                        streamInfo);
                transformRequest.setId(id);
                Pair<Boolean, String> updateState = managerClient.updateTransform(transformRequest);
                if (!updateState.getKey()) {
                    throw new RuntimeException(String.format("Update transform=%s failed with err=%s", transformRequest,
                            updateState.getValue()));
                }
                updateTransformNames.add(transformName);
            }
        }
        for (Map.Entry<String, StreamTransform> transformEntry : this.streamTransforms.entrySet()) {
            String transformName = transformEntry.getKey();
            if (updateTransformNames.contains(transformName)) {
                continue;
            }
            StreamTransform transform = transformEntry.getValue();
            TransformRequest transformRequest = StreamTransformTransfer.createTransformRequest(transform,
                    streamInfo);
            managerClient.createTransform(transformRequest);
        }
    }

    private void initOrUpdateSource(InlongStreamInfo streamInfo) {
        List<SourceListResponse> sourceListResponses = managerClient.listSources(inlongGroupId, inlongStreamId);
        List<String> updateSourceNames = Lists.newArrayList();
        for (SourceListResponse sourceListResponse : sourceListResponses) {
            final String sourceName = sourceListResponse.getSourceName();
            final int id = sourceListResponse.getId();
            if (this.streamSources.get(sourceName) == null) {
                boolean isDelete = managerClient.deleteSource(id);
                if (!isDelete) {
                    throw new RuntimeException(String.format("Delete source=%s failed", sourceListResponse));
                }
            } else {
                StreamSource streamSource = this.streamSources.get(sourceName);
                streamSource.setId(id);
                streamSource.setInlongGroupId(streamInfo.getInlongGroupId());
                streamSource.setInlongStreamId(streamInfo.getInlongStreamId());
                Pair<Boolean, String> updateState = managerClient.updateSource(streamSource.genSourceRequest());
                if (!updateState.getKey()) {
                    throw new RuntimeException(String.format("Update source=%s failed with err=%s", streamSource,
                            updateState.getValue()));
                }
                updateSourceNames.add(sourceName);
            }
        }
        for (Map.Entry<String, StreamSource> sourceEntry : streamSources.entrySet()) {
            String sourceName = sourceEntry.getKey();
            if (updateSourceNames.contains(sourceName)) {
                continue;
            }
            StreamSource streamSource = sourceEntry.getValue();
            streamSource.setInlongGroupId(streamInfo.getInlongGroupId());
            streamSource.setInlongStreamId(streamInfo.getInlongStreamId());
            managerClient.createSource(streamSource.genSourceRequest());
        }
    }

    private void initOrUpdateSink(InlongStreamInfo streamInfo) {
        List<SinkListResponse> sinkListResponses = managerClient.listSinks(inlongGroupId, inlongStreamId);
        // delete or update the sink info
        List<String> updateSinkNames = Lists.newArrayList();
        for (SinkListResponse sinkResponse : sinkListResponses) {
            final String sinkName = sinkResponse.getSinkName();
            final int id = sinkResponse.getId();
            if (this.streamSinks.get(sinkName) == null) {
                boolean isDelete = managerClient.deleteSink(id);
                if (!isDelete) {
                    throw new RuntimeException(String.format("Delete sink=%s failed", sinkResponse));
                }
            } else {
                StreamSink streamSink = this.streamSinks.get(sinkName);
                streamSink.setId(id);
                streamSink.setInlongGroupId(streamInfo.getInlongGroupId());
                streamSink.setInlongStreamId(streamInfo.getInlongStreamId());
                Pair<Boolean, String> updateState = managerClient.updateSink(streamSink.genSinkRequest());
                if (!updateState.getKey()) {
                    throw new RuntimeException(String.format("Update sink=%s failed with err=%s", streamSink,
                            updateState.getValue()));
                }
                updateSinkNames.add(sinkName);
            }
        }

        // create sink info after deleting or updating
        for (Map.Entry<String, StreamSink> sinkEntry : streamSinks.entrySet()) {
            String sinkName = sinkEntry.getKey();
            if (updateSinkNames.contains(sinkName)) {
                continue;
            }
            StreamSink streamSink = sinkEntry.getValue();
            streamSink.setInlongGroupId(streamInfo.getInlongGroupId());
            streamSink.setInlongStreamId(streamInfo.getInlongStreamId());
            managerClient.createSink(streamSink.genSinkRequest());
        }
    }

}
