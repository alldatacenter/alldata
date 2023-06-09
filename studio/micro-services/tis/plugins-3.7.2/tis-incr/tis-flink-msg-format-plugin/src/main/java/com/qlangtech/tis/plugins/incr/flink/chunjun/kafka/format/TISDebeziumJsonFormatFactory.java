/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.incr.flink.chunjun.kafka.format;

import com.qlangtech.plugins.incr.flink.launch.FlinkDescriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.formats.json.JsonOptions;
import org.apache.flink.formats.json.debezium.DebeziumJsonFormatFactory;
import org.apache.flink.formats.json.debezium.DebeziumJsonOptions;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.data.RowData;

/**
 * https://nightlies.apache.org/flink/flink-docs-release-1.16/docs/connectors/table/formats/debezium/
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-15 12:25
 **/
public class TISDebeziumJsonFormatFactory extends FormatFactory {
//    @FormField(ordinal = 0, type = FormFieldType.ENUM, advance = true)
//    public Boolean schemaInclude;

    @FormField(ordinal = 1, type = FormFieldType.ENUM, advance = true)
    public Boolean ignoreParseErrors;
    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, advance = true)
    public String timestampFormat;

    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, advance = true)
    public String nullKeyMode;
    @FormField(ordinal = 5, type = FormFieldType.INPUTTEXT, advance = true)
    public String nullKeyLiteral;
    @FormField(ordinal = 6, type = FormFieldType.ENUM, advance = true)
    public Boolean encodeDecimal;

    @Override
    public EncodingFormat<SerializationSchema<RowData>> createEncodingFormat(String targetTableName) {
        DebeziumJsonFormatFactory djsonFormatFactory = new DebeziumJsonFormatFactory();
        // ReadableConfig cfg = new Configuration();
        DftDescriptor desc = (DftDescriptor) this.getDescriptor();
        Configuration cfg = desc.options.createFlinkCfg(this);

        return djsonFormatFactory
                .createEncodingFormat(null, cfg.set(JsonOptions.TARGET_TABLE_NAME, targetTableName));
    }

    @TISExtension
    public static final class DftDescriptor extends FlinkDescriptor<FormatFactory> {
        Options options;

        public DftDescriptor() {
            super();
            this.options = this.createFlinkOptions();
//            // schemaInclude 不支持
//            options.add("schemaInclude", DebeziumJsonOptions.SCHEMA_INCLUDE);
            options.add("ignoreParseErrors", DebeziumJsonOptions.IGNORE_PARSE_ERRORS);
            options.add("timestampFormat", DebeziumJsonOptions.TIMESTAMP_FORMAT);
            options.add("nullKeyMode", DebeziumJsonOptions.JSON_MAP_NULL_KEY_MODE);
            options.add("nullKeyLiteral", DebeziumJsonOptions.JSON_MAP_NULL_KEY_LITERAL);
            options.add("encodeDecimal", JsonOptions.ENCODE_DECIMAL_AS_PLAIN_NUMBER);
        }

        @Override
        public String getDisplayName() {
            return org.apache.flink.formats.json.debezium.DebeziumJsonFormatFactory.IDENTIFIER;
        }
    }
}
