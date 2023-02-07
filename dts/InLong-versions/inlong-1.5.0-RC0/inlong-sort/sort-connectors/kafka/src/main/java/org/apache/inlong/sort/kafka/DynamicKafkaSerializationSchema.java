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

package org.apache.inlong.sort.kafka;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.streaming.connectors.kafka.KafkaContextAware;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.formats.raw.RawFormatSerializationSchema;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.base.dirty.DirtyData;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.apache.inlong.sort.base.metric.sub.SinkTopicMetricData;
import org.apache.inlong.sort.kafka.KafkaDynamicSink.WritableMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A specific {@link KafkaSerializationSchema} for {@link KafkaDynamicSink}.
 */
class DynamicKafkaSerializationSchema implements KafkaSerializationSchema<RowData>, KafkaContextAware<RowData> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DynamicKafkaSerializationSchema.class);

    private final @Nullable FlinkKafkaPartitioner<RowData> partitioner;

    private final String topic;

    private final @Nullable SerializationSchema<RowData> keySerialization;

    private final SerializationSchema<RowData> valueSerialization;

    private final RowData.FieldGetter[] keyFieldGetters;

    private final RowData.FieldGetter[] valueFieldGetters;

    private final boolean hasMetadata;

    private final boolean upsertMode;

    private final String topicPattern;
    /**
     * Contains the position for each value of {@link WritableMetadata} in the
     * consumed row or -1 if this metadata key is not used.
     */
    private final int[] metadataPositions;
    private final String sinkMultipleFormat;
    private boolean multipleSink;
    private JsonDynamicSchemaFormat jsonDynamicSchemaFormat;
    private final DirtyOptions dirtyOptions;
    private final @Nullable DirtySink<Object> dirtySink;
    private int[] partitions;

    private int parallelInstanceId;

    private int numParallelInstances;
    private SinkTopicMetricData metricData;

    DynamicKafkaSerializationSchema(
            String topic,
            @Nullable FlinkKafkaPartitioner<RowData> partitioner,
            @Nullable SerializationSchema<RowData> keySerialization,
            SerializationSchema<RowData> valueSerialization,
            RowData.FieldGetter[] keyFieldGetters,
            RowData.FieldGetter[] valueFieldGetters,
            boolean hasMetadata,
            int[] metadataPositions,
            boolean upsertMode,
            @Nullable String sinkMultipleFormat,
            @Nullable String topicPattern,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink) {
        if (upsertMode) {
            Preconditions.checkArgument(
                    keySerialization != null && keyFieldGetters.length > 0,
                    "Key must be set in upsert mode for serialization schema.");
        }
        this.topic = topic;
        this.partitioner = partitioner;
        this.keySerialization = keySerialization;
        this.valueSerialization = valueSerialization;
        this.keyFieldGetters = keyFieldGetters;
        this.valueFieldGetters = valueFieldGetters;
        this.hasMetadata = hasMetadata;
        this.metadataPositions = metadataPositions;
        this.upsertMode = upsertMode;
        this.sinkMultipleFormat = sinkMultipleFormat;
        this.topicPattern = topicPattern;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
    }

    public void setMetricData(SinkTopicMetricData metricData) {
        this.metricData = metricData;
    }

    static RowData createProjectedRow(
            RowData consumedRow, RowKind kind, RowData.FieldGetter[] fieldGetters) {
        final int arity = fieldGetters.length;
        final GenericRowData genericRowData = new GenericRowData(kind, arity);
        for (int fieldPos = 0; fieldPos < arity; fieldPos++) {
            genericRowData.setField(fieldPos, fieldGetters[fieldPos].getFieldOrNull(consumedRow));
        }
        return genericRowData;
    }

    @Override
    public void open(SerializationSchema.InitializationContext context) throws Exception {
        if (keySerialization != null) {
            keySerialization.open(context);
        }
        valueSerialization.open(context);
        if (partitioner != null) {
            partitioner.open(parallelInstanceId, numParallelInstances);
        }
        if (dirtySink != null) {
            dirtySink.open(new Configuration());
        }
        // Only support dynamic topic when the topicPattern is specified
        // and the valueSerialization is RawFormatSerializationSchema
        if (valueSerialization instanceof RawFormatSerializationSchema && StringUtils.isNotBlank(sinkMultipleFormat)) {
            multipleSink = true;
            jsonDynamicSchemaFormat =
                    (JsonDynamicSchemaFormat) DynamicSchemaFormatFactory.getFormat(sinkMultipleFormat);
        }
    }

    @Override
    public ProducerRecord<byte[], byte[]> serialize(RowData consumedRow, @Nullable Long timestamp) {
        // shortcut in case no input projection is required
        if (keySerialization == null && !hasMetadata) {
            final byte[] valueSerialized = serializeWithDirtyHandle(consumedRow,
                    DirtyType.VALUE_SERIALIZE_ERROR, valueSerialization);
            if (valueSerialized != null) {
                return new ProducerRecord<>(
                        getTargetTopic(consumedRow),
                        extractPartition(consumedRow, null, valueSerialized),
                        null,
                        valueSerialized);
            }
            return null;
        }
        final byte[] keySerialized;
        boolean mayDirtyData = false;
        if (keySerialization == null) {
            keySerialized = null;
        } else {
            final RowData keyRow = createProjectedRow(consumedRow, RowKind.INSERT, keyFieldGetters);
            keySerialized = serializeWithDirtyHandle(keyRow, DirtyType.KEY_SERIALIZE_ERROR, keySerialization);
            mayDirtyData = keySerialized == null;
        }

        final byte[] valueSerialized;
        final RowKind kind = consumedRow.getRowKind();
        final RowData valueRow = createProjectedRow(consumedRow, kind, valueFieldGetters);
        if (upsertMode) {
            if (kind == RowKind.DELETE || kind == RowKind.UPDATE_BEFORE) {
                // transform the message as the tombstone message
                valueSerialized = null;
            } else {
                // make the message to be INSERT to be compliant with the INSERT-ONLY format
                valueRow.setRowKind(RowKind.INSERT);
                valueSerialized = serializeWithDirtyHandle(valueRow,
                        DirtyType.VALUE_SERIALIZE_ERROR, valueSerialization);
                mayDirtyData = mayDirtyData || valueSerialized == null;
            }
        } else {
            valueSerialized = serializeWithDirtyHandle(valueRow, DirtyType.VALUE_SERIALIZE_ERROR, valueSerialization);
            mayDirtyData = mayDirtyData || valueSerialized == null;
        }
        if (mayDirtyData) {
            return null;
        }
        return new ProducerRecord<>(
                getTargetTopic(consumedRow),
                extractPartition(consumedRow, keySerialized, valueSerialized),
                readMetadata(consumedRow, KafkaDynamicSink.WritableMetadata.TIMESTAMP),
                keySerialized,
                valueSerialized,
                readMetadata(consumedRow, KafkaDynamicSink.WritableMetadata.HEADERS));
    }

    private byte[] serializeWithDirtyHandle(RowData consumedRow, DirtyType dirtyType,
            SerializationSchema<RowData> serialization) {
        if (!dirtyOptions.ignoreDirty()) {
            return serialization.serialize(consumedRow);
        }
        byte[] value = null;
        try {
            value = serialization.serialize(consumedRow);
        } catch (Exception e) {
            LOG.error(String.format("serialize error, raw data: %s", consumedRow.toString()), e);
            if (dirtySink != null) {
                DirtyData.Builder<Object> builder = DirtyData.builder();
                try {
                    builder.setData(consumedRow)
                            .setDirtyType(dirtyType)
                            .setLabels(dirtyOptions.getLabels())
                            .setLogTag(dirtyOptions.getLogTag())
                            .setDirtyMessage(e.getMessage())
                            .setIdentifier(dirtyOptions.getIdentifier());
                    dirtySink.invoke(builder.build());
                } catch (Exception ex) {
                    if (!dirtyOptions.ignoreSideOutputErrors()) {
                        throw new RuntimeException(ex);
                    }
                    LOG.warn("Dirty sink failed", ex);
                }
            }
            metricData.invokeDirtyWithEstimate(consumedRow);
        }
        return value;
    }

    private void serializeWithDirtyHandle(Map<String, Object> baseMap, JsonNode rootNode,
            JsonNode dataNode, List<ProducerRecord<byte[], byte[]>> values) {
        String topic = null;
        try {
            byte[] data = jsonDynamicSchemaFormat.objectMapper.writeValueAsBytes(baseMap);
            topic = jsonDynamicSchemaFormat.parse(rootNode, topicPattern);
            values.add(new ProducerRecord<>(topic,
                    extractPartition(null, null, data), null, data));
        } catch (Exception e) {
            LOG.error(String.format("serialize error, raw data: %s", baseMap), e);
            if (!dirtyOptions.ignoreDirty()) {
                throw new RuntimeException(e);
            }
            if (dirtySink != null) {
                DirtyData.Builder<Object> builder = DirtyData.builder();
                try {
                    builder.setData(dataNode)
                            .setDirtyType(DirtyType.VALUE_DESERIALIZE_ERROR)
                            .setLabels(jsonDynamicSchemaFormat.parse(rootNode, dirtyOptions.getLabels()))
                            .setLogTag(jsonDynamicSchemaFormat.parse(rootNode, dirtyOptions.getLogTag()))
                            .setDirtyMessage(e.getMessage())
                            .setIdentifier(jsonDynamicSchemaFormat.parse(rootNode, dirtyOptions.getIdentifier()));
                    dirtySink.invoke(builder.build());
                } catch (Exception ex) {
                    if (!dirtyOptions.ignoreSideOutputErrors()) {
                        throw new RuntimeException(ex);
                    }
                    LOG.warn("Dirty sink failed", ex);
                }
            }
            metricData.sendOutMetrics(topic, 1, dataNode.toString().getBytes(StandardCharsets.UTF_8).length);
        }
    }

    /**
     * Serialize for list it is used for multiple sink scenes when a record contains mulitple real records.
     *
     * @param consumedRow The consumeRow
     * @param timestamp The timestamp
     * @return List of ProducerRecord
     */
    public List<ProducerRecord<byte[], byte[]>> serializeForList(RowData consumedRow, @Nullable Long timestamp) {
        List<ProducerRecord<byte[], byte[]>> values = new ArrayList<>();
        if (!multipleSink) {
            ProducerRecord<byte[], byte[]> value = serialize(consumedRow, timestamp);
            if (value != null) {
                values.add(value);
            }
            return values;
        }
        String topic = null;
        try {
            JsonNode rootNode = jsonDynamicSchemaFormat.deserialize(consumedRow.getBinary(0));
            boolean isDDL = jsonDynamicSchemaFormat.extractDDLFlag(rootNode);
            if (isDDL) {
                values.add(new ProducerRecord<>(
                        jsonDynamicSchemaFormat.parse(rootNode, topicPattern),
                        extractPartition(consumedRow, null, consumedRow.getBinary(0)),
                        null,
                        consumedRow.getBinary(0)));
                return values;
            }
            JsonNode updateBeforeNode = jsonDynamicSchemaFormat.getUpdateBefore(rootNode);
            JsonNode updateAfterNode = jsonDynamicSchemaFormat.getUpdateAfter(rootNode);
            if (!splitRequired(updateBeforeNode, updateAfterNode)) {
                topic = jsonDynamicSchemaFormat.parse(rootNode, topicPattern);
                values.add(new ProducerRecord<>(
                        topic, extractPartition(consumedRow, null, consumedRow.getBinary(0)),
                        null, consumedRow.getBinary(0)));
            } else {
                split2JsonArray(rootNode, updateBeforeNode, updateAfterNode, values);
            }
        } catch (Exception e) {
            LOG.error(String.format("serialize error, raw data: %s", new String(consumedRow.getBinary(0))), e);
            if (!dirtyOptions.ignoreDirty()) {
                throw new RuntimeException(e);
            }
            if (dirtySink != null) {
                DirtyData.Builder<Object> builder = DirtyData.builder();
                try {
                    builder.setData(new String(consumedRow.getBinary(0)))
                            .setDirtyType(DirtyType.VALUE_DESERIALIZE_ERROR)
                            .setLabels(dirtyOptions.getLabels())
                            .setLogTag(dirtyOptions.getLogTag())
                            .setIdentifier(dirtyOptions.getIdentifier());
                    dirtySink.invoke(builder.build());
                } catch (Exception ex) {
                    if (!dirtyOptions.ignoreSideOutputErrors()) {
                        throw new RuntimeException(ex);
                    }
                    LOG.warn("Dirty sink failed", ex);
                }
            }
            metricData.sendDirtyMetrics(topic, 1, consumedRow.getBinary(0).length);
        }
        return values;
    }

    private boolean splitRequired(JsonNode updateBeforeNode, JsonNode updateAfterNode) {
        return (updateAfterNode != null && updateAfterNode.isArray()
                && updateAfterNode.size() > 1)
                || (updateBeforeNode != null && updateBeforeNode.isArray()
                        && updateBeforeNode.size() > 1);
    }

    private void split2JsonArray(JsonNode rootNode,
            JsonNode updateBeforeNode, JsonNode updateAfterNode, List<ProducerRecord<byte[], byte[]>> values) {
        Iterator<Entry<String, JsonNode>> iterator = rootNode.fields();
        Map<String, Object> baseMap = new LinkedHashMap<>();
        String updateBeforeKey = null;
        String updateAfterKey = null;
        while (iterator.hasNext()) {
            Entry<String, JsonNode> kv = iterator.next();
            if (kv.getValue() == null || (!kv.getValue().equals(updateBeforeNode) && !kv.getValue()
                    .equals(updateAfterNode))) {
                baseMap.put(kv.getKey(), kv.getValue());
                continue;
            }
            if (kv.getValue().equals(updateAfterNode)) {
                updateAfterKey = kv.getKey();
            } else if (kv.getValue().equals(updateBeforeNode)) {
                updateBeforeKey = kv.getKey();
            }
        }
        if (updateAfterNode != null) {
            for (int i = 0; i < updateAfterNode.size(); i++) {
                baseMap.put(updateAfterKey, Collections.singletonList(updateAfterNode.get(i)));
                if (updateBeforeNode != null && updateBeforeNode.size() > i) {
                    baseMap.put(updateBeforeKey, Collections.singletonList(updateBeforeNode.get(i)));
                } else if (updateBeforeKey != null) {
                    baseMap.remove(updateBeforeKey);
                }
                serializeWithDirtyHandle(baseMap, rootNode, updateAfterNode.get(i), values);
            }
        } else {
            // In general, it will not run to this branch
            for (int i = 0; i < updateBeforeNode.size(); i++) {
                baseMap.put(updateBeforeKey, Collections.singletonList(updateBeforeNode.get(i)));
                serializeWithDirtyHandle(baseMap, rootNode, updateBeforeNode.get(i), values);
            }
        }
    }

    @Override
    public void setParallelInstanceId(int parallelInstanceId) {
        this.parallelInstanceId = parallelInstanceId;
    }

    @Override
    public void setNumParallelInstances(int numParallelInstances) {
        this.numParallelInstances = numParallelInstances;
    }

    @Override
    public void setPartitions(int[] partitions) {
        this.partitions = partitions;
    }

    @Override
    public String getTargetTopic(RowData element) {
        if (multipleSink) {
            try {
                // Extract the index '0' as the raw data is determined by the Raw format:
                // The Raw format allows to read and write raw (byte based) values as a single column
                return jsonDynamicSchemaFormat.parse(element.getBinary(0), topicPattern);
            } catch (Exception e) {
                // Ignore the parse error and it will return the default topic final.
                LOG.warn("parse dynamic topic error", e);
            }
        }
        return topic;
    }

    @SuppressWarnings("unchecked")
    private <T> T readMetadata(RowData consumedRow, KafkaDynamicSink.WritableMetadata metadata) {
        final int pos = metadataPositions[metadata.ordinal()];
        if (pos < 0) {
            return null;
        }
        return (T) metadata.converter.read(consumedRow, pos);
    }

    private Integer extractPartition(
            RowData consumedRow, @Nullable byte[] keySerialized, byte[] valueSerialized) {
        if (partitioner != null) {
            return partitioner.partition(
                    consumedRow, keySerialized, valueSerialized, topic, partitions);
        }
        return null;
    }

    // --------------------------------------------------------------------------------------------

    interface MetadataConverter extends Serializable {

        Object read(RowData consumedRow, int pos);
    }
}
