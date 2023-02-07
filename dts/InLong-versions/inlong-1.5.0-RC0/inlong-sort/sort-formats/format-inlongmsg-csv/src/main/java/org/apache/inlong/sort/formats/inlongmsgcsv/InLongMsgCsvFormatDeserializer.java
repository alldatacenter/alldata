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

package org.apache.inlong.sort.formats.inlongmsgcsv;

import static org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils.DEFAULT_ATTRIBUTES_FIELD_NAME;
import static org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils.DEFAULT_TIME_FIELD_NAME;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.types.Row;
import org.apache.inlong.sort.formats.base.TableFormatConstants;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.inlongmsg.AbstractInLongMsgFormatDeserializer;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgBody;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgHead;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils;

/**
 * The deserializer for the records in InLongMsgCsv format.
 */
public final class InLongMsgCsvFormatDeserializer extends AbstractInLongMsgFormatDeserializer {

    private static final long serialVersionUID = 1L;

    /**
     * Format information describing the result type.
     */
    @Nonnull
    private final RowFormatInfo rowFormatInfo;

    /**
     * The name of the time field.
     */
    @Nonnull
    private final String timeFieldName;

    /**
     * The name of the attributes field.
     */
    @Nonnull
    private final String attributesFieldName;

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

    /**
     * True if the head delimiter should be removed.
     */
    private final boolean deleteHeadDelimiter;

    public InLongMsgCsvFormatDeserializer(
            @Nonnull RowFormatInfo rowFormatInfo,
            @Nonnull String timeFieldName,
            @Nonnull String attributesFieldName,
            @Nonnull String charset,
            @Nonnull Character delimiter,
            @Nullable Character escapeChar,
            @Nullable Character quoteChar,
            @Nullable String nullLiteral,
            boolean deleteHeadDelimiter,
            boolean ignoreErrors) {
        super(ignoreErrors);

        this.rowFormatInfo = rowFormatInfo;
        this.timeFieldName = timeFieldName;
        this.attributesFieldName = attributesFieldName;
        this.delimiter = delimiter;
        this.charset = charset;
        this.escapeChar = escapeChar;
        this.quoteChar = quoteChar;
        this.nullLiteral = nullLiteral;
        this.deleteHeadDelimiter = deleteHeadDelimiter;
    }

    public InLongMsgCsvFormatDeserializer(
            @Nonnull RowFormatInfo rowFormatInfo) {
        this(
                rowFormatInfo,
                DEFAULT_TIME_FIELD_NAME,
                DEFAULT_ATTRIBUTES_FIELD_NAME,
                TableFormatConstants.DEFAULT_CHARSET,
                TableFormatConstants.DEFAULT_DELIMITER,
                null,
                null,
                null,
                InLongMsgCsvUtils.DEFAULT_DELETE_HEAD_DELIMITER,
                TableFormatConstants.DEFAULT_IGNORE_ERRORS);
    }

    @Override
    public TypeInformation<Row> getProducedType() {
        return InLongMsgUtils.buildRowType(timeFieldName, attributesFieldName, rowFormatInfo);
    }

    @Override
    protected InLongMsgHead parseHead(String attr) {
        return InLongMsgUtils.parseHead(attr);
    }

    @Override
    protected InLongMsgBody parseBody(byte[] bytes) {
        return InLongMsgCsvUtils.parseBody(
                bytes,
                charset,
                delimiter,
                escapeChar,
                quoteChar,
                deleteHeadDelimiter);
    }

    @Override
    protected Row convertRow(InLongMsgHead head, InLongMsgBody body) {
        return InLongMsgCsvUtils.buildRow(
                rowFormatInfo,
                nullLiteral,
                head.getTime(),
                head.getAttributes(),
                head.getPredefinedFields(),
                body.getFields());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        InLongMsgCsvFormatDeserializer that = (InLongMsgCsvFormatDeserializer) o;
        return deleteHeadDelimiter == that.deleteHeadDelimiter
                && rowFormatInfo.equals(that.rowFormatInfo)
                && timeFieldName.equals(that.timeFieldName)
                && attributesFieldName.equals(that.attributesFieldName)
                && charset.equals(that.charset)
                && delimiter.equals(that.delimiter)
                && Objects.equals(escapeChar, that.escapeChar)
                && Objects.equals(quoteChar, that.quoteChar)
                && Objects.equals(nullLiteral, that.nullLiteral);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), rowFormatInfo, timeFieldName,
                attributesFieldName, charset, delimiter, escapeChar, quoteChar,
                nullLiteral, deleteHeadDelimiter);
    }
}
