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

package org.apache.inlong.sort.base.dirty.utils;

import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.formats.json.JsonOptions.MapNullKeyMode;
import org.apache.flink.formats.json.RowDataToJsonConverters;
import org.apache.flink.formats.json.RowDataToJsonConverters.RowDataToJsonConverter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.LogicalType;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import static org.apache.flink.table.data.RowData.createFieldGetter;

/**
 * Format utils
 */
public final class FormatUtils {

    /**
     * The value for nullable field that is used for 'csv' format
     */
    private static final String NULL_VALUE = "null";
    /**
     * The default value of field delimiter that is used for 'csv' format
     */
    private static final String DEFAULT_FIELD_DELIMITER = ",";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectNode reuse = MAPPER.createObjectNode();

    private FormatUtils() {
    }

    /**
     * Parse FieldGetter from LogicalType
     * @param rowType The row type
     * @return A array of FieldGetter
     */
    public static RowData.FieldGetter[] parseFieldGetters(LogicalType rowType) {
        List<LogicalType> logicalTypes = rowType.getChildren();
        RowData.FieldGetter[] fieldGetters = new RowData.FieldGetter[logicalTypes.size()];
        for (int i = 0; i < logicalTypes.size(); i++) {
            fieldGetters[i] = createFieldGetter(logicalTypes.get(i), i);
        }
        return fieldGetters;
    }

    /**
     * Parse RowDataToJsonConverter
     * @param rowType The row type
     * @return RowDataToJsonConverter
     */
    public static RowDataToJsonConverter parseRowDataToJsonConverter(LogicalType rowType) {
        return new RowDataToJsonConverters(TimestampFormat.SQL, MapNullKeyMode.DROP, null)
                .createConverter(rowType);
    }

    /**
     * Csv format for 'RowData'
     *
     * @param data The data wrapper with 'RowData'
     * @param fieldGetters The field getters of 'RowData'
     * @param labels The labels of dirty sink
     * @param fieldDelimiter The field delimiter
     * @return The value after format
     */
    public static String csvFormat(RowData data, RowData.FieldGetter[] fieldGetters,
            Map<String, String> labels, String fieldDelimiter) {
        StringJoiner result = csvFormatForLabels(labels, fieldDelimiter);
        for (int i = 0; i < data.getArity(); i++) {
            Object value = fieldGetters[i].getFieldOrNull(data);
            result.add(value != null ? value.toString() : NULL_VALUE);
        }
        return result.toString();
    }

    /**
     * Csv format for 'JsonNode'
     *
     * @param data The data wrapper with 'JsonNode'
     * @param labels The labels of dirty sink
     * @param fieldDelimiter The field delimiter
     * @return The value after format
     */
    public static String csvFormat(JsonNode data, Map<String, String> labels, String fieldDelimiter) {
        StringJoiner result = csvFormatForLabels(labels, fieldDelimiter);
        Iterator<Entry<String, JsonNode>> iterator = data.fields();
        while (iterator.hasNext()) {
            Entry<String, JsonNode> kv = iterator.next();
            result.add(kv.getValue() != null ? kv.getValue().asText() : NULL_VALUE);
        }
        return result.toString();
    }

    /**
     * Csv format for 'Object'
     *
     * @param data Any object
     * @param labels The labels of dirty sink
     * @param fieldDelimiter The field delimiter
     * @return The value after format
     */
    public static String csvFormat(Object data, Map<String, String> labels, String fieldDelimiter) {
        StringJoiner result = csvFormatForLabels(labels, fieldDelimiter);
        result.add(data == null ? NULL_VALUE : data.toString());
        return result.toString();
    }

    /**
     * Csv format for labels
     *
     * @param labels The labels of dirty sink
     * @param result The result that store the value after format
     * @return The value after format
     */
    public static StringJoiner csvFormatForLabels(Map<String, String> labels, StringJoiner result) {
        if (labels == null || labels.isEmpty()) {
            return result;
        }
        for (Entry<String, String> kv : labels.entrySet()) {
            result.add(kv.getValue() != null ? kv.getValue() : NULL_VALUE);
        }
        return result;
    }

    /**
     * Csv format for labels
     *
     * @param labels The labels of dirty sink
     * @param fieldDelimiter The field delimiter
     * @return The value after format
     */
    public static StringJoiner csvFormatForLabels(Map<String, String> labels, String fieldDelimiter) {
        return csvFormatForLabels(labels,
                new StringJoiner(fieldDelimiter == null ? DEFAULT_FIELD_DELIMITER : fieldDelimiter));
    }

    /**
     * Json format for 'RowData'
     *
     * @param data The data wrapper with 'RowData'
     * @param converter The converter of 'RowData'
     * @param labels The labels of dirty sink
     * @return The value after format
     * @throws JsonProcessingException The exception may be thrown when executing
     */
    public static String jsonFormat(RowData data, RowDataToJsonConverter converter,
            Map<String, String> labels) throws JsonProcessingException {
        return jsonFormat(converter.convert(MAPPER, reuse, data), labels);
    }

    /**
     * Json format for 'JsonNode'
     *
     * @param data The data wrapper with 'JsonNode'
     * @param labels The labels of dirty sink
     * @return The value after format
     * @throws JsonProcessingException The exception may be thrown when executing
     */
    public static String jsonFormat(JsonNode data, Map<String, String> labels)
            throws JsonProcessingException {
        Map<String, Object> result = new LinkedHashMap<>(labels);
        Iterator<Entry<String, JsonNode>> iterator = data.fields();
        while (iterator.hasNext()) {
            Entry<String, JsonNode> kv = iterator.next();
            result.put(kv.getKey(), kv.getValue());
        }
        return MAPPER.writeValueAsString(result);
    }
}
