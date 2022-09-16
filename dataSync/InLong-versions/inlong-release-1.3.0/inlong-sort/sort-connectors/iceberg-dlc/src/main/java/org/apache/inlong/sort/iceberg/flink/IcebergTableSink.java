/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.sort.iceberg.flink;

import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.constraints.UniqueConstraint;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.sink.DataStreamSinkProvider;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.abilities.SupportsOverwrite;
import org.apache.flink.table.connector.sink.abilities.SupportsPartitioning;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.iceberg.flink.actions.SyncRewriteDataFilesActionOption;
import org.apache.inlong.sort.iceberg.flink.sink.FlinkSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Copy from iceberg-flink:iceberg-flink-1.13:0.13.2
 * Add an option `sink.ignore.changelog` to support insert-only mode without primaryKey.
 * Add a table property `write.compact.enable` to support small file compact.
 * Add option `inlong.metric` and `inlong.audit` to support collect inlong metrics and audit
 */
public class IcebergTableSink implements DynamicTableSink, SupportsPartitioning, SupportsOverwrite {
    private static final Logger LOG = LoggerFactory.getLogger(IcebergTableSink.class);
    private final TableLoader tableLoader;
    private final TableSchema tableSchema;
    private final SyncRewriteDataFilesActionOption compactAction;
    private final MetricOption metricOption;
    private final boolean appendMode;

    private boolean overwrite = false;

    private IcebergTableSink(IcebergTableSink toCopy) {
        this.tableLoader = toCopy.tableLoader;
        this.tableSchema = toCopy.tableSchema;
        this.compactAction = toCopy.compactAction;
        this.metricOption = toCopy.metricOption;
        this.overwrite = toCopy.overwrite;
        this.appendMode = toCopy.appendMode;
    }

    public IcebergTableSink(
            TableLoader tableLoader,
            TableSchema tableSchema,
            SyncRewriteDataFilesActionOption compactAction,
            MetricOption metricOption,
            boolean appendMode) {
        this.tableLoader = tableLoader;
        this.tableSchema = tableSchema;
        this.compactAction = compactAction;
        this.metricOption = metricOption;
        this.appendMode = appendMode;
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        Preconditions.checkState(!overwrite || context.isBounded(),
                "Unbounded data stream doesn't support overwrite operation.");

        List<String> equalityColumns = tableSchema.getPrimaryKey()
                .map(UniqueConstraint::getColumns)
                .orElseGet(ImmutableList::of);

        return (DataStreamSinkProvider) dataStream -> FlinkSink.forRowData(dataStream)
                .tableLoader(tableLoader)
                .tableSchema(tableSchema)
                .equalityFieldColumns(equalityColumns)
                .overwrite(overwrite)
                .metric(metricOption)
                .appendMode(appendMode)
                .compact(compactAction)
                .append();
    }

    @Override
    public void applyStaticPartition(Map<String, String> partition) {
        // The flink's PartitionFanoutWriter will handle the static partition write policy automatically.
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        if (appendMode) {
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
