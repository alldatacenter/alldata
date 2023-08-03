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

import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;
import org.apache.inlong.sort.protocol.ddl.expressions.AlterColumn;
import org.apache.inlong.sort.protocol.ddl.operations.AlterOperation;
import org.apache.inlong.sort.protocol.ddl.operations.CreateTableOperation;
import org.apache.inlong.sort.protocol.ddl.operations.Operation;
import org.apache.inlong.sort.protocol.enums.SchemaChangePolicy;
import org.apache.inlong.sort.protocol.enums.SchemaChangeType;
import org.apache.inlong.sort.util.SchemaChangeUtils;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schema change helper
 * */
public abstract class SchemaChangeHelper implements SchemaChangeHandle {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaChangeHelper.class);
    private final boolean schemaChange;
    protected final Map<SchemaChangeType, SchemaChangePolicy> policyMap;
    protected final JsonDynamicSchemaFormat dynamicSchemaFormat;
    private final String databasePattern;
    private final String tablePattern;
    protected final SchemaUpdateExceptionPolicy exceptionPolicy;
    private final SinkTableMetricData metricData;
    private final DirtySinkHelper<Object> dirtySinkHelper;

    public SchemaChangeHelper(JsonDynamicSchemaFormat dynamicSchemaFormat, boolean schemaChange,
            Map<SchemaChangeType, SchemaChangePolicy> policyMap, String databasePattern, String tablePattern,
            SchemaUpdateExceptionPolicy exceptionPolicy,
            SinkTableMetricData metricData, DirtySinkHelper<Object> dirtySinkHelper) {
        this.dynamicSchemaFormat = Preconditions.checkNotNull(dynamicSchemaFormat, "dynamicSchemaFormat is null");
        this.schemaChange = schemaChange;
        this.policyMap = policyMap;
        this.databasePattern = databasePattern;
        this.tablePattern = tablePattern;
        this.exceptionPolicy = exceptionPolicy;
        this.metricData = metricData;
        this.dirtySinkHelper = dirtySinkHelper;
    }

    public void process(byte[] originData, JsonNode data) {
        // 1. Extract the schema change type from the data;
        if (!schemaChange) {
            return;
        }
        String database;
        String table;
        try {
            database = dynamicSchemaFormat.parse(data, databasePattern);
            table = dynamicSchemaFormat.parse(data, tablePattern);
        } catch (Exception e) {
            if (exceptionPolicy == SchemaUpdateExceptionPolicy.THROW_WITH_STOP) {
                throw new SchemaChangeHandleException(
                        String.format("Parse database, table from origin data failed, origin data: %s",
                                new String(originData)),
                        e);
            }
            LOGGER.warn("Parse database, table from origin data failed, origin data: {}", new String(originData), e);
            if (exceptionPolicy == SchemaUpdateExceptionPolicy.LOG_WITH_IGNORE) {
                dirtySinkHelper.invoke(new String(originData), DirtyType.JSON_PROCESS_ERROR, e);
            }
            if (metricData != null) {
                metricData.invokeDirty(1, originData.length);
            }
            return;
        }
        Operation operation;
        try {
            JsonNode operationNode = Preconditions.checkNotNull(data.get("operation"),
                    "Operation node is null");
            operation = Preconditions.checkNotNull(
                    dynamicSchemaFormat.objectMapper.convertValue(operationNode, new TypeReference<Operation>() {
                    }), "Operation is null");
        } catch (Exception e) {
            if (exceptionPolicy == SchemaUpdateExceptionPolicy.THROW_WITH_STOP) {
                throw new SchemaChangeHandleException(
                        String.format("Extract Operation from origin data failed,origin data: %s", data), e);
            }
            LOGGER.warn("Extract Operation from origin data failed,origin data: {}", data, e);
            handleDirtyData(data, originData, database, table, DirtyType.JSON_PROCESS_ERROR, e);
            return;
        }
        String originSchema = dynamicSchemaFormat.extractDDL(data);
        SchemaChangeType type = SchemaChangeUtils.extractSchemaChangeType(operation);
        if (type == null) {
            LOGGER.warn("Unsupported for schema-change: {}", originSchema);
            return;
        }

        // 2. Apply schema change;
        SchemaChangePolicy policy = policyMap.get(type);
        if (policy != SchemaChangePolicy.ENABLE) {
            doSchemaChangeBase(type, policy, originSchema);
        } else {
            switch (type) {
                case ALTER:
                    handleAlterOperation(database, table, originData, originSchema, data, (AlterOperation) operation);
                    break;
                case CREATE_TABLE:
                    doCreateTable(originData, database, table, type, originSchema, data,
                            (CreateTableOperation) operation);
                    break;
                case DROP_TABLE:
                    doDropTable(type, originSchema);
                    break;
                case RENAME_TABLE:
                    doRenameTable(type, originSchema);
                    break;
                case TRUNCATE_TABLE:
                    doTruncateTable(type, originSchema);
                    break;
                default:
                    LOGGER.warn("Unsupported for {}: {}", type, originSchema);
            }
        }
    }

    @Override
    public void handleAlterOperation(String database, String table, byte[] originData,
            String originSchema, JsonNode data, AlterOperation operation) {
        if (operation.getAlterColumns() == null || operation.getAlterColumns().isEmpty()) {
            if (exceptionPolicy == SchemaUpdateExceptionPolicy.THROW_WITH_STOP) {
                throw new SchemaChangeHandleException(
                        String.format("Alter columns is empty, origin schema: %s", originSchema));
            }
            LOGGER.warn("Alter columns is empty, origin schema: {}", originSchema);
            return;
        }

        Map<SchemaChangeType, List<AlterColumn>> typeMap = new LinkedHashMap<>();
        for (AlterColumn alterColumn : operation.getAlterColumns()) {
            Set<SchemaChangeType> types = null;
            try {
                types = SchemaChangeUtils.extractSchemaChangeType(alterColumn);
                Preconditions.checkState(!types.isEmpty(), "Schema change types is empty");
            } catch (Exception e) {
                if (exceptionPolicy == SchemaUpdateExceptionPolicy.THROW_WITH_STOP) {
                    throw new SchemaChangeHandleException(
                            String.format("Extract schema change type failed, origin schema: %s", originSchema), e);
                }
                LOGGER.warn("Extract schema change type failed, origin schema: {}", originSchema, e);
            }
            if (types == null) {
                continue;
            }
            if (types.size() == 1) {
                SchemaChangeType type = types.stream().findFirst().get();
                typeMap.computeIfAbsent(type, k -> new ArrayList<>()).add(alterColumn);
            } else {
                // Handle change column, it only exists change column type and rename column in this scenario for now.
                for (SchemaChangeType type : types) {
                    LOGGER.warn("Unsupported for {}: {}", type, originSchema);
                }
            }
        }

        if (!typeMap.isEmpty()) {
            doAlterOperation(database, table, originData, originSchema, data, typeMap);
        }
    }

    @Override
    public void doAddColumn(SchemaChangeType type, String originSchema) {
        throw new SchemaChangeHandleException(String.format("Unsupported for %s: %s", type, originSchema));
    }

    @Override
    public void doChangeColumnType(SchemaChangeType type, String originSchema) {
        throw new SchemaChangeHandleException(String.format("Unsupported for %s: %s", type, originSchema));
    }

    @Override
    public void doRenameColumn(SchemaChangeType type, String originSchema) {
        throw new SchemaChangeHandleException(String.format("Unsupported for %s: %s", type, originSchema));
    }

    @Override
    public void doDropColumn(SchemaChangeType type, String originSchema) {
        throw new SchemaChangeHandleException(String.format("Unsupported for %s: %s", type, originSchema));
    }

    @Override
    public void doCreateTable(byte[] originData, String database, String table, SchemaChangeType type,
            String originSchema, JsonNode data, CreateTableOperation operation) {
        throw new SchemaChangeHandleException(String.format("Unsupported for %s: %s", type, originSchema));
    }

    @Override
    public void doDropTable(SchemaChangeType type, String originSchema) {
        throw new SchemaChangeHandleException(String.format("Unsupported for %s: %s", type, originSchema));
    }

    @Override
    public void doRenameTable(SchemaChangeType type, String originSchema) {
        throw new SchemaChangeHandleException(String.format("Unsupported for %s: %s", type, originSchema));
    }

    @Override
    public void doTruncateTable(SchemaChangeType type, String originSchema) {
        throw new SchemaChangeHandleException(String.format("Unsupported for %s: %s", type, originSchema));
    }

    protected void handleDirtyData(JsonNode data, byte[] originData, String database,
            String table, DirtyType dirtyType, Throwable e) {
        if (exceptionPolicy == SchemaUpdateExceptionPolicy.LOG_WITH_IGNORE) {
            String label = parseValue(data, dirtySinkHelper.getDirtyOptions().getLabels());
            String logTag = parseValue(data, dirtySinkHelper.getDirtyOptions().getLogTag());
            String identifier = parseValue(data, dirtySinkHelper.getDirtyOptions().getIdentifier());
            dirtySinkHelper.invoke(new String(originData), dirtyType, label, logTag, identifier, e);
        }
        if (metricData != null) {
            metricData.outputDirtyMetricsWithEstimate(database, table, 1, originData.length);
        }
    }
    private String parseValue(JsonNode data, String pattern) {
        try {
            return dynamicSchemaFormat.parse(data, pattern);
        } catch (Exception e) {
            LOGGER.warn("Parse value from pattern failed,the pattern: {}, data: {}", pattern, data);
        }
        return pattern;
    }

    protected void doSchemaChangeBase(SchemaChangeType type, SchemaChangePolicy policy, String schema) {
        if (policy == null) {
            LOGGER.warn("Unsupported for {}: {}", type, schema);
            return;
        }
        switch (policy) {
            case LOG:
                LOGGER.warn("Unsupported for {}: {}", type, schema);
                break;
            case ERROR:
                throw new SchemaChangeHandleException(String.format("Unsupported for %s: %s", type, schema));
            default:
        }
    }
}
