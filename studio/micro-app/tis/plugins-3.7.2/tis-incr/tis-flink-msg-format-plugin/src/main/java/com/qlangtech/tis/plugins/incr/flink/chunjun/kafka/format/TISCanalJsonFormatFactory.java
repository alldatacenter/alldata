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
import org.apache.flink.formats.json.JsonOptions;
import org.apache.flink.formats.json.canal.CanalJsonFormatFactory;
import org.apache.flink.formats.json.canal.CanalJsonOptions;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.data.RowData;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-04-15 12:42
 **/
public class TISCanalJsonFormatFactory extends FormatFactory {

    @FormField(ordinal = 0, type = FormFieldType.ENUM, advance = true)
    public Boolean ignoreParseErrors;
    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, advance = true)
    public String timestampFormat;
    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, advance = true)
    public String dbInclude;
    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, advance = true)
    public String tableInclude;
    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, advance = true)
    public String nullKeyMode;
    @FormField(ordinal = 5, type = FormFieldType.INPUTTEXT, advance = true)
    public String nullKeyLiteral;
    @FormField(ordinal = 6, type = FormFieldType.ENUM, advance = true)
    public Boolean encodeDecimal;

    @Override
    public EncodingFormat<SerializationSchema<RowData>> createEncodingFormat(final String targetTabName) {
        CanalJsonFormatFactory canalFormatFactory = new CanalJsonFormatFactory();
        DftDescriptor desc = (DftDescriptor) this.getDescriptor();
        return canalFormatFactory.createEncodingFormat(null
                , desc.options.createFlinkCfg(this).set(JsonOptions.TARGET_TABLE_NAME, targetTabName));
    }

//    public static class DefaultDescriptor extends FlinkDescriptor<CompactionConfig> {
//        public DefaultDescriptor() {
//            addFieldDescriptor("payloadClass", FlinkOptions.PAYLOAD_CLASS_NAME);
//            addFieldDescriptor("targetIOPerInMB", FlinkOptions.COMPACTION_TARGET_IO);
//            addFieldDescriptor("triggerStrategy", FlinkOptions.COMPACTION_TRIGGER_STRATEGY);
//            addFieldDescriptor("maxNumDeltaCommitsBefore", FlinkOptions.COMPACTION_DELTA_COMMITS);
//            addFieldDescriptor("maxDeltaSecondsBefore", FlinkOptions.COMPACTION_DELTA_SECONDS);
//            addFieldDescriptor("asyncClean", FlinkOptions.CLEAN_ASYNC_ENABLED);
//            addFieldDescriptor("retainCommits", FlinkOptions.CLEAN_RETAIN_COMMITS);
//            addFieldDescriptor(KEY_archiveMinCommits, FlinkOptions.ARCHIVE_MIN_COMMITS);
//            addFieldDescriptor(KEY_archiveMaxCommits, FlinkOptions.ARCHIVE_MAX_COMMITS);
//        }


    @TISExtension
    public static final class DftDescriptor extends FlinkDescriptor<FormatFactory> {
        public Options options;

        public DftDescriptor() {
            //  super();
            this.options = createFlinkOptions();
            options.add("ignoreParseErrors", CanalJsonOptions.IGNORE_PARSE_ERRORS);
            options.add("timestampFormat", CanalJsonOptions.TIMESTAMP_FORMAT);
            options.add("dbInclude", CanalJsonOptions.DATABASE_INCLUDE);
            options.add("tableInclude", CanalJsonOptions.TABLE_INCLUDE);
            options.add("nullKeyMode", CanalJsonOptions.JSON_MAP_NULL_KEY_MODE);
            options.add("nullKeyLiteral", CanalJsonOptions.JSON_MAP_NULL_KEY_LITERAL);
            options.add("encodeDecimal", JsonOptions.ENCODE_DECIMAL_AS_PLAIN_NUMBER);
        }


        @Override
        public String getDisplayName() {
            return CanalJsonFormatFactory.IDENTIFIER;
        }
    }

    public static void main(String[] args) {

    }

}



