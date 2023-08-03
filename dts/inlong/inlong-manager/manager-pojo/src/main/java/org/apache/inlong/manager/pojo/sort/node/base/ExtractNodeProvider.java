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

package org.apache.inlong.manager.pojo.sort.node.base;

import org.apache.inlong.common.enums.DataTypeEnum;
import org.apache.inlong.manager.common.fieldtype.strategy.FieldTypeMappingStrategy;
import org.apache.inlong.manager.pojo.sort.util.FieldInfoUtils;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.node.format.AvroFormat;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.format.DebeziumJsonFormat;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.node.format.InLongMsgFormat;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.format.RawFormat;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Interface of the extract node provider
 */
public interface ExtractNodeProvider extends NodeProvider {

    /**
     * Create extract node by stream node info
     *
     * @param nodeInfo stream node info
     * @return the extract node
     */
    ExtractNode createExtractNode(StreamNode nodeInfo);

    /**
     * Parse StreamFieldInfos
     *
     * @param streamFields The stream fields
     * @param nodeId The node id
     * @return FieldInfo list
     */
    default List<FieldInfo> parseStreamFieldInfos(List<StreamField> streamFields, String nodeId) {
        // Filter constant fields
        return parseStreamFieldInfos(streamFields, nodeId, null);
    }

    /**
     * Parse StreamFieldInfos
     *
     * @param streamFields The stream fields
     * @param nodeId The node id
     * @param fieldTypeMappingStrategy The field type mapping operation strategy
     * @return FieldInfo list
     */
    default List<FieldInfo> parseStreamFieldInfos(List<StreamField> streamFields, String nodeId,
            FieldTypeMappingStrategy fieldTypeMappingStrategy) {
        // Filter constant fields
        return streamFields.stream().filter(s -> Objects.isNull(s.getFieldValue()))
                .map(streamFieldInfo -> FieldInfoUtils
                        .parseStreamFieldInfo(streamFieldInfo, nodeId, fieldTypeMappingStrategy))
                .collect(Collectors.toList());
    }

    /**
     * Parse format
     *
     * @param serializationType data serialization, support: csv, json, canal, avro, etc
     * @param wrapWithInlongMsg whether wrap content with {@link InLongMsgFormat}
     * @param separatorStr the separator of data content
     * @param ignoreParseErrors whether ignore deserialization error data
     * @return the format for serialized content
     */
    default Format parsingFormat(
            String serializationType,
            boolean wrapWithInlongMsg,
            String separatorStr,
            boolean ignoreParseErrors) {
        Format format;
        DataTypeEnum dataType = DataTypeEnum.forType(serializationType);
        switch (dataType) {
            case CSV:
                if (StringUtils.isNumeric(separatorStr)) {
                    char dataSeparator = (char) Integer.parseInt(separatorStr);
                    separatorStr = Character.toString(dataSeparator);
                }
                CsvFormat csvFormat = new CsvFormat(separatorStr);
                csvFormat.setIgnoreParseErrors(ignoreParseErrors);
                format = csvFormat;
                break;
            case AVRO:
                format = new AvroFormat();
                break;
            case JSON:
                JsonFormat jsonFormat = new JsonFormat();
                jsonFormat.setIgnoreParseErrors(ignoreParseErrors);
                format = jsonFormat;
                break;
            case CANAL:
                format = new CanalJsonFormat();
                break;
            case DEBEZIUM_JSON:
                DebeziumJsonFormat debeziumJsonFormat = new DebeziumJsonFormat();
                debeziumJsonFormat.setIgnoreParseErrors(ignoreParseErrors);
                format = debeziumJsonFormat;
                break;
            case RAW:
                format = new RawFormat();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported dataType=%s", dataType));
        }
        if (wrapWithInlongMsg) {
            Format innerFormat = format;
            format = new InLongMsgFormat(innerFormat, false);
        }
        return format;
    }
}
