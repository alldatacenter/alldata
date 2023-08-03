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

package org.apache.inlong.sort.doris.schema;

import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.schema.SchemaChangeHandleException;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;
import org.apache.inlong.sort.doris.http.HttpGetEntity;
import org.apache.inlong.sort.protocol.ddl.expressions.AlterColumn;
import org.apache.inlong.sort.protocol.ddl.operations.AlterOperation;
import org.apache.inlong.sort.protocol.ddl.operations.CreateTableOperation;
import org.apache.inlong.sort.protocol.ddl.operations.Operation;
import org.apache.inlong.sort.protocol.enums.SchemaChangePolicy;
import org.apache.inlong.sort.protocol.enums.SchemaChangeType;
import org.apache.inlong.sort.util.SchemaChangeUtils;

import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.shaded.org.apache.commons.codec.binary.Base64;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.util.Preconditions;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Schema change helper
 */
public class SchemaChangeHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaChangeHelper.class);

    private static final String CHECK_LIGHT_SCHEMA_CHANGE_API = "http://%s/api/enable_light_schema_change/%s/%s";
    private static final String SCHEMA_CHANGE_API = "http://%s/api/query/default_cluster/%s";
    private static final String DORIS_HTTP_CALL_SUCCESS = "0";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private final boolean schemaChange;
    private final Map<SchemaChangeType, SchemaChangePolicy> policyMap;
    private final DorisOptions options;
    private final JsonDynamicSchemaFormat dynamicSchemaFormat;
    private final String databasePattern;
    private final String tablePattern;
    private final int maxRetries;
    private final OperationHelper operationHelper;
    private final SchemaUpdateExceptionPolicy exceptionPolicy;
    private final SinkTableMetricData metricData;
    private final DirtySinkHelper<Object> dirtySinkHelper;

    private SchemaChangeHelper(JsonDynamicSchemaFormat dynamicSchemaFormat, DorisOptions options, boolean schemaChange,
            Map<SchemaChangeType, SchemaChangePolicy> policyMap, String databasePattern, String tablePattern,
            int maxRetries, SchemaUpdateExceptionPolicy exceptionPolicy,
            SinkTableMetricData metricData, DirtySinkHelper<Object> dirtySinkHelper) {
        this.dynamicSchemaFormat = Preconditions.checkNotNull(dynamicSchemaFormat, "dynamicSchemaFormat is null");
        this.options = Preconditions.checkNotNull(options, "doris options is null");
        this.schemaChange = schemaChange;
        this.policyMap = policyMap;
        this.databasePattern = databasePattern;
        this.tablePattern = tablePattern;
        this.maxRetries = maxRetries;
        this.exceptionPolicy = exceptionPolicy;
        this.metricData = metricData;
        this.dirtySinkHelper = dirtySinkHelper;
        operationHelper = OperationHelper.of(dynamicSchemaFormat);
    }

    public static SchemaChangeHelper of(JsonDynamicSchemaFormat dynamicSchemaFormat, DorisOptions options,
            boolean schemaChange, Map<SchemaChangeType, SchemaChangePolicy> policyMap, String databasePattern,
            String tablePattern, int maxRetries, SchemaUpdateExceptionPolicy exceptionPolicy,
            SinkTableMetricData metricData, DirtySinkHelper<Object> dirtySinkHelper) {
        return new SchemaChangeHelper(dynamicSchemaFormat, options, schemaChange, policyMap, databasePattern,
                tablePattern, maxRetries, exceptionPolicy, metricData, dirtySinkHelper);
    }

    /**
     * Process schema change for Doris
     *
     * @param data The origin data
     */
    public void process(byte[] originData, JsonNode data) {
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
        switch (type) {
            case ALTER:
                handleAlterOperation(database, table, originData, originSchema, data, (AlterOperation) operation);
                break;
            case CREATE_TABLE:
                doCreateTable(originData, database, table, type, originSchema, data, (CreateTableOperation) operation);
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

    private void handleDirtyData(JsonNode data, byte[] originData, String database,
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

    private void reportMetric(String database, String table, int len) {
        if (metricData != null) {
            metricData.outputMetrics(database, table, 1, len);
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

    private void handleAlterOperation(String database, String table, byte[] originData,
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
                    SchemaChangePolicy policy = policyMap.get(type);
                    if (policy == SchemaChangePolicy.ENABLE) {
                        LOGGER.warn("Unsupported for {}: {}", type, originSchema);
                    } else {
                        doSchemaChangeBase(type, policy, originSchema);
                    }
                }
            }
        }
        if (!typeMap.isEmpty()) {
            doAlterOperation(database, table, originData, originSchema, data, typeMap);
        }
    }

    private void doAlterOperation(String database, String table, byte[] originData, String originSchema, JsonNode data,
            Map<SchemaChangeType, List<AlterColumn>> typeMap) {
        StringJoiner joiner = new StringJoiner(",");
        for (Entry<SchemaChangeType, List<AlterColumn>> kv : typeMap.entrySet()) {
            SchemaChangePolicy policy = policyMap.get(kv.getKey());
            doSchemaChangeBase(kv.getKey(), policy, originSchema);
            if (policy == SchemaChangePolicy.ENABLE) {
                String alterStatement = null;
                try {
                    switch (kv.getKey()) {
                        case ADD_COLUMN:
                            alterStatement = doAddColumn(kv.getValue());
                            break;
                        case DROP_COLUMN:
                            alterStatement = doDropColumn(kv.getValue());
                            break;
                        case RENAME_COLUMN:
                            alterStatement = doRenameColumn(kv.getKey(), originSchema);
                            break;
                        case CHANGE_COLUMN_TYPE:
                            alterStatement = doChangeColumnType(kv.getKey(), originSchema);
                            break;
                        default:
                    }
                } catch (Exception e) {
                    if (exceptionPolicy == SchemaUpdateExceptionPolicy.THROW_WITH_STOP) {
                        throw new SchemaChangeHandleException(
                                String.format("Build alter statement failed, origin schema: %s", originSchema), e);
                    }
                    LOGGER.warn("Build alter statement failed, origin schema: {}", originSchema, e);
                }
                if (alterStatement != null) {
                    joiner.add(alterStatement);
                }
            }
        }
        String statement = joiner.toString();
        if (statement.length() != 0) {
            try {
                String alterStatementCommon = operationHelper.buildAlterStatementCommon(database, table);
                statement = alterStatementCommon + statement;
                // The checkLightSchemaChange is removed because most scenarios support it
                boolean result = executeStatement(database, statement);
                if (!result) {
                    LOGGER.error("Alter table failed,statement: {}", statement);
                    throw new SchemaChangeHandleException(String.format("Add column failed,statement: %s", statement));
                }
                LOGGER.info("Alter table success,statement: {}", statement);
                reportMetric(database, table, originData.length);
            } catch (Exception e) {
                if (exceptionPolicy == SchemaUpdateExceptionPolicy.THROW_WITH_STOP) {
                    throw new SchemaChangeHandleException(
                            String.format("Alter table failed, origin schema: %s", originSchema), e);
                }
                handleDirtyData(data, originData, database, table, DirtyType.HANDLE_ALTER_TABLE_ERROR, e);
            }
        }
    }

    private String doChangeColumnType(SchemaChangeType type, String originSchema) {
        LOGGER.warn("Unsupported for {}: {}", type, originSchema);
        return null;
    }

    private String doRenameColumn(SchemaChangeType type, String originSchema) {
        LOGGER.warn("Unsupported for {}: {}", type, originSchema);
        return null;
    }

    private String doDropColumn(List<AlterColumn> alterColumns) {
        return operationHelper.buildDropColumnStatement(alterColumns);
    }

    private String doAddColumn(List<AlterColumn> alterColumns) {
        return operationHelper.buildAddColumnStatement(alterColumns);
    }

    private void doTruncateTable(SchemaChangeType type, String originSchema) {
        SchemaChangePolicy policy = policyMap.get(SchemaChangeType.TRUNCATE_TABLE);
        if (policy == SchemaChangePolicy.ENABLE) {
            LOGGER.warn("Unsupported for {}: {}", type, originSchema);
            return;
        }
        doSchemaChangeBase(type, policy, originSchema);
    }

    private void doRenameTable(SchemaChangeType type, String originSchema) {
        SchemaChangePolicy policy = policyMap.get(SchemaChangeType.RENAME_TABLE);
        if (policy == SchemaChangePolicy.ENABLE) {
            LOGGER.warn("Unsupported for {}: {}", type, originSchema);
            return;
        }
        doSchemaChangeBase(type, policy, originSchema);
    }

    private void doDropTable(SchemaChangeType type, String originSchema) {
        SchemaChangePolicy policy = policyMap.get(SchemaChangeType.DROP_TABLE);
        if (policy == SchemaChangePolicy.ENABLE) {
            LOGGER.warn("Unsupported for {}: {}", type, originSchema);
            return;
        }
        doSchemaChangeBase(type, policy, originSchema);
    }

    private void doCreateTable(byte[] originData, String database, String table, SchemaChangeType type,
            String originSchema, JsonNode data, CreateTableOperation operation) {
        SchemaChangePolicy policy = policyMap.get(type);
        if (policy == SchemaChangePolicy.ENABLE) {
            try {
                List<String> primaryKeys = dynamicSchemaFormat.extractPrimaryKeyNames(data);
                String stmt = operationHelper.buildCreateTableStatement(database, table, primaryKeys, operation);
                boolean result = executeStatement(database, stmt);
                if (!result) {
                    LOGGER.error("Create table failed,statement: {}", stmt);
                    throw new IOException(String.format("Create table failed,statement: %s", stmt));
                }
                reportMetric(database, table, originData.length);
                return;
            } catch (Exception e) {
                if (exceptionPolicy == SchemaUpdateExceptionPolicy.THROW_WITH_STOP) {
                    throw new SchemaChangeHandleException(
                            String.format("Drop column failed, origin schema: %s", originSchema), e);
                }
                handleDirtyData(data, originData, database, table, DirtyType.CREATE_TABLE_ERROR, e);
                return;
            }
        }
        doSchemaChangeBase(type, policy, originSchema);
    }

    private void doSchemaChangeBase(SchemaChangeType type, SchemaChangePolicy policy, String schema) {
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

    private Map<String, Object> buildRequestParam(String column, boolean dropColumn) {
        Map<String, Object> params = new HashMap<>();
        params.put("isDropColumn", dropColumn);
        params.put("columnName", column);
        return params;
    }

    private String authHeader() {
        return "Basic " + new String(Base64.encodeBase64((options.getUsername() + ":"
                + options.getPassword()).getBytes(StandardCharsets.UTF_8)));
    }

    private boolean executeStatement(String database, String stmt) throws IOException {
        Map<String, String> param = new HashMap<>();
        param.put("stmt", stmt);
        String requestUrl = String.format(SCHEMA_CHANGE_API, options.getFenodes(), database);
        HttpPost httpPost = new HttpPost(requestUrl);
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, authHeader());
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
        httpPost.setEntity(new StringEntity(dynamicSchemaFormat.objectMapper.writeValueAsString(param)));
        return sendRequest(httpPost);
    }

    private boolean checkLightSchemaChange(String database, String table, String column, boolean dropColumn)
            throws IOException {
        String url = String.format(CHECK_LIGHT_SCHEMA_CHANGE_API, options.getFenodes(), database, table);
        Map<String, Object> param = buildRequestParam(column, dropColumn);
        HttpGetEntity httpGet = new HttpGetEntity(url);
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader());
        httpGet.setEntity(new StringEntity(dynamicSchemaFormat.objectMapper.writeValueAsString(param)));
        boolean success = sendRequest(httpGet);
        if (!success) {
            LOGGER.warn("schema change can not do table {}.{}", database, table);
        }
        return success;
    }

    @SuppressWarnings("unchecked")
    private boolean sendRequest(HttpUriRequest request) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            for (int i = 0; i < maxRetries; i++) {
                try {
                    CloseableHttpResponse response = httpclient.execute(request);
                    final int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_OK && response.getEntity() != null) {
                        String loadResult = EntityUtils.toString(response.getEntity());
                        Map<String, Object> responseMap = dynamicSchemaFormat.objectMapper
                                .readValue(loadResult, Map.class);
                        String code = responseMap.getOrDefault("code", "-1").toString();
                        if (DORIS_HTTP_CALL_SUCCESS.equals(code)) {
                            return true;
                        }
                        LOGGER.error("send request error: {}", loadResult);
                    }
                } catch (Exception e) {
                    if (i >= maxRetries) {
                        LOGGER.error("send http requests error", e);
                        throw new IOException(e);
                    }
                    try {
                        Thread.sleep(1000L * i);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IOException("unable to send http request,interrupted while doing another attempt", e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("send request error", e);
            throw new SchemaChangeHandleException("send request error", e);
        }
        return false;
    }
}
