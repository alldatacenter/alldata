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

package org.apache.inlong.manager.pojo.sink.hudi;

import lombok.Getter;

/**
 * Hudi data type
 */
public enum HudiType {

    BOOLEAN("boolean", "boolean"),
    INT("int", "int"),
    LONG("long", "bigint"),
    FLOAT("float", "float"),
    DOUBLE("double", "double"),
    DATE("date", "date"),
    TIME("time", "time(0)"),
    TIMESTAMP("timestamp", "timestamp(3)"),
    TIMESTAMPT_Z("timestamptz", "timestamp(6)"),
    STRING("string", "varchar(" + Integer.MAX_VALUE + ")"),
    BINARY("binary", "tinyint"),
    UUID("uuid", "uuid"),
    FIXED("fixed", null),
    DECIMAL("decimal", null);

    @Getter
    private final String type;

    @Getter
    private final String hiveType;

    HudiType(String type, String hiveType) {
        this.type = type;
        this.hiveType = hiveType;
    }

    /**
     * Get type from name
     */
    public static HudiType forType(String type) {
        for (HudiType ibType : values()) {
            if (ibType.getType().equalsIgnoreCase(type)) {
                return ibType;
            }
        }
        throw new IllegalArgumentException(String.format("invalid hudi type = %s", type));
    }
}
