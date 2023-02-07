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

package org.apache.inlong.manager.common.consts;

import org.apache.inlong.common.enums.TaskTypeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants of source type.
 */
public class SourceType {

    public static final String AUTO_PUSH = "AUTO_PUSH";
    public static final String TUBEMQ = "TUBEMQ";
    public static final String PULSAR = "PULSAR";
    public static final String KAFKA = "KAFKA";

    public static final String FILE = "FILE";
    public static final String MYSQL_SQL = "MYSQL_SQL";
    public static final String MYSQL_BINLOG = "MYSQL_BINLOG";
    public static final String POSTGRESQL = "POSTGRESQL";
    public static final String ORACLE = "ORACLE";
    public static final String SQLSERVER = "SQLSERVER";
    public static final String MONGODB = "MONGODB";
    public static final String REDIS = "REDIS";
    public static final String MQTT = "MQTT";
    public static final String HUDI = "HUDI";

    public static final Map<String, TaskTypeEnum> SOURCE_TASK_MAP = new HashMap<String, TaskTypeEnum>() {

        {
            put(AUTO_PUSH, null);
            put(TUBEMQ, TaskTypeEnum.TUBEMQ);
            put(PULSAR, TaskTypeEnum.PULSAR);
            put(KAFKA, TaskTypeEnum.KAFKA);

            put(FILE, TaskTypeEnum.FILE);
            put(MYSQL_SQL, TaskTypeEnum.SQL);
            put(MYSQL_BINLOG, TaskTypeEnum.BINLOG);
            put(POSTGRESQL, TaskTypeEnum.POSTGRES);
            put(ORACLE, TaskTypeEnum.ORACLE);
            put(SQLSERVER, TaskTypeEnum.SQLSERVER);
            put(MONGODB, TaskTypeEnum.MONGODB);
            put(REDIS, TaskTypeEnum.REDIS);
            put(MQTT, TaskTypeEnum.MQTT);
            put(HUDI, TaskTypeEnum.HUDI);

        }
    };

}
