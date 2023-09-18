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

package com.qlangtech.plugins.incr.flink.chunjun.postgresql.dialect;

import com.dtstack.chunjun.conf.ChunJunCommonConf;
import com.dtstack.chunjun.conf.SyncConf;
import com.dtstack.chunjun.connector.jdbc.conf.JdbcConf;
import com.dtstack.chunjun.connector.jdbc.sink.IFieldNamesAttachedStatement;
import com.dtstack.chunjun.connector.jdbc.sink.JdbcSinkFactory;
import org.apache.flink.connector.jdbc.statement.FieldNamedPreparedStatement;
import com.dtstack.chunjun.connector.postgresql.dialect.PostgresqlDialect;
import com.dtstack.chunjun.converter.AbstractRowConverter;
import com.dtstack.chunjun.converter.IDeserializationConverter;
import com.dtstack.chunjun.converter.ISerializationConverter;
import com.dtstack.chunjun.converter.RawTypeConverter;
import com.qlangtech.plugins.incr.flink.chunjun.postgresql.converter.TISPostgresqlColumnConverter;
import com.qlangtech.tis.plugins.incr.flink.cdc.AbstractRowDataMapper;
import io.vertx.core.json.JsonArray;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.table.types.logical.LogicalType;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-23 10:44
 **/
public class TISPostgresqlDialect extends PostgresqlDialect {

    private final JdbcConf jdbcConf;

    public TISPostgresqlDialect(SyncConf syncConf) {
        this.jdbcConf = JdbcSinkFactory.getJdbcConf(syncConf);
    }

//    @Override
//    public AbstractRowConverter<ResultSet, JsonArray, FieldNamedPreparedStatement, LogicalType>
//    getColumnConverter(RowType rowType, ChunJunCommonConf commonConf) {
//        return new TISPostgresqlColumnConverter(rowType, commonConf);
//    }

    @Override
    public AbstractRowConverter<ResultSet, JsonArray, IFieldNamesAttachedStatement, LogicalType>
    getColumnConverter(
            ChunJunCommonConf commonConf, int fieldCount, List<IDeserializationConverter> toInternalConverters
            , List<Pair<ISerializationConverter<IFieldNamesAttachedStatement>, LogicalType>> toExternalConverters) {
        return new TISPostgresqlColumnConverter(commonConf, fieldCount, toInternalConverters, toExternalConverters);
    }


//    @Override
//    public AbstractRowConverter<ResultSet, JsonArray, FieldNamedPreparedStatement, LogicalType>
//    getRowConverter(
//            int fieldCount, List<IDeserializationConverter> toInternalConverters
//            , List<Pair<ISerializationConverter<FieldNamedPreparedStatement>, LogicalType>> toExternalConverters) {
//        return new TISPostgresqlColumnConverter(fieldCount, toInternalConverters, toExternalConverters);
//    }

    @Override
    public RawTypeConverter getRawTypeConverter() {
        return (colMeta) -> AbstractRowDataMapper.mapFlinkCol(colMeta, -1).type;
    }


//    @Override
//    public Optional<String> getUpsertStatement(String schema, String tableName, String[] fieldNames, String[] uniqueKeyFields, boolean allReplace) {
//        return super.getUpsertStatement(schema, tableName, fieldNames, uniqueKeyFields, allReplace);
//    }

    @Override
    public Optional<String> getReplaceStatement(String schema, String tableName, List<String> fieldNames) {
        if (CollectionUtils.isEmpty(jdbcConf.getUniqueKey())) {
            throw new IllegalArgumentException("jdbcConf.getUniqueKey() can not be empty");
        }
        return getUpsertStatement(schema, tableName, fieldNames, jdbcConf.getUniqueKey(), false);
    }

    @Override
    public Optional<String> getUpdateBeforeStatement(String schema, String tableName, List<String> conditionFields) {
        throw new UnsupportedOperationException("update_before is not support");
    }
}
