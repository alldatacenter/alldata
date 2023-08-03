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

package org.apache.inlong.sort.protocol.enums;

/**
 * The class defines the type of schema change.
 */
public enum SchemaChangeType {

    /**
     * Create table
     */
    CREATE_TABLE(1),
    /**
     * Drop table
     */
    DROP_TABLE(2),
    /**
     * Rename table
     */
    RENAME_TABLE(3),
    /**
     * Truncate table
     */
    TRUNCATE_TABLE(4),
    /**
     * Add column
     */
    ADD_COLUMN(5),
    /**
     * Drop column
     */
    DROP_COLUMN(6),
    /**
     * Rename column
     */
    RENAME_COLUMN(7),
    /**
     * Change column type
     */
    CHANGE_COLUMN_TYPE(8),
    /**
     * Alter table, it is a unified description of modified table, which may contain multiple operations, it is not
     * exposed to the outside world in most scenarios.
     */
    ALTER(-1);

    /**
     * The code represents this schema change type
     */
    private final int code;

    SchemaChangeType(int code) {
        this.code = code;
    }

    public static SchemaChangeType getInstance(int code) {
        for (SchemaChangeType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported type of schema-change: %s for InLong", code));
    }

    /**
     * Get the code of {@link SchemaChangeType}
     *
     * @return The code of {@link SchemaChangeType}
     */
    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", this.name(), getCode());
    }
}
