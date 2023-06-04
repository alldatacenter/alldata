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

package org.apache.inlong.sort.iceberg.sink;

import java.util.Set;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.RowData.FieldGetter;
import org.apache.flink.table.data.util.DataFormatConverters;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.Row;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DistributionMode;
import org.apache.iceberg.PartitionField;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.SerializableTable;
import org.apache.iceberg.Table;
import org.apache.iceberg.actions.ActionsProvider;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.flink.CatalogLoader;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.iceberg.flink.FlinkWriteConf;
import org.apache.iceberg.flink.FlinkWriteOptions;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.flink.sink.TaskWriterFactory;
import org.apache.iceberg.flink.util.FlinkCompatibilityUtil;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.types.TypeUtil;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.sink.MultipleSinkOption;
import org.apache.inlong.sort.iceberg.sink.collections.PartitionGroupBuffer;
import org.apache.inlong.sort.iceberg.sink.collections.PartitionGroupBuffer.BufferType;
import org.apache.inlong.sort.iceberg.sink.multiple.IcebergMultipleFilesCommiter;
import org.apache.inlong.sort.iceberg.sink.multiple.IcebergMultipleStreamWriter;
import org.apache.inlong.sort.iceberg.sink.multiple.IcebergProcessOperator;
import org.apache.inlong.sort.iceberg.sink.multiple.IcebergSingleFileCommiter;
import org.apache.inlong.sort.iceberg.sink.multiple.IcebergSingleStreamWriter;
import org.apache.inlong.sort.iceberg.sink.multiple.MultipleWriteResult;
import org.apache.inlong.sort.iceberg.sink.multiple.RecordWithSchema;
import org.apache.inlong.sort.iceberg.sink.multiple.DynamicSchemaHandleOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.iceberg.TableProperties.WRITE_DISTRIBUTION_MODE;
import static org.apache.inlong.sort.iceberg.FlinkDynamicTableFactory.WRITE_MINI_BATCH_BUFFER_TYPE;
import static org.apache.inlong.sort.iceberg.FlinkDynamicTableFactory.WRITE_MINI_BATCH_ENABLE;
import static org.apache.inlong.sort.iceberg.FlinkDynamicTableFactory.WRITE_MINI_BATCH_PRE_AGG_ENABLE;
import static org.apache.inlong.sort.iceberg.FlinkDynamicTableFactory.WRITE_RATE_LIMIT;

/**
 * Copy from iceberg-flink:iceberg-flink-1.13:0.13.2
 * Add an option `sink.ignore.changelog` to support insert-only mode without primaryKey.
 * Add a table property `write.compact.enable` to support small file compact.
 * Add option `inlong.metric` and `metrics.audit.proxy.hosts` to support collect inlong metrics and audit.
 */
public class FlinkSink {

    private static final Logger LOG = LoggerFactory.getLogger(FlinkSink.class);

    private static final String ICEBERG_STREAM_WRITER_NAME = IcebergSingleStreamWriter.class.getSimpleName();
    private static final String ICEBERG_FILES_COMMITTER_NAME = IcebergSingleFileCommiter.class.getSimpleName();
    private static final String ICEBERG_MULTIPLE_STREAM_WRITER_NAME =
            IcebergMultipleStreamWriter.class.getSimpleName();
    private static final String ICEBERG_MULTIPLE_FILES_COMMITTER_NAME =
            IcebergMultipleFilesCommiter.class.getSimpleName();
    private static final String ICEBERG_WHOLE_DATABASE_MIGRATION_NAME =
            DynamicSchemaHandleOperator.class.getSimpleName();

    private FlinkSink() {
    }

    /**
     * Initialize a {@link Builder} to export the data from generic input data stream into iceberg table. We use
     * {@link RowData} inside the sink connector, so users need to provide a mapper function and a
     * {@link TypeInformation} to convert those generic records to a RowData DataStream.
     *
     * @param input      the generic source input data stream.
     * @param mapper     function to convert the generic data to {@link RowData}
     * @param outputType to define the {@link TypeInformation} for the input data.
     * @param <T>        the data type of records.
     * @return {@link Builder} to connect the iceberg table.
     */
    public static <T> Builder builderFor(DataStream<T> input,
            MapFunction<T, RowData> mapper,
            TypeInformation<RowData> outputType) {
        return new Builder().forMapperOutputType(input, mapper, outputType);
    }

    /**
     * Initialize a {@link Builder} to export the data from input data stream with {@link Row}s into iceberg table.
     * We use {@link RowData} inside the sink connector, so users need to provide a {@link TableSchema} for builder to
     * convert those {@link Row}s to a {@link RowData} DataStream.
     *
     * @param input       the source input data stream with {@link Row}s.
     * @param tableSchema defines the {@link TypeInformation} for input data.
     * @return {@link Builder} to connect the iceberg table.
     */
    public static Builder forRow(DataStream<Row> input, TableSchema tableSchema) {
        RowType rowType = (RowType) tableSchema.toRowDataType().getLogicalType();
        DataType[] fieldDataTypes = tableSchema.getFieldDataTypes();

        DataFormatConverters.RowConverter rowConverter = new DataFormatConverters.RowConverter(fieldDataTypes);
        return builderFor(input, rowConverter::toInternal, FlinkCompatibilityUtil.toTypeInfo(rowType))
                .tableSchema(tableSchema);
    }

    /**
     * Initialize a {@link Builder} to export the data from input data stream with {@link RowData}s into iceberg table.
     *
     * @param input the source input data stream with {@link RowData}s.
     * @return {@link Builder} to connect the iceberg table.
     */
    public static Builder forRowData(DataStream<RowData> input) {
        return new Builder().forRowData(input);
    }

    public static class Builder {

        private Function<String, DataStream<RowData>> inputCreator = null;
        private TableLoader tableLoader;
        private Table table;
        private TableSchema tableSchema;
        private ActionsProvider actionProvider;
        private boolean overwrite = false;
        private boolean appendMode = false;
        private DistributionMode distributionMode = null;
        private Integer writeParallelism = null;
        private boolean upsert = false;
        private List<String> equalityFieldColumns = null;
        private String uidPrefix = null;
        private final Map<String, String> writeOptions = Maps.newHashMap();
        private FlinkWriteConf flinkWriteConf = null;
        private String inlongMetric = null;
        private String auditHostAndPorts = null;
        private CatalogLoader catalogLoader = null;
        private boolean multipleSink = false;
        private MultipleSinkOption multipleSinkOption = null;
        private DirtyOptions dirtyOptions;
        private @Nullable DirtySink<Object> dirtySink;
        private ReadableConfig tableOptions = new Configuration();

        private Builder() {
        }

        private Builder forRowData(DataStream<RowData> newRowDataInput) {
            this.inputCreator = ignored -> newRowDataInput;
            return this;
        }

        private <T> Builder forMapperOutputType(DataStream<T> input,
                MapFunction<T, RowData> mapper,
                TypeInformation<RowData> outputType) {
            this.inputCreator = newUidPrefix -> {
                if (newUidPrefix != null) {
                    return input.map(mapper, outputType)
                            .name(operatorName(newUidPrefix))
                            .uid(newUidPrefix + "-mapper");
                } else {
                    return input.map(mapper, outputType);
                }
            };
            return this;
        }

        /**
         * This iceberg {@link Table} instance is used for initializing {@link IcebergSingleStreamWriter} which will
         * write all the records into {@link DataFile}s and emit them to downstream operator. Providing a table would \
         * avoid so many table loading from each separate task.
         *
         * @param newTable the loaded iceberg table instance.
         * @return {@link Builder} to connect the iceberg table.
         */
        public Builder table(Table newTable) {
            this.table = newTable;
            return this;
        }

        /**
         * The table loader is used for loading tables in {@link IcebergSingleFileCommiter} lazily, we need this loader
         * because {@link Table} is not serializable and could not just use the loaded table from Builder#table in the
         * remote task manager.
         *
         * @param newTableLoader to load iceberg table inside tasks.
         * @return {@link Builder} to connect the iceberg table.
         */
        public Builder tableLoader(TableLoader newTableLoader) {
            this.tableLoader = newTableLoader;
            return this;
        }

        public Builder tableSchema(TableSchema newTableSchema) {
            this.tableSchema = newTableSchema;
            return this;
        }

        /**
         * The catalog loader is used for loading tables in {@link IcebergMultipleStreamWriter} and
         * {@link DynamicSchemaHandleOperator} lazily, we need this loader because in multiple sink scene which table
         * to load is determined in runtime, so we should hold a {@link org.apache.iceberg.catalog.Catalog} at runtime.
         *
         * @param catalogLoader to load iceberg catalog inside tasks.
         * @return {@link Builder} to connect the iceberg table.
         */
        public Builder catalogLoader(CatalogLoader catalogLoader) {
            this.catalogLoader = catalogLoader;
            return this;
        }

        public Builder multipleSink(boolean multipleSink) {
            this.multipleSink = multipleSink;
            return this;
        }

        public Builder multipleSinkOption(MultipleSinkOption multipleSinkOption) {
            this.multipleSinkOption = multipleSinkOption;
            return this;
        }

        public Builder overwrite(boolean newOverwrite) {
            this.overwrite = newOverwrite;
            writeOptions.put(FlinkWriteOptions.OVERWRITE_MODE.key(), Boolean.toString(newOverwrite));
            return this;
        }

        /**
         * The appendMode properties is used to insert data without equality field columns.
         *
         * @param appendMode append mode properties.
         * @return {@link FlinkSink.Builder} to connect the iceberg table.
         */
        public FlinkSink.Builder appendMode(boolean appendMode) {
            this.appendMode = appendMode;
            return this;
        }

        /**
         * The action properties is used to do some iceberg action like datafile rewrite action in flink runtime.
         *
         * @param actionProvider auto compact properties.
         * @return {@link FlinkSink.Builder} to connect the iceberg table.
         */
        public FlinkSink.Builder action(ActionsProvider actionProvider) {
            this.actionProvider = actionProvider;
            return this;
        }

        public FlinkSink.Builder tableOptions(ReadableConfig tableOptions) {
            this.tableOptions = tableOptions;
            return this;
        }

        /**
         * Add metric output for iceberg writer
         * @param inlongMetric
         * @param auditHostAndPorts
         * @return
         */
        public Builder metric(String inlongMetric, String auditHostAndPorts) {
            this.inlongMetric = inlongMetric;
            this.auditHostAndPorts = auditHostAndPorts;
            return this;
        }

        public Builder dirtyOptions(DirtyOptions dirtyOptions) {
            this.dirtyOptions = dirtyOptions;
            return this;
        }

        public Builder dirtySink(DirtySink<Object> dirtySink) {
            this.dirtySink = dirtySink;
            return this;
        }

        /**
         * Configure the write {@link DistributionMode} that the flink sink will use. Currently, flink support
         * {@link DistributionMode#NONE} and {@link DistributionMode#HASH}.
         *
         * @param mode to specify the write distribution mode.
         * @return {@link Builder} to connect the iceberg table.
         */
        public Builder distributionMode(DistributionMode mode) {
            Preconditions.checkArgument(
                    !DistributionMode.RANGE.equals(mode),
                    "Flink does not support 'range' write distribution mode now.");
            if (mode != null) {
                this.distributionMode = mode;
                writeOptions.put(FlinkWriteOptions.DISTRIBUTION_MODE.key(), mode.modeName());
            }
            return this;
        }

        /**
         * Configuring the write parallel number for iceberg stream writer.
         *
         * @param newWriteParallelism the number of parallel iceberg stream writer.
         * @return {@link Builder} to connect the iceberg table.
         */
        public Builder writeParallelism(int newWriteParallelism) {
            this.writeParallelism = newWriteParallelism;
            return this;
        }

        /**
         * All INSERT/UPDATE_AFTER events from input stream will be transformed to UPSERT events, which means it will
         * DELETE the old records and then INSERT the new records. In partitioned table, the partition fields should be
         * a subset of equality fields, otherwise the old row that located in partition-A could not be deleted by the
         * new row that located in partition-B.
         *
         * @param enabled indicate whether it should transform all INSERT/UPDATE_AFTER events to UPSERT.
         * @return {@link Builder} to connect the iceberg table.
         */
        public Builder upsert(boolean enabled) {
            this.upsert = enabled;
            writeOptions.put(FlinkWriteOptions.WRITE_UPSERT_ENABLED.key(), Boolean.toString(enabled));
            return this;
        }

        /**
         * Configuring the equality field columns for iceberg table that accept CDC or UPSERT events.
         *
         * @param columns defines the iceberg table's key.
         * @return {@link Builder} to connect the iceberg table.
         */
        public Builder equalityFieldColumns(List<String> columns) {
            this.equalityFieldColumns = columns;
            return this;
        }

        /**
         * Set the uid prefix for FlinkSink operators. Note that FlinkSink internally consists of multiple operators
         * (like writer, committer, dummy sink etc.) Actually operator uid will be appended with a suffix like
         * "uidPrefix-writer".
         * <br><br>
         * If provided, this prefix is also applied to operator names.
         * <br><br>
         * Flink auto generates operator uid if not set explicitly. It is a recommended
         * <a href="https://ci.apache.org/projects/flink/flink-docs-master/docs/ops/production_ready/">
         * best-practice to set uid for all operators</a> before deploying to production. Flink has an option to {@code
         * pipeline.auto-generate-uid=false} to disable auto-generation and force explicit setting of all operator uid.
         * <br><br>
         * Be careful with setting this for an existing job, because now we are changing the operator uid from an
         * auto-generated one to this new value. When deploying the change with a checkpoint, Flink won't be able to
         * restore the previous Flink sink operator state (more specifically the committer operator state). You need to
         * use {@code --allowNonRestoredState} to ignore the previous sink state. During restore Flink sink state is
         * used to check if last commit was actually successful or not. {@code --allowNonRestoredState} can lead to data
         * loss if the Iceberg commit failed in the last completed checkpoint.
         *
         * @param newPrefix prefix for Flink sink operator uid and name
         * @return {@link Builder} to connect the iceberg table.
         */
        public Builder uidPrefix(String newPrefix) {
            this.uidPrefix = newPrefix;
            return this;
        }

        private <T> DataStreamSink<T> chainIcebergOperators() {
            return multipleSink ? chainIcebergMultipleSinkOperators() : chainIcebergSingleSinkOperators();
        }

        private <T> DataStreamSink<T> chainIcebergSingleSinkOperators() {

            Preconditions.checkArgument(inputCreator != null,
                    "Please use forRowData() or forMapperOutputType() to initialize the input DataStream.");
            Preconditions.checkNotNull(tableLoader, "Table loader shouldn't be null");

            DataStream<RowData> rowDataInput = inputCreator.apply(uidPrefix);

            if (table == null) {
                tableLoader.open();
                try (TableLoader loader = tableLoader) {
                    this.table = loader.loadTable();
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to load iceberg table from table loader: " + tableLoader, e);
                }
            }

            flinkWriteConf = new FlinkWriteConf(table, writeOptions, tableOptions);

            // Find out the equality field id list based on the user-provided equality field column names.
            List<Integer> equalityFieldIds = checkAndGetEqualityFieldIds();

            // Convert the requested flink table schema to flink row type.
            RowType flinkRowType = toFlinkRowType(table.schema(), tableSchema);

            // Distribute the records from input data stream based on the write.distribution-mode and
            // equality fields.
            DataStream<RowData> distributeStream =
                    distributeDataStream(
                            rowDataInput, equalityFieldIds, table.spec(), table.schema(), flinkRowType);

            // Add parallel writers that append rows to files
            SingleOutputStreamOperator<WriteResult> writerStream =
                    appendWriter(distributeStream, flinkRowType, equalityFieldIds);

            // Add single-parallelism committer that commits files
            // after successful checkpoint or end of input
            SingleOutputStreamOperator<Void> committerStream = appendCommitter(writerStream);

            // Add dummy discard sink
            return appendDummySink(committerStream);
        }

        private <T> DataStreamSink<T> chainIcebergMultipleSinkOperators() {
            Preconditions.checkArgument(inputCreator != null,
                    "Please use forRowData() or forMapperOutputType() to initialize the input DataStream.");
            DataStream<RowData> rowDataInput = inputCreator.apply(uidPrefix);

            // Add rate limit if necessary
            DataStream<RowData> inputWithRateLimit = appendWithRateLimit(rowDataInput);

            // Add parallel writers that append rows to files
            SingleOutputStreamOperator<MultipleWriteResult> writerStream = appendMultipleWriter(inputWithRateLimit);

            // Add single-parallelism committer that commits files
            // after successful checkpoint or end of input
            SingleOutputStreamOperator<Void> committerStream = appendMultipleCommitter(writerStream);

            // Add dummy discard sink
            return appendDummySink(committerStream);
        }

        /**
         * Append the iceberg sink operators to write records to iceberg table.
         *
         * @return {@link DataStreamSink} for sink.
         * @deprecated this will be removed in 0.14.0; use {@link #append()} because its returned {@link DataStreamSink}
         *             has a more correct data type.
         */
        @Deprecated
        public DataStreamSink<RowData> build() {
            return chainIcebergOperators();
        }

        /**
         * Append the iceberg sink operators to write records to iceberg table.
         *
         * @return {@link DataStreamSink} for sink.
         */
        public DataStreamSink<Void> append() {
            return chainIcebergOperators();
        }

        private String operatorName(String suffix) {
            return uidPrefix != null ? uidPrefix + "-" + suffix : suffix;
        }

        @VisibleForTesting
        List<Integer> checkAndGetEqualityFieldIds() {
            List<Integer> equalityFieldIds = Lists.newArrayList(table.schema().identifierFieldIds());
            if (equalityFieldColumns != null && equalityFieldColumns.size() > 0) {
                Set<Integer> equalityFieldSet =
                        Sets.newHashSetWithExpectedSize(equalityFieldColumns.size());
                for (String column : equalityFieldColumns) {
                    org.apache.iceberg.types.Types.NestedField field = table.schema().findField(column);
                    Preconditions.checkNotNull(
                            field,
                            "Missing required equality field column '%s' in table schema %s",
                            column,
                            table.schema());
                    equalityFieldSet.add(field.fieldId());
                }

                if (!equalityFieldSet.equals(table.schema().identifierFieldIds())) {
                    LOG.warn(
                            "The configured equality field column IDs {} are not matched with the schema identifier "
                                    + "field IDs {}, use job specified equality field columns as the equality fields "
                                    + "by default.",
                            equalityFieldSet,
                            table.schema().identifierFieldIds());
                }
                equalityFieldIds = Lists.newArrayList(equalityFieldSet);
            }
            return equalityFieldIds;
        }

        @SuppressWarnings("unchecked")
        private <T> DataStreamSink<T> appendDummySink(SingleOutputStreamOperator<Void> committerStream) {
            DataStreamSink<T> resultStream = committerStream
                    .addSink(new DiscardingSink())
                    .name(operatorName(
                            String.format("IcebergSink %s", multipleSink ? "whole migration" : this.table.name())))
                    .setParallelism(1);
            if (uidPrefix != null) {
                resultStream = resultStream.uid(uidPrefix + "-dummysink");
            }
            return resultStream;
        }

        private <T> DataStream<T> appendWithRateLimit(DataStream<T> input) {
            if (tableOptions.get(WRITE_RATE_LIMIT) <= 0) {
                return input;
            }

            int parallelism = writeParallelism == null ? input.getParallelism() : writeParallelism;
            SingleOutputStreamOperator<T> inputWithRateLimit = input
                    .map(new RateLimitMapFunction(tableOptions.get(WRITE_RATE_LIMIT)))
                    .name("rate_limit")
                    .setParallelism(parallelism);

            if (uidPrefix != null) {
                ((SingleOutputStreamOperator) inputWithRateLimit).uid(uidPrefix + "_rate_limit");
            }
            return inputWithRateLimit;
        }

        /**
         * This operator is used to buffer a mini batch data group by field before writing to save memory resources.
         *
         * This operator must have the same degree of parallelism as the writer operator to ensure that the data of
         * the same batch of equality fields in a checkpoint period will only be processed once by a certain writer
         * subtask (rather than processed multiple times).
         *
         * @param input
         * @return
         */
        private DataStream<RowData> appendWithMiniBatchGroup(DataStream<RowData> input,
                RowType flinkType,
                Set<Integer> equalityFieldIds) {
            if (!tableOptions.get(WRITE_MINI_BATCH_ENABLE)) {
                return input;
            }

            FieldGetter[] fieldGetters =
                    IntStream.range(0, flinkType.getFieldCount())
                            .mapToObj(i -> RowData.createFieldGetter(flinkType.getTypeAt(i), i))
                            .toArray(RowData.FieldGetter[]::new);
            Schema writeSchema = TypeUtil.reassignIds(
                    FlinkSchemaUtil.convert(FlinkSchemaUtil.toSchema(flinkType)), table.schema());
            Schema deleteSchema = TypeUtil.select(table.schema(), equalityFieldIds);
            PartitionKey partitionKey = new PartitionKey(table.spec(), table.schema());

            int parallelism = writeParallelism == null ? input.getParallelism() : writeParallelism;
            boolean preAggregation = tableOptions.get(WRITE_MINI_BATCH_PRE_AGG_ENABLE) && !equalityFieldIds.isEmpty();
            BufferType bufferType = tableOptions.get(WRITE_MINI_BATCH_BUFFER_TYPE);
            PartitionGroupBuffer buffer = preAggregation
                    ? PartitionGroupBuffer.preAggInstance(
                            fieldGetters, deleteSchema, writeSchema, partitionKey, bufferType)
                    : PartitionGroupBuffer.nonPreAggInstance(writeSchema, partitionKey, bufferType);
            SingleOutputStreamOperator<RowData> inputWithMiniBatchGroup = input
                    .transform("mini_batch_group",
                            input.getType(),
                            new IcebergMiniBatchGroupOperator(buffer))
                    .setParallelism(parallelism);
            if (uidPrefix != null) {
                inputWithMiniBatchGroup.uid(uidPrefix + "mini_batch_group");
            }
            return inputWithMiniBatchGroup;
        }

        private SingleOutputStreamOperator<Void> appendCommitter(SingleOutputStreamOperator<WriteResult> writerStream) {
            IcebergProcessOperator<WriteResult, Void> filesCommitter = new IcebergProcessOperator<>(
                    new IcebergSingleFileCommiter(
                            TableIdentifier.of(table.name()),
                            tableLoader,
                            flinkWriteConf.overwriteMode(),
                            actionProvider,
                            tableOptions));
            SingleOutputStreamOperator<Void> committerStream = writerStream
                    .transform(operatorName(ICEBERG_FILES_COMMITTER_NAME), Types.VOID, filesCommitter)
                    .setParallelism(1)
                    .setMaxParallelism(1);
            if (uidPrefix != null) {
                committerStream = committerStream.uid(uidPrefix + "-committer");
            }
            return committerStream;
        }

        private SingleOutputStreamOperator<Void> appendMultipleCommitter(
                SingleOutputStreamOperator<MultipleWriteResult> writerStream) {
            IcebergProcessOperator<MultipleWriteResult, Void> multipleFilesCommiter =
                    new IcebergProcessOperator<>(new IcebergMultipleFilesCommiter(catalogLoader, overwrite,
                            actionProvider, tableOptions));
            SingleOutputStreamOperator<Void> committerStream = writerStream
                    .transform(operatorName(ICEBERG_MULTIPLE_FILES_COMMITTER_NAME), Types.VOID, multipleFilesCommiter)
                    .setParallelism(1)
                    .setMaxParallelism(1);
            if (uidPrefix != null) {
                committerStream = committerStream.uid(uidPrefix + "-committer");
            }
            return committerStream;
        }

        private SingleOutputStreamOperator<WriteResult> appendWriter(DataStream<RowData> input, RowType flinkRowType,
                List<Integer> equalityFieldIds) {
            // Fallback to use upsert mode parsed from table properties if don't specify in job level.
            // Only if not appendMode, upsert can be valid.
            boolean upsertMode = flinkWriteConf.upsertMode() && !appendMode;

            // Validate the equality fields and partition fields if we enable the upsert mode.
            if (upsertMode) {
                Preconditions.checkState(!flinkWriteConf.overwriteMode(),
                        "OVERWRITE mode shouldn't be enable when configuring to use UPSERT data stream.");
                Preconditions.checkState(!equalityFieldIds.isEmpty(),
                        "Equality field columns shouldn't be empty when configuring to use UPSERT data stream.");
                if (!table.spec().isUnpartitioned()) {
                    for (PartitionField partitionField : table.spec().fields()) {
                        Preconditions.checkState(equalityFieldIds.contains(partitionField.sourceId()),
                                "In UPSERT mode, partition field '%s' should be included in equality fields: '%s'",
                                partitionField, equalityFieldColumns);
                    }
                }
            }

            // Add rate limit if necessary
            DataStream<RowData> inputWithRateLimit = appendWithRateLimit(input);
            DataStream<RowData> inputWithMiniBatch = appendWithMiniBatchGroup(
                    inputWithRateLimit, flinkRowType, equalityFieldIds.stream().collect(Collectors.toSet()));

            IcebergProcessOperator<RowData, WriteResult> streamWriter = createStreamWriter(
                    table, flinkRowType, equalityFieldIds, flinkWriteConf, appendMode, inlongMetric,
                    auditHostAndPorts, dirtyOptions, dirtySink, tableOptions.get(WRITE_MINI_BATCH_ENABLE));

            int parallelism = writeParallelism == null ? input.getParallelism() : writeParallelism;
            SingleOutputStreamOperator<WriteResult> writerStream = inputWithMiniBatch
                    .transform(operatorName(ICEBERG_STREAM_WRITER_NAME),
                            TypeInformation.of(WriteResult.class),
                            streamWriter)
                    .setParallelism(parallelism);
            if (uidPrefix != null) {
                writerStream = writerStream.uid(uidPrefix + "-writer");
            }
            return writerStream;
        }

        private SingleOutputStreamOperator<MultipleWriteResult> appendMultipleWriter(DataStream<RowData> input) {
            // equality field will be initialized at runtime
            // upsert mode will be initialized at runtime

            int parallelism = writeParallelism == null ? input.getParallelism() : writeParallelism;
            DynamicSchemaHandleOperator routeOperator = new DynamicSchemaHandleOperator(
                    catalogLoader, multipleSinkOption, dirtyOptions, dirtySink, inlongMetric, auditHostAndPorts);
            SingleOutputStreamOperator<RecordWithSchema> routeStream = input
                    .transform(operatorName(ICEBERG_WHOLE_DATABASE_MIGRATION_NAME),
                            TypeInformation.of(RecordWithSchema.class),
                            routeOperator)
                    .setParallelism(parallelism);

            IcebergProcessOperator streamWriter =
                    new IcebergProcessOperator(new IcebergMultipleStreamWriter(
                            appendMode, catalogLoader, inlongMetric, auditHostAndPorts,
                            multipleSinkOption, dirtyOptions, dirtySink));
            SingleOutputStreamOperator<MultipleWriteResult> writerStream = routeStream
                    .transform(operatorName(ICEBERG_MULTIPLE_STREAM_WRITER_NAME),
                            TypeInformation.of(IcebergProcessOperator.class),
                            streamWriter)
                    .setParallelism(parallelism);
            if (uidPrefix != null) {
                writerStream = writerStream.uid(uidPrefix + "-writer");
            }
            return writerStream;
        }

        private DataStream<RowData> distributeDataStream(
                DataStream<RowData> input,
                List<Integer> equalityFieldIds,
                PartitionSpec partitionSpec,
                Schema iSchema,
                RowType flinkRowType) {
            DistributionMode writeMode = flinkWriteConf.distributionMode();
            LOG.info("Write distribution mode is '{}'", writeMode.modeName());
            switch (writeMode) {
                case NONE:
                    if (equalityFieldIds.isEmpty()) {
                        return input;
                    } else {
                        LOG.info("Distribute rows by equality fields, because there are equality fields set");
                        return input.keyBy(
                                new EqualityFieldKeySelector(iSchema, flinkRowType, equalityFieldIds));
                    }

                case HASH:
                    if (equalityFieldIds.isEmpty()) {
                        if (partitionSpec.isUnpartitioned()) {
                            LOG.warn(
                                    "Fallback to use 'none' distribution mode, because there are no equality fields "
                                            + "set and table is unpartitioned");
                            return input;
                        } else {
                            return input.keyBy(new PartitionKeySelector(partitionSpec, iSchema, flinkRowType));
                        }
                    } else {
                        if (partitionSpec.isUnpartitioned()) {
                            LOG.info(
                                    "Distribute rows by equality fields, because there are equality fields set "
                                            + "and table is unpartitioned");
                            return input.keyBy(
                                    new EqualityFieldKeySelector(iSchema, flinkRowType, equalityFieldIds));
                        } else {
                            for (PartitionField partitionField : partitionSpec.fields()) {
                                Preconditions.checkState(
                                        equalityFieldIds.contains(partitionField.sourceId()),
                                        "In 'hash' distribution mode with equality fields set, partition field '%s' "
                                                + "should be included in equality fields: '%s'",
                                        partitionField,
                                        equalityFieldColumns);
                            }
                            return input.keyBy(new PartitionKeySelector(partitionSpec, iSchema, flinkRowType));
                        }
                    }

                case RANGE:
                    if (equalityFieldIds.isEmpty()) {
                        LOG.warn(
                                "Fallback to use 'none' distribution mode, because there are no equality fields set "
                                        + "and {}=range is not supported yet in flink",
                                WRITE_DISTRIBUTION_MODE);
                        return input;
                    } else {
                        LOG.info(
                                "Distribute rows by equality fields, because there are equality fields set "
                                        + "and{}=range is not supported yet in flink",
                                WRITE_DISTRIBUTION_MODE);
                        return input.keyBy(
                                new EqualityFieldKeySelector(iSchema, flinkRowType, equalityFieldIds));
                    }

                default:
                    throw new RuntimeException("Unrecognized " + WRITE_DISTRIBUTION_MODE + ": " + writeMode);
            }
        }

    }

    static RowType toFlinkRowType(Schema schema, TableSchema requestedSchema) {
        if (requestedSchema != null) {
            // Convert the flink schema to iceberg schema firstly, then reassign ids to match the existing iceberg
            // schema.
            Schema writeSchema = TypeUtil.reassignIds(FlinkSchemaUtil.convert(requestedSchema), schema);
            TypeUtil.validateWriteSchema(schema, writeSchema, true, true);

            // We use this flink schema to read values from RowData. The flink's TINYINT and SMALLINT will be promoted
            // to iceberg INTEGER, that means if we use iceberg's table schema to read TINYINT (backend by 1 'byte'),
            // we will read 4 bytes rather than 1 byte, it will mess up the byte array in BinaryRowData. So here we must
            // use flink schema.
            return (RowType) requestedSchema.toRowDataType().getLogicalType();
        } else {
            return FlinkSchemaUtil.convert(schema);
        }
    }

    static IcebergProcessOperator<RowData, WriteResult> createStreamWriter(Table table,
            RowType flinkRowType,
            List<Integer> equalityFieldIds,
            FlinkWriteConf flinkWriteConf,
            boolean appendMode,
            String inlongMetric,
            String auditHostAndPorts,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink,
            boolean miniBatchMode) {
        // flink A, iceberg a
        Preconditions.checkArgument(table != null, "Iceberg table should't be null");

        Table serializableTable = SerializableTable.copyOf(table);
        TaskWriterFactory<RowData> taskWriterFactory =
                new RowDataTaskWriterFactory(
                        serializableTable,
                        serializableTable.schema(),
                        flinkRowType,
                        flinkWriteConf.targetDataFileSize(),
                        flinkWriteConf.dataFileFormat(),
                        equalityFieldIds,
                        flinkWriteConf.upsertMode(),
                        appendMode,
                        miniBatchMode);

        return new IcebergProcessOperator<>(new IcebergSingleStreamWriter<>(
                table.name(), taskWriterFactory, inlongMetric, auditHostAndPorts,
                null, dirtyOptions, dirtySink, false));
    }

}
