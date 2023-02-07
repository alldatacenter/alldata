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

package org.apache.inlong.sort.formats.inlongmsgpb;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.DelegatingConfiguration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.factories.DeserializationFormatFactory;
import org.apache.flink.table.factories.DynamicTableFactory.Context;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.factories.SerializationFormatFactory;

import java.util.HashSet;
import java.util.Set;

import static org.apache.inlong.sort.formats.inlongmsgpb.InLongMsgPbOptions.DECOMPRESS_TYPE;
import static org.apache.inlong.sort.formats.inlongmsgpb.InLongMsgPbOptions.IGNORE_PARSE_ERRORS;
import static org.apache.inlong.sort.formats.inlongmsgpb.InLongMsgPbOptions.INNER_FORMAT;
import static org.apache.inlong.sort.formats.inlongmsgpb.InLongMsgPbOptions.IGNORE_TRAILING_UNMAPPABLE;
import static org.apache.inlong.sort.formats.inlongmsgpb.InLongMsgPbOptions.validateDecodingFormatOptions;

/**
 * factory class for inLong msg pb format
 */
public final class InLongMsgPbFormatFactory
        implements
            DeserializationFormatFactory,
            SerializationFormatFactory {

    public static final String IDENTIFIER = "inlong-msg-pb";

    public static final String INLONG_PREFIX = "inlong-msg-pb.";

    @Override
    public DecodingFormat<DeserializationSchema<RowData>> createDecodingFormat(Context context,
            ReadableConfig formatOptions) {
        validateDecodingFormatOptions(formatOptions);

        final DeserializationFormatFactory innerFactory = FactoryUtil.discoverFactory(
                context.getClassLoader(),
                DeserializationFormatFactory.class,
                formatOptions.get(INNER_FORMAT));
        Configuration allOptions = Configuration.fromMap(context.getCatalogTable().getOptions());
        String innerFormatMetaPrefix = formatOptions.get(INNER_FORMAT) + ".";
        String innerFormatPrefix = INLONG_PREFIX + innerFormatMetaPrefix;
        DecodingFormat<DeserializationSchema<RowData>> innerFormat =
                innerFactory.createDecodingFormat(context, new DelegatingConfiguration(allOptions, innerFormatPrefix));
        boolean ignoreErrors = formatOptions.get(IGNORE_PARSE_ERRORS);
        boolean ignoreTrailingUnmappable = formatOptions.get(IGNORE_TRAILING_UNMAPPABLE);
        String decompressType = formatOptions.get(DECOMPRESS_TYPE);
        return new InLongMsgPbDecodingFormat(innerFormat, innerFormatMetaPrefix,
                ignoreErrors, ignoreTrailingUnmappable, decompressType);
    }

    @Override
    public EncodingFormat<SerializationSchema<RowData>> createEncodingFormat(Context context,
            ReadableConfig formatOptions) {
        throw new RuntimeException("Do not support inlong pb format serialize.");
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(INNER_FORMAT);
        options.add(DECOMPRESS_TYPE);
        return options;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(IGNORE_PARSE_ERRORS);
        options.add(IGNORE_TRAILING_UNMAPPABLE);
        return options;
    }
}
