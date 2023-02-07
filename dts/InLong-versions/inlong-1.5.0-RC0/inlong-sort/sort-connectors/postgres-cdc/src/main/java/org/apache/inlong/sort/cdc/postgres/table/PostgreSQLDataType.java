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

package org.apache.inlong.sort.cdc.postgres.table;

import java.util.HashMap;
import java.util.Map;

public enum PostgreSQLDataType {

    TINYINT,
    INT,
    LARGEINT,
    SMALLINT,
    BOOLEAN,
    DECIMAL,
    DOUBLE,
    FLOAT,
    BIGINT,
    VARCHAR,
    CHAR,
    STRING,
    JSON,
    DATE,
    DATETIME,
    TIMESTAMP,
    UNKNOWN;

    private static final Map<String, PostgreSQLDataType> dataTypeMap = new HashMap<>();

    static {
        PostgreSQLDataType[] postgreSQLDataTypes = PostgreSQLDataType.values();

        for (PostgreSQLDataType postgreSQLDataType : postgreSQLDataTypes) {
            dataTypeMap.put(postgreSQLDataType.name(), postgreSQLDataType);
            dataTypeMap.put(postgreSQLDataType.name().toLowerCase(), postgreSQLDataType);
        }
    }

    /**
     * convert string type to PostgreSQLDataType instance
     *
     * @param typeString
     * @return
     */
    public static PostgreSQLDataType fromString(String typeString) {
        if (typeString == null) {
            return UNKNOWN;
        }

        PostgreSQLDataType starRocksDataType = dataTypeMap.get(typeString);
        if (starRocksDataType == null) {
            starRocksDataType = dataTypeMap.getOrDefault(typeString.toUpperCase(), PostgreSQLDataType.UNKNOWN);
        }

        return starRocksDataType;
    }
}
