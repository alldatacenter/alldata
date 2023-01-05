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

package com.qlangtech.tis.plugins.incr.flink.chunjun.common;

import com.dtstack.chunjun.connector.jdbc.conf.JdbcConf;
import com.dtstack.chunjun.connector.jdbc.converter.JdbcColumnConverter;
import com.dtstack.chunjun.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.chunjun.converter.AbstractRowConverter;
import com.dtstack.chunjun.converter.IDeserializationConverter;
import com.dtstack.chunjun.converter.ISerializationConverter;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;
import com.qlangtech.tis.plugins.incr.flink.cdc.AbstractRowDataMapper;
import io.vertx.core.json.JsonArray;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.connector.jdbc.statement.FieldNamedPreparedStatement;
import org.apache.flink.table.types.logical.LogicalType;

import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-21 10:37
 **/
public class DialectUtils {

    /**
     * @param jdbcDialect
     * @param jdbcConf
     * @param colsMeta
     * @param internalConverterCreator
     * @param externalConverterCreator
     * @return
     */
    public static AbstractRowConverter<ResultSet, JsonArray, FieldNamedPreparedStatement, LogicalType>
    createColumnConverter( //
                           JdbcDialect jdbcDialect //
            , JdbcConf jdbcConf //
            , List<IColMetaGetter> colsMeta //
            , Function<LogicalType, IDeserializationConverter> internalConverterCreator
            , Function<FlinkCol, ISerializationConverter<FieldNamedPreparedStatement>> externalConverterCreator) {
        Objects.requireNonNull(jdbcDialect, "jdbcDialect can not be null");
        Objects.requireNonNull(jdbcConf, "jdbcConf can not be null");
        if (CollectionUtils.isEmpty(colsMeta)) {
            throw new IllegalArgumentException("colsMeta can not be empty");
        }
        List<FlinkCol> flinkCols = AbstractRowDataMapper.getAllTabColsMeta(colsMeta.stream().collect(Collectors.toList()));
        List<IDeserializationConverter> toInternalConverters = Lists.newArrayList();
        List<Pair<ISerializationConverter<FieldNamedPreparedStatement>, LogicalType>> toExternalConverters = Lists.newArrayList();
        LogicalType type = null;
        for (FlinkCol col : flinkCols) {
            type = col.type.getLogicalType();
            toInternalConverters.add(internalConverterCreator.apply(type));
            toExternalConverters.add(Pair.of(externalConverterCreator.apply(col), type));
        }

        return jdbcDialect.getColumnConverter(jdbcConf, flinkCols.size(), toInternalConverters, toExternalConverters);
    }


    public static AbstractRowConverter<ResultSet, JsonArray, FieldNamedPreparedStatement, LogicalType>
    createColumnConverter(JdbcDialect jdbcDialect, JdbcConf jdbcConf, List<IColMetaGetter> colsMeta
    ) {
        return createColumnConverter(jdbcDialect, jdbcConf, colsMeta
                , JdbcColumnConverter::getRowDataValConverter
        );
    }

    /**
     * @param jdbcDialect
     * @param jdbcConf
     * @param colsMeta
     * @param internalConverterCreator
     * @return
     */
    public static AbstractRowConverter<ResultSet, JsonArray, FieldNamedPreparedStatement, LogicalType>
    createColumnConverter(JdbcDialect jdbcDialect, JdbcConf jdbcConf, List<IColMetaGetter> colsMeta
            , Function<LogicalType, IDeserializationConverter> internalConverterCreator
    ) {
        return createColumnConverter(jdbcDialect, jdbcConf, colsMeta
                , internalConverterCreator
                , (flinkCol) -> JdbcColumnConverter.createJdbcStatementValConverter(
                        flinkCol.type.getLogicalType(), flinkCol.getRowDataValGetter()));
    }

}
