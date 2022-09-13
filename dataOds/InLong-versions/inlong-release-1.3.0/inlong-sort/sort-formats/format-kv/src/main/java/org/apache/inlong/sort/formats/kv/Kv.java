/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.formats.kv;

import static org.apache.flink.table.descriptors.FormatDescriptorValidator.FORMAT_DERIVE_SCHEMA;
import static org.apache.flink.util.Preconditions.checkNotNull;

import java.nio.charset.Charset;
import java.util.Map;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.descriptors.FormatDescriptor;
import org.apache.inlong.sort.formats.base.TableFormatConstants;

/**
 * Format descriptor for KVs.
 */
public class Kv extends FormatDescriptor {

    public static final String FORMAT_TYPE_VALUE = "tdkv";

    private DescriptorProperties internalProperties =
            new DescriptorProperties(true);

    public Kv() {
        super(FORMAT_TYPE_VALUE, 1);
    }

    /**
     * Sets the entry delimiter character ('&' by default).
     *
     * @param delimiter the entry delimiter character
     */
    public Kv entryDelimiter(char delimiter) {
        internalProperties.putCharacter(TableFormatConstants.FORMAT_ENTRY_DELIMITER, delimiter);
        return this;
    }

    /**
     * Sets the kv delimiter character ('=' by default).
     *
     * @param delimiter the kv delimiter character
     */
    public Kv kvDelimiter(char delimiter) {
        internalProperties.putCharacter(TableFormatConstants.FORMAT_KV_DELIMITER, delimiter);
        return this;
    }

    /**
     * Sets the escape character (disabled by default).
     *
     * @param escapeCharacter escaping character (e.g. backslash).
     */
    public Kv escapeCharacter(char escapeCharacter) {
        internalProperties
                .putCharacter(TableFormatConstants.FORMAT_ESCAPE_CHARACTER, escapeCharacter);
        return this;
    }

    /**
     * Sets the quote character (disabled by default).
     *
     * @param quoteCharacter quoting character (e.g. quotation).
     */
    public Kv quoteCharacter(char quoteCharacter) {
        internalProperties.putCharacter(TableFormatConstants.FORMAT_QUOTE_CHARACTER, quoteCharacter);
        return this;
    }

    /**
     * Sets the null literal string that is interpreted as a null value
     * (disabled by default).
     *
     * @param nullLiteral null literal (e.g. "null" or "n/a")
     */
    public Kv nullLiteral(String nullLiteral) {
        checkNotNull(nullLiteral);
        internalProperties.putString(TableFormatConstants.FORMAT_NULL_LITERAL, nullLiteral);
        return this;
    }

    /**
     * Sets the charset of the text.
     *
     * @param charset The charset of the text.
     */
    public Kv charset(Charset charset) {
        checkNotNull(charset);
        internalProperties.putString(TableFormatConstants.FORMAT_CHARSET, charset.name());
        return this;
    }

    /**
     * Ignores the errors in the serialization and deserialization.
     */
    public Kv ignoreErrors() {
        internalProperties.putBoolean(TableFormatConstants.FORMAT_IGNORE_ERRORS, true);
        return this;
    }

    /**
     * Sets the format schema. Required if schema is not derived.
     *
     * @param schema format schema string.
     */
    public Kv schema(String schema) {
        checkNotNull(schema);
        internalProperties.putString(TableFormatConstants.FORMAT_SCHEMA, schema);
        return this;
    }

    /**
     * Derives the format schema from the table's schema. Required if no format
     * schema is defined.
     *
     * <p>This allows for defining schema information only once.
     *
     * <p>The names, types, and fields' order of the format are determined by
     * the table's schema. Time attributes are ignored if their origin is not a
     * field. A "from" definition is interpreted as a field renaming in the
     * format.
     */
    public Kv deriveSchema() {
        internalProperties.putBoolean(FORMAT_DERIVE_SCHEMA, true);
        return this;
    }

    @Override
    protected Map<String, String> toFormatProperties() {
        final DescriptorProperties properties = new DescriptorProperties();
        properties.putProperties(internalProperties);
        return properties.asMap();
    }
}
