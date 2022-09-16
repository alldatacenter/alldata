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

import static org.apache.inlong.sort.formats.util.StringUtils.concatKv;

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

/**
 * The serializer for the records in kv format.
 */
public final class KvSerializationSchema implements SerializationSchema<Row> {

    private static final long serialVersionUID = 1L;

    /**
     * Format information describing the result type.
     */
    private final RowFormatInfo rowFormatInfo;

    /**
     * The charset of the text.
     */
    @Nonnull
    private final String charset;

    /**
     * The delimiter between entries.
     */
    @Nonnull
    private final Character entryDelimiter;

    /**
     * The delimiter between key and value.
     */
    @Nonnull
    private final Character kvDelimiter;

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

    public KvSerializationSchema(
            @Nonnull RowFormatInfo rowFormatInfo,
            @Nonnull String charset,
            @Nonnull Character entryDelimiter,
            @Nonnull Character kvDelimiter,
            @Nullable Character escapeChar,
            @Nullable Character quoteChar,
            @Nullable String nullLiteral
    ) {
        this.rowFormatInfo = rowFormatInfo;
        this.charset = charset;
        this.entryDelimiter = entryDelimiter;
        this.kvDelimiter = kvDelimiter;
        this.escapeChar = escapeChar;
        this.quoteChar = quoteChar;
        this.nullLiteral = nullLiteral;
    }

    public KvSerializationSchema(
            @Nonnull RowFormatInfo rowFormatInfo
    ) {
        this(
                rowFormatInfo,
                TableFormatConstants.DEFAULT_CHARSET,
                TableFormatConstants.DEFAULT_ENTRY_DELIMITER,
                TableFormatConstants.DEFAULT_KV_DELIMITER,
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
            throw new RuntimeException("The number of fields mismatches: "
                   + fieldFormatInfos.length + " expected, but was " + row.getArity() + ".");
        }

        String[] fieldTexts = new String[row.getArity()];

        for (int i = 0; i < row.getArity(); ++i) {
            Object field = row.getField(i);

            String fieldText =
                    TableFormatUtils.serializeBasicField(
                            fieldNames[i],
                            fieldFormatInfos[i],
                            field,
                            nullLiteral
                    );

            fieldTexts[i] = fieldText;
        }

        String text =
                concatKv(
                        fieldNames,
                        fieldTexts,
                        entryDelimiter,
                        kvDelimiter,
                        escapeChar,
                        quoteChar
                );

        return text.getBytes(Charset.forName(charset));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        KvSerializationSchema that = (KvSerializationSchema) o;
        return Objects.equals(rowFormatInfo, that.rowFormatInfo)
                       && Objects.equals(charset, that.charset)
                       && Objects.equals(entryDelimiter, that.entryDelimiter)
                       && Objects.equals(kvDelimiter, that.kvDelimiter)
                       && Objects.equals(escapeChar, that.escapeChar)
                       && Objects.equals(quoteChar, that.quoteChar)
                       && Objects.equals(nullLiteral, that.nullLiteral);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowFormatInfo, charset, entryDelimiter, kvDelimiter,
                escapeChar, quoteChar, nullLiteral);
    }

    /**
     * Builder for {@link KvSerializationSchema}.
     */
    public static class Builder {

        private final RowFormatInfo rowFormatInfo;

        private String charset = TableFormatConstants.DEFAULT_CHARSET;
        private char entryDelimiter = TableFormatConstants.DEFAULT_ENTRY_DELIMITER;
        private char kvDelimiter = TableFormatConstants.DEFAULT_KV_DELIMITER;
        private Character escapeChar = null;
        private Character quoteChar = null;
        private String nullLiteral = null;

        /**
         * Creates a KV serialization schema for the given type information.
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

        public Builder setEntryDelimiter(char delimiter) {
            this.entryDelimiter = delimiter;
            return this;
        }

        public Builder setKvDelimiter(char delimiter) {
            this.kvDelimiter = delimiter;
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

        public KvSerializationSchema build() {
            return new KvSerializationSchema(
                    rowFormatInfo,
                    charset,
                    entryDelimiter,
                    kvDelimiter,
                    escapeChar,
                    quoteChar,
                    nullLiteral
            );
        }
    }
}
