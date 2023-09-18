/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.connector.api;

import io.datavines.common.enums.DataType;
import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Dialect {

    String getDriver();

    String getColumnPrefix();

    String getColumnSuffix();

    default Map<String,String> getDialectKeyMap() {
        return new HashMap<>();
    }

    List<String> getExcludeDatabases();

    default String invalidateItemCanOutput(){
        return "true";
    }

    default String getJDBCType(DataType dataType){
        return dataType.toString();
    }

    default DataType getDataType(String jdbcType) {
        return DataType.valueOf(jdbcType);
    }

    default String quoteIdentifier(String column) {
        return "`" + column + "`";
    }

    default String getTableExistsQuery(String table) {
        return String.format("SELECT * FROM %s WHERE 1=0", table);
    }

    default String getSchemaQuery(String table) {
        return String.format("SELECT * FROM %s WHERE 1=0", table);
    }
}
