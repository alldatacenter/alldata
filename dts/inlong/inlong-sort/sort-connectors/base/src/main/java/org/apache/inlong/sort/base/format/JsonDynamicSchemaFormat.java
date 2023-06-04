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

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.shaded.guava18.com.google.common.collect.ImmutableMap;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.types.logical.BigIntType;
import org.apache.flink.table.types.logical.BinaryType;
import org.apache.flink.table.types.logical.BooleanType;
import org.apache.flink.table.types.logical.CharType;
import org.apache.flink.table.types.logical.DateType;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.DoubleType;
import org.apache.flink.table.types.logical.FloatType;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.LocalZonedTimestampType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.RowType.RowField;
import org.apache.flink.table.types.logical.SmallIntType;
import org.apache.flink.table.types.logical.TimeType;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.table.types.logical.TinyIntType;
import org.apache.flink.table.types.logical.VarBinaryType;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.flink.types.RowKind;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_TYPE_MAP_COMPATIBLE_WITH_SPARK;

/**
 * Json dynamic format class
 * This class main handle:
 * 1. deserialize data from byte array
 * 2. parse pattern and get the real value from the raw data(contains meta data and physical data)
 * Such as:
 * 1). give a pattern "${a}{b}{c}" and the root Node contains the keys(a: '1', b: '2', c: '3')
 * the result of pared will be '123'
 * 2). give a pattern "${a}_{b}_{c}" and the root Node contains the keys(a: '1', b: '2', c: '3')
 * the result of pared will be '1_2_3'
 * 3). give a pattern "prefix_${a}_{b}_{c}_suffix" and the root Node contains the keys(a: '1', b: '2', c: '3')
 * the result of pared will be 'prefix_1_2_3_suffix'
 */
@SuppressWarnings("LanguageDetectionInspection")
public abstract class JsonDynamicSchemaFormat extends AbstractDynamicSchemaFormat<JsonNode> {

    /**
     * The first item of array
     */
    private static final Integer FIRST = 0;

    private static final int DEFAULT_DECIMAL_PRECISION = 15;
    private static final int DEFAULT_DECIMAL_SCALE = 5;

    private static final Map<Integer, LogicalType> SQL_TYPE_2_FLINK_TYPE_MAPPING =
            ImmutableMap.<Integer, LogicalType>builder()
                    .put(java.sql.Types.CHAR, new CharType())
                    .put(java.sql.Types.VARCHAR, new VarCharType())
                    .put(java.sql.Types.SMALLINT, new SmallIntType())
                    .put(java.sql.Types.INTEGER, new IntType())
                    .put(java.sql.Types.BIGINT, new BigIntType())
                    .put(java.sql.Types.REAL, new FloatType())
                    .put(java.sql.Types.DOUBLE, new DoubleType())
                    .put(java.sql.Types.FLOAT, new FloatType())
                    .put(java.sql.Types.DECIMAL, new DecimalType(DEFAULT_DECIMAL_PRECISION, DEFAULT_DECIMAL_SCALE))
                    .put(java.sql.Types.NUMERIC, new DecimalType(DEFAULT_DECIMAL_PRECISION, DEFAULT_DECIMAL_SCALE))
                    .put(java.sql.Types.BIT, new BooleanType())
                    .put(java.sql.Types.TIME, new TimeType())
                    .put(java.sql.Types.TIME_WITH_TIMEZONE, new TimeType())
                    .put(java.sql.Types.TIMESTAMP_WITH_TIMEZONE, new LocalZonedTimestampType())
                    .put(java.sql.Types.TIMESTAMP, new TimestampType())
                    .put(java.sql.Types.BINARY, new BinaryType())
                    .put(java.sql.Types.VARBINARY, new VarBinaryType())
                    .put(java.sql.Types.BLOB, new VarBinaryType())
                    .put(java.sql.Types.CLOB, new VarBinaryType())
                    .put(java.sql.Types.DATE, new DateType())
                    .put(java.sql.Types.BOOLEAN, new BooleanType())
                    .put(java.sql.Types.LONGNVARCHAR, new VarCharType())
                    .put(java.sql.Types.LONGVARBINARY, new VarCharType())
                    .put(java.sql.Types.LONGVARCHAR, new VarCharType())
                    .put(java.sql.Types.ARRAY, new VarCharType())
                    .put(java.sql.Types.NCHAR, new CharType())
                    .put(java.sql.Types.NCLOB, new VarBinaryType())
                    .put(java.sql.Types.TINYINT, new TinyIntType())
                    .put(java.sql.Types.OTHER, new VarCharType())
                    .build();

    private static final Map<Integer, LogicalType> SQL_TYPE_2_SPARK_SUPPORTED_FLINK_TYPE_MAPPING =
            ImmutableMap.<Integer, LogicalType>builder()
                    .put(java.sql.Types.CHAR, new CharType())
                    .put(java.sql.Types.VARCHAR, new VarCharType())
                    .put(java.sql.Types.SMALLINT, new SmallIntType())
                    .put(java.sql.Types.INTEGER, new IntType())
                    .put(java.sql.Types.BIGINT, new BigIntType())
                    .put(java.sql.Types.REAL, new FloatType())
                    .put(java.sql.Types.DOUBLE, new DoubleType())
                    .put(java.sql.Types.FLOAT, new FloatType())
                    .put(java.sql.Types.DECIMAL, new DecimalType(DEFAULT_DECIMAL_PRECISION, DEFAULT_DECIMAL_SCALE))
                    .put(java.sql.Types.NUMERIC, new DecimalType(DEFAULT_DECIMAL_PRECISION, DEFAULT_DECIMAL_SCALE))
                    .put(java.sql.Types.BIT, new BooleanType())
                    .put(java.sql.Types.TIME, new VarCharType())
                    .put(java.sql.Types.TIMESTAMP_WITH_TIMEZONE, new LocalZonedTimestampType())
                    .put(java.sql.Types.TIMESTAMP, new LocalZonedTimestampType())
                    .put(java.sql.Types.BINARY, new BinaryType())
                    .put(java.sql.Types.VARBINARY, new VarBinaryType())
                    .put(java.sql.Types.BLOB, new VarBinaryType())
                    .put(java.sql.Types.DATE, new DateType())
                    .put(java.sql.Types.BOOLEAN, new BooleanType())
                    .put(java.sql.Types.OTHER, new VarCharType())
                    .build();

    public final ObjectMapper objectMapper = new ObjectMapper();
    protected final JsonToRowDataConverters rowDataConverters;
    protected final boolean adaptSparkEngine;

    public JsonDynamicSchemaFormat(Map<String, String> properties) {
        ReadableConfig config = Configuration.fromMap(properties);
        this.adaptSparkEngine = config.get(SINK_MULTIPLE_TYPE_MAP_COMPATIBLE_WITH_SPARK);
        this.rowDataConverters =
                new JsonToRowDataConverters(
                        false,
                        false,
                        TimestampFormat.ISO_8601,
                        adaptSparkEngine);
    }

    /**
     * Extract values by keys from the raw data
     *
     * @param root The raw data
     * @param keys The key list that will be used to extract
     * @return The value list maps the keys
     */
    @Override
    public List<String> extractValues(JsonNode root, String... keys) {
        if (keys == null || keys.length == 0) {
            return new ArrayList<>();
        }
        JsonNode physicalNode = getPhysicalData(root);
        if (physicalNode.isArray()) {
            // Extract from the first value when the physicalNode is array
            physicalNode = physicalNode.get(FIRST);
        }
        List<String> values = new ArrayList<>(keys.length);
        if (physicalNode == null) {
            for (String key : keys) {
                values.add(extract(root, key));
            }
            return values;
        }
        for (String key : keys) {
            String value = extract(physicalNode, key);
            if (value == null) {
                value = extract(root, key);
            }
            values.add(value);
        }
        return values;
    }

    /**
     * Extract value by key from ${@link JsonNode}
     *
     * @param jsonNode The json node
     * @param key The key that will be used to extract
     * @return The value maps the key in the json node
     */
    @Override
    public String extract(JsonNode jsonNode, String key) {
        if (jsonNode == null || key == null) {
            return null;
        }
        JsonNode value = jsonNode.get(key);
        if (value != null) {
            return value.asText();
        }
        int index = key.indexOf(".");
        if (index > 0 && index + 1 < key.length()) {
            return extract(jsonNode.get(key.substring(0, index)), key.substring(index + 1));
        }
        return null;
    }

    /**
     * Deserialize from byte array and return a ${@link JsonNode}
     *
     * @param message The byte array of raw data
     * @return The JsonNode
     * @throws IOException The exceptions may throws when deserialize
     */
    @Override
    public JsonNode deserialize(byte[] message) throws IOException {
        return objectMapper.readTree(message);
    }

    /**
     * Parse msg and replace the value by key from meta data and physical.
     * See details {@link JsonDynamicSchemaFormat#parse(JsonNode, String)}
     *
     * @param message The source of data rows format by bytes
     * @param pattern The pattern value
     * @return The result of parsed
     * @throws IOException The exception will throws
     */
    @Override
    public String parse(byte[] message, String pattern) throws IOException {
        return parse(deserialize(message), pattern);
    }

    /**
     * Parse msg and replace the value by key from meta data and physical.
     * Such as:
     * 1. give a pattern "${a}{b}{c}" and the root Node contains the keys(a: '1', b: '2', c: '3')
     * the result of pared will be '123'
     * 2. give a pattern "${a}_{b}_{c}" and the root Node contains the keys(a: '1', b: '2', c: '3')
     * the result of pared will be '1_2_3'
     * 3. give a pattern "prefix_${a}_{b}_{c}_suffix" and the root Node contains the keys(a: '1', b: '2', c: '3')
     * the result of pared will be 'prefix_1_2_3_suffix'
     *
     * @param rootNode The root node of json
     * @param pattern The pattern value
     * @return The result of parsed
     * @throws IOException The exception will throws
     */
    @Override
    public String parse(JsonNode rootNode, String pattern) throws IOException {
        Matcher matcher = PATTERN.matcher(pattern);
        StringBuffer sb = new StringBuffer();
        JsonNode physicalNode = getPhysicalData(rootNode);
        if (physicalNode.isArray()) {
            // Extract from the first value when the physicalNode is array
            physicalNode = physicalNode.get(FIRST);
        }
        while (matcher.find()) {
            String keyText = matcher.group(1);
            String replacement = extract(physicalNode, keyText);
            if (replacement == null) {
                replacement = extract(rootNode, keyText);
            }
            if (replacement == null) {
                // The variable replacement here is mainly used for
                // multi-sink scenario synchronization destination positioning, so the value of null cannot be ignored.
                throw new IOException(String.format("Can't find value for key: %s", keyText));
            }
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Get physical data from the root json node
     *
     * @param root The json root node
     * @return The physical data node
     */
    public JsonNode getPhysicalData(JsonNode root) {
        JsonNode physicalData = getUpdateAfter(root);
        if (physicalData == null) {
            physicalData = getUpdateBefore(root);
        }
        return physicalData;
    }

    /**
     * Get physical data of update after
     *
     * @param root The json root node
     * @return The physical data node of update after
     */
    public abstract JsonNode getUpdateAfter(JsonNode root);

    /**
     * Get physical data of update before
     *
     * @param root The json root node
     * @return The physical data node of update before
     */
    public abstract JsonNode getUpdateBefore(JsonNode root);

    /**
     * Convert opType to RowKind
     *
     * @param opType The opTyoe of data
     * @return The RowKind of data
     */
    public abstract List<RowKind> opType2RowKind(String opType);

    /**
     * Get opType of data
     *
     * @param root The json root node
     * @return The opType of data
     */
    public abstract String getOpType(JsonNode root);

    protected RowType extractSchemaNode(JsonNode schema, List<String> pkNames) {
        Iterator<Entry<String, JsonNode>> schemaFields = schema.fields();
        List<RowField> fields = new ArrayList<>();
        while (schemaFields.hasNext()) {
            Entry<String, JsonNode> entry = schemaFields.next();
            String name = entry.getKey();
            LogicalType type = sqlType2FlinkType(entry.getValue().asInt());
            if (pkNames.contains(name)) {
                type = type.copy(false);
            }
            fields.add(new RowField(name, type));
        }
        return new RowType(fields);
    }

    private LogicalType sqlType2FlinkType(int jdbcType) {
        Map<Integer, LogicalType> typeMap = adaptSparkEngine
                ? SQL_TYPE_2_SPARK_SUPPORTED_FLINK_TYPE_MAPPING
                : SQL_TYPE_2_FLINK_TYPE_MAPPING;
        if (typeMap.containsKey(jdbcType)) {
            return typeMap.get(jdbcType);
        } else {
            throw new IllegalArgumentException("Unsupported jdbcType: " + jdbcType);
        }
    }

    /**
     * Convert json node to map
     *
     * @param data The json node
     * @return The List of json node
     * @throws IOException The exception may be thrown when executing
     */
    public List<Map<String, String>> jsonNode2Map(JsonNode data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<Map<String, String>> values = new ArrayList<>();
        if (data.isArray()) {
            for (int i = 0; i < data.size(); i++) {
                values.add(objectMapper.convertValue(data.get(i), new TypeReference<Map<String, String>>() {
                }));
            }
        } else {
            values.add(objectMapper.convertValue(data, new TypeReference<Map<String, String>>() {
            }));
        }
        return values;
    }
}
