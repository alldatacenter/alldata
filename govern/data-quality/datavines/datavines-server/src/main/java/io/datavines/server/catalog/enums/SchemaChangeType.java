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
package io.datavines.server.catalog.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.HashMap;
import java.util.Map;

public enum SchemaChangeType {

    /**
     *
     */
    DATABASE_ADDED(0, "DATABASE_ADDED"),
    DATABASE_DELETED(1, "DATABASE_DELETED"),
    TABLE_ADDED(2, "TABLE_ADDED"),
    TABLE_DELETED(3, "TABLE_DELETED"),
    TABLE_COMMENT_CHANGE(4, "COLUMN_COMMENT_CHANGE"),
    COLUMN_ADDED(5, "COLUMN_ADDED"),
    COLUMN_DELETED(6, "COLUMN_DELETED"),
    COLUMN_TYPE_CHANGE(7, "COLUMN_TYPE_CHANGE"),
    COLUMN_COMMENT_CHANGE(8, "COLUMN_COMMENT_CHANGE")
    ;

    SchemaChangeType(int code, String description){
        this.code = code;
        this.description = description;
    }

    @EnumValue
    private final int code;

    @EnumValue
    private final String description;

    private static final Map<Integer, SchemaChangeType> SCHEMA_CHANGE_TYPE_MAP = new HashMap<>();

    static {
        for (SchemaChangeType schemaChangeType : SchemaChangeType.values()) {
            SCHEMA_CHANGE_TYPE_MAP.put(schemaChangeType.code,schemaChangeType);
        }
    }

    public static SchemaChangeType of(Integer status) {
        if (SCHEMA_CHANGE_TYPE_MAP.containsKey(status)) {
            return SCHEMA_CHANGE_TYPE_MAP.get(status);
        }
        throw new IllegalArgumentException("invalid schema change type : " + status);
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
