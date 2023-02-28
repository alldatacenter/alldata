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
package io.datavines.common.enums;

import java.util.HashMap;

public enum DataType {
    /**
     *
     */
    NULL_TYPE,
    BOOLEAN_TYPE,
    BYTE_TYPE,
    SHORT_TYPE,
    INT_TYPE,
    LONG_TYPE,
    FLOAT_TYPE,
    DOUBLE_TYPE,
    TIME_TYPE,
    DATE_TYPE,
    TIMESTAMP_TYPE,
    STRING_TYPE,
    BYTES_TYPE,
    BIG_DECIMAL_TYPE,
    OBJECT;

    private static final HashMap<String, DataType> DATA_TYPE_MAP = new HashMap<>();

    static {
        for (DataType dataType: DataType.values()){
            DATA_TYPE_MAP.put(dataType.name(), dataType);
        }
    }

    public static DataType of(String dataType){
        if(DATA_TYPE_MAP.containsKey(dataType)){
            return DATA_TYPE_MAP.get(dataType);
        }
        throw new IllegalArgumentException("invalid data type : " + dataType);
    }
}
