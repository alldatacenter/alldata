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

package org.apache.inlong.sort.tubemq.table;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.connector.source.abilities.SupportsReadingMetadata;
import org.apache.flink.table.connector.source.abilities.SupportsWatermarkPushDown;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.utils.LogicalTypeChecks;
import org.apache.flink.table.types.utils.DataTypeUtils;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.tubemq.FlinkTubeMQConsumer;
import org.apache.inlong.sort.tubemq.table.DynamicTubeMQDeserializationSchema.MetadataConverter;
import org.apache.inlong.tubemq.corebase.Message;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * .
 */
public class TubeMQTableSource implements ScanTableSource, SupportsReadingMetadata, SupportsWatermarkPushDown {

    private static final String VALUE_METADATA_PREFIX = "value.";

    // --------------------------------------------------------------------------------------------
    // Mutable attributes
    // --------------------------------------------------------------------------------------------
    /**
     * Data type to configure the formats.
     */
    private final DataType physicalDataType;
    /**
     * Format for decoding values from TubeMQ.
     */
    private final DecodingFormat<DeserializationSchema<RowData>> valueDecodingFormat;

    // -------------------------------------------------------------------
    /**
     * The address of TubeMQ master, format eg: 127.0.0.1:8715,127.0.0.2:8715.
     */
    private final String masterAddress;
    /**
     * The TubeMQ topic name.
     */
    private final String topic;
    /**
     * The TubeMQ tid filter collection.
     */
    private final TreeSet<String> tidSet;
    /**
     * The TubeMQ consumer group name.
     */
    private final String consumerGroup;
    /**
     * The parameters collection for TubeMQ consumer.
     */
    private final Configuration configuration;
    /**
     * The TubeMQ session key.
     */
    private final String sessionKey;
    /**
     * Field name of the processing time attribute, null if no processing time
     * field is defined.
     */
    private final Optional<String> proctimeAttribute;
    /**
     * status of error
     */
    private final boolean ignoreErrors;
    /**
     * The InLong inner format.
     */
    private final boolean innerFormat;
    /**
     * Data type that describes the final output of the source.
     */
    protected DataType producedDataType;
    /**
     * Metadata that is appended at the end of a physical source row.
     */
    protected List<String> metadataKeys;
    /**
     * Watermark strategy that is used to generate per-partition watermark.
     */
    @Nullable
    private WatermarkStrategy<RowData> watermarkStrategy;

    public TubeMQTableSource(DataType physicalDataType,
            DecodingFormat<DeserializationSchema<RowData>> valueDecodingFormat,
            String masterAddress, String topic,
            TreeSet<String> tidSet, String consumerGroup, String sessionKey,
            Configuration configuration, @Nullable WatermarkStrategy<RowData> watermarkStrategy,
            Optional<String> proctimeAttribute, Boolean ignoreErrors, Boolean innerFormat) {

        Preconditions.checkNotNull(physicalDataType, "Physical data type must not be null.");
        Preconditions.checkNotNull(valueDecodingFormat, "The deserialization schema must not be null.");
        Preconditions.checkNotNull(masterAddress, "The master address must not be null.");
        Preconditions.checkNotNull(topic, "The topic must not be null.");
        Preconditions.checkNotNull(tidSet, "The tid set must not be null.");
        Preconditions.checkNotNull(consumerGroup, "The consumer group must not be null.");
        Preconditions.checkNotNull(configuration, "The configuration must not be null.");

        this.physicalDataType = physicalDataType;
        this.producedDataType = physicalDataType;
        this.metadataKeys = Collections.emptyList();
        this.valueDecodingFormat = valueDecodingFormat;
        this.masterAddress = masterAddress;
        this.topic = topic;
        this.tidSet = tidSet;
        this.consumerGroup = consumerGroup;
        this.sessionKey = sessionKey;
        this.configuration = configuration;
        this.watermarkStrategy = watermarkStrategy;
        this.proctimeAttribute = proctimeAttribute;
        this.ignoreErrors = ignoreErrors;
        this.innerFormat = innerFormat;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        return valueDecodingFormat.getChangelogMode();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext context) {
        final LogicalType physicalType = physicalDataType.getLogicalType();
        final int physicalFieldCount = LogicalTypeChecks.getFieldCount(physicalType);
        final IntStream physicalFields = IntStream.range(0, physicalFieldCount);
        final DeserializationSchema<RowData> deserialization = createDeserialization(context,
                valueDecodingFormat, physicalFields.toArray(), null);

        final TypeInformation<RowData> producedTypeInfo = context.createTypeInformation(physicalDataType);

        final FlinkTubeMQConsumer<RowData> tubeMQConsumer = createTubeMQConsumer(deserialization, producedTypeInfo,
                ignoreErrors);

        return SourceFunctionProvider.of(tubeMQConsumer, false);
    }

    @Override
    public DynamicTableSource copy() {
        return new TubeMQTableSource(
                physicalDataType, valueDecodingFormat, masterAddress,
                topic, tidSet, consumerGroup, sessionKey, configuration,
                watermarkStrategy, proctimeAttribute, ignoreErrors, innerFormat);
    }

    @Override
    public String asSummaryString() {
        return "TubeMQ table source";
    }

    @Override
    public Map<String, DataType> listReadableMetadata() {
        final Map<String, DataType> metadataMap = new LinkedHashMap<>();
        valueDecodingFormat
                .listReadableMetadata()
                .forEach((key, value) -> metadataMap.put(VALUE_METADATA_PREFIX + key, value));

        // add connector metadata
        Stream.of(ReadableMetadata.values())
                .forEachOrdered(m -> metadataMap.putIfAbsent(m.key, m.dataType));

        return metadataMap;
    }

    @Override
    public void applyReadableMetadata(List<String> metadataKeys, DataType producedDataType) {
        // separate connector and format metadata
        final List<String> formatMetadataKeys =
                metadataKeys.stream()
                        .filter(k -> k.startsWith(VALUE_METADATA_PREFIX))
                        .collect(Collectors.toList());
        final List<String> connectorMetadataKeys = new ArrayList<>(metadataKeys);
        connectorMetadataKeys.removeAll(formatMetadataKeys);

        // push down format metadata
        final Map<String, DataType> formatMetadata = valueDecodingFormat.listReadableMetadata();
        if (formatMetadata.size() > 0) {
            final List<String> requestedFormatMetadataKeys =
                    formatMetadataKeys.stream()
                            .map(k -> k.substring(VALUE_METADATA_PREFIX.length()))
                            .collect(Collectors.toList());
            valueDecodingFormat.applyReadableMetadata(requestedFormatMetadataKeys);
        }
        this.metadataKeys = connectorMetadataKeys;
        this.producedDataType = producedDataType;

    }

    @Override
    public void applyWatermark(WatermarkStrategy<RowData> watermarkStrategy) {
        this.watermarkStrategy = watermarkStrategy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TubeMQTableSource that = (TubeMQTableSource) o;
        return Objects.equals(physicalDataType, that.physicalDataType)
                && Objects.equals(valueDecodingFormat, that.valueDecodingFormat)
                && Objects.equals(masterAddress, that.masterAddress)
                && Objects.equals(topic, that.topic)
                && Objects.equals(String.valueOf(tidSet), String.valueOf(that.tidSet))
                && Objects.equals(consumerGroup, that.consumerGroup)
                && Objects.equals(proctimeAttribute, that.proctimeAttribute)
                && Objects.equals(watermarkStrategy, that.watermarkStrategy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                physicalDataType,
                valueDecodingFormat,
                masterAddress,
                topic,
                tidSet,
                consumerGroup,
                configuration,
                watermarkStrategy,
                proctimeAttribute);
    }

    // --------------------------------------------------------------------------------------------
    // Metadata handling
    // --------------------------------------------------------------------------------------------

    @Nullable
    private DeserializationSchema<RowData> createDeserialization(
            DynamicTableSource.Context context,
            @Nullable DecodingFormat<DeserializationSchema<RowData>> format,
            int[] projection,
            @Nullable String prefix) {
        if (format == null) {
            return null;
        }
        DataType physicalFormatDataType = DataTypeUtils.projectRow(this.physicalDataType, projection);
        if (prefix != null) {
            physicalFormatDataType = DataTypeUtils.stripRowPrefix(physicalFormatDataType, prefix);
        }
        return format.createRuntimeDecoder(context, physicalFormatDataType);
    }

    protected FlinkTubeMQConsumer<RowData> createTubeMQConsumer(
            DeserializationSchema<RowData> deserialization,
            TypeInformation<RowData> producedTypeInfo,
            boolean ignoreErrors) {
        final MetadataConverter[] metadataConverters =
                metadataKeys.stream()
                        .map(k -> Stream.of(ReadableMetadata.values())
                                .filter(rm -> rm.key.equals(k))
                                .findFirst()
                                .orElseThrow(IllegalStateException::new))
                        .map(m -> m.converter)
                        .toArray(MetadataConverter[]::new);
        final DeserializationSchema<RowData> tubeMQDeserializer = new DynamicTubeMQDeserializationSchema(
                deserialization, metadataConverters, producedTypeInfo, ignoreErrors);

        final FlinkTubeMQConsumer<RowData> tubeMQConsumer = new FlinkTubeMQConsumer(masterAddress, topic, tidSet,
                consumerGroup, tubeMQDeserializer, configuration, sessionKey, innerFormat);
        return tubeMQConsumer;
    }

    // --------------------------------------------------------------------------------------------
    // Metadata handling
    // --------------------------------------------------------------------------------------------

    enum ReadableMetadata {

        TOPIC(
                "topic",
                DataTypes.STRING().notNull(),
                new MetadataConverter() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object read(Message msg) {
                        return StringData.fromString(msg.getTopic());
                    }
                });

        final String key;

        final DataType dataType;

        final MetadataConverter converter;

        ReadableMetadata(String key, DataType dataType, MetadataConverter converter) {
            this.key = key;
            this.dataType = dataType;
            this.converter = converter;
        }
    }
}
