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

package org.apache.inlong.manager.common.enums;

import org.apache.inlong.manager.common.util.Preconditions;

public enum FieldType {

    INT,
    TINYINT,
    SMALLINT,
    BIGINT,
    SHORT,
    LONG,
    DOUBLE,
    FLOAT,
    DECIMAL,
    STRING,
    FIXED,
    BYTE,
    BINARY,
    BOOLEAN,
    DATE,
    TIME,
    INT8,
    INT16,
    INT32,
    INT64,
    FLOAT32,
    FLOAT64,
    DATETIME,
    TIMESTAMP,
    LOCAL_ZONE_TIMESTAMP,
    ARRAY,
    MAP,
    STRUCT,
    FUNCTION;

    public static FieldType forName(String name) {
        Preconditions.checkNotNull(name, "FieldType should not be null");
        for (FieldType value : values()) {
            if (value.toString().equalsIgnoreCase(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FieldType : %s", name));
    }

    @Override
    public String toString() {
        return name();
    }
}
