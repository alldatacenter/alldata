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

package org.apache.inlong.sort.base.format;

import org.apache.flink.shaded.guava18.com.google.common.collect.ImmutableMap;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.BigIntType;
import org.apache.flink.table.types.logical.BooleanType;
import org.apache.flink.table.types.logical.DoubleType;
import org.apache.flink.table.types.logical.FloatType;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.SmallIntType;
import org.apache.flink.table.types.logical.TinyIntType;
import org.apache.flink.table.types.logical.VarBinaryType;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.base.format.JsonToRowDataConverters.JsonToRowDataConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Debezium json dynamic format
 */
public class DebeziumJsonDynamicSchemaFormat extends JsonDynamicSchemaFormat {

    private static final String DDL_FLAG = "ddl";
    private static final String SCHEMA = "schema";
    private static final String SQL_TYPE = "sqlType";
    private static final String AFTER = "after";
    private static final String BEFORE = "before";
    private static final String SOURCE = "source";
    private static final String PK_NAMES = "pkNames";
    private static final String OP_TYPE = "op";
    private static final String PAYLOAD = "payload";
    private static final String FIELDS = "fields";
    private static final String FIELD = "field";
    private static final String TYPE = "type";
    /**
     * Snapshot read
     */
    private static final String OP_READ = "r";
    /**
     * Insert
     */
    private static final String OP_CREATE = "c";
    /**
     * Update
     */
    private static final String OP_UPDATE = "u";
    /**
     * Delete
     */
    private static final String OP_DELETE = "d";

    private static final Map<String, LogicalType> DEBEZIUM_TYPE_2_FLINK_TYPE_MAPPING =
            ImmutableMap.<String, LogicalType>builder()
                    .put("BOOLEAN", new BooleanType())
                    .put("INT8", new TinyIntType())
                    .put("INT16", new SmallIntType())
                    .put("INT32", new IntType())
                    .put("INT64", new BigIntType())
                    .put("FLOAT32", new FloatType())
                    .put("FLOAT64", new DoubleType())
                    .put("STRING", new VarCharType())
                    .put("BYTES", new VarBinaryType())
                    .build();

    protected DebeziumJsonDynamicSchemaFormat(Map<String, String> props) {
        super(props);
    }

    @Override
    public JsonNode getPhysicalData(JsonNode root) {
        JsonNode payload = root.get(PAYLOAD);
        if (payload == null) {
            JsonNode physicalData = root.get(AFTER);
            if (physicalData == null) {
                physicalData = root.get(BEFORE);
            }
            return physicalData;
        }
        return getPhysicalData(payload);
    }

    @Override
    public JsonNode getUpdateAfter(JsonNode root) {
        JsonNode payload = root.get(PAYLOAD);
        if (payload == null) {
            return root.get(AFTER);
        }
        return getUpdateAfter(payload);
    }

    @Override
    public JsonNode getUpdateBefore(JsonNode root) {
        JsonNode payload = root.get(PAYLOAD);
        if (payload == null) {
            return root.get(BEFORE);
        }
        return getUpdateBefore(payload);
    }

    @Override
    public List<RowKind> opType2RowKind(String opType) {
        List<RowKind> rowKinds = new ArrayList<>();
        switch (opType) {
            case OP_CREATE:
            case OP_READ:
                rowKinds.add(RowKind.INSERT);
                break;
            case OP_UPDATE:
                rowKinds.add(RowKind.UPDATE_BEFORE);
                rowKinds.add(RowKind.UPDATE_AFTER);
                break;
            case OP_DELETE:
                rowKinds.add(RowKind.DELETE);
                break;
            default:
                throw new IllegalArgumentException("Unsupported op_type: " + opType);
        }
        return rowKinds;
    }

    @Override
    public String getOpType(JsonNode root) {
        JsonNode payload = root.get(PAYLOAD);
        if (payload == null) {
            JsonNode opNode = root.get(OP_TYPE);
            if (opNode == null) {
                throw new IllegalArgumentException(String.format("Error node: %s, %s is null", root, OP_TYPE));
            }
            return opNode.asText();
        }
        return getOpType(payload);
    }

    @Override
    public List<String> extractPrimaryKeyNames(JsonNode data) {
        List<String> pkNames = new ArrayList<>();
        JsonNode payload = data.get(PAYLOAD);
        if (payload == null) {
            JsonNode sourceNode = data.get(SOURCE);
            if (sourceNode == null) {
                return pkNames;
            }
            JsonNode pkNamesNode = sourceNode.get(PK_NAMES);
            if (pkNamesNode != null && pkNamesNode.isArray()) {
                for (int i = 0; i < pkNamesNode.size(); i++) {
                    pkNames.add(pkNamesNode.get(i).asText());
                }
            }
            return pkNames;
        }
        return extractPrimaryKeyNames(payload);
    }

    @Override
    public String parse(JsonNode rootNode, String pattern) throws IOException {
        JsonNode payload = rootNode.get(PAYLOAD);
        if (payload == null) {
            return super.parse(rootNode, pattern);
        }
        return super.parse(payload, pattern);
    }

    @Override
    public boolean extractDDLFlag(JsonNode data) {
        JsonNode payload = data.get(PAYLOAD);
        if (payload == null) {
            return data.has(DDL_FLAG) && data.get(DDL_FLAG).asBoolean(false);
        }
        return extractDDLFlag(payload);
    }

    public RowType extractSchemaFromExtractInfo(JsonNode data, List<String> pkNames) {
        JsonNode payload = data.get(PAYLOAD);
        if (payload == null) {
            JsonNode sourceNode = data.get(SOURCE);
            if (sourceNode == null) {
                throw new IllegalArgumentException(String.format("Error schema: %s.", data));
            }
            JsonNode schemaNode = sourceNode.get(SQL_TYPE);
            if (schemaNode == null) {
                throw new IllegalArgumentException(String.format("Error schema: %s.", data));
            }
            return super.extractSchemaNode(schemaNode, pkNames);
        }
        return extractSchemaFromExtractInfo(payload, pkNames);
    }

    @Override
    public RowType extractSchema(JsonNode data, List<String> pkNames) {
        // first get schema from 'sqlType', fallback to get it from 'schema'
        try {
            return extractSchemaFromExtractInfo(data, pkNames);
        } catch (IllegalArgumentException e) {
            JsonNode schema = data.get(SCHEMA);
            if (schema == null) {
                throw new IllegalArgumentException(String.format("Not found schema from: %s", data));
            }
            for (JsonNode field : schema.get(FIELDS)) {
                if (AFTER.equals(field.get(FIELD).asText())) {
                    return extractSchemaNode(field.get(FIELDS), pkNames);
                }
            }
            throw new IllegalArgumentException(String.format("Error schema: %s.", schema));
        }
    }

    @Override
    public RowType extractSchemaNode(JsonNode schema, List<String> pkNames) {
        List<RowType.RowField> fields = new ArrayList<>();
        for (JsonNode field : schema) {
            String name = field.get(FIELD).asText();
            LogicalType type = debeziumType2FlinkType(field.get(TYPE).asText());
            if (pkNames.contains(name)) {
                type = type.copy(false);
            }
            fields.add(new RowType.RowField(name, type));
        }
        return new RowType(fields);
    }

    @Override
    public List<RowData> extractRowData(JsonNode data, RowType rowType) {
        JsonNode payload = data.get(PAYLOAD);
        if (payload == null) {
            JsonNode opNode = data.get(OP_TYPE);
            JsonNode dataBeforeNode = data.get(BEFORE);
            JsonNode dataAfterNode = data.get(AFTER);
            if (opNode == null || (dataBeforeNode == null && dataAfterNode == null)) {
                throw new IllegalArgumentException(
                        String.format("Error opNode: %s, or dataBeforeNode: %s, dataAfterNode: %s",
                                opNode, dataBeforeNode, dataAfterNode));
            }
            List<RowData> rowDataList = new ArrayList<>();
            JsonToRowDataConverter rowDataConverter = rowDataConverters.createConverter(rowType);

            String op = data.get(OP_TYPE).asText();
            if (OP_CREATE.equals(op) || OP_READ.equals(op)) {
                RowData rowData = (RowData) rowDataConverter.convert(dataAfterNode);
                rowData.setRowKind(RowKind.INSERT);
                rowDataList.add(rowData);
            } else if (OP_UPDATE.equals(op)) {
                RowData rowData = (RowData) rowDataConverter.convert(dataBeforeNode);
                rowData.setRowKind(RowKind.UPDATE_BEFORE);
                rowDataList.add(rowData);
                rowData = (RowData) rowDataConverter.convert(dataAfterNode);
                rowData.setRowKind(RowKind.UPDATE_AFTER);
                rowDataList.add(rowData);
            } else if (OP_DELETE.equals(op)) {
                RowData rowData = (RowData) rowDataConverter.convert(dataBeforeNode);
                rowData.setRowKind(RowKind.DELETE);
                rowDataList.add(rowData);
            } else {
                throw new IllegalArgumentException("Unsupported op_type: " + op);
            }
            return rowDataList;
        }
        return extractRowData(payload, rowType);
    }

    private LogicalType debeziumType2FlinkType(String debeziumType) {
        if (DEBEZIUM_TYPE_2_FLINK_TYPE_MAPPING.containsKey(debeziumType.toUpperCase())) {
            return DEBEZIUM_TYPE_2_FLINK_TYPE_MAPPING.get(debeziumType.toUpperCase());
        } else {
            throw new IllegalArgumentException("Unsupported debeziumType: " + debeziumType.toUpperCase());
        }
    }
}
