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

package org.apache.inlong.sort.kafka.table;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumerBase;
import org.apache.flink.streaming.connectors.kafka.config.StartupMode;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner;
import org.apache.flink.streaming.connectors.kafka.table.KafkaOptions;
import org.apache.flink.streaming.connectors.kafka.table.KafkaSinkSemantic;
import org.apache.flink.streaming.connectors.kafka.table.SinkBufferFlushMode;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.format.Format;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DeserializationFormatFactory;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.factories.FactoryUtil.TableFactoryHelper;
import org.apache.flink.table.factories.SerializationFormatFactory;
import org.apache.flink.table.formats.raw.RawFormatSerializationSchema;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.VarBinaryType;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.dirty.utils.DirtySinkFactoryUtils;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.kafka.KafkaDynamicSink;
import org.apache.inlong.sort.kafka.partitioner.RawDataHashPartitioner;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.KEY_FIELDS;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.KEY_FIELDS_PREFIX;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.KEY_FORMAT;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.PROPERTIES_PREFIX;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.PROPS_BOOTSTRAP_SERVERS;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.PROPS_GROUP_ID;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.SCAN_STARTUP_MODE;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.SCAN_STARTUP_SPECIFIC_OFFSETS;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.SCAN_STARTUP_TIMESTAMP_MILLIS;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.SCAN_TOPIC_PARTITION_DISCOVERY;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.SINK_PARTITIONER;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.SINK_PARTITIONER_VALUE_ROUND_ROBIN;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.SINK_SEMANTIC;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.SINK_SEMANTIC_VALUE_AT_LEAST_ONCE;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.SINK_SEMANTIC_VALUE_EXACTLY_ONCE;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.SINK_SEMANTIC_VALUE_NONE;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.StartupOptions;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.TOPIC;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.TOPIC_PATTERN;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.VALUE_FIELDS_INCLUDE;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.VALUE_FORMAT;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.autoCompleteSchemaRegistrySubject;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.createKeyFormatProjection;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.createValueFormatProjection;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.getKafkaProperties;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.getSinkSemantic;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.getStartupOptions;
import static org.apache.flink.streaming.connectors.kafka.table.KafkaOptions.validateTableSourceOptions;
import static org.apache.flink.table.factories.FactoryUtil.FORMAT;
import static org.apache.flink.table.factories.FactoryUtil.SINK_PARALLELISM;
import static org.apache.inlong.sort.base.Constants.DIRTY_PREFIX;
import static org.apache.inlong.sort.base.Constants.INLONG_AUDIT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_FORMAT;
import static org.apache.inlong.sort.kafka.table.KafkaOptions.KAFKA_IGNORE_ALL_CHANGELOG;

/**
 * Copy from org.apache.flink:flink-connector-kafka_2.11:1.13.5
 * <p>
 * Factory for creating configured instances of {@link KafkaDynamicSource} and {@link
 * KafkaDynamicSink}.We modify KafkaDynamicTableSink to support format metadata writeable.</p>
 */
@Internal
public class KafkaDynamicTableFactory implements DynamicTableSourceFactory, DynamicTableSinkFactory {

    public static final String IDENTIFIER = "kafka-inlong";

    public static final String SINK_PARTITIONER_VALUE_RAW_HASH = "raw-hash";

    public static final ConfigOption<String> SINK_MULTIPLE_PARTITION_PATTERN =
            ConfigOptions.key("sink.multiple.partition-pattern")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("option 'sink.multiple.partition-pattern' used when the partitioner is raw-hash.");

    private static final Set<String> SINK_SEMANTIC_ENUMS =
            new HashSet<>(
                    Arrays.asList(
                            SINK_SEMANTIC_VALUE_AT_LEAST_ONCE,
                            SINK_SEMANTIC_VALUE_EXACTLY_ONCE,
                            SINK_SEMANTIC_VALUE_NONE));

    private static Optional<DecodingFormat<DeserializationSchema<RowData>>> getKeyDecodingFormat(
            TableFactoryHelper helper) {
        final Optional<DecodingFormat<DeserializationSchema<RowData>>> keyDecodingFormat =
                helper.discoverOptionalDecodingFormat(
                        DeserializationFormatFactory.class, KEY_FORMAT);
        keyDecodingFormat.ifPresent(
                format -> {
                    if (!format.getChangelogMode().containsOnly(RowKind.INSERT)) {
                        throw new ValidationException(
                                String.format(
                                        "A key format should only deal with INSERT-only records. "
                                                + "But %s has a changelog mode of %s.",
                                        helper.getOptions().get(KEY_FORMAT),
                                        format.getChangelogMode()));
                    }
                });
        return keyDecodingFormat;
    }

    private static Optional<EncodingFormat<SerializationSchema<RowData>>> getKeyEncodingFormat(
            TableFactoryHelper helper) {
        final Optional<EncodingFormat<SerializationSchema<RowData>>> keyEncodingFormat =
                helper.discoverOptionalEncodingFormat(SerializationFormatFactory.class, KEY_FORMAT);
        keyEncodingFormat.ifPresent(
                format -> {
                    if (!format.getChangelogMode().containsOnly(RowKind.INSERT)) {
                        throw new ValidationException(
                                String.format(
                                        "A key format should only deal with INSERT-only records. "
                                                + "But %s has a changelog mode of %s.",
                                        helper.getOptions().get(KEY_FORMAT),
                                        format.getChangelogMode()));
                    }
                });
        return keyEncodingFormat;
    }

    private static DecodingFormat<DeserializationSchema<RowData>> getValueDecodingFormat(
            TableFactoryHelper helper) {
        return helper.discoverOptionalDecodingFormat(
                DeserializationFormatFactory.class, FactoryUtil.FORMAT)
                .orElseGet(
                        () -> helper.discoverDecodingFormat(
                                DeserializationFormatFactory.class, VALUE_FORMAT));
    }

    private static EncodingFormat<SerializationSchema<RowData>> getValueEncodingFormat(
            TableFactoryHelper helper) {
        return helper.discoverOptionalEncodingFormat(
                SerializationFormatFactory.class, FactoryUtil.FORMAT)
                .orElseGet(
                        () -> helper.discoverEncodingFormat(
                                SerializationFormatFactory.class, VALUE_FORMAT));
    }

    private static String getSinkMultipleFormat(
            TableFactoryHelper helper) {
        return helper.getOptions().getOptional(SINK_MULTIPLE_FORMAT).orElse(null);
    }

    // --------------------------------------------------------------------------------------------

    private static void validatePKConstraints(
            ObjectIdentifier tableName, CatalogTable catalogTable, Format format) {
        if (catalogTable.getSchema().getPrimaryKey().isPresent()
                && format.getChangelogMode().containsOnly(RowKind.INSERT)) {
            Configuration options = Configuration.fromMap(catalogTable.getOptions());
            String formatName =
                    options.getOptional(FactoryUtil.FORMAT).orElse(options.get(VALUE_FORMAT));
            throw new ValidationException(
                    String.format(
                            "The Kafka table '%s' with '%s' format doesn't support defining PRIMARY KEY constraint"
                                    + " on the table, because it can't guarantee the semantic of primary key.",
                            tableName.asSummaryString(), formatName));
        }
    }

    private static void validateSinkPartitioner(ReadableConfig tableOptions) {
        tableOptions
                .getOptional(SINK_PARTITIONER)
                .ifPresent(partitioner -> {
                    if (partitioner.equals(SINK_PARTITIONER_VALUE_ROUND_ROBIN)
                            && tableOptions.getOptional(KEY_FIELDS).isPresent()) {
                        throw new ValidationException(
                                "Currently 'round-robin' partitioner only works "
                                        + "when option 'key.fields' is not specified.");
                    } else if (SINK_PARTITIONER_VALUE_RAW_HASH.equals(partitioner)
                            || "org.apache.inlong.sort.kafka.partitioner.RawDataHashPartitioner".equals(partitioner)) {
                        boolean invalid = !"raw".equals(tableOptions.getOptional(FORMAT).orElse(null))
                                || !tableOptions.getOptional(SINK_MULTIPLE_FORMAT).isPresent()
                                || !tableOptions.getOptional(SINK_MULTIPLE_PARTITION_PATTERN).isPresent()
                                || tableOptions.getOptional(SINK_MULTIPLE_FORMAT).get().isEmpty()
                                || tableOptions.getOptional(SINK_MULTIPLE_PARTITION_PATTERN).get().isEmpty();
                        if (invalid) {
                            throw new ValidationException(
                                    "Currently 'raw-hash' partitioner only works "
                                            + "when option 'format' is 'raw' and option 'sink.multiple.format' "
                                            + "and 'sink.multiple.partition-pattern' is specified.");
                        }
                    } else if (partitioner.isEmpty()) {
                        throw new ValidationException(
                                String.format(
                                        "Option '%s' should be a non-empty string.",
                                        SINK_PARTITIONER.key()));
                    }
                });
    }

    private Optional<FlinkKafkaPartitioner<RowData>> getFlinkKafkaPartitioner(
            ReadableConfig tableOptions, ClassLoader classLoader) {
        if (tableOptions.getOptional(SINK_PARTITIONER).isPresent()
                && SINK_PARTITIONER_VALUE_RAW_HASH.equals(tableOptions.getOptional(SINK_PARTITIONER).get())) {
            RawDataHashPartitioner<RowData> rawHashPartitioner = new RawDataHashPartitioner<>();
            rawHashPartitioner.setSinkMultipleFormat(tableOptions.getOptional(SINK_MULTIPLE_FORMAT).orElse(null));
            rawHashPartitioner.setPartitionPattern(tableOptions.getOptional(SINK_MULTIPLE_PARTITION_PATTERN)
                    .orElse(null));
            return Optional.of(rawHashPartitioner);
        }
        Optional<FlinkKafkaPartitioner<RowData>> partitioner = KafkaOptions
                .getFlinkKafkaPartitioner(tableOptions, classLoader);
        if (partitioner.isPresent()) {
            if (partitioner.get() instanceof RawDataHashPartitioner) {
                RawDataHashPartitioner<RowData> rawHashPartitioner =
                        (RawDataHashPartitioner<RowData>) partitioner.get();
                rawHashPartitioner.setSinkMultipleFormat(tableOptions.getOptional(SINK_MULTIPLE_FORMAT).orElse(null));
                rawHashPartitioner.setPartitionPattern(tableOptions.getOptional(SINK_MULTIPLE_PARTITION_PATTERN)
                        .orElse(null));
            }
        }
        return partitioner;
    }

    private void validateSinkSemantic(ReadableConfig tableOptions) {
        tableOptions
                .getOptional(SINK_SEMANTIC)
                .ifPresent(semantic -> {
                    if (!SINK_SEMANTIC_ENUMS.contains(semantic)) {
                        throw new ValidationException(String.format(
                                "Unsupported value '%s' for '%s'. "
                                        + "Supported values are ['at-least-once', 'exactly-once', 'none'].",
                                semantic, SINK_SEMANTIC.key()));
                    }
                });
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(PROPS_BOOTSTRAP_SERVERS);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(FactoryUtil.FORMAT);
        options.add(KEY_FORMAT);
        options.add(KEY_FIELDS);
        options.add(KEY_FIELDS_PREFIX);
        options.add(VALUE_FORMAT);
        options.add(VALUE_FIELDS_INCLUDE);
        options.add(TOPIC);
        options.add(TOPIC_PATTERN);
        options.add(PROPS_GROUP_ID);
        options.add(SCAN_STARTUP_MODE);
        options.add(SCAN_STARTUP_SPECIFIC_OFFSETS);
        options.add(SCAN_TOPIC_PARTITION_DISCOVERY);
        options.add(SCAN_STARTUP_TIMESTAMP_MILLIS);
        options.add(SINK_PARTITIONER);
        options.add(SINK_SEMANTIC);
        options.add(SINK_PARALLELISM);
        options.add(KAFKA_IGNORE_ALL_CHANGELOG);
        options.add(INLONG_METRIC);
        options.add(INLONG_AUDIT);
        options.add(SINK_MULTIPLE_FORMAT);
        options.add(SINK_MULTIPLE_PARTITION_PATTERN);
        return options;
    }

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        final TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);

        final ReadableConfig tableOptions = helper.getOptions();

        final Optional<DecodingFormat<DeserializationSchema<RowData>>> keyDecodingFormat =
                getKeyDecodingFormat(helper);

        final DecodingFormat<DeserializationSchema<RowData>> valueDecodingFormat =
                getValueDecodingFormat(helper);

        helper.validateExcept(PROPERTIES_PREFIX, DIRTY_PREFIX);

        validateTableSourceOptions(tableOptions);

        validatePKConstraints(
                context.getObjectIdentifier(), context.getCatalogTable(), valueDecodingFormat);

        final StartupOptions startupOptions = getStartupOptions(tableOptions);

        final Properties properties = getKafkaProperties(context.getCatalogTable().getOptions());

        // add topic-partition discovery
        properties.setProperty(
                FlinkKafkaConsumerBase.KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS,
                String.valueOf(
                        tableOptions
                                .getOptional(SCAN_TOPIC_PARTITION_DISCOVERY)
                                .map(Duration::toMillis)
                                .orElse(FlinkKafkaConsumerBase.PARTITION_DISCOVERY_DISABLED)));

        final DataType physicalDataType =
                context.getCatalogTable().getSchema().toPhysicalRowDataType();

        final int[] keyProjection = createKeyFormatProjection(tableOptions, physicalDataType);

        final int[] valueProjection = createValueFormatProjection(tableOptions, physicalDataType);

        final String keyPrefix = tableOptions.getOptional(KEY_FIELDS_PREFIX).orElse(null);

        final String inlongMetric = tableOptions.getOptional(INLONG_METRIC).orElse(null);

        final String auditHostAndPorts = tableOptions.getOptional(INLONG_AUDIT).orElse(null);
        // Build the dirty data side-output
        final DirtyOptions dirtyOptions = DirtyOptions.fromConfig(tableOptions);
        final DirtySink<String> dirtySink = DirtySinkFactoryUtils.createDirtySink(context, dirtyOptions);
        return createKafkaTableSource(
                physicalDataType,
                keyDecodingFormat.orElse(null),
                valueDecodingFormat,
                keyProjection,
                valueProjection,
                keyPrefix,
                KafkaOptions.getSourceTopics(tableOptions),
                KafkaOptions.getSourceTopicPattern(tableOptions),
                properties,
                startupOptions.startupMode,
                startupOptions.specificOffsets,
                startupOptions.startupTimestampMillis,
                inlongMetric,
                auditHostAndPorts,
                dirtyOptions,
                dirtySink);
    }

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        final TableFactoryHelper helper =
                FactoryUtil.createTableFactoryHelper(
                        this, autoCompleteSchemaRegistrySubject(context));

        final ReadableConfig tableOptions = helper.getOptions();

        final Optional<EncodingFormat<SerializationSchema<RowData>>> keyEncodingFormat =
                getKeyEncodingFormat(helper);

        final EncodingFormat<SerializationSchema<RowData>> valueEncodingFormat =
                getValueEncodingFormat(helper);

        final String sinkMultipleFormat = getSinkMultipleFormat(helper);
        helper.validateExcept(PROPERTIES_PREFIX, DIRTY_PREFIX);

        validateSinkPartitioner(tableOptions);
        validateSinkSemantic(tableOptions);

        validatePKConstraints(
                context.getObjectIdentifier(), context.getCatalogTable(), valueEncodingFormat);

        final DataType physicalDataType =
                context.getCatalogTable().getSchema().toPhysicalRowDataType();

        validateSinkMultipleFormatAndPhysicalDataType(physicalDataType, valueEncodingFormat, sinkMultipleFormat);

        final int[] keyProjection = createKeyFormatProjection(tableOptions, physicalDataType);

        final int[] valueProjection = createValueFormatProjection(tableOptions, physicalDataType);

        final String keyPrefix = tableOptions.getOptional(KEY_FIELDS_PREFIX).orElse(null);

        final Integer parallelism = tableOptions.getOptional(SINK_PARALLELISM).orElse(null);

        final String inlongMetric = tableOptions.getOptional(INLONG_METRIC).orElse(null);

        final String auditHostAndPorts = tableOptions.getOptional(INLONG_AUDIT).orElse(null);
        // Build the dirty data side-output
        final DirtyOptions dirtyOptions = DirtyOptions.fromConfig(tableOptions);
        final DirtySink<Object> dirtySink = DirtySinkFactoryUtils.createDirtySink(context, dirtyOptions);
        final boolean multipleSink = tableOptions.getOptional(SINK_MULTIPLE_FORMAT).isPresent();
        return createKafkaTableSink(
                physicalDataType,
                keyEncodingFormat.orElse(null),
                valueEncodingFormat,
                keyProjection,
                valueProjection,
                keyPrefix,
                tableOptions.get(TOPIC).get(0),
                getKafkaProperties(context.getCatalogTable().getOptions()),
                context.getCatalogTable(),
                getFlinkKafkaPartitioner(tableOptions, context.getClassLoader()).orElse(null),
                getSinkSemantic(tableOptions),
                parallelism,
                inlongMetric,
                auditHostAndPorts,
                sinkMultipleFormat,
                tableOptions.getOptional(TOPIC_PATTERN).orElse(null),
                dirtyOptions,
                dirtySink,
                multipleSink);
    }

    private void validateSinkMultipleFormatAndPhysicalDataType(DataType physicalDataType,
            EncodingFormat<SerializationSchema<RowData>> valueEncodingFormat, String sinkMultipleFormat) {
        if (multipleSink(valueEncodingFormat, sinkMultipleFormat)) {
            DynamicSchemaFormatFactory.getFormat(sinkMultipleFormat);
            Set<String> supportFormats = DynamicSchemaFormatFactory.SUPPORT_FORMATS.keySet();
            if (!supportFormats.contains(sinkMultipleFormat)) {
                throw new ValidationException(String.format(
                        "Unsupported value '%s' for '%s'. "
                                + "Supported values are %s.",
                        sinkMultipleFormat, SINK_MULTIPLE_FORMAT.key(), supportFormats));
            }
            if (physicalDataType.getLogicalType() instanceof VarBinaryType) {
                throw new ValidationException(
                        "Only supports 'BYTES' or 'VARBINARY(n)' of PhysicalDataType "
                                + "when the option 'format' is 'raw' and option 'sink.multiple.format' is specified.");
            }
        }
    }

    private boolean multipleSink(EncodingFormat<SerializationSchema<RowData>> valueEncodingFormat,
            String sinkMultipleFormat) {
        return valueEncodingFormat instanceof RawFormatSerializationSchema
                && StringUtils.isNotBlank(sinkMultipleFormat);
    }

    // --------------------------------------------------------------------------------------------

    protected KafkaDynamicSource createKafkaTableSource(
            DataType physicalDataType,
            @Nullable DecodingFormat<DeserializationSchema<RowData>> keyDecodingFormat,
            DecodingFormat<DeserializationSchema<RowData>> valueDecodingFormat,
            int[] keyProjection,
            int[] valueProjection,
            @Nullable String keyPrefix,
            @Nullable List<String> topics,
            @Nullable Pattern topicPattern,
            Properties properties,
            StartupMode startupMode,
            Map<KafkaTopicPartition, Long> specificStartupOffsets,
            long startupTimestampMillis,
            String inlongMetric,
            String auditHostAndPorts,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<String> dirtySink) {
        return new KafkaDynamicSource(
                physicalDataType,
                keyDecodingFormat,
                valueDecodingFormat,
                keyProjection,
                valueProjection,
                keyPrefix,
                topics,
                topicPattern,
                properties,
                startupMode,
                specificStartupOffsets,
                startupTimestampMillis,
                false,
                inlongMetric,
                auditHostAndPorts,
                dirtyOptions,
                dirtySink);
    }

    protected KafkaDynamicSink createKafkaTableSink(
            DataType physicalDataType,
            @Nullable EncodingFormat<SerializationSchema<RowData>> keyEncodingFormat,
            EncodingFormat<SerializationSchema<RowData>> valueEncodingFormat,
            int[] keyProjection,
            int[] valueProjection,
            @Nullable String keyPrefix,
            String topic,
            Properties properties,
            CatalogTable table,
            FlinkKafkaPartitioner<RowData> partitioner,
            KafkaSinkSemantic semantic,
            Integer parallelism,
            String inlongMetric,
            String auditHostAndPorts,
            @Nullable String sinkMultipleFormat,
            @Nullable String topicPattern,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink,
            boolean multipleSink) {
        return new KafkaDynamicSink(
                physicalDataType,
                physicalDataType,
                keyEncodingFormat,
                valueEncodingFormat,
                keyProjection,
                valueProjection,
                keyPrefix,
                topic,
                properties,
                table,
                partitioner,
                semantic,
                false,
                SinkBufferFlushMode.DISABLED,
                parallelism,
                inlongMetric,
                auditHostAndPorts,
                sinkMultipleFormat,
                topicPattern,
                dirtyOptions,
                dirtySink,
                multipleSink);
    }
}