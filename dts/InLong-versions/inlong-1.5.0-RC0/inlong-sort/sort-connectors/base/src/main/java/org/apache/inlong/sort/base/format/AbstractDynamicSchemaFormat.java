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

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Abstact dynamic format class
 * This class main handle:
 * 1. deserialize data from byte array to get raw data
 * 2. parse pattern and get the real value from the raw data
 * Such as:
 * 1). give a pattern "${a}{b}{c}" and the raw data contains the keys(a: '1', b: '2', c: '3')
 * the result of pared will be '123'
 * 2). give a pattern "${a}_{b}_{c}" and the raw data contains the keys(a: '1', b: '2', c: '3')
 * the result of pared will be '1_2_3'
 * 3). give a pattern "prefix_${a}_{b}_{c}_suffix" and the raw Node contains the keys(a: '1', b: '2', c: '3')
 * the result of pared will be 'prefix_1_2_3_suffix'
 */
public abstract class AbstractDynamicSchemaFormat<T> {

    public static final Pattern PATTERN = Pattern.compile("\\$\\{\\s*([\\w.-]+)\\s*}", Pattern.CASE_INSENSITIVE);

    /**
     * Extract values by key from the raw data
     *
     * @param message The byte array of raw data
     * @param keys The key list that will be used to extract
     * @return The value list maps the keys
     * @throws IOException The exceptions may throws when extract
     */
    public List<String> extractValues(byte[] message, String... keys) throws IOException {
        if (keys == null || keys.length == 0) {
            return new ArrayList<>();
        }
        return extractValues(deserialize(message), keys);
    }

    /**
     * Extract values by key from the raw data
     *
     * @param data The raw data
     * @param keys The key list that will be used to extract
     * @return The value list maps the keys
     */
    public List<String> extractValues(T data, String... keys) {
        if (keys == null || keys.length == 0) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>(keys.length);
        for (String key : keys) {
            values.add(extract(data, key));
        }
        return values;
    }

    /**
     * Extract value by key from the raw data
     *
     * @param data The raw data
     * @param key The key that will be used to extract
     * @return The value maps the key in the raw data
     */
    public abstract String extract(T data, String key);

    /**
     * Extract primary key names
     *
     * @param data The raw data
     * @return The primary key name list
     */
    public abstract List<String> extractPrimaryKeyNames(T data);

    /**
     * Extract primary key values
     *
     * @param message The byte array of raw data
     * @return The values of primary key
     * @throws IOException The exception may be thrown when executing
     */
    public List<String> extractPrimaryKeyValues(byte[] message) throws IOException {
        return extractPrimaryKeyValues(deserialize(message));
    }

    /**
     * Extract primary key values
     *
     * @param data The raw data
     * @return The values of primary key
     */
    public List<String> extractPrimaryKeyValues(T data) {
        List<String> pkNames = extractPrimaryKeyNames(data);
        if (pkNames == null || pkNames.isEmpty()) {
            return new ArrayList<>();
        }
        return extractValues(data, pkNames.toArray(new String[]{}));
    }

    /**
     * Extract is-ddl flag
     *
     * @param data The raw data
     * @return The flag of whether is ddl
     */
    public abstract boolean extractDDLFlag(T data);

    public RowType extractSchema(T data) {
        return extractSchema(data, extractPrimaryKeyNames(data));
    }

    /**
     * Extract data schema info {@link RowType} from data
     *
     * @param data The raw data
     * @return The data schema info
     */
    public abstract RowType extractSchema(T data, List<String> pkNames);

    public List<RowData> extractRowData(T data) {
        return extractRowData(data, extractSchema(data));
    }

    /**
     * Extract data {@link RowData} from data
     *
     * @param data The raw data
     * @return The row data
     */
    public abstract List<RowData> extractRowData(T data, RowType rowType);

    /**
     * Deserialize from byte array
     *
     * @param message The byte array of raw data
     * @return The raw data T
     * @throws IOException The exceptions may throws when deserialize
     */
    public abstract T deserialize(byte[] message) throws IOException;

    /**
     * Parse msg and replace the value by key from meta data and physical data.
     * See details {@link AbstractDynamicSchemaFormat#parse(T, String)}
     *
     * @param message The source of data rows format by bytes
     * @param pattern The pattern value
     * @return The result of parsed
     * @throws IOException The exception that will throws
     */
    public String parse(byte[] message, String pattern) throws IOException {
        return parse(deserialize(message), pattern);
    }

    /**
     * Parse msg and replace the value by key from the raw data
     * Such as:
     * 1. give a pattern "${a}{b}{c}" and the data contains the keys(a: '1', b: '2', c: '3')
     * the result of pared will be '123'
     * 2. give a pattern "${a}_{b}_{c}" and the data contains the keys(a: '1', b: '2', c: '3')
     * the result of pared will be '1_2_3'
     * 3. give a pattern "prefix_${a}_{b}_{c}_suffix" and the data contains the keys(a: '1', b: '2', c: '3')
     * the result of pared will be 'prefix_1_2_3_suffix'
     *
     * @param data The raw data
     * @param pattern The pattern value
     * @return The result of parsed
     * @throws IOException The exception will throws
     */
    public abstract String parse(T data, String pattern) throws IOException;
}
