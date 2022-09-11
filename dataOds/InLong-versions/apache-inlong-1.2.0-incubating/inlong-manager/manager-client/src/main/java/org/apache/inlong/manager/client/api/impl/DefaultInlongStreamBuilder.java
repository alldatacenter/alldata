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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.manager.client.api.InlongStream;
import org.apache.inlong.manager.client.api.InlongStreamBuilder;
import org.apache.inlong.manager.client.api.inner.InnerGroupContext;
import org.apache.inlong.manager.client.api.inner.InnerInlongManagerClient;
import org.apache.inlong.manager.client.api.inner.InnerStreamContext;
import org.apache.inlong.manager.client.api.util.StreamTransformTransfer;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.sink.SinkListResponse;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.pojo.source.SourceRequest;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.inlong.manager.common.pojo.stream.StreamPipeline;
import org.apache.inlong.manager.common.pojo.stream.StreamTransform;
import org.apache.inlong.manager.common.pojo.transform.TransformRequest;
import org.apache.inlong.manager.common.pojo.transform.TransformResponse;
import org.apache.inlong.manager.common.util.JsonUtils;

import java.util.List;
import java.util.Map;

/**
 * Default inlong stream builder.
 */
public class DefaultInlongStreamBuilder extends InlongStreamBuilder {

    private final InlongStreamImpl inlongStream;
    private final InnerStreamContext streamContext;
    private final InnerInlongManagerClient managerClient;

    public DefaultInlongStreamBuilder(InlongStreamInfo streamInfo, InnerGroupContext groupContext,
            InnerInlongManagerClient managerClient) {
        this.managerClient = managerClient;
        if (MapUtils.isEmpty(groupContext.getStreamContextMap())) {
            groupContext.setStreamContextMap(Maps.newHashMap());
        }

        InlongGroupInfo groupInfo = groupContext.getGroupInfo();
        String groupId = groupInfo.getInlongGroupId();
        streamInfo.setInlongGroupId(groupId);
        streamInfo.setCreator(groupInfo.getCreator());
        InnerStreamContext streamContext = new InnerStreamContext(streamInfo);
        groupContext.setStreamContext(streamContext);
        this.streamContext = streamContext;

        this.inlongStream = new InlongStreamImpl(groupId, streamInfo.getInlongStreamId(), managerClient);
        if (CollectionUtils.isNotEmpty(streamInfo.getFieldList())) {
            this.inlongStream.setStreamFields(streamInfo.getFieldList());
        }
        groupContext.setStream(this.inlongStream);
    }

    @Override
    public InlongStreamBuilder source(StreamSource source) {
        InlongStreamInfo streamInfo = streamContext.getStreamInfo();
        source.setInlongGroupId(streamInfo.getInlongGroupId());
        source.setInlongStreamId(streamInfo.getInlongStreamId());
        inlongStream.addSource(source);
        streamContext.setSourceRequest(source.genSourceRequest());
        return this;
    }

    @Override
    public InlongStreamBuilder sink(StreamSink streamSink) {
        InlongStreamInfo streamInfo = streamContext.getStreamInfo();
        streamSink.setInlongGroupId(streamInfo.getInlongGroupId());
        streamSink.setInlongStreamId(streamInfo.getInlongStreamId());
        inlongStream.addSink(streamSink);
        streamContext.setSinkRequest(streamSink.genSinkRequest());
        return this;
    }

    @Override
    public InlongStreamBuilder fields(List<StreamField> fieldList) {
        inlongStream.setStreamFields(fieldList);
        streamContext.updateStreamFields(fieldList);
        return this;
    }

    @Override
    public InlongStreamBuilder transform(StreamTransform streamTransform) {
        inlongStream.addTransform(streamTransform);
        TransformRequest transformRequest = StreamTransformTransfer.createTransformRequest(streamTransform,
                streamContext.getStreamInfo());
        streamContext.setTransformRequest(transformRequest);
        return this;
    }

    @Override
    public InlongStream init() {
        InlongStreamInfo streamInfo = streamContext.getStreamInfo();
        StreamPipeline streamPipeline = inlongStream.createPipeline();
        streamInfo.setExtParams(JsonUtils.toJsonString(streamPipeline));
        streamInfo.setId(managerClient.createStreamInfo(streamInfo));
        // Create source and update index
        List<SourceRequest> sourceRequests = Lists.newArrayList(streamContext.getSourceRequests().values());
        for (SourceRequest sourceRequest : sourceRequests) {
            sourceRequest.setId(managerClient.createSource(sourceRequest));
        }
        // Create sink and update index
        List<SinkRequest> sinkRequests = Lists.newArrayList(streamContext.getSinkRequests().values());
        for (SinkRequest sinkRequest : sinkRequests) {
            sinkRequest.setId(managerClient.createSink(sinkRequest));
        }
        // Create transform and update index
        List<TransformRequest> transformRequests = Lists.newArrayList(streamContext.getTransformRequests().values());
        for (TransformRequest transformRequest : transformRequests) {
            transformRequest.setId(managerClient.createTransform(transformRequest));
        }
        return inlongStream;
    }

    @Override
    public InlongStream initOrUpdate() {
        InlongStreamInfo dataStreamInfo = streamContext.getStreamInfo();
        StreamPipeline streamPipeline = inlongStream.createPipeline();
        dataStreamInfo.setExtParams(JsonUtils.toJsonString(streamPipeline));
        Boolean isExist = managerClient.isStreamExists(dataStreamInfo);
        if (isExist) {
            Pair<Boolean, String> updateMsg = managerClient.updateStreamInfo(dataStreamInfo);
            if (!updateMsg.getKey()) {
                throw new RuntimeException(String.format("Update data stream failed:%s", updateMsg.getValue()));
            }
            initOrUpdateTransform();
            initOrUpdateSource();
            initOrUpdateSink();
            return inlongStream;
        } else {
            return init();
        }
    }

    private void initOrUpdateTransform() {
        Map<String, TransformRequest> transformRequests = streamContext.getTransformRequests();
        InlongStreamInfo streamInfo = streamContext.getStreamInfo();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        List<TransformResponse> transformResponses = managerClient.listTransform(groupId, streamId);
        List<String> updateTransformNames = Lists.newArrayList();
        for (TransformResponse transformResponse : transformResponses) {
            StreamTransform transform = StreamTransformTransfer.parseStreamTransform(transformResponse);
            final String transformName = transform.getTransformName();
            final int id = transformResponse.getId();
            if (transformRequests.get(transformName) == null) {
                TransformRequest transformRequest = StreamTransformTransfer.createTransformRequest(transform,
                        streamInfo);
                boolean isDelete = managerClient.deleteTransform(transformRequest);
                if (!isDelete) {
                    throw new RuntimeException(String.format("Delete transform=%s failed", transformRequest));
                }
            } else {
                TransformRequest transformRequest = transformRequests.get(transformName);
                transformRequest.setId(id);
                Pair<Boolean, String> updateState = managerClient.updateTransform(transformRequest);
                if (!updateState.getKey()) {
                    throw new RuntimeException(String.format("Update transform=%s failed with err=%s", transformRequest,
                            updateState.getValue()));
                }
                transformRequest.setId(transformResponse.getId());
                updateTransformNames.add(transformName);
            }
        }
        for (Map.Entry<String, TransformRequest> requestEntry : transformRequests.entrySet()) {
            String transformName = requestEntry.getKey();
            if (updateTransformNames.contains(transformName)) {
                continue;
            }
            TransformRequest transformRequest = requestEntry.getValue();
            transformRequest.setId(managerClient.createTransform(transformRequest));
        }
    }

    private void initOrUpdateSource() {
        Map<String, SourceRequest> sourceRequests = streamContext.getSourceRequests();
        InlongStreamInfo streamInfo = streamContext.getStreamInfo();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        List<SourceListResponse> sourceListResponses = managerClient.listSources(groupId, streamId);
        List<String> updateSourceNames = Lists.newArrayList();
        for (SourceListResponse sourceListResponse : sourceListResponses) {
            final String sourceName = sourceListResponse.getSourceName();
            final int id = sourceListResponse.getId();
            if (sourceRequests.get(sourceName) == null) {
                boolean isDelete = managerClient.deleteSource(id);
                if (!isDelete) {
                    throw new RuntimeException(String.format("Delete source=%s failed", sourceListResponse));
                }
            } else {
                SourceRequest sourceRequest = sourceRequests.get(sourceName);
                sourceRequest.setId(id);
                Pair<Boolean, String> updateState = managerClient.updateSource(sourceRequest);
                if (!updateState.getKey()) {
                    throw new RuntimeException(String.format("Update source=%s failed with err=%s", sourceRequest,
                            updateState.getValue()));
                }
                updateSourceNames.add(sourceName);
                sourceRequest.setId(sourceListResponse.getId());
            }
        }
        for (Map.Entry<String, SourceRequest> requestEntry : sourceRequests.entrySet()) {
            String sourceName = requestEntry.getKey();
            if (updateSourceNames.contains(sourceName)) {
                continue;
            }
            SourceRequest sourceRequest = requestEntry.getValue();
            sourceRequest.setId(managerClient.createSource(sourceRequest));
        }
    }

    private void initOrUpdateSink() {
        Map<String, SinkRequest> sinkRequests = streamContext.getSinkRequests();
        InlongStreamInfo streamInfo = streamContext.getStreamInfo();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        List<SinkListResponse> sinkListResponses = managerClient.listSinks(groupId, streamId);
        List<String> updateSinkNames = Lists.newArrayList();
        for (SinkListResponse sinkListResponse : sinkListResponses) {
            final String sinkName = sinkListResponse.getSinkName();
            final int id = sinkListResponse.getId();
            if (sinkRequests.get(sinkName) == null) {
                boolean isDelete = managerClient.deleteSink(id);
                if (!isDelete) {
                    throw new RuntimeException(String.format("Delete sink=%s failed", sinkListResponse));
                }
            } else {
                SinkRequest sinkRequest = sinkRequests.get(sinkName);
                sinkRequest.setId(id);
                Pair<Boolean, String> updateState = managerClient.updateSink(sinkRequest);
                if (!updateState.getKey()) {
                    throw new RuntimeException(String.format("Update sink=%s failed with err=%s", sinkRequest,
                            updateState.getValue()));
                }
                updateSinkNames.add(sinkName);
                sinkRequest.setId(sinkListResponse.getId());
            }
        }
        for (Map.Entry<String, SinkRequest> requestEntry : sinkRequests.entrySet()) {
            String sinkName = requestEntry.getKey();
            if (updateSinkNames.contains(sinkName)) {
                continue;
            }
            SinkRequest sinkRequest = requestEntry.getValue();
            sinkRequest.setId(managerClient.createSink(sinkRequest));
        }
    }
}
