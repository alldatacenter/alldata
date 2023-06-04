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

package org.apache.inlong.sort.doris.table;

import org.apache.doris.flink.cfg.DorisExecutionOptions;
import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.flink.cfg.DorisReadOptions;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;
import org.apache.inlong.sort.doris.internal.GenericDorisSinkFunction;

import javax.annotation.Nullable;

/**
 * DorisDynamicTableSink copy from {@link org.apache.doris.flink.table.DorisDynamicTableSink}
 * It supports both single table sink and multiple table sink
 **/
public class DorisDynamicTableSink implements DynamicTableSink {

    private final DorisOptions options;
    private final DorisReadOptions readOptions;
    private final DorisExecutionOptions executionOptions;
    private final TableSchema tableSchema;
    private final boolean multipleSink;
    private final String sinkMultipleFormat;
    private final String databasePattern;
    private final String tablePattern;
    private final boolean ignoreSingleTableErrors;
    private final SchemaUpdateExceptionPolicy schemaUpdatePolicy;
    private final String inlongMetric;
    private final String auditHostAndPorts;
    private final Integer parallelism;
    private final DirtyOptions dirtyOptions;
    private @Nullable final DirtySink<Object> dirtySink;

    public DorisDynamicTableSink(DorisOptions options,
            DorisReadOptions readOptions,
            DorisExecutionOptions executionOptions,
            TableSchema tableSchema,
            boolean multipleSink,
            String sinkMultipleFormat,
            String databasePattern,
            String tablePattern,
            boolean ignoreSingleTableErrors,
            SchemaUpdateExceptionPolicy schemaUpdatePolicy,
            String inlongMetric,
            String auditHostAndPorts,
            Integer parallelism,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink) {
        this.options = options;
        this.readOptions = readOptions;
        this.executionOptions = executionOptions;
        this.tableSchema = tableSchema;
        this.multipleSink = multipleSink;
        this.sinkMultipleFormat = sinkMultipleFormat;
        this.databasePattern = databasePattern;
        this.tablePattern = tablePattern;
        this.ignoreSingleTableErrors = ignoreSingleTableErrors;
        this.schemaUpdatePolicy = schemaUpdatePolicy;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
        this.parallelism = parallelism;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode changelogMode) {
        return ChangelogMode.all();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        DorisDynamicSchemaOutputFormat.Builder builder = DorisDynamicSchemaOutputFormat.builder()
                .setFenodes(options.getFenodes())
                .setUsername(options.getUsername())
                .setPassword(options.getPassword())
                .setReadOptions(readOptions)
                .setExecutionOptions(executionOptions)
                .setInlongMetric(inlongMetric)
                .setAuditHostAndPorts(auditHostAndPorts)
                .setTableIdentifier(options.getTableIdentifier())
                .setFieldDataTypes(tableSchema.getFieldDataTypes())
                .setFieldNames(tableSchema.getFieldNames())
                .setInlongMetric(inlongMetric)
                .setAuditHostAndPorts(auditHostAndPorts)
                .setMultipleSink(multipleSink)
                .setDatabasePattern(databasePattern)
                .setTablePattern(tablePattern)
                .setDynamicSchemaFormat(sinkMultipleFormat)
                .setIgnoreSingleTableErrors(ignoreSingleTableErrors)
                .setSchemaUpdatePolicy(schemaUpdatePolicy)
                .setDirtyOptions(dirtyOptions)
                .setDirtySink(dirtySink);
        return SinkFunctionProvider.of(
                new GenericDorisSinkFunction<>(builder.build()), parallelism);
    }

    @Override
    public DynamicTableSink copy() {
        return new DorisDynamicTableSink(options,
                readOptions,
                executionOptions,
                tableSchema,
                multipleSink,
                sinkMultipleFormat,
                databasePattern,
                tablePattern,
                ignoreSingleTableErrors,
                schemaUpdatePolicy,
                inlongMetric,
                auditHostAndPorts,
                parallelism,
                dirtyOptions,
                dirtySink);
    }

    @Override
    public String asSummaryString() {
        return "Doris Table Sink Of InLong";
    }
}
