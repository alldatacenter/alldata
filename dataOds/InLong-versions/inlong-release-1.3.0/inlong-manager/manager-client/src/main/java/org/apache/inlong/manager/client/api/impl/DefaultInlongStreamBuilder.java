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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.InlongStream;
import org.apache.inlong.manager.client.api.InlongStreamBuilder;
import org.apache.inlong.manager.client.api.inner.InnerGroupContext;
import org.apache.inlong.manager.client.api.inner.InnerStreamContext;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.inner.client.InlongStreamClient;
import org.apache.inlong.manager.client.api.inner.client.StreamSinkClient;
import org.apache.inlong.manager.client.api.inner.client.StreamSourceClient;
import org.apache.inlong.manager.client.api.inner.client.StreamTransformClient;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.client.api.util.StreamTransformTransfer;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamPipeline;
import org.apache.inlong.manager.pojo.stream.StreamTransform;
import org.apache.inlong.manager.pojo.transform.TransformRequest;
import org.apache.inlong.manager.pojo.transform.TransformResponse;

import java.util.List;
import java.util.Map;

/**
 * Default inlong stream builder.
 */
@Slf4j
public class DefaultInlongStreamBuilder extends InlongStreamBuilder {

    private final InlongStreamImpl inlongStream;
    private final InnerStreamContext streamContext;

    private final InlongStreamClient streamClient;
    private final StreamSourceClient sourceClient;
    private final StreamSinkClient sinkClient;
    private final StreamTransformClient transformClient;

    public DefaultInlongStreamBuilder(InlongStreamInfo streamInfo, InnerGroupContext groupContext,
            ClientConfiguration configuration) {

        ClientFactory clientFactory = ClientUtils.getClientFactory(configuration);
        this.streamClient = clientFactory.getStreamClient();
        this.sourceClient = clientFactory.getSourceClient();
        this.sinkClient = clientFactory.getSinkClient();
        this.transformClient = clientFactory.getTransformClient();

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

        this.inlongStream = new InlongStreamImpl(groupId, streamInfo.getInlongStreamId(), configuration);
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
        streamInfo.setId(streamClient.createStreamInfo(streamInfo));
        // Create source and update index
        List<SourceRequest> sourceRequests = Lists.newArrayList(streamContext.getSourceRequests().values());
        for (SourceRequest sourceRequest : sourceRequests) {
            sourceRequest.setId(sourceClient.createSource(sourceRequest));
        }
        // Create sink and update index
        List<SinkRequest> sinkRequests = Lists.newArrayList(streamContext.getSinkRequests().values());
        for (SinkRequest sinkRequest : sinkRequests) {
            sinkRequest.setId(sinkClient.createSink(sinkRequest));
        }
        // Create transform and update index
        List<TransformRequest> transformRequests = Lists.newArrayList(streamContext.getTransformRequests().values());
        for (TransformRequest transformRequest : transformRequests) {
            transformRequest.setId(transformClient.createTransform(transformRequest));
        }
        return inlongStream;
    }

    @Override
    public InlongStream initOrUpdate() {
        InlongStreamInfo dataStreamInfo = streamContext.getStreamInfo();
        StreamPipeline streamPipeline = inlongStream.createPipeline();
        dataStreamInfo.setExtParams(JsonUtils.toJsonString(streamPipeline));
        InlongStreamInfo existStreamInfo = streamClient.getStreamIfExists(dataStreamInfo);
        if (existStreamInfo != null) {
            dataStreamInfo.setVersion(existStreamInfo.getVersion());
            Pair<Boolean, String> updateMsg = streamClient.updateStreamInfo(dataStreamInfo);
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
        List<TransformResponse> transformResponses = transformClient.listTransform(groupId, streamId);
        List<String> updateTransformNames = Lists.newArrayList();
        for (TransformResponse transformResponse : transformResponses) {
            StreamTransform transform = StreamTransformTransfer.parseStreamTransform(transformResponse);
            final String transformName = transform.getTransformName();
            final int id = transformResponse.getId();
            if (transformRequests.get(transformName) != null) {
                TransformRequest transformRequest = transformRequests.get(transformName);
                transformRequest.setId(id);
                transformRequest.setVersion(transformResponse.getVersion());
                Pair<Boolean, String> updateState = transformClient.updateTransform(transformRequest);
                if (!updateState.getKey()) {
                    throw new RuntimeException(String.format("Update transform=%s failed with err=%s", transformRequest,
                            updateState.getValue()));
                }
                transformRequest.setId(transformResponse.getId());
                updateTransformNames.add(transformName);
            } else {
                log.warn("Unknown transform {} from server", transformName);
            }
        }
        for (Map.Entry<String, TransformRequest> requestEntry : transformRequests.entrySet()) {
            String transformName = requestEntry.getKey();
            if (updateTransformNames.contains(transformName)) {
                continue;
            }
            TransformRequest transformRequest = requestEntry.getValue();
            transformRequest.setId(transformClient.createTransform(transformRequest));
        }
    }

    private void initOrUpdateSource() {
        Map<String, SourceRequest> sourceRequests = streamContext.getSourceRequests();
        InlongStreamInfo streamInfo = streamContext.getStreamInfo();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        List<StreamSource> streamSources = sourceClient.listSources(groupId, streamId);
        List<String> updateSourceNames = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(streamSources)) {
            for (StreamSource source : streamSources) {
                final String sourceName = source.getSourceName();
                final int id = source.getId();
                if (sourceRequests.get(sourceName) != null) {
                    SourceRequest sourceRequest = sourceRequests.get(sourceName);
                    sourceRequest.setId(id);
                    sourceRequest.setVersion(source.getVersion());
                    Pair<Boolean, String> updateState = sourceClient.updateSource(sourceRequest);
                    if (!updateState.getKey()) {
                        throw new RuntimeException(String.format("Update source=%s failed with err=%s", sourceRequest,
                                updateState.getValue()));
                    }
                    updateSourceNames.add(sourceName);
                    sourceRequest.setId(source.getId());
                } else {
                    log.warn("Unknown source {} from server", sourceName);
                }
            }
        }
        for (Map.Entry<String, SourceRequest> requestEntry : sourceRequests.entrySet()) {
            String sourceName = requestEntry.getKey();
            if (updateSourceNames.contains(sourceName)) {
                continue;
            }
            SourceRequest sourceRequest = requestEntry.getValue();
            sourceRequest.setId(sourceClient.createSource(sourceRequest));
        }
    }

    private void initOrUpdateSink() {
        Map<String, SinkRequest> sinkRequests = streamContext.getSinkRequests();
        InlongStreamInfo streamInfo = streamContext.getStreamInfo();
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        List<StreamSink> streamSinks = sinkClient.listSinks(groupId, streamId);
        List<String> updateSinkNames = Lists.newArrayList();
        for (StreamSink sink : streamSinks) {
            final String sinkName = sink.getSinkName();
            final int id = sink.getId();
            if (sinkRequests.get(sinkName) != null) {
                SinkRequest sinkRequest = sinkRequests.get(sinkName);
                sinkRequest.setId(id);
                sinkRequest.setVersion(sink.getVersion());
                Pair<Boolean, String> updateState = sinkClient.updateSink(sinkRequest);
                if (!updateState.getKey()) {
                    throw new RuntimeException(String.format("Update sink=%s failed with err=%s", sinkRequest,
                            updateState.getValue()));
                }
                updateSinkNames.add(sinkName);
                sinkRequest.setId(sink.getId());
            } else {
                log.warn("Unknown sink {} from server", sinkName);
            }
        }
        for (Map.Entry<String, SinkRequest> requestEntry : sinkRequests.entrySet()) {
            String sinkName = requestEntry.getKey();
            if (updateSinkNames.contains(sinkName)) {
                continue;
            }
            SinkRequest sinkRequest = requestEntry.getValue();
            sinkRequest.setId(sinkClient.createSink(sinkRequest));
        }
    }
}
