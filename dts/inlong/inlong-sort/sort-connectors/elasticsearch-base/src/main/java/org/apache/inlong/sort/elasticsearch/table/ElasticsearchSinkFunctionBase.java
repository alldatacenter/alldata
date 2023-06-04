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

package org.apache.inlong.sort.elasticsearch.table;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.elasticsearch.ElasticsearchSinkFunction;
import org.apache.inlong.sort.elasticsearch.RequestIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

/**
 * Sink function for converting upserts into Elasticsearch ActionRequests.
 */
public abstract class ElasticsearchSinkFunctionBase<Request, ContentType>
        implements
            ElasticsearchSinkFunction<RowData, Request> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchSinkFunctionBase.class);

    private final IndexGenerator indexGenerator;
    private final String docType;
    private final SerializationSchema<RowData> serializationSchema;
    private final ContentType contentType;
    private final RequestFactory<Request, ContentType> requestFactory;
    private final Function<RowData, String> createKey;
    private final Function<RowData, String> createRouting;
    private final DirtySinkHelper<Object> dirtySinkHelper;
    private SinkMetricData sinkMetricData;

    public ElasticsearchSinkFunctionBase(
            IndexGenerator indexGenerator,
            @Nullable String docType, // this is deprecated in es 7+
            SerializationSchema<RowData> serializationSchema,
            ContentType contentType,
            RequestFactory<Request, ContentType> requestFactory,
            Function<RowData, String> createKey,
            @Nullable Function<RowData, String> createRouting,
            DirtySinkHelper<Object> dirtySinkHelper) {
        this.indexGenerator = Preconditions.checkNotNull(indexGenerator);
        this.docType = docType;
        this.serializationSchema = Preconditions.checkNotNull(serializationSchema);
        this.contentType = Preconditions.checkNotNull(contentType);
        this.requestFactory = Preconditions.checkNotNull(requestFactory);
        this.createKey = Preconditions.checkNotNull(createKey);
        this.createRouting = createRouting;
        this.dirtySinkHelper = dirtySinkHelper;
    }

    @Override
    public void open(RuntimeContext ctx, SinkMetricData sinkMetricData) {
        indexGenerator.open();
        this.sinkMetricData = sinkMetricData;
    }

    private void sendMetrics(byte[] document) {
        if (sinkMetricData != null) {
            sinkMetricData.invoke(1, document.length);
        }
    }

    @Override
    public void initializeState(FunctionInitializationContext context) {
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) {
    }

    @Override
    public void process(RowData element, RuntimeContext ctx, RequestIndexer<Request> indexer) {
        final byte[] document;
        try {
            document = serializationSchema.serialize(element);
        } catch (Exception e) {
            LOGGER.error(String.format("Serialize error, raw data: %s", element), e);
            dirtySinkHelper.invoke(element, DirtyType.SERIALIZE_ERROR, e);
            if (sinkMetricData != null) {
                sinkMetricData.invokeDirty(1, element.toString().getBytes(StandardCharsets.UTF_8).length);
            }
            return;
        }
        final String key;
        try {
            key = createKey.apply(element);
        } catch (Exception e) {
            LOGGER.error(String.format("Generate index id error, raw data: %s", element), e);
            dirtySinkHelper.invoke(element, DirtyType.INDEX_ID_GENERATE_ERROR, e);
            if (sinkMetricData != null) {
                sinkMetricData.invokeDirty(1, document.length);
            }
            return;
        }
        final String index;
        try {
            index = indexGenerator.generate(element);
        } catch (Exception e) {
            LOGGER.error(String.format("Generate index error, raw data: %s", element), e);
            dirtySinkHelper.invoke(element, DirtyType.INDEX_GENERATE_ERROR, e);
            if (sinkMetricData != null) {
                sinkMetricData.invokeDirty(1, document.length);
            }
            return;
        }
        addDocument(element, key, index, document, indexer);
    }

    @SuppressWarnings("unchecked")
    private void addDocument(RowData element, String key, String index, byte[] document,
            RequestIndexer<Request> indexer) {
        Request request;
        switch (element.getRowKind()) {
            case INSERT:
            case UPDATE_AFTER:
                if (key != null) {
                    request = requestFactory.createUpdateRequest(index, docType, key, contentType, document);
                    if (addRouting(request, element, document)) {
                        indexer.add(request);
                        sendMetrics(document);
                    }
                } else {
                    request = requestFactory.createIndexRequest(index, docType, key, contentType, document);
                    if (addRouting(request, element, document)) {
                        indexer.add(request);
                        sendMetrics(document);
                    }
                }
                break;
            case DELETE:
                request = requestFactory.createDeleteRequest(index, docType, key);
                if (addRouting(request, element, document)) {
                    indexer.add(request);
                    sendMetrics(document);
                }
                break;
            case UPDATE_BEFORE:
                sendMetrics(document);
                break;
            default:
                LOGGER.error(String.format("The type of element should be 'RowData' only, raw data: %s", element));
                dirtySinkHelper.invoke(element, DirtyType.UNSUPPORTED_DATA_TYPE,
                        new RuntimeException("The type of element should be 'RowData' only."));
                if (sinkMetricData != null) {
                    sinkMetricData.invokeDirty(1, document.length);
                }
        }
    }

    private boolean addRouting(Request request, RowData row, byte[] document) {
        if (null != createRouting) {
            try {
                String routing = createRouting.apply(row);
                handleRouting(request, routing);
            } catch (Exception e) {
                LOGGER.error(String.format("Routing error, raw data: %s", row), e);
                dirtySinkHelper.invoke(row, DirtyType.INDEX_ROUTING_ERROR, e);
                if (sinkMetricData != null) {
                    sinkMetricData.invokeDirty(1, document.length);
                }
                return false;
            }
        }
        return true;
    }

    public abstract void handleRouting(Request request, String routing);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        ElasticsearchSinkFunctionBase<Request, ContentType> that =
                (ElasticsearchSinkFunctionBase<Request, ContentType>) o;
        return Objects.equals(indexGenerator, that.indexGenerator)
                && Objects.equals(docType, that.docType)
                && Objects.equals(serializationSchema, that.serializationSchema)
                && contentType == that.contentType
                && Objects.equals(requestFactory, that.requestFactory)
                && Objects.equals(createKey, that.createKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                indexGenerator,
                docType,
                serializationSchema,
                contentType,
                requestFactory,
                createKey);
    }
}
