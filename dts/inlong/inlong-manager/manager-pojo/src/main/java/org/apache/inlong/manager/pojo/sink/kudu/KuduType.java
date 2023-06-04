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

package org.apache.inlong.manager.pojo.sink.kudu;

import lombok.Getter;

/**
 * Kudu data type
 */
public enum KuduType {

    BOOLEAN("boolean", "bool"),
    INT("int", "int32"),
    LONG("long", "int64"),
    FLOAT("float", "float"),
    DOUBLE("double", "double"),
    DATE("date", "date"),
    TIMESTAMP("timestamp", "unixtime_micros"),
    STRING("string", "string"),
    BINARY("binary", "binary"),
    FIXED("fixed", null),
    DECIMAL("decimal", "decimal"),
    ;

    @Getter
    private final String type;

    @Getter
    private final String kuduType;

    KuduType(String type, String kuduType) {
        this.type = type;
        this.kuduType = kuduType;
    }

    /**
     * Get type from name
     */
    public static KuduType forType(String type) {
        for (KuduType ibType : values()) {
            if (ibType.getType().equalsIgnoreCase(type)) {
                return ibType;
            }
        }
        throw new IllegalArgumentException(String.format("invalid type = %s", type));
    }

    /**
     * Get type by Kudu type name
     */
    public static final KuduType forKuduType(String kuduType) {
        for (KuduType type : values()) {
            if (type.getKuduType() == kuduType) {
                return type;
            }
        }
        throw new IllegalArgumentException(String.format("invalid kudu type = %s", kuduType));
    }

    public String kuduType() {
        return kuduType;
    }
}
