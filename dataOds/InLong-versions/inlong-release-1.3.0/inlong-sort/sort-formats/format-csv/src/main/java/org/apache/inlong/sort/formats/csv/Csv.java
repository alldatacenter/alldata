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

package org.apache.inlong.sort.formats.csv;

import static org.apache.flink.table.descriptors.FormatDescriptorValidator.FORMAT_DERIVE_SCHEMA;
import static org.apache.flink.util.Preconditions.checkNotNull;

import java.nio.charset.Charset;
import java.util.Map;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.descriptors.FormatDescriptor;
import org.apache.inlong.sort.formats.base.TableFormatConstants;

/**
 * Format descriptor for comma-separated values (CSV).
 *
 * <p>This descriptor aims to comply with RFC-4180 ("Common Format and MIME Type
 * for Comma-Separated Values (CSV) Files") proposed by the Internet Engineering
 * Task Force (IETF).
 */
public class Csv extends FormatDescriptor {

    public static final String FORMAT_TYPE_VALUE = "tdcsv";

    private DescriptorProperties internalProperties =
            new DescriptorProperties(true);

    public Csv() {
        super(FORMAT_TYPE_VALUE, 1);
    }

    /**
     * Sets the delimiter character (',' by default).
     *
     * @param delimiter the field delimiter character
     */
    public Csv delimiter(char delimiter) {
        internalProperties.putCharacter(TableFormatConstants.FORMAT_DELIMITER, delimiter);
        return this;
    }

    /**
     * Sets the escape character (disabled by default).
     *
     * @param escapeCharacter escaping character (e.g. backslash).
     */
    public Csv escapeCharacter(char escapeCharacter) {
        internalProperties
                .putCharacter(TableFormatConstants.FORMAT_ESCAPE_CHARACTER, escapeCharacter);
        return this;
    }

    /**
     * Sets the quote character (disabled by default).
     *
     * @param quoteCharacter quoting character (e.g. quotation).
     */
    public Csv quoteCharacter(char quoteCharacter) {
        internalProperties.putCharacter(TableFormatConstants.FORMAT_QUOTE_CHARACTER, quoteCharacter);
        return this;
    }

    /**
     * Sets the null literal string that is interpreted as a null value
     * (disabled by default).
     *
     * @param nullLiteral null literal (e.g. "null" or "n/a")
     */
    public Csv nullLiteral(String nullLiteral) {
        checkNotNull(nullLiteral);
        internalProperties.putString(TableFormatConstants.FORMAT_NULL_LITERAL, nullLiteral);
        return this;
    }

    /**
     * Sets the charset of the text.
     *
     * @param charset The charset of the text.
     */
    public Csv charset(Charset charset) {
        checkNotNull(charset);
        internalProperties.putString(TableFormatConstants.FORMAT_CHARSET, charset.name());
        return this;
    }

    /**
     * Ignores the errors in the serialization and deserialization.
     */
    public Csv ignoreErrors() {
        internalProperties.putBoolean(TableFormatConstants.FORMAT_IGNORE_ERRORS, true);
        return this;
    }

    /**
     * Sets the format schema. Required if schema is not derived.
     *
     * @param schema format schema string.
     */
    public Csv schema(String schema) {
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
    public Csv deriveSchema() {
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
