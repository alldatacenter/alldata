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

package org.apache.inlong.manager.pojo.sink.iceberg;

import lombok.Getter;

/**
 * Iceberg data type
 */
public enum IcebergType {

    BOOLEAN("boolean"),
    INT("int"),
    LONG("long"),
    FLOAT("float"),
    DOUBLE("double"),
    DECIMAL("decimal"),
    DATE("date"),
    TIME("time"),
    TIMESTAMP("timestamp"),
    TIMESTAMPTZ("timestamptz"),
    STRING("string"),
    UUID("uuid"),
    FIXED("fixed"),
    BINARY("binary");

    @Getter
    private final String type;

    IcebergType(String type) {
        this.type = type;
    }

    /**
     * Get type from name
     */
    public static IcebergType forType(String type) {
        for (IcebergType ibType : values()) {
            if (ibType.getType().equalsIgnoreCase(type)) {
                return ibType;
            }
        }
        throw new IllegalArgumentException(String.format("invalid iceberg type = %s", type));
    }
}
