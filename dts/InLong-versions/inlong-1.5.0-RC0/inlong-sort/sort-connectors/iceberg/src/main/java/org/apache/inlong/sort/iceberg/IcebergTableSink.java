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

package org.apache.inlong.sort.iceberg;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.constraints.UniqueConstraint;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.sink.DataStreamSinkProvider;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.abilities.SupportsOverwrite;
import org.apache.flink.table.connector.sink.abilities.SupportsPartitioning;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.actions.ActionsProvider;
import org.apache.iceberg.flink.CatalogLoader;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.sink.MultipleSinkOption;
import org.apache.inlong.sort.iceberg.sink.FlinkSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static org.apache.inlong.sort.base.Constants.IGNORE_ALL_CHANGELOG;
import static org.apache.inlong.sort.base.Constants.INLONG_AUDIT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_DATABASE_PATTERN;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_ENABLE;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_FORMAT;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_PK_AUTO_GENERATED;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_SCHEMA_UPDATE_POLICY;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_TABLE_PATTERN;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_TYPE_MAP_COMPATIBLE_WITH_SPARK;

/**
 * Copy from iceberg-flink:iceberg-flink-1.13:0.13.2
 * Add an option `sink.ignore.changelog` to support insert-only mode without primaryKey.
 * Add a table property `write.compact.enable` to support small file compact.
 * Add option `inlong.metric` and `metrics.audit.proxy.hosts` to support collect inlong metrics and audit
 */
public class IcebergTableSink implements DynamicTableSink, SupportsPartitioning, SupportsOverwrite {

    private static final Logger LOG = LoggerFactory.getLogger(IcebergTableSink.class);

    private final TableLoader tableLoader;
    private final TableSchema tableSchema;

    private final CatalogTable catalogTable;
    private final CatalogLoader catalogLoader;

    private final ActionsProvider actionsProvider;

    private boolean overwrite = false;

    private final DirtyOptions dirtyOptions;
    private @Nullable final DirtySink<Object> dirtySink;

    private IcebergTableSink(IcebergTableSink toCopy) {
        this.tableLoader = toCopy.tableLoader;
        this.tableSchema = toCopy.tableSchema;
        this.overwrite = toCopy.overwrite;
        this.catalogTable = toCopy.catalogTable;
        this.catalogLoader = toCopy.catalogLoader;
        this.actionsProvider = toCopy.actionsProvider;
        this.dirtyOptions = toCopy.dirtyOptions;
        this.dirtySink = toCopy.dirtySink;
    }

    public IcebergTableSink(TableLoader tableLoader,
            TableSchema tableSchema,
            CatalogTable catalogTable,
            CatalogLoader catalogLoader,
            ActionsProvider actionsProvider,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink) {
        this.tableLoader = tableLoader;
        this.tableSchema = tableSchema;
        this.catalogTable = catalogTable;
        this.catalogLoader = catalogLoader;
        this.actionsProvider = actionsProvider;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        Preconditions.checkState(!overwrite || context.isBounded(),
                "Unbounded data stream doesn't support overwrite operation.");

        List<String> equalityColumns = tableSchema.getPrimaryKey()
                .map(UniqueConstraint::getColumns)
                .orElseGet(ImmutableList::of);

        final ReadableConfig tableOptions = Configuration.fromMap(catalogTable.getOptions());
        boolean multipleSink = tableOptions.get(SINK_MULTIPLE_ENABLE);
        if (multipleSink) {
            return (DataStreamSinkProvider) dataStream -> FlinkSink.forRowData(dataStream)
                    .overwrite(overwrite)
                    .appendMode(tableOptions.get(IGNORE_ALL_CHANGELOG))
                    .metric(tableOptions.get(INLONG_METRIC), tableOptions.get(INLONG_AUDIT))
                    .catalogLoader(catalogLoader)
                    .multipleSink(tableOptions.get(SINK_MULTIPLE_ENABLE))
                    .multipleSinkOption(MultipleSinkOption.builder()
                            .withFormat(tableOptions.get(SINK_MULTIPLE_FORMAT))
                            .withSparkEngineEnable(tableOptions.get(SINK_MULTIPLE_TYPE_MAP_COMPATIBLE_WITH_SPARK))
                            .withDatabasePattern(tableOptions.get(SINK_MULTIPLE_DATABASE_PATTERN))
                            .withTablePattern(tableOptions.get(SINK_MULTIPLE_TABLE_PATTERN))
                            .withSchemaUpdatePolicy(tableOptions.get(SINK_MULTIPLE_SCHEMA_UPDATE_POLICY))
                            .withPkAutoGenerated(tableOptions.get(SINK_MULTIPLE_PK_AUTO_GENERATED))
                            .build())
                    .append();
        } else {
            return (DataStreamSinkProvider) dataStream -> FlinkSink.forRowData(dataStream)
                    .tableLoader(tableLoader)
                    .tableSchema(tableSchema)
                    .equalityFieldColumns(equalityColumns)
                    .overwrite(overwrite)
                    .appendMode(tableOptions.get(IGNORE_ALL_CHANGELOG))
                    .metric(tableOptions.get(INLONG_METRIC), tableOptions.get(INLONG_AUDIT))
                    .dirtyOptions(dirtyOptions)
                    .dirtySink(dirtySink)
                    .action(actionsProvider)
                    .append();
        }
    }

    @Override
    public void applyStaticPartition(Map<String, String> partition) {
        // The flink's PartitionFanoutWriter will handle the static partition write policy automatically.
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        if (org.apache.flink.configuration.Configuration.fromMap(catalogTable.getOptions())
                .get(IGNORE_ALL_CHANGELOG)) {
            LOG.warn("Iceberg sink receive all changelog record. "
                    + "Regard any other record as insert-only record.");
            return ChangelogMode.all();
        } else {
            ChangelogMode.Builder builder = ChangelogMode.newBuilder();
            for (RowKind kind : requestedMode.getContainedKinds()) {
                builder.addContainedKind(kind);
            }
            return builder.build();
        }
    }

    @Override
    public DynamicTableSink copy() {
        return new IcebergTableSink(this);
    }

    @Override
    public String asSummaryString() {
        return "Iceberg table sink";
    }

    @Override
    public void applyOverwrite(boolean newOverwrite) {
        this.overwrite = newOverwrite;
    }
}
