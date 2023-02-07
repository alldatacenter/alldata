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

package org.apache.inlong.sort.redis.table;

import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.redis.common.mapper.RedisCommand;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Schema validator
 */
public class SchemaValidator {

    private final Map<RedisCommand, LogicalTypeRoot[]> schemaMap = new HashMap<>();

    /**
     * Register schema validator
     *
     * @param redisCommand The redis command
     * @param requiredLogicalTypes The requiredLogicalTypes
     * @return Schema validator
     */
    public SchemaValidator register(RedisCommand redisCommand,
            LogicalTypeRoot[] requiredLogicalTypes) {
        schemaMap.putIfAbsent(redisCommand, requiredLogicalTypes);
        return this;
    }

    /**
     * Validate the schema
     *
     * @param redisCommand The redis command
     * @param tableSchema The table schema
     */
    public void validate(RedisCommand redisCommand, ResolvedSchema tableSchema) {
        LogicalType[] logicalTypes = tableSchema.getColumns().stream().filter(Column::isPhysical)
                .map(s -> s.getDataType().getLogicalType()).toArray(LogicalType[]::new);
        LogicalTypeRoot[] requiredLogicalTypes = schemaMap.get(redisCommand);
        for (int i = 0; i < requiredLogicalTypes.length; i++) {
            Preconditions.checkState(requiredLogicalTypes[i] == logicalTypes[i].getTypeRoot(),
                    "Table schema " + Arrays.deepToString(logicalTypes) + " is invalid. Table schema "
                            + Arrays.deepToString(requiredLogicalTypes) + " is required for command " + redisCommand
                                    .name());
        }
    }
}
