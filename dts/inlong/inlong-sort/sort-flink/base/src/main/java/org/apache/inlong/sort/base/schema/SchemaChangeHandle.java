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

package org.apache.inlong.sort.base.schema;

import org.apache.inlong.sort.protocol.ddl.expressions.AlterColumn;
import org.apache.inlong.sort.protocol.ddl.operations.AlterOperation;
import org.apache.inlong.sort.protocol.ddl.operations.CreateTableOperation;
import org.apache.inlong.sort.protocol.enums.SchemaChangeType;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Schema change helper interface
 * */
public interface SchemaChangeHandle {

    /**
     * execute ddl statement
     * */
    void process(byte[] originData, JsonNode data);
    /**
     * Handle modify column operations
     * */
    void handleAlterOperation(String database, String table, byte[] originData, String originSchema,
            JsonNode data, AlterOperation operation);
    /**
     * apply the modify column operation
     * */
    void doAlterOperation(String database, String table, byte[] originData, String originSchema, JsonNode data,
            Map<SchemaChangeType, List<AlterColumn>> typeMap);
    /**
     * apply the add column operation
     * */
    void doAddColumn(SchemaChangeType type, String originSchema);
    /**
     * apply the operation of changing the column type
     * */
    void doChangeColumnType(SchemaChangeType type, String originSchema);
    /**
     * apply the change column name operation
     * */
    void doRenameColumn(SchemaChangeType type, String originSchema);
    /**
     * apply the delete column operation
     * */
    void doDropColumn(SchemaChangeType type, String originSchema);
    /**
     * handle the create table operation
     * */
    void doCreateTable(byte[] originData, String database, String table, SchemaChangeType type,
            String originSchema, JsonNode data, CreateTableOperation operation);
    /**
     * handle drop table operation
     * */
    void doDropTable(SchemaChangeType type, String originSchema);
    /**
     * handle rename table operation
     * */
    void doRenameTable(SchemaChangeType type, String originSchema);
    /**
     * handle truncate table operation
     * */
    void doTruncateTable(SchemaChangeType type, String originSchema);
}
