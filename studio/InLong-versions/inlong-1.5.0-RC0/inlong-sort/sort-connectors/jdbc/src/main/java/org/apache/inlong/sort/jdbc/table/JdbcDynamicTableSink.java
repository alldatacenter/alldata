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

package org.apache.inlong.sort.jdbc.table;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.internal.options.JdbcDmlOptions;
import org.apache.flink.connector.jdbc.internal.options.JdbcOptions;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.jdbc.internal.GenericJdbcSinkFunction;

import javax.annotation.Nullable;
import java.util.Objects;

import static org.apache.flink.util.Preconditions.checkState;

/**
 * Copy from org.apache.flink:flink-connector-jdbc_2.11:1.13.5
 *
 * A {@link DynamicTableSink} for JDBC.
 * Add an option `sink.ignore.changelog` to support insert-only mode without primaryKey.
 */
@Internal
public class JdbcDynamicTableSink implements DynamicTableSink {

    private final JdbcOptions jdbcOptions;
    private final JdbcExecutionOptions executionOptions;
    private final JdbcDmlOptions dmlOptions;
    private final TableSchema tableSchema;
    private final String dialectName;

    private final boolean multipleSink;
    private final String inlongMetric;
    private final String auditHostAndPorts;
    private final boolean appendMode;
    private final String sinkMultipleFormat;
    private final String databasePattern;
    private final String tablePattern;
    private final String schemaPattern;
    private final SchemaUpdateExceptionPolicy schemaUpdateExceptionPolicy;

    private final DirtyOptions dirtyOptions;
    private @Nullable final DirtySink<Object> dirtySink;

    public JdbcDynamicTableSink(
            JdbcOptions jdbcOptions,
            JdbcExecutionOptions executionOptions,
            JdbcDmlOptions dmlOptions,
            TableSchema tableSchema,
            boolean appendMode,
            boolean multipleSink,
            String sinkMultipleFormat,
            String databasePattern,
            String tablePattern,
            String schemaPattern,
            String inlongMetric,
            String auditHostAndPorts,
            SchemaUpdateExceptionPolicy schemaUpdateExceptionPolicy,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink) {
        this.jdbcOptions = jdbcOptions;
        this.executionOptions = executionOptions;
        this.dmlOptions = dmlOptions;
        this.tableSchema = tableSchema;
        this.dialectName = dmlOptions.getDialect().dialectName();
        this.appendMode = appendMode;
        this.multipleSink = multipleSink;
        this.sinkMultipleFormat = sinkMultipleFormat;
        this.databasePattern = databasePattern;
        this.tablePattern = tablePattern;
        this.schemaPattern = schemaPattern;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
        this.schemaUpdateExceptionPolicy = schemaUpdateExceptionPolicy;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
    }

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        if (!multipleSink) {
            validatePrimaryKey(requestedMode);
        }
        return ChangelogMode.all();
    }

    private void validatePrimaryKey(ChangelogMode requestedMode) {
        checkState(
                ChangelogMode.insertOnly().equals(requestedMode)
                        || dmlOptions.getKeyFields().isPresent() || appendMode,
                "please declare primary key or appendMode for sink table when query contains update/delete record.");
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {
        final TypeInformation<RowData> rowDataTypeInformation =
                context.createTypeInformation(tableSchema.toRowDataType());
        final JdbcDynamicOutputFormatBuilder builder = new JdbcDynamicOutputFormatBuilder();
        builder.setAppendMode(appendMode);
        builder.setJdbcOptions(jdbcOptions);
        builder.setJdbcDmlOptions(dmlOptions);
        builder.setJdbcExecutionOptions(executionOptions);
        builder.setInLongMetric(inlongMetric);
        builder.setAuditHostAndPorts(auditHostAndPorts);
        builder.setDirtyOptions(dirtyOptions);
        builder.setDirtySink(dirtySink);
        if (multipleSink) {
            builder.setSinkMultipleFormat(sinkMultipleFormat);
            builder.setDatabasePattern(databasePattern);
            builder.setTablePattern(tablePattern);
            builder.setSchemaPattern(schemaPattern);
            builder.setSchemaUpdatePolicy(schemaUpdateExceptionPolicy);
            return SinkFunctionProvider.of(
                    new GenericJdbcSinkFunction<>(builder.buildMulti()), jdbcOptions.getParallelism());
        } else {
            builder.setRowDataTypeInfo(rowDataTypeInformation);
            builder.setFieldDataTypes(tableSchema.getFieldDataTypes());
            return SinkFunctionProvider.of(
                    new GenericJdbcSinkFunction<>(builder.build()), jdbcOptions.getParallelism());
        }
    }

    @Override
    public DynamicTableSink copy() {
        return new JdbcDynamicTableSink(jdbcOptions, executionOptions, dmlOptions,
                tableSchema, appendMode, multipleSink, sinkMultipleFormat,
                databasePattern, tablePattern, schemaPattern,
                inlongMetric, auditHostAndPorts,
                schemaUpdateExceptionPolicy, dirtyOptions, dirtySink);
    }

    @Override
    public String asSummaryString() {
        return "JDBC:" + dialectName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JdbcDynamicTableSink)) {
            return false;
        }
        JdbcDynamicTableSink that = (JdbcDynamicTableSink) o;
        return Objects.equals(jdbcOptions, that.jdbcOptions)
                && Objects.equals(executionOptions, that.executionOptions)
                && Objects.equals(dmlOptions, that.dmlOptions)
                && Objects.equals(tableSchema, that.tableSchema)
                && Objects.equals(dialectName, that.dialectName)
                && Objects.equals(inlongMetric, that.inlongMetric)
                && Objects.equals(auditHostAndPorts, that.auditHostAndPorts)
                && Objects.equals(dirtyOptions, that.dirtyOptions)
                && Objects.equals(dirtySink, that.dirtySink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jdbcOptions, executionOptions, dmlOptions, tableSchema, dialectName,
                inlongMetric, auditHostAndPorts);
    }
}