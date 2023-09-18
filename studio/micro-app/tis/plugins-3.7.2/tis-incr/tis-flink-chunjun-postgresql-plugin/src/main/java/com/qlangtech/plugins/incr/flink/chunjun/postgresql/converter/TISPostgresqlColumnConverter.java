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

package com.qlangtech.plugins.incr.flink.chunjun.postgresql.converter;

import com.dtstack.chunjun.conf.ChunJunCommonConf;
import com.dtstack.chunjun.connector.jdbc.sink.IFieldNamesAttachedStatement;
import org.apache.flink.connector.jdbc.statement.FieldNamedPreparedStatement;
import com.dtstack.chunjun.connector.postgresql.converter.PostgresqlColumnConverter;
import com.dtstack.chunjun.converter.IDeserializationConverter;
import com.dtstack.chunjun.converter.ISerializationConverter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.table.types.logical.LogicalType;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-23 10:46
 **/
public class TISPostgresqlColumnConverter extends PostgresqlColumnConverter {

    public TISPostgresqlColumnConverter(ChunJunCommonConf commonConf, int fieldCount
            , List<IDeserializationConverter> toInternalConverters
            , List<Pair<ISerializationConverter<IFieldNamesAttachedStatement>
            , LogicalType>> toExternalConverters) {
        super(commonConf, fieldCount, toInternalConverters, toExternalConverters);
    }

//    public TISPostgresqlColumnConverter(RowType rowType, ChunJunCommonConf commonConf) {
//        super(rowType, commonConf);
//    }

//    @Override
//    protected ISerializationConverter<FieldNamedPreparedStatement> createExternalConverter(LogicalType type) {
//        return super.createExternalConverter(type);
//        switch (type.getTypeRoot()) {
//            case BOOLEAN:
//                return (val, index, statement) -> {
//                    statement.setBoolean(index, val.getBoolean(index));
//                };
//            case TINYINT:
//                return (val, index, statement) -> statement.setByte(index, val.getByte(index));
//            case SMALLINT:
//            case INTEGER:
//            case INTERVAL_YEAR_MONTH:
//                return (val, index, statement) ->
//                        statement.setInt(index, val.getInt(index));// ((ColumnRowData) val).getField(index).asYearInt());
//            case FLOAT:
//                return (val, index, statement) -> {
//                    statement.setFloat(index, val.getFloat(index));
//                };// ((ColumnRowData) val).getField(index).asFloat());
//            case DOUBLE:
//                return (val, index, statement) ->
//                        statement.setDouble(
//                                index,  ((ColumnRowData) val).getField(index).asDouble());
//
//            case BIGINT:
//                return (val, index, statement) ->
//                        statement.setLong(index, ((ColumnRowData) val).getField(index).asLong());
//            case DECIMAL:
//                return (val, index, statement) ->
//                        statement.setBigDecimal(
//                                index, ((ColumnRowData) val).getField(index).asBigDecimal());
//            case CHAR:
//            case VARCHAR:
//                return (val, index, statement) ->
//                        statement.setString(
//                                index, ((ColumnRowData) val).getField(index).asString());
//            case DATE:
//                return (val, index, statement) ->
//                        statement.setDate(index, ((ColumnRowData) val).getField(index).asSqlDate());
//            case TIME_WITHOUT_TIME_ZONE:
//                return (val, index, statement) ->
//                        statement.setTime(index, ((ColumnRowData) val).getField(index).asTime());
//            case TIMESTAMP_WITH_TIME_ZONE:
//            case TIMESTAMP_WITHOUT_TIME_ZONE:
//                return (val, index, statement) ->
//                        statement.setTimestamp(
//                                index, ((ColumnRowData) val).getField(index).asTimestamp());
//
//            case BINARY:
//            case VARBINARY:
//                return (val, index, statement) ->
//                        statement.setBytes(index, ((ColumnRowData) val).getField(index).asBytes());
//            case ARRAY:
//                return (val, index, statement) ->
//                        statement.setArray(
//                                index, (Array) ((ColumnRowData) val).getField(index).getData());
//            default:
//                throw new UnsupportedOperationException("Unsupported type:" + type);
//        }


   // }
}
