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

import java.nio.charset.Charset;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.types.Row;
import org.apache.inlong.sort.formats.base.TableFormatConstants;
import org.apache.inlong.sort.formats.base.TableFormatUtils;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The deserializer for the records in csv format.
 */
public final class CsvDeserializationSchema implements DeserializationSchema<Row> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(CsvDeserializationSchema.class);

    /**
     * Format information describing the result type.
     */
    @Nonnull
    private final RowFormatInfo rowFormatInfo;

    /**
     * The charset of the text.
     */
    @Nonnull
    private final String charset;

    /**
     * The delimiter between fields.
     */
    @Nonnull
    private final Character delimiter;

    /**
     * Escape character. Null if escaping is disabled.
     */
    @Nullable
    private final Character escapeChar;

    /**
     * Quote character. Null if quoting is disabled.
     */
    @Nullable
    private final Character quoteChar;

    /**
     * The literal represented null values, default "".
     */
    @Nullable
    private final String nullLiteral;

    public CsvDeserializationSchema(
            @Nonnull RowFormatInfo rowFormatInfo,
            @Nonnull String charset,
            @Nonnull Character delimiter,
            @Nullable Character escapeChar,
            @Nullable Character quoteChar,
            @Nullable String nullLiteral
    ) {
        this.rowFormatInfo = rowFormatInfo;
        this.charset = charset;
        this.delimiter = delimiter;
        this.escapeChar = escapeChar;
        this.quoteChar = quoteChar;
        this.nullLiteral = nullLiteral;
    }

    public CsvDeserializationSchema(
            @Nonnull RowFormatInfo rowFormatInfo
    ) {
        this(
                rowFormatInfo,
                TableFormatConstants.DEFAULT_CHARSET,
                TableFormatConstants.DEFAULT_DELIMITER,
                null,
                null,
                null
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public TypeInformation<Row> getProducedType() {
        return (TypeInformation<Row>) TableFormatUtils.getType(rowFormatInfo.getTypeInfo());
    }

    @Override
    public boolean isEndOfStream(Row t) {
        return false;
    }

    @Override
    public Row deserialize(byte[] bytes) {
        String text = new String(bytes, Charset.forName(charset));

        String[] fieldNames = rowFormatInfo.getFieldNames();
        FormatInfo[] fieldFormatInfos = rowFormatInfo.getFieldFormatInfos();

        String[] fieldTexts =
                StringUtils.splitCsv(text, delimiter, escapeChar, quoteChar);

        if (fieldTexts.length != fieldNames.length) {
            LOG.warn("The number of fields mismatches: " + fieldNames.length
                     + " expected, but was " + fieldTexts.length + ".");
        }

        Row row = new Row(fieldNames.length);

        for (int i = 0; i < fieldNames.length; ++i) {
            if (i >= fieldTexts.length) {
                row.setField(i, null);
            } else {
                Object field =
                        TableFormatUtils.deserializeBasicField(
                                fieldNames[i],
                                fieldFormatInfos[i],
                                fieldTexts[i],
                                nullLiteral
                        );

                row.setField(i, field);
            }
        }

        return row;
    }

    /**
     * Builder for {@link CsvDeserializationSchema}.
     */
    public static class Builder {

        private final RowFormatInfo rowFormatInfo;

        private char delimiter = TableFormatConstants.DEFAULT_DELIMITER;
        private String charset = TableFormatConstants.DEFAULT_CHARSET;
        private Character escapeChar = null;
        private Character quoteChar = null;
        private String nullLiteral = null;

        /**
         * Creates a CSV deserialization schema for the given type information.
         *
         * @param rowFormatInfo Type information describing the result type.
         */
        public Builder(RowFormatInfo rowFormatInfo) {
            this.rowFormatInfo = rowFormatInfo;
        }

        public Builder setCharset(String charset) {
            this.charset = charset;
            return this;
        }

        public Builder setDelimiter(char delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public Builder setEscapeCharacter(char escapeChar) {
            this.escapeChar = escapeChar;
            return this;
        }

        public Builder setQuoteCharacter(char quoteChar) {
            this.quoteChar = quoteChar;
            return this;
        }

        public Builder setNullLiteral(String nullLiteral) {
            this.nullLiteral = nullLiteral;
            return this;
        }

        public CsvDeserializationSchema build() {
            return new CsvDeserializationSchema(
                    rowFormatInfo,
                    charset,
                    delimiter,
                    escapeChar,
                    quoteChar,
                    nullLiteral
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CsvDeserializationSchema that = (CsvDeserializationSchema) o;
        return rowFormatInfo.equals(that.rowFormatInfo)
                       && Objects.equals(charset, that.charset)
                       && Objects.equals(delimiter, that.delimiter)
                       && Objects.equals(escapeChar, that.escapeChar)
                       && Objects.equals(quoteChar, that.quoteChar)
                       && Objects.equals(nullLiteral, that.nullLiteral);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowFormatInfo, charset, delimiter,
                escapeChar, quoteChar, nullLiteral);
    }
}
