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

package org.apache.inlong.sort.formats.inlongmsgcsv;

import static org.apache.inlong.sort.formats.base.TableFormatConstants.DEFAULT_CHARSET;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.DEFAULT_DELIMITER;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.DEFAULT_IGNORE_ERRORS;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_CHARSET;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_DELIMITER;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_ESCAPE_CHARACTER;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_IGNORE_ERRORS;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_NULL_LITERAL;
import static org.apache.inlong.sort.formats.base.TableFormatConstants.FORMAT_QUOTE_CHARACTER;
import static org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils.FORMAT_ATTRIBUTES_FIELD_NAME;
import static org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils.FORMAT_TIME_FIELD_NAME;
import static org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils.getDataFormatInfo;
import static org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils.validateFieldNames;
import static org.apache.inlong.sort.formats.inlongmsgcsv.InLongMsgCsvUtils.FORMAT_DELETE_HEAD_DELIMITER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.factories.TableFormatFactoryBase;
import org.apache.flink.types.Row;
import org.apache.inlong.sort.formats.base.TableFormatConstants;
import org.apache.inlong.sort.formats.base.TableFormatDeserializer;
import org.apache.inlong.sort.formats.base.TableFormatDeserializerFactory;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgMixedFormatFactory;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgMixedValidator;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgValidator;

/**
 * Table format factory for providing configured instances of InLongMsgCsv-to-row
 * serializer and deserializer.
 */
public final class InLongMsgCsvFormatFactory
        extends TableFormatFactoryBase<Row>
        implements TableFormatDeserializerFactory, InLongMsgMixedFormatFactory {

    public InLongMsgCsvFormatFactory() {
        super(InLongMsgCsv.FORMAT_TYPE_VALUE, 1, true);
    }

    @Override
    public List<String> supportedFormatProperties() {
        final List<String> properties = new ArrayList<>();
        properties.add(FORMAT_CHARSET);
        properties.add(FORMAT_DELIMITER);
        properties.add(FORMAT_ESCAPE_CHARACTER);
        properties.add(FORMAT_QUOTE_CHARACTER);
        properties.add(FORMAT_NULL_LITERAL);
        properties.add(FORMAT_DELETE_HEAD_DELIMITER);
        properties.add(TableFormatConstants.FORMAT_SCHEMA);
        properties.add(FORMAT_TIME_FIELD_NAME);
        properties.add(FORMAT_ATTRIBUTES_FIELD_NAME);
        properties.add(FORMAT_IGNORE_ERRORS);
        return properties;
    }

    @Override
    public TableFormatDeserializer createFormatDeserializer(
            Map<String, String> properties
    ) {
        final DescriptorProperties descriptorProperties =
                new DescriptorProperties(true);
        descriptorProperties.putProperties(properties);

        final InLongMsgValidator validator = new InLongMsgValidator();
        validator.validate(descriptorProperties);

        RowFormatInfo rowFormatInfo = getDataFormatInfo(descriptorProperties);

        String timeFieldName =
                descriptorProperties
                        .getOptionalString(FORMAT_TIME_FIELD_NAME)
                        .orElse(InLongMsgUtils.DEFAULT_TIME_FIELD_NAME);
        String attributesFieldName =
                descriptorProperties
                        .getOptionalString(FORMAT_ATTRIBUTES_FIELD_NAME)
                        .orElse(InLongMsgUtils.DEFAULT_ATTRIBUTES_FIELD_NAME);

        validateFieldNames(timeFieldName, attributesFieldName, rowFormatInfo);

        String charset =
                descriptorProperties
                        .getOptionalString(FORMAT_CHARSET)
                        .orElse(DEFAULT_CHARSET);
        Character delimiter =
                descriptorProperties
                        .getOptionalCharacter(FORMAT_DELIMITER)
                        .orElse(DEFAULT_DELIMITER);
        Character escapeCharacter =
                descriptorProperties
                        .getOptionalCharacter(FORMAT_ESCAPE_CHARACTER)
                        .orElse(null);
        Character quoteCharacter =
                descriptorProperties
                        .getOptionalCharacter(FORMAT_QUOTE_CHARACTER)
                        .orElse(null);
        String nullLiteral =
                descriptorProperties
                        .getOptionalString(FORMAT_NULL_LITERAL)
                        .orElse(null);
        Boolean deleteHeadDelimiter =
                descriptorProperties
                        .getOptionalBoolean(FORMAT_DELETE_HEAD_DELIMITER)
                        .orElse(InLongMsgCsvUtils.DEFAULT_DELETE_HEAD_DELIMITER);
        boolean ignoreErrors =
                descriptorProperties
                        .getOptionalBoolean(FORMAT_IGNORE_ERRORS)
                        .orElse(DEFAULT_IGNORE_ERRORS);

        return new InLongMsgCsvFormatDeserializer(
                rowFormatInfo,
                timeFieldName,
                attributesFieldName,
                charset,
                delimiter,
                escapeCharacter,
                quoteCharacter,
                nullLiteral,
                deleteHeadDelimiter,
                ignoreErrors
        );
    }

    @Override
    public InLongMsgCsvMixedFormatDeserializer createMixedFormatDeserializer(
            Map<String, String> properties
    ) {
        final DescriptorProperties descriptorProperties =
                new DescriptorProperties(true);
        descriptorProperties.putProperties(properties);

        final InLongMsgMixedValidator validator = new InLongMsgMixedValidator();
        validator.validate(descriptorProperties);

        String charset =
                descriptorProperties
                        .getOptionalString(FORMAT_CHARSET)
                        .orElse(DEFAULT_CHARSET);
        Character delimiter =
                descriptorProperties
                        .getOptionalCharacter(FORMAT_DELIMITER)
                        .orElse(DEFAULT_DELIMITER);
        Character escapeCharacter =
                descriptorProperties
                        .getOptionalCharacter(FORMAT_ESCAPE_CHARACTER)
                        .orElse(null);
        Character quoteCharacter =
                descriptorProperties
                        .getOptionalCharacter(FORMAT_QUOTE_CHARACTER)
                        .orElse(null);
        Boolean deleteHeadDelimiter =
                descriptorProperties
                        .getOptionalBoolean(FORMAT_DELETE_HEAD_DELIMITER)
                        .orElse(InLongMsgCsvUtils.DEFAULT_DELETE_HEAD_DELIMITER);
        boolean ignoreErrors =
                descriptorProperties
                        .getOptionalBoolean(FORMAT_IGNORE_ERRORS)
                        .orElse(DEFAULT_IGNORE_ERRORS);

        return new InLongMsgCsvMixedFormatDeserializer(
                charset,
                delimiter,
                escapeCharacter,
                quoteCharacter,
                deleteHeadDelimiter,
                ignoreErrors
        );
    }

    @Override
    public InLongMsgCsvMixedFormatConverter createMixedFormatConverter(
            Map<String, String> properties
    ) {
        final DescriptorProperties descriptorProperties =
                new DescriptorProperties(true);
        descriptorProperties.putProperties(properties);

        RowFormatInfo rowFormatInfo = getDataFormatInfo(descriptorProperties);

        String timeFieldName =
                descriptorProperties
                        .getOptionalString(FORMAT_TIME_FIELD_NAME)
                        .orElse(InLongMsgUtils.DEFAULT_TIME_FIELD_NAME);
        String attributesFieldName =
                descriptorProperties
                        .getOptionalString(FORMAT_ATTRIBUTES_FIELD_NAME)
                        .orElse(InLongMsgUtils.DEFAULT_ATTRIBUTES_FIELD_NAME);

        validateFieldNames(timeFieldName, attributesFieldName, rowFormatInfo);

        String nullLiteral =
                descriptorProperties
                        .getOptionalString(FORMAT_NULL_LITERAL)
                        .orElse(null);
        boolean ignoreErrors =
                descriptorProperties
                        .getOptionalBoolean(FORMAT_IGNORE_ERRORS)
                        .orElse(DEFAULT_IGNORE_ERRORS);

        return new InLongMsgCsvMixedFormatConverter(
                rowFormatInfo,
                timeFieldName,
                attributesFieldName,
                nullLiteral,
                ignoreErrors
        );
    }
}
