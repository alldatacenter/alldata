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

package org.apache.inlong.sort.jdbc.internal;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.connector.jdbc.dialect.JdbcDialect;
import org.apache.flink.connector.jdbc.internal.converter.JdbcRowConverter;
import org.apache.flink.connector.jdbc.internal.executor.JdbcBatchStatementExecutor;
import org.apache.flink.connector.jdbc.internal.executor.TableBufferReducedStatementExecutor;
import org.apache.flink.connector.jdbc.internal.executor.TableBufferedStatementExecutor;
import org.apache.flink.connector.jdbc.internal.executor.TableInsertOrUpdateStatementExecutor;
import org.apache.flink.connector.jdbc.internal.executor.TableSimpleStatementExecutor;
import org.apache.flink.connector.jdbc.internal.options.JdbcDmlOptions;
import org.apache.flink.connector.jdbc.internal.options.JdbcOptions;
import org.apache.flink.connector.jdbc.statement.FieldNamedPreparedStatement;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;

import java.util.Arrays;
import java.util.function.Function;

import static org.apache.flink.table.data.RowData.createFieldGetter;
import static org.apache.flink.util.Preconditions.checkArgument;

/**
 * Comm function for A JDBC multi-table outputFormat to get or create JDBC Executor
 */
public class JdbcMultiBatchingComm {

    public static JdbcBatchStatementExecutor<RowData> createBufferReduceExecutor(
            JdbcDmlOptions opt,
            RuntimeContext ctx,
            TypeInformation<RowData> rowDataTypeInfo,
            LogicalType[] fieldTypes) {
        checkArgument(opt.getKeyFields().isPresent());
        JdbcDialect dialect = opt.getDialect();
        String tableName = opt.getTableName();
        String[] pkNames = opt.getKeyFields().get();
        int[] pkFields =
                Arrays.stream(pkNames)
                        .mapToInt(Arrays.asList(opt.getFieldNames())::indexOf)
                        .toArray();
        LogicalType[] pkTypes =
                Arrays.stream(pkFields).mapToObj(f -> fieldTypes[f]).toArray(LogicalType[]::new);
        final TypeSerializer<RowData> typeSerializer =
                rowDataTypeInfo.createSerializer(ctx.getExecutionConfig());
        final Function<RowData, RowData> valueTransform =
                ctx.getExecutionConfig().isObjectReuseEnabled()
                        ? typeSerializer::copy
                        : Function.identity();

        return new TableBufferReducedStatementExecutor(
                createUpsertRowExecutor(
                        dialect,
                        tableName,
                        opt.getFieldNames(),
                        fieldTypes,
                        pkFields,
                        pkNames,
                        pkTypes),
                createDeleteExecutor(dialect, tableName, pkNames, pkTypes),
                createRowKeyExtractor(fieldTypes, pkFields),
                valueTransform);
    }

    public static JdbcBatchStatementExecutor<RowData> createSimpleBufferedExecutor(
            RuntimeContext ctx,
            JdbcDialect dialect,
            String[] fieldNames,
            LogicalType[] fieldTypes,
            String sql,
            TypeInformation<RowData> rowDataTypeInfo) {
        final TypeSerializer<RowData> typeSerializer =
                rowDataTypeInfo.createSerializer(ctx.getExecutionConfig());
        return new TableBufferedStatementExecutor(
                createSimpleRowExecutor(dialect, fieldNames, fieldTypes, sql),
                ctx.getExecutionConfig().isObjectReuseEnabled()
                        ? typeSerializer::copy
                        : Function.identity());
    }

    private static JdbcBatchStatementExecutor<RowData> createUpsertRowExecutor(
            JdbcDialect dialect,
            String tableName,
            String[] fieldNames,
            LogicalType[] fieldTypes,
            int[] pkFields,
            String[] pkNames,
            LogicalType[] pkTypes) {
        return dialect.getUpsertStatement(tableName, fieldNames, pkNames)
                .map(sql -> createSimpleRowExecutor(dialect, fieldNames, fieldTypes, sql))
                .orElseGet(
                        () -> createInsertOrUpdateExecutor(
                                dialect,
                                tableName,
                                fieldNames,
                                fieldTypes,
                                pkFields,
                                pkNames,
                                pkTypes));
    }

    private static JdbcBatchStatementExecutor<RowData> createDeleteExecutor(
            JdbcDialect dialect, String tableName, String[] pkNames, LogicalType[] pkTypes) {
        String deleteSql = dialect.getDeleteStatement(tableName, pkNames);
        return createSimpleRowExecutor(dialect, pkNames, pkTypes, deleteSql);
    }

    private static JdbcBatchStatementExecutor<RowData> createSimpleRowExecutor(
            JdbcDialect dialect, String[] fieldNames, LogicalType[] fieldTypes, final String sql) {
        final JdbcRowConverter rowConverter = dialect.getRowConverter(RowType.of(fieldTypes));
        return new TableSimpleStatementExecutor(
                connection -> FieldNamedPreparedStatement.prepareStatement(connection, sql, fieldNames),
                rowConverter);
    }

    private static JdbcBatchStatementExecutor<RowData> createInsertOrUpdateExecutor(
            JdbcDialect dialect,
            String tableName,
            String[] fieldNames,
            LogicalType[] fieldTypes,
            int[] pkFields,
            String[] pkNames,
            LogicalType[] pkTypes) {
        final String existStmt = dialect.getRowExistsStatement(tableName, pkNames);
        final String insertStmt = dialect.getInsertIntoStatement(tableName, fieldNames);
        final String updateStmt = dialect.getUpdateStatement(tableName, fieldNames, pkNames);
        return new TableInsertOrUpdateStatementExecutor(
                connection -> FieldNamedPreparedStatement.prepareStatement(
                        connection, existStmt, pkNames),
                connection -> FieldNamedPreparedStatement.prepareStatement(
                        connection, insertStmt, fieldNames),
                connection -> FieldNamedPreparedStatement.prepareStatement(
                        connection, updateStmt, fieldNames),
                dialect.getRowConverter(RowType.of(pkTypes)),
                dialect.getRowConverter(RowType.of(fieldTypes)),
                dialect.getRowConverter(RowType.of(fieldTypes)),
                createRowKeyExtractor(fieldTypes, pkFields));
    }

    private static Function<RowData, RowData> createRowKeyExtractor(
            LogicalType[] logicalTypes, int[] pkFields) {
        final RowData.FieldGetter[] fieldGetters = new RowData.FieldGetter[pkFields.length];
        for (int i = 0; i < pkFields.length; i++) {
            fieldGetters[i] = createFieldGetter(logicalTypes[pkFields[i]], pkFields[i]);
        }
        return row -> getPrimaryKey(row, fieldGetters);
    }

    private static RowData getPrimaryKey(RowData row, RowData.FieldGetter[] fieldGetters) {
        GenericRowData pkRow = new GenericRowData(fieldGetters.length);
        for (int i = 0; i < fieldGetters.length; i++) {
            pkRow.setField(i, fieldGetters[i].getFieldOrNull(row));
        }
        return pkRow;
    }

    public static JdbcOptions getExecJdbcOptions(JdbcOptions jdbcOptions, String tableIdentifier) {
        JdbcOptions jdbcExecOptions =
                JdbcOptions.builder()
                        .setDBUrl(jdbcOptions.getDbURL() + "/" + getDatabaseNameFromIdentifier(tableIdentifier))
                        .setTableName(getTableNameFromIdentifier(tableIdentifier))
                        .setDialect(jdbcOptions.getDialect())
                        .setParallelism(jdbcOptions.getParallelism())
                        .setConnectionCheckTimeoutSeconds(jdbcOptions.getConnectionCheckTimeoutSeconds())
                        .setDriverName(jdbcOptions.getDriverName())
                        .setUsername(jdbcOptions.getUsername().orElse(""))
                        .setPassword(jdbcOptions.getPassword().orElse(""))
                        .build();
        return jdbcExecOptions;
    }

    /**
     * Get table name From tableIdentifier
     * tableIdentifier maybe: ${dbName}.${tbName} or ${dbName}.${schemaName}.${tbName}
     *
     * @param tableIdentifier The table identifier for which to get table name.
     */
    public static String getTableNameFromIdentifier(String tableIdentifier) {
        String[] fieldArray = tableIdentifier.split("\\.");
        if (2 == fieldArray.length) {
            return fieldArray[1];
        }
        if (3 == fieldArray.length) {
            return fieldArray[1] + "." + fieldArray[2];
        }
        return null;
    }

    /**
     * Get database name From tableIdentifier
     * tableIdentifier maybe: ${dbName}.${tbName} or ${dbName}.${schemaName}.${tbName}
     *
     * @param tableIdentifier The table identifier for which to get table name.
     */
    public static String getDatabaseNameFromIdentifier(String tableIdentifier) {
        String[] fileArray = tableIdentifier.split("\\.");
        return fileArray[0];
    }

}
