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
import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.formats.json.JsonOptions.MapNullKeyMode;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.RowData;
import org.apache.flink.formats.json.JsonRowDataSerializationSchema;

import java.util.HashSet;
import java.util.UUID;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;
import org.apache.inlong.sort.elasticsearch.ElasticsearchSinkFunction;
import org.apache.inlong.sort.elasticsearch.RequestIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Sink function for converting upserts into Elasticsearch ActionRequests.
 */
public abstract class MultipleElasticsearchSinkFunctionBase<Request, ContentType>
        implements
            ElasticsearchSinkFunction<RowData, Request> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchSinkFunctionBase.class);

    private final String docType;
    private final ContentType contentType;
    private final RequestFactory<Request, ContentType> requestFactory;
    private final Function<RowData, String> createKey;
    private final Function<RowData, String> createRouting;
    private final DirtySinkHelper<Object> dirtySinkHelper;
    private final String multipleFormat;
    private final String indexPattern;
    private final TableSchemaFactory tableSchemaFactory;
    // initialized and reserved for a later feature.
    private final SchemaUpdateExceptionPolicy schemaUpdateExceptionPolicy;
    // open and store an index generator for each new index.
    private Map<String, IndexGenerator> indexGeneratorMap;
    // table level metrics
    private SinkTableMetricData sinkMetricData;
    private transient JsonDynamicSchemaFormat jsonDynamicSchemaFormat;
    private transient SerializationSchema<RowData> serializationSchema;
    // a hashset containing indices which are skipped due to exceptions.
    private final HashSet<String> errorSet = new HashSet<>();

    public MultipleElasticsearchSinkFunctionBase(
            @Nullable String docType, // this is deprecated in es 7+
            SerializationSchema<RowData> serializationSchema,
            ContentType contentType,
            RequestFactory<Request, ContentType> requestFactory,
            Function<RowData, String> createKey,
            @Nullable Function<RowData, String> createRouting,
            DirtySinkHelper<Object> dirtySinkHelper,
            TableSchemaFactory tableSchemaFactory,
            String multipleFormat,
            String indexPattern,
            SchemaUpdateExceptionPolicy schemaUpdateExceptionPolicy) {
        this.docType = docType;
        this.serializationSchema = Preconditions.checkNotNull(serializationSchema);
        this.contentType = Preconditions.checkNotNull(contentType);
        this.requestFactory = Preconditions.checkNotNull(requestFactory);
        this.createKey = Preconditions.checkNotNull(createKey);
        this.createRouting = createRouting;
        this.dirtySinkHelper = dirtySinkHelper;
        this.tableSchemaFactory = tableSchemaFactory;
        this.multipleFormat = multipleFormat;
        this.indexPattern = indexPattern;
        this.schemaUpdateExceptionPolicy = schemaUpdateExceptionPolicy;
    }

    @Override
    public void open(RuntimeContext ctx, SinkMetricData sinkMetricData) {
        indexGeneratorMap = new HashMap<>();
        this.sinkMetricData = (SinkTableMetricData) sinkMetricData;
    }

    private void sendMetrics(byte[] document, String index) {
        if (sinkMetricData != null) {
            sinkMetricData.outputMetrics(index, 1, document.length);
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
        JsonNode rootNode;
        // parse rootnode
        try {
            jsonDynamicSchemaFormat =
                    (JsonDynamicSchemaFormat) DynamicSchemaFormatFactory.getFormat(multipleFormat);
            rootNode = jsonDynamicSchemaFormat.deserialize(element.getBinary(0));
            // Ignore ddl change for now
            boolean isDDL = jsonDynamicSchemaFormat.extractDDLFlag(rootNode);
            if (isDDL) {
                LOGGER.error("ddl change unsupported");
                return;
            }
        } catch (Exception e) {
            LOGGER.error(String.format("deserialize error, raw data: %s", new String(element.getBinary(0))), e);
            throw new RuntimeException(e);
        }

        RowType rowType = jsonDynamicSchemaFormat.extractSchema(rootNode);
        RowData data = jsonDynamicSchemaFormat.extractRowData(rootNode, rowType).get(0);
        // generate the serialization schema
        serializationSchema = new JsonRowDataSerializationSchema(
                rowType, TimestampFormat.ISO_8601, MapNullKeyMode.LITERAL, "null", true);

        final byte[] document;
        try {
            // for multiple sink, need to update runtimeconverter to correct rowtype
            // for now create custom schema
            document = serializationSchema.serialize(data);
        } catch (Exception e) {
            LOGGER.error(String.format("Serialize error, raw data: %s", data), e);
            handleDirty(data, DirtyType.SERIALIZE_ERROR, e, null);
            return;
        }
        final String index;
        try {
            index = parseIndex(data, rootNode);
        } catch (Exception e) {
            LOGGER.error(String.format("Generate index error, raw data: %s", data), e);
            handleDirty(data, DirtyType.INDEX_GENERATE_ERROR, e, null);
            return;
        }
        final String key;
        try {
            // use uuid3 as key
            JsonNode physicalData = jsonDynamicSchemaFormat.getPhysicalData(rootNode);
            key = UUID.nameUUIDFromBytes(physicalData.toString().getBytes(StandardCharsets.UTF_8)).toString();
        } catch (Exception e) {
            LOGGER.error(String.format("Generate index id error, raw data: %s", data), e);
            handleDirty(data, DirtyType.INDEX_ID_GENERATE_ERROR, e, index);
            return;
        }
        // if the index is contained in errorset, then skip this record.
        if (errorSet.contains(index)) {
            return;
        }
        addDocument(data, key, index, document, indexer);
    }

    private void handleDirty(RowData rowData, DirtyType dirtyType, Exception e, String index) {
        // skip the index in which the error has occurred
        if (SchemaUpdateExceptionPolicy.STOP_PARTIAL == schemaUpdateExceptionPolicy) {
            if (index != null) {
                errorSet.add(index);
            } else {
                return;
            }
        }

        // keep retry the entire task until it succeeds
        if (SchemaUpdateExceptionPolicy.THROW_WITH_STOP == schemaUpdateExceptionPolicy) {
            throw new RuntimeException(String.format("Writing records %s failed, restarting task",
                    rowData), e);
        }

        // dirty data & archive
        if (SchemaUpdateExceptionPolicy.LOG_WITH_IGNORE == schemaUpdateExceptionPolicy) {
            dirtySinkHelper.invoke(rowData, dirtyType, e);
            if (sinkMetricData != null && index != null) {
                sinkMetricData.outputDirtyMetrics(index, 1,
                        rowData.toString().getBytes(StandardCharsets.UTF_8).length);
            } else {
                sinkMetricData.invokeDirty(1, rowData.toString().getBytes(StandardCharsets.UTF_8).length);
            }
        }
    }

    private String parseIndex(RowData rowData, JsonNode rootNode)
            throws Exception {
        String index;
        // parse the index dynamically, put index generators
        try {
            String rawIndex = jsonDynamicSchemaFormat.parse(rootNode, indexPattern);
            if (!indexGeneratorMap.containsKey(rawIndex)) {
                TableSchema schema = tableSchemaFactory.getSchema();
                IndexGenerator indexGenerator = IndexGeneratorFactory.createIndexGenerator(rawIndex, schema);
                indexGeneratorMap.put(rawIndex, indexGenerator);
                indexGenerator.open();
            }
            index = indexGeneratorMap.get(rawIndex).generate(rowData);
        } catch (Exception e) {
            LOGGER.error(String.format("json parse error, raw data: %s", new String(rowData.getBinary(0))), e);
            throw e;
        }
        return index;
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
                        sendMetrics(document, index);
                    }
                } else {
                    request = requestFactory.createIndexRequest(index, docType, key, contentType, document);
                    if (addRouting(request, element, document)) {
                        indexer.add(request);
                        sendMetrics(document, index);
                    }
                }
                break;
            case DELETE:
                request = requestFactory.createDeleteRequest(index, docType, key);
                if (addRouting(request, element, document)) {
                    indexer.add(request);
                    sendMetrics(document, index);
                }
                break;
            case UPDATE_BEFORE:
                sendMetrics(document, index);
                break;
            default:
                LOGGER.error(String.format("The type of element should be 'RowData' only, raw data: %s", element));
                handleDirty(element, DirtyType.UNSUPPORTED_DATA_TYPE,
                        new RuntimeException("The type of element should be 'RowData' only."), index);
        }
    }

    private boolean addRouting(Request request, RowData row, byte[] document) {
        if (null != createRouting) {
            try {
                String routing = createRouting.apply(row);
                handleRouting(request, routing);
            } catch (Exception e) {
                LOGGER.error(String.format("Routing error, raw data: %s", row), e);
                handleDirty(row, DirtyType.INDEX_ROUTING_ERROR, e, null);
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
        MultipleElasticsearchSinkFunctionBase<Request, ContentType> that =
                (MultipleElasticsearchSinkFunctionBase<Request, ContentType>) o;
        return Objects.equals(docType, that.docType)
                && Objects.equals(serializationSchema, that.serializationSchema)
                && contentType == that.contentType
                && Objects.equals(requestFactory, that.requestFactory)
                && Objects.equals(createKey, that.createKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                docType,
                serializationSchema,
                contentType,
                requestFactory,
                createKey);
    }
}
