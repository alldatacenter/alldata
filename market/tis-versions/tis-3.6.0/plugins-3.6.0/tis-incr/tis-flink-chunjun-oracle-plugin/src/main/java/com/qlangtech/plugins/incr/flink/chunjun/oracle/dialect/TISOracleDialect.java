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

package com.qlangtech.plugins.incr.flink.chunjun.oracle.dialect;

import com.dtstack.chunjun.conf.SyncConf;
import com.dtstack.chunjun.connector.jdbc.conf.JdbcConf;
import com.dtstack.chunjun.connector.jdbc.sink.JdbcSinkFactory;
import com.dtstack.chunjun.connector.oracle.dialect.OracleDialect;
import com.dtstack.chunjun.converter.RawTypeConverter;
import com.qlangtech.tis.plugins.incr.flink.cdc.AbstractRowDataMapper;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-25 15:01
 **/
public class TISOracleDialect extends OracleDialect {
//    @Override
//    public Optional<String> getReplaceStatement(String schema, String tableName, List<String> fieldNames) {
//        return super.getReplaceStatement(schema, tableName, fieldNames);
//
////        String schema,
////        String tableName,
////        List<String> fieldNames,
////        List<String> uniqueKeyFields,
////        boolean allReplace
//
//        if (CollectionUtils.isEmpty(jdbcConf.getUniqueKey())) {
//            t
//    }

    private final JdbcConf jdbcConf;

    public TISOracleDialect(SyncConf syncConf) {

        this.jdbcConf = JdbcSinkFactory.getJdbcConf(syncConf);
    }

    @Override
    public RawTypeConverter getRawTypeConverter() {
        //return OracleRawTypeConverter::apply;
        //DataType apply(ColMeta type);
        return (colMeta) -> AbstractRowDataMapper.mapFlinkCol(colMeta, -1).type;
    }

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
