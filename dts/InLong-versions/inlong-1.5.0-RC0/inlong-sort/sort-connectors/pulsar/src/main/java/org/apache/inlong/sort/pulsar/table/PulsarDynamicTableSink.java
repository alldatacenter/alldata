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

package org.apache.inlong.sort.pulsar.table;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.pulsar.FlinkPulsarSink;
import org.apache.flink.streaming.connectors.pulsar.internal.PulsarClientUtils;
import org.apache.flink.streaming.connectors.pulsar.internal.PulsarOptions;
import org.apache.flink.streaming.connectors.pulsar.table.PulsarSinkSemantic;
import org.apache.flink.streaming.util.serialization.PulsarSerializationSchema;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.connector.sink.abilities.SupportsWritingMetadata;
import org.apache.flink.table.data.ArrayData;
import org.apache.flink.table.data.MapData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.utils.DataTypeUtils;
import org.apache.flink.util.Preconditions;
import org.apache.pulsar.client.api.MessageRouter;
import org.apache.pulsar.client.impl.conf.ClientConfigurationData;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * pulsar dynamic table sink.
 */
public class PulsarDynamicTableSink implements DynamicTableSink, SupportsWritingMetadata {

    // --------------------------------------------------------------------------------------------
    // Mutable attributes
    // --------------------------------------------------------------------------------------------

    /**
     * Data type to configure the formats.
     */
    protected final DataType physicalDataType;

    // --------------------------------------------------------------------------------------------
    // Format attributes
    // --------------------------------------------------------------------------------------------
    /**
     * The pulsar topic to write to.
     */
    protected final String topic;
    protected final String serviceUrl;
    protected final String adminUrl;
    /**
     * Properties for the pulsar producer.
     */
    protected final Properties properties;
    /**
     * Optional format for encoding keys to Pulsar.
     */
    protected final @Nullable EncodingFormat<SerializationSchema<RowData>> keyEncodingFormat;
    /**
     * Sink format for encoding records to pulsar.
     */
    protected final EncodingFormat<SerializationSchema<RowData>> valueEncodingFormat;
    /**
     * Indices that determine the key fields and the source position in the consumed row.
     */
    protected final int[] keyProjection;
    /**
     * Indices that determine the value fields and the source position in the consumed row.
     */
    protected final int[] valueProjection;
    /**
     * Prefix that needs to be removed from fields when constructing the physical data type.
     */
    protected final @Nullable String keyPrefix;
    /**
     * Flag to determine sink mode. In upsert mode sink transforms the delete/update-before message to
     * tombstone message.
     */
    protected final boolean upsertMode;
    /**
     * Parallelism of the physical Pulsar producer.
     **/
    protected final @Nullable Integer parallelism;
    /**
     * Sink commit semantic.
     */
    protected final PulsarSinkSemantic semantic;
    private final String formatType;
    private final MessageRouter messageRouter;
    /**
     * Metadata that is appended at the end of a physical sink row.
     */
    protected List<String> metadataKeys;

    protected PulsarDynamicTableSink(
            String serviceUrl,
            String adminUrl,
            String topic,
            DataType physicalDataType,
            Properties properties,
            @Nullable EncodingFormat<SerializationSchema<RowData>> keyEncodingFormat,
            EncodingFormat<SerializationSchema<RowData>> valueEncodingFormat,
            int[] keyProjection,
            int[] valueProjection,
            @Nullable String keyPrefix,
            PulsarSinkSemantic semantic,
            String formatType,
            boolean upsertMode,
            @Nullable Integer parallelism,
            @Nullable MessageRouter messageRouter) {
        this.serviceUrl = Preconditions.checkNotNull(serviceUrl, "serviceUrl data type must not be null.");
        this.adminUrl = Preconditions.checkNotNull(adminUrl, "adminUrl data type must not be null.");
        this.topic = Preconditions.checkNotNull(topic, "Topic must not be null.");
        this.physicalDataType = Preconditions.checkNotNull(physicalDataType, "Consumed data type must not be null.");
        // Mutable attributes
        this.metadataKeys = Collections.emptyList();
        this.properties = Preconditions.checkNotNull(properties, "Properties must not be null.");
        this.keyEncodingFormat = keyEncodingFormat;
        this.valueEncodingFormat = Preconditions.checkNotNull(valueEncodingFormat, "Encoding format must not be null.");
        this.keyProjection = Preconditions.checkNotNull(keyProjection, "Key projection must not be null.");
        this.valueProjection = Preconditions.checkNotNull(valueProjection, "Value projection must not be null.");
        this.keyPrefix = keyPrefix;
        this.semantic = Preconditions.checkNotNull(semantic, "Semantic must not be null.");
        this.formatType = Preconditions.checkNotNull(formatType, "FormatType must not be null.");
        this.upsertMode = upsertMode;
        this.parallelism = parallelism;
        this.messageRouter = messageRouter;
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        return this.valueEncodingFormat.getChangelogMode();
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        final SerializationSchema<RowData> keySerialization =
                createSerialization(context, keyEncodingFormat, keyProjection, keyPrefix);

        final SerializationSchema<RowData> valueSerialization =
                createSerialization(context, valueEncodingFormat, valueProjection, null);

        final PulsarSerializationSchema<RowData> pulsarSerializer =
                createPulsarSerializer(keySerialization, valueSerialization);

        final SinkFunction<RowData> pulsarSink = createPulsarSink(
                this.topic,
                this.properties,
                pulsarSerializer);

        return SinkFunctionProvider.of(pulsarSink, parallelism);
    }

    private PulsarSerializationSchema<RowData> createPulsarSerializer(SerializationSchema<RowData> keySerialization,
            SerializationSchema<RowData> valueSerialization) {
        final List<LogicalType> physicalChildren = physicalDataType.getLogicalType().getChildren();

        final RowData.FieldGetter[] keyFieldGetters = Arrays.stream(keyProjection)
                .mapToObj(targetField -> RowData.createFieldGetter(physicalChildren.get(targetField), targetField))
                .toArray(RowData.FieldGetter[]::new);

        final RowData.FieldGetter[] valueFieldGetters = Arrays.stream(valueProjection)
                .mapToObj(targetField -> RowData.createFieldGetter(physicalChildren.get(targetField), targetField))
                .toArray(RowData.FieldGetter[]::new);

        // determine the positions of metadata in the consumed row
        final int[] metadataPositions = Stream.of(WritableMetadata.values())
                .mapToInt(m -> {
                    final int pos = metadataKeys.indexOf(m.key);
                    if (pos < 0) {
                        return -1;
                    }
                    return physicalChildren.size() + pos;
                })
                .toArray();

        // check if metadata is used at all
        final boolean hasMetadata = metadataKeys.size() > 0;

        final long delayMilliseconds = Optional.ofNullable(this.properties
                .getProperty(PulsarOptions.SEND_DELAY_MILLISECONDS, "0"))
                .filter(StringUtils::isNumeric)
                .map(Long::valueOf)
                .orElse(0L);

        return new DynamicPulsarSerializationSchema(
                keySerialization,
                valueSerialization,
                keyFieldGetters,
                valueFieldGetters,
                hasMetadata,
                metadataPositions,
                upsertMode,
                DataTypeUtils.projectRow(physicalDataType, valueProjection),
                formatType,
                delayMilliseconds);
    }

    private SinkFunction<RowData> createPulsarSink(String topic, Properties properties,
            PulsarSerializationSchema<RowData> pulsarSerializer) {
        final ClientConfigurationData configurationData = PulsarClientUtils
                .newClientConf(serviceUrl, properties);
        return new FlinkPulsarSink<RowData>(
                adminUrl,
                Optional.ofNullable(topic),
                configurationData,
                properties,
                pulsarSerializer,
                messageRouter,
                PulsarSinkSemantic.valueOf(semantic.toString()));
    }

    public MessageRouter getMessageRouter() {
        return messageRouter;
    }

    private @Nullable SerializationSchema<RowData> createSerialization(
            Context context,
            @Nullable EncodingFormat<SerializationSchema<RowData>> format,
            int[] projection,
            @Nullable String prefix) {
        if (format == null) {
            return null;
        }
        DataType physicalFormatDataType = DataTypeUtils.projectRow(this.physicalDataType, projection);
        if (prefix != null) {
            physicalFormatDataType = DataTypeUtils.stripRowPrefix(physicalFormatDataType, prefix);
        }
        return format.createRuntimeEncoder(context, physicalFormatDataType);
    }

    @Override
    public DynamicTableSink copy() {
        final PulsarDynamicTableSink copy = new PulsarDynamicTableSink(
                this.serviceUrl,
                this.adminUrl,
                this.topic,
                this.physicalDataType,
                this.properties,
                this.keyEncodingFormat,
                this.valueEncodingFormat,
                this.keyProjection,
                this.valueProjection,
                this.keyPrefix,
                this.semantic,
                this.formatType,
                this.upsertMode,
                this.parallelism,
                this.messageRouter);
        copy.metadataKeys = metadataKeys;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PulsarDynamicTableSink)) {
            return false;
        }
        PulsarDynamicTableSink that = (PulsarDynamicTableSink) o;
        return upsertMode == that.upsertMode && Objects.equals(metadataKeys, that.metadataKeys)
                && Objects.equals(physicalDataType, that.physicalDataType)
                && Objects.equals(topic, that.topic) && Objects.equals(serviceUrl, that.serviceUrl)
                && Objects.equals(adminUrl, that.adminUrl)
                && Objects.equals(properties, that.properties)
                && Objects.equals(keyEncodingFormat, that.keyEncodingFormat)
                && Objects.equals(valueEncodingFormat, that.valueEncodingFormat)
                && Arrays.equals(keyProjection, that.keyProjection)
                && Arrays.equals(valueProjection, that.valueProjection)
                && Objects.equals(keyPrefix, that.keyPrefix)
                && Objects.equals(parallelism, that.parallelism) && semantic == that.semantic
                && Objects.equals(formatType, that.formatType)
                && Objects.equals(messageRouter, that.messageRouter);
    }

    @Override
    public int hashCode() {
        int result =
                Objects.hash(metadataKeys, physicalDataType, topic, serviceUrl, adminUrl, properties, keyEncodingFormat,
                        valueEncodingFormat, keyPrefix, upsertMode, parallelism, semantic, formatType, messageRouter);
        result = 31 * result + Arrays.hashCode(keyProjection);
        result = 31 * result + Arrays.hashCode(valueProjection);
        return result;
    }

    @Override
    public String asSummaryString() {
        return "Pulsar dynamic table sink";
    }

    @Override
    public Map<String, DataType> listWritableMetadata() {
        final Map<String, DataType> metadataMap = new LinkedHashMap<>();
        Stream.of(WritableMetadata.values()).forEachOrdered(m -> metadataMap.put(m.key, m.dataType));
        return metadataMap;
    }

    @Override
    public void applyWritableMetadata(List<String> metadataKeys, DataType consumedDataType) {
        this.metadataKeys = metadataKeys;
    }

    enum WritableMetadata {

        PROPERTIES(
                "properties",
                // key and value of the map are nullable to make handling easier in queries
                DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.STRING().nullable()).nullable(),
                (row, pos) -> {
                    if (row.isNullAt(pos)) {
                        return null;
                    }
                    final MapData map = row.getMap(pos);
                    final ArrayData keyArray = map.keyArray();
                    final ArrayData valueArray = map.valueArray();

                    final Properties properties = new Properties();
                    for (int i = 0; i < keyArray.size(); i++) {
                        if (!keyArray.isNullAt(i) && !valueArray.isNullAt(i)) {
                            final String key = keyArray.getString(i).toString();
                            final String value = valueArray.getString(i).toString();
                            properties.put(key, value);
                        }
                    }
                    return properties;
                }),

        EVENT_TIME(
                "eventTime",
                DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).nullable(),
                (row, pos) -> {
                    if (row.isNullAt(pos)) {
                        return null;
                    }
                    return row.getTimestamp(pos, 3).getMillisecond();
                });
        final String key;

        final DataType dataType;

        final DynamicPulsarSerializationSchema.MetadataConverter converter;

        WritableMetadata(String key, DataType dataType, DynamicPulsarSerializationSchema.MetadataConverter converter) {
            this.key = key;
            this.dataType = dataType;
            this.converter = converter;
        }
    }
}
