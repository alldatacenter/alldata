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

import lombok.Getter;
import org.apache.inlong.common.enums.TaskTypeEnum;

import java.util.Locale;

/**
 * The enum of source type
 */
public enum SourceType {

    AUTO_PUSH("AUTO_PUSH", null),
    FILE("FILE", TaskTypeEnum.FILE),
    SQL("SQL", TaskTypeEnum.SQL),
    BINLOG("BINLOG", TaskTypeEnum.BINLOG),
    KAFKA("KAFKA", TaskTypeEnum.KAFKA),
    PULSAR("PULSAR", TaskTypeEnum.PULSAR),
    POSTGRES("POSTGRES", TaskTypeEnum.POSTGRES),
    ORACLE("ORACLE", TaskTypeEnum.ORACLE),
    SQLSERVER("SQLSERVER", TaskTypeEnum.SQLSERVER),
    MONGODB("MONGO", TaskTypeEnum.MONGODB),

    ;

    public static final String SOURCE_AUTO_PUSH = "AUTO_PUSH";
    public static final String SOURCE_FILE = "FILE";
    public static final String SOURCE_SQL = "SQL";
    public static final String SOURCE_BINLOG = "BINLOG";
    public static final String SOURCE_KAFKA = "KAFKA";
    public static final String SOURCE_PULSAR = "PULSAR";
    public static final String SOURCE_POSTGRES = "POSTGRES";
    public static final String SOURCE_ORACLE = "ORACLE";
    public static final String SOURCE_SQLSERVER = "SQLSERVER";
    public static final String SOURCE_MONGODB = "MONGODB";

    @Getter
    private final String type;

    @Getter
    private final TaskTypeEnum taskType;

    SourceType(String type, TaskTypeEnum taskType) {
        this.type = type;
        this.taskType = taskType;
    }

    /**
     * Get the SourceType enum via the given sourceType string
     */
    public static SourceType forType(String sourceType) {
        for (SourceType type : values()) {
            if (type.getType().equals(sourceType)) {
                return type;
            }
        }
        throw new IllegalArgumentException(String.format("Illegal sink type for %s", sourceType));
    }

    @Override
    public String toString() {
        return this.name().toUpperCase(Locale.ROOT);
    }

}
