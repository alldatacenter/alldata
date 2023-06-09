/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.incr.flink.connector;

import com.dtstack.chunjun.conf.ChunJunCommonConf;
import com.dtstack.chunjun.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.chunjun.connector.jdbc.sink.IFieldNamesAttachedStatement;
import com.dtstack.chunjun.connector.jdbc.source.JdbcInputSplit;
import com.dtstack.chunjun.converter.AbstractRowConverter;
import com.dtstack.chunjun.converter.IDeserializationConverter;
import com.dtstack.chunjun.converter.ISerializationConverter;
import com.dtstack.chunjun.converter.RawTypeConverter;
import com.dtstack.chunjun.sink.WriteMode;
import io.vertx.core.json.JsonArray;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.connector.jdbc.statement.FieldNamedPreparedStatement;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-26 20:24
 **/
public class WriteModeFilterJdbcDialect implements JdbcDialect {
    private final JdbcDialect dialect;
    private final Set<WriteMode> supportWriteModes;

    public WriteModeFilterJdbcDialect(JdbcDialect dialect, Set<WriteMode> supportWriteModes) {
        this.dialect = dialect;
        this.supportWriteModes = supportWriteModes;
    }

    @Override
    public Optional<String> getReplaceStatement(String schema, String tableName, List<String> fieldNames) {
        if (!supportWriteModes.contains(WriteMode.REPLACE)) {
            return Optional.empty();
        }
        return dialect.getReplaceStatement(schema, tableName, fieldNames);
    }

    @Override
    public String getInsertIntoStatement(String schema, String tableName, List<String> fieldNames) {
        if (!supportWriteModes.contains(WriteMode.INSERT)) {
            throw new UnsupportedOperationException("insert is not support");
        }
        return dialect.getInsertIntoStatement(schema, tableName, fieldNames);
    }

    @Override
    public String getUpdateStatement(String schema, String tableName, String[] fieldNames, String[] conditionFields) {
        if (!supportWriteModes.contains(WriteMode.UPDATE)) {
            throw new UnsupportedOperationException("update is not support," + dialect.getClass().getName());
        }
        return dialect.getUpdateStatement(schema, tableName, fieldNames, conditionFields);
    }

    @Override
    public Optional<String> getUpsertStatement(String schema, String tableName, List<String> fieldNames, List<String> uniqueKeyFields, boolean allReplace) {
        if (!supportWriteModes.contains(WriteMode.UPSERT)) {
            return Optional.empty();
        }
        return dialect.getUpsertStatement(schema, tableName, fieldNames, uniqueKeyFields, allReplace);
    }

    @Override
    public String getDeleteStatement(String schema, String tableName, List<String> conditionFields) {
        return dialect.getDeleteStatement(schema, tableName, conditionFields);
    }


    @Override
    public String dialectName() {
        return dialect.dialectName();
    }

    @Override
    public boolean canHandle(String url) {
        return dialect.canHandle(url);
    }

    @Override
    public RawTypeConverter getRawTypeConverter() {
        return dialect.getRawTypeConverter();
    }

    @Override
    public AbstractRowConverter<ResultSet, JsonArray, FieldNamedPreparedStatement, LogicalType> getRowConverter(int fieldCount, List<IDeserializationConverter> toInternalConverters, List<Pair<ISerializationConverter<FieldNamedPreparedStatement>, LogicalType>> toExternalConverters) {
        return dialect.getRowConverter(fieldCount, toInternalConverters, toExternalConverters);
    }

    @Override
    public AbstractRowConverter<ResultSet, JsonArray, FieldNamedPreparedStatement, LogicalType> getRowConverter(RowType rowType) {
        return dialect.getRowConverter(rowType);
    }

    @Override
    public AbstractRowConverter<ResultSet, JsonArray, FieldNamedPreparedStatement, LogicalType> getColumnConverter(RowType rowType) {
        return dialect.getColumnConverter(rowType);
    }

    @Override
    public AbstractRowConverter<ResultSet, JsonArray, FieldNamedPreparedStatement, LogicalType> getColumnConverter(RowType rowType, ChunJunCommonConf commonConf) {
        return dialect.getColumnConverter(rowType, commonConf);
    }

    @Override
    public AbstractRowConverter<ResultSet, JsonArray, IFieldNamesAttachedStatement, LogicalType> getColumnConverter(
            ChunJunCommonConf commonConf, int fieldCount, List<IDeserializationConverter> toInternalConverters
            , List<Pair<ISerializationConverter<IFieldNamesAttachedStatement>, LogicalType>> toExternalConverters) {
        return dialect.getColumnConverter(commonConf, fieldCount, toInternalConverters, toExternalConverters);
    }

    @Override
    public void validate(TableSchema schema) throws ValidationException {
        dialect.validate(schema);
    }

    @Override
    public Optional<String> defaultDriverName() {
        return dialect.defaultDriverName();
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return dialect.quoteIdentifier(identifier);
    }


    @Override
    public String getSqlQueryFields(String schema, String tableName) {
        return dialect.getSqlQueryFields(schema, tableName);
    }

    @Override
    public String quoteTable(String table) {
        return dialect.quoteTable(table);
    }

    @Override
    public String getRowExistsStatement(String schema, String tableName, String[] conditionFields) {
        return dialect.getRowExistsStatement(schema, tableName, conditionFields);
    }


    @Override
    public Optional<String> getUpdateBeforeStatement(String schema, String tableName, List<String> conditionFields) {
        return dialect.getUpdateBeforeStatement(schema, tableName, conditionFields);
    }

    @Override
    public String getSelectFromStatement(String schema, String tableName, String[] selectFields, String[] conditionFields) {
        return dialect.getSelectFromStatement(schema, tableName, selectFields, conditionFields);
    }

    @Override
    public String getSelectFromStatement(String schemaName, String tableName, String customSql, String[] selectFields, String where) {
        return dialect.getSelectFromStatement(schemaName, tableName, customSql, selectFields, where);
    }

    @Override
    public String getSelectFromStatement(String schemaName, String tableName, String customSql, String[] selectFields, String[] selectCustomFields, String where) {
        return dialect.getSelectFromStatement(schemaName, tableName, customSql, selectFields, selectCustomFields, where);
    }

    @Override
    public String buildTableInfoWithSchema(String schema, String tableName) {
        return dialect.buildTableInfoWithSchema(schema, tableName);
    }

    @Override
    public String getRowNumColumn(String orderBy) {
        return dialect.getRowNumColumn(orderBy);
    }

    @Override
    public String getRowNumColumnAlias() {
        return dialect.getRowNumColumnAlias();
    }

    @Override
    public String getSplitRangeFilter(JdbcInputSplit split, String splitPkName) {
        return dialect.getSplitRangeFilter(split, splitPkName);
    }

    @Override
    public String getSplitModFilter(JdbcInputSplit split, String splitPkName) {
        return dialect.getSplitModFilter(split, splitPkName);
    }
}
