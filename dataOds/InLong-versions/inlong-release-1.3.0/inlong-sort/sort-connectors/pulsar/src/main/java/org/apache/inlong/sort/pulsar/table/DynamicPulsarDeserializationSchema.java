/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.inlong.sort.pulsar.table;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.RuntimeContextInitializationContextAdapters;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.connectors.pulsar.table.PulsarDynamicTableSource;
import org.apache.flink.streaming.util.serialization.FlinkSchema;
import org.apache.flink.streaming.util.serialization.PulsarDeserializationSchema;
import org.apache.flink.streaming.util.serialization.ThreadSafeDeserializationSchema;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.DeserializationException;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Collector;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.audit.AuditImp;
import org.apache.inlong.sort.base.metric.SourceMetricData;
import org.apache.inlong.sort.pulsar.withoutadmin.CallbackCollector;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Schema;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.apache.inlong.sort.base.Constants.DELIMITER;

/**
 * A specific {@link PulsarDeserializationSchema} for {@link PulsarDynamicTableSource}.
 */
class DynamicPulsarDeserializationSchema implements PulsarDeserializationSchema<RowData> {

    private static final long serialVersionUID = 1L;
    private static final ThreadLocal<SimpleCollector<RowData>> tlsCollector =
            new ThreadLocal<SimpleCollector<RowData>>() {
                @Override
                public SimpleCollector initialValue() {
                    return new SimpleCollector();
                }
            };
    @Nullable
    private final DeserializationSchema<RowData> keyDeserialization;
    private final DeserializationSchema<RowData> valueDeserialization;
    private final boolean hasMetadata;
    private final OutputProjectionCollector outputCollector;
    private final TypeInformation<RowData> producedTypeInfo;
    private final boolean upsertMode;
    private SourceMetricData sourceMetricData;
    private String inlongMetric;
    private String auditHostAndPorts;

    private AuditImp auditImp;

    private String inlongGroupId;

    private String inlongStreamId;

    DynamicPulsarDeserializationSchema(
            int physicalArity,
            @Nullable DeserializationSchema<RowData> keyDeserialization,
            int[] keyProjection,
            DeserializationSchema<RowData> valueDeserialization,
            int[] valueProjection,
            boolean hasMetadata,
            MetadataConverter[] metadataConverters,
            TypeInformation<RowData> producedTypeInfo,
            boolean upsertMode,
            String inlongMetric, String auditHostAndPorts) {
        if (upsertMode) {
            Preconditions.checkArgument(
                    keyDeserialization != null && keyProjection.length > 0,
                    "Key must be set in upsert mode for deserialization schema.");
        }
        this.keyDeserialization = ThreadSafeDeserializationSchema.of(keyDeserialization);
        this.valueDeserialization = ThreadSafeDeserializationSchema.of(valueDeserialization);
        this.hasMetadata = hasMetadata;
        this.outputCollector = new OutputProjectionCollector(
                physicalArity,
                keyProjection,
                valueProjection,
                metadataConverters,
                upsertMode);
        this.producedTypeInfo = producedTypeInfo;
        this.upsertMode = upsertMode;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
    }

    @Override
    public void open(DeserializationSchema.InitializationContext context) throws Exception {
        if (keyDeserialization != null) {
            keyDeserialization.open(context);
        }
        valueDeserialization.open(context);

        if (inlongMetric != null && !inlongMetric.isEmpty()) {
            String[] inlongMetricArray = inlongMetric.split(DELIMITER);
            inlongGroupId = inlongMetricArray[0];
            inlongStreamId = inlongMetricArray[1];
            String nodeId = inlongMetricArray[2];
            sourceMetricData = new SourceMetricData(inlongGroupId, inlongStreamId, nodeId, getMetricGroup(context));
            sourceMetricData.registerMetricsForNumBytesIn();
            sourceMetricData.registerMetricsForNumBytesInPerSecond();
            sourceMetricData.registerMetricsForNumRecordsIn();
            sourceMetricData.registerMetricsForNumRecordsInPerSecond();
        }

        if (auditHostAndPorts != null) {
            AuditImp.getInstance().setAuditProxy(new HashSet<>(Arrays.asList(auditHostAndPorts.split(DELIMITER))));
            auditImp = AuditImp.getInstance();
        }

    }

    /**
     * reflect get metricGroup
     *
     * @param context Contextual information that can be used during initialization.
     * @return metric group that can be used to register new metrics with Flink and to create a nested hierarchy based
     *         on the group names.
     */
    private MetricGroup getMetricGroup(DeserializationSchema.InitializationContext context)
            throws NoSuchFieldException, IllegalAccessException {
        MetricGroup metricGroup;
        String className = "RuntimeContextDeserializationInitializationContextAdapter";
        String fieldName = "runtimeContext";
        Class runtimeContextDeserializationInitializationContextAdapter = null;
        Class[] innerClazz = RuntimeContextInitializationContextAdapters.class.getDeclaredClasses();
        for (Class clazz : innerClazz) {
            int mod = clazz.getModifiers();
            if (Modifier.isPrivate(mod)) {
                if (className.equalsIgnoreCase(clazz.getSimpleName())) {
                    runtimeContextDeserializationInitializationContextAdapter = clazz;
                    break;
                }
            }
        }
        if (runtimeContextDeserializationInitializationContextAdapter != null) {
            Field field = runtimeContextDeserializationInitializationContextAdapter.getDeclaredField(fieldName);
            field.setAccessible(true);
            RuntimeContext runtimeContext = (RuntimeContext) field.get(context);
            metricGroup = runtimeContext.getMetricGroup();
        } else {
            metricGroup = context.getMetricGroup();
        }
        return metricGroup;
    }

    @Override
    public boolean isEndOfStream(RowData nextElement) {
        return false;
    }

    @Override
    public RowData deserialize(Message<RowData> message) throws IOException {
        final SimpleCollector<RowData> collector = tlsCollector.get();
        deserialize(message, collector);
        return collector.takeRecord();
    }

    @Override
    public void deserialize(Message<RowData> message, Collector<RowData> collector) throws IOException {
        // shortcut in case no output projection is required,
        // also not for a cartesian product with the keys
        if (keyDeserialization == null && !hasMetadata) {
            valueDeserialization.deserialize(message.getData(), new CallbackCollector<>(inputRow -> {
                sourceMetricData.outputMetrics(1L, inputRow.toString().getBytes(StandardCharsets.UTF_8).length);
                collector.collect(inputRow);
            }));
            return;
        }
        BufferingCollector keyCollector = new BufferingCollector();

        // buffer key(s)
        if (keyDeserialization != null) {
            keyDeserialization.deserialize(message.getKeyBytes(), keyCollector);
        }

        // project output while emitting values
        outputCollector.inputMessage = message;
        outputCollector.physicalKeyRows = keyCollector.buffer;
        outputCollector.outputCollector = collector;
        if ((message.getData() == null || message.getData().length == 0) && upsertMode) {
            // collect tombstone messages in upsert mode by hand
            outputCollector.collect(null);
        } else {
            valueDeserialization.deserialize(message.getData(), new CallbackCollector<>(inputRow -> {
                sourceMetricData.outputMetrics(1L, inputRow.toString().getBytes(StandardCharsets.UTF_8).length);
                outputCollector.collect(inputRow);
            }));
        }

        keyCollector.buffer.clear();
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        return producedTypeInfo;
    }

    @Override
    public Schema<RowData> getSchema() {
        return new FlinkSchema<>(Schema.BYTES.getSchemaInfo(), null, valueDeserialization);
    }

    // --------------------------------------------------------------------------------------------

    interface MetadataConverter extends Serializable {

        Object read(Message<?> message);
    }

    // --------------------------------------------------------------------------------------------

    private static final class BufferingCollector implements Collector<RowData>, Serializable {

        private static final long serialVersionUID = 1L;

        private final List<RowData> buffer = new ArrayList<>();

        @Override
        public void collect(RowData record) {
            buffer.add(record);
        }

        @Override
        public void close() {
            // nothing to do
        }
    }

    private static class SimpleCollector<T> implements Collector<T> {

        private T record;

        @Override
        public void collect(T record) {
            this.record = record;
        }

        @Override
        public void close() {

        }

        private T getRecord() {
            return record;
        }

        private T takeRecord() {
            T result = record;
            reset();
            return result;
        }

        private void reset() {
            record = null;
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Emits a row with key, value, and metadata fields.
     *
     * <p>The collector is able to handle the following kinds of keys:
     * <ul>
     *     <li>No key is used.
     *     <li>A key is used.
     *     <li>The deserialization schema emits multiple keys.
     *     <li>Keys and values have overlapping fields.
     *     <li>Keys are used and value is null.
     * </ul>
     */
    private static final class OutputProjectionCollector implements Collector<RowData>, Serializable {

        private static final long serialVersionUID = 1L;

        private final int physicalArity;

        private final int[] keyProjection;

        private final int[] valueProjection;

        private final MetadataConverter[] metadataConverters;

        private final boolean upsertMode;

        private transient Message<?> inputMessage;

        private transient List<RowData> physicalKeyRows;

        private transient Collector<RowData> outputCollector;

        OutputProjectionCollector(
                int physicalArity,
                int[] keyProjection,
                int[] valueProjection,
                MetadataConverter[] metadataConverters,
                boolean upsertMode) {
            this.physicalArity = physicalArity;
            this.keyProjection = keyProjection;
            this.valueProjection = valueProjection;
            this.metadataConverters = metadataConverters;
            this.upsertMode = upsertMode;
        }

        @Override
        public void collect(RowData physicalValueRow) {
            // no key defined
            if (keyProjection.length == 0) {
                emitRow(null, (GenericRowData) physicalValueRow);
                return;
            }

            // otherwise emit a value for each key
            for (RowData physicalKeyRow : physicalKeyRows) {
                emitRow((GenericRowData) physicalKeyRow, (GenericRowData) physicalValueRow);
            }
        }

        @Override
        public void close() {
            // nothing to do
        }

        private void emitRow(@Nullable GenericRowData physicalKeyRow, @Nullable GenericRowData physicalValueRow) {
            final RowKind rowKind;
            if (physicalValueRow == null) {
                if (upsertMode) {
                    rowKind = RowKind.DELETE;
                } else {
                    throw new DeserializationException(
                            "Invalid null value received in non-upsert mode. "
                                    + "Could not to set row kind for output record.");
                }
            } else {
                rowKind = physicalValueRow.getRowKind();
            }

            final int metadataArity = metadataConverters.length;
            final GenericRowData producedRow = new GenericRowData(
                    rowKind,
                    physicalArity + metadataArity);

            if (physicalValueRow != null) {
                for (int valuePos = 0; valuePos < valueProjection.length; valuePos++) {
                    producedRow.setField(valueProjection[valuePos], physicalValueRow.getField(valuePos));
                }
            }

            for (int keyPos = 0; keyPos < keyProjection.length; keyPos++) {
                assert physicalKeyRow != null;
                producedRow.setField(keyProjection[keyPos], physicalKeyRow.getField(keyPos));
            }

            for (int metadataPos = 0; metadataPos < metadataArity; metadataPos++) {
                producedRow.setField(physicalArity + metadataPos, metadataConverters[metadataPos].read(inputMessage));
            }

            outputCollector.collect(producedRow);
        }
    }
}
