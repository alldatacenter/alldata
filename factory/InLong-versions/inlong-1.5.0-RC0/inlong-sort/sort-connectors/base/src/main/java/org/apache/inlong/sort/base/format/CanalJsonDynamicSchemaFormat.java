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

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.base.format.JsonToRowDataConverters.JsonToRowDataConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Canal json dynamic format
 */
public class CanalJsonDynamicSchemaFormat extends JsonDynamicSchemaFormat {

    private static final String DDL_FLAG = "ddl";
    private static final String DATA = "data";
    private static final String OLD = "old";
    private static final String PK_NAMES = "pkNames";
    private static final String SCHEMA = "sqlType";
    private static final String OP_TYPE = "type";
    private static final String OP_INSERT = "INSERT";
    private static final String OP_UPDATE = "UPDATE";
    private static final String OP_DELETE = "DELETE";

    protected CanalJsonDynamicSchemaFormat(Map<String, String> props) {
        super(props);
    }

    @Override
    public JsonNode getUpdateAfter(JsonNode root) {
        return root.get(DATA);
    }

    @Override
    public JsonNode getUpdateBefore(JsonNode root) {
        return root.get(OLD);
    }

    @Override
    public List<RowKind> opType2RowKind(String opType) {
        List<RowKind> rowKinds = new ArrayList<>();
        switch (opType) {
            case OP_INSERT:
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
        JsonNode opNode = root.get(OP_TYPE);
        if (opNode == null) {
            throw new IllegalArgumentException(String.format("Error node: %s, %s is null", root, OP_TYPE));
        }
        return opNode.asText();
    }

    @Override
    public List<String> extractPrimaryKeyNames(JsonNode data) {
        JsonNode pkNamesNode = data.get(PK_NAMES);
        List<String> pkNames = new ArrayList<>();
        if (pkNamesNode != null && pkNamesNode.isArray()) {
            for (int i = 0; i < pkNamesNode.size(); i++) {
                pkNames.add(pkNamesNode.get(i).asText());
            }
        }
        return pkNames;
    }

    @Override
    public boolean extractDDLFlag(JsonNode data) {
        return data.has(DDL_FLAG) && data.get(DDL_FLAG).asBoolean(false);
    }

    @Override
    public RowType extractSchema(JsonNode data, List<String> pkNames) {
        JsonNode schema = data.get(SCHEMA);
        if (schema == null) {
            throw new IllegalArgumentException(String.format("Not found schema from: %s", data));
        }
        return extractSchemaNode(schema, pkNames);
    }

    @Override
    public List<RowData> extractRowData(JsonNode data, RowType rowType) {
        JsonNode opNode = data.get(OP_TYPE);
        JsonNode dataNode = data.get(DATA);
        JsonNode oldNode = data.get(OLD);
        if (opNode == null || dataNode == null || !dataNode.isArray()) {
            throw new IllegalArgumentException(String.format("Error opNode: %s, or dataNode: %s", opNode, dataNode));
        }

        String op = data.get(OP_TYPE).asText();
        JsonToRowDataConverter rowDataConverter = rowDataConverters.createConverter(rowType);
        List<RowData> rowDataList = new ArrayList<>();
        if (OP_INSERT.equals(op)) {
            for (JsonNode row : dataNode) {
                RowData rowData = (RowData) rowDataConverter.convert(row);
                rowData.setRowKind(RowKind.INSERT);
                rowDataList.add(rowData);
            }
        } else if (OP_UPDATE.equals(op)) {
            for (int i = 0; i < dataNode.size(); i++) {
                GenericRowData after = (GenericRowData) rowDataConverter.convert(dataNode.get(i));

                if (oldNode != null) {
                    GenericRowData before = (GenericRowData) rowDataConverter.convert(oldNode.get(i));
                    for (int f = 0; f < rowType.getFieldCount(); f++) {
                        if (before.isNullAt(f) && oldNode.get(i).findValue(rowType.getFieldNames().get(f)) == null) {
                            // fields in "old" (before) means the fields are changed
                            // fields not in "old" (before) means the fields are not changed
                            // so we just copy the not changed fields into before
                            before.setField(f, after.getField(f));
                        }
                    }
                    before.setRowKind(RowKind.UPDATE_BEFORE);
                    rowDataList.add(before);
                }

                after.setRowKind(RowKind.UPDATE_AFTER);
                rowDataList.add(after);
            }
        } else if (OP_DELETE.equals(op)) {
            for (JsonNode row : dataNode) {
                RowData rowData = (RowData) rowDataConverter.convert(row);
                rowData.setRowKind(RowKind.DELETE);
                rowDataList.add(rowData);
            }
        } else {
            throw new IllegalArgumentException("Unsupported op_type: " + op);
        }

        return rowDataList;
    }
}
