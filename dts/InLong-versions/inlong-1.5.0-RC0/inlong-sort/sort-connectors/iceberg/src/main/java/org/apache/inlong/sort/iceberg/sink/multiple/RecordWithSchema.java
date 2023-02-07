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

package org.apache.inlong.sort.iceberg.sink.multiple;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.Schema;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types.BinaryType;
import org.apache.iceberg.types.Types.BooleanType;
import org.apache.iceberg.types.Types.DateType;
import org.apache.iceberg.types.Types.DoubleType;
import org.apache.iceberg.types.Types.FloatType;
import org.apache.iceberg.types.Types.IntegerType;
import org.apache.iceberg.types.Types.ListType;
import org.apache.iceberg.types.Types.LongType;
import org.apache.iceberg.types.Types.MapType;
import org.apache.iceberg.types.Types.NestedField;
import org.apache.iceberg.types.Types.StringType;
import org.apache.iceberg.types.Types.StructType;
import org.apache.iceberg.types.Types.TimeType;
import org.apache.iceberg.types.Types.TimestampType;
import org.apache.iceberg.types.Types.UUIDType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class RecordWithSchema {

    public RecordWithSchema(
            JsonNode originalData,
            Schema schema,
            TableIdentifier tableId,
            List<String> primaryKeys) {
        this.originalData = originalData;
        this.schema = schema;
        this.tableId = tableId;
        this.primaryKeys = primaryKeys;
    }

    private transient JsonNode originalData;

    private List<RowData> data;

    private Schema schema;

    private final TableIdentifier tableId;

    private final List<String> primaryKeys;

    public List<RowData> getData() {
        return data;
    }

    public Schema getSchema() {
        return schema;
    }

    public TableIdentifier getTableId() {
        return tableId;
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public RecordWithSchema refreshFieldId(Schema newSchema) {
        // Solve the problem that there is no fieldId on the schema parsed from the data
        // So here refresh catalog loaded schema
        schema = newSchema.select(schema.columns().stream().map(NestedField::name).collect(Collectors.toList()));
        return this;
    }

    public RecordWithSchema refreshRowData(BiFunction<JsonNode, Schema, List<RowData>> rowDataExtractor) {
        // Solve the problem of type error during downstream parsing. Here, rowData is set to be compatible with rowType
        data = rowDataExtractor.apply(originalData, schema);
        return this;
    }

    // todo: here RecordWithSchema is deserialized from network, it's `Schema` is the new object, the `Type` is the
    // same.However `Type` do not implement equals method, so some method will return unexpected result when
    // compare this schema with Table#schema loaded from catalog, For example, Schema#sameSchema will return false
    // even thought schema is the same, can't get the comparators of Type even thought type is the same.
    public void replaceSchema() {
        List<NestedField> columns = schema.columns();
        List<NestedField> newColumns = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            newColumns.add(replaceField(columns.get(i)));
        }
        schema = new Schema(schema.schemaId(), newColumns, schema.getAliases(), schema.identifierFieldIds());
    }

    private static NestedField replaceField(NestedField nestedField) {
        return NestedField.of(
                nestedField.fieldId(),
                nestedField.isOptional(),
                nestedField.name(),
                replaceType(nestedField.type()),
                nestedField.doc());
    }

    private static Type replaceType(Type type) {
        switch (type.typeId()) {
            case BOOLEAN:
                return BooleanType.get();
            case INTEGER:
                return IntegerType.get();
            case LONG:
                return LongType.get();
            case FLOAT:
                return FloatType.get();
            case DOUBLE:
                return DoubleType.get();
            case DATE:
                return DateType.get();
            case TIME:
                return TimeType.get();
            case TIMESTAMP:
                return ((TimestampType) type).shouldAdjustToUTC()
                        ? TimestampType.withZone()
                        : TimestampType.withoutZone();
            case STRING:
                return StringType.get();
            case UUID:
                return UUIDType.get();
            case BINARY:
                return BinaryType.get();
            case FIXED:
            case DECIMAL:
                return type;
            case STRUCT:
                return StructType.of(
                        ((StructType) type).fields()
                                .stream()
                                .map(RecordWithSchema::replaceField)
                                .collect(Collectors.toList()));
            case LIST:
                ListType listType = ((ListType) type);
                return listType.isElementOptional()
                        ? ListType.ofOptional(listType.elementId(), replaceType(listType.elementType()))
                        : ListType.ofRequired(listType.elementId(), replaceType(listType.elementType()));
            case MAP:
                MapType mapType = ((MapType) type);
                return mapType.isValueOptional()
                        ? MapType.ofOptional(mapType.keyId(), mapType.valueId(),
                                replaceType(mapType.keyType()), replaceType(mapType.valueType()))
                        : MapType.ofRequired(mapType.keyId(), mapType.valueId(),
                                replaceType(mapType.keyType()), replaceType(mapType.valueType()));
            default:
                throw new UnsupportedOperationException("Unspported type: " + type);
        }
    }
}
