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
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.types.Row;
import org.apache.inlong.sort.formats.base.TableFormatConstants;
import org.apache.inlong.sort.formats.base.TableFormatUtils;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.util.StringUtils;

/**
 * The serializer for the records in csv format.
 */
public class CsvSerializationSchema implements SerializationSchema<Row> {

    private static final long serialVersionUID = 1L;

    /**
     * Format information describing the result type.
     */
    private final RowFormatInfo rowFormatInfo;

    /**
     * The charset for the text.
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

    public CsvSerializationSchema(
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

    public CsvSerializationSchema(
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

    @Override
    public byte[] serialize(Row row) {
        if (row == null) {
            return null;
        }

        String[] fieldNames = rowFormatInfo.getFieldNames();
        FormatInfo[] fieldFormatInfos = rowFormatInfo.getFieldFormatInfos();

        if (row.getArity() != fieldFormatInfos.length) {
            throw new IllegalStateException("The number of fields " + "mismatches: "
                    + fieldFormatInfos.length + " expected, " + "but was " + row.getArity() + ".");
        }

        String[] fieldTexts = new String[row.getArity()];

        for (int i = 0; i < row.getArity(); ++i) {

            String fieldText =
                    TableFormatUtils.serializeBasicField(
                            fieldNames[i],
                            fieldFormatInfos[i],
                            row.getField(i),
                            nullLiteral
                    );
            fieldTexts[i] = fieldText;
        }

        String result =
                StringUtils.concatCsv(fieldTexts, delimiter, escapeChar, quoteChar);

        return result.getBytes(Charset.forName(charset));
    }

    /**
     * Builder for {@link CsvSerializationSchema}.
     */
    public static class Builder {

        private final RowFormatInfo rowFormatInfo;

        private char delimiter = TableFormatConstants.DEFAULT_DELIMITER;
        private String charset = TableFormatConstants.DEFAULT_CHARSET;
        private Character escapeChar = null;
        private Character quoteChar = null;
        private String nullLiteral = null;

        /**
         * Creates a CSV serialization schema for the given type information.
         *
         * @param rowFormatInfo Type information describing the result type.
         */
        public Builder(RowFormatInfo rowFormatInfo) {
            this.rowFormatInfo = rowFormatInfo;
        }

        public Builder setDelimiter(char delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public Builder setCharset(String charset) {
            this.charset = charset;
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

        public CsvSerializationSchema build() {
            return new CsvSerializationSchema(
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

        CsvSerializationSchema that = (CsvSerializationSchema) o;
        return Objects.equals(rowFormatInfo, that.rowFormatInfo)
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
