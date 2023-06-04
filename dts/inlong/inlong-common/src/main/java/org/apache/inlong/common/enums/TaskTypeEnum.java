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

package org.apache.inlong.common.enums;

/**
 * Enum of task type.
 */
public enum TaskTypeEnum {

    DATABASE_MIGRATION(0),
    SQL(1),
    BINLOG(2),
    FILE(3),
    KAFKA(4),
    PULSAR(5),
    POSTGRES(6),
    ORACLE(7),
    SQLSERVER(8),
    MONGODB(9),
    TUBEMQ(10),
    REDIS(11),
    MQTT(12),
    HUDI(13),

    // only used for unit test
    MOCK(201)

    ;

    private final int type;

    TaskTypeEnum(int type) {
        this.type = type;
    }

    public static TaskTypeEnum getTaskType(int taskType) {
        switch (taskType) {
            case 0:
                return DATABASE_MIGRATION;
            case 1:
                return SQL;
            case 2:
                return BINLOG;
            case 3:
                return FILE;
            case 4:
                return KAFKA;
            case 5:
                return PULSAR;
            case 6:
                return POSTGRES;
            case 7:
                return ORACLE;
            case 8:
                return SQLSERVER;
            case 9:
                return MONGODB;
            case 10:
                return TUBEMQ;
            case 11:
                return REDIS;
            case 12:
                return MQTT;
            case 13:
                return HUDI;
            case 201:
                return MOCK;
            default:
                throw new RuntimeException("Unsupported task type " + taskType);
        }
    }

    public int getType() {
        return type;
    }

}
