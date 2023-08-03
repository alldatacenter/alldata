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
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.common.fieldtype.strategy.FieldTypeMappingStrategy;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sort.util.FieldInfoUtils;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.formats.common.StringTypeInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.DebeziumJsonFormat;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;
import org.apache.inlong.sort.protocol.transformation.function.CustomFunction;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Interface of the load node provider
 */
public interface LoadNodeProvider extends NodeProvider {

    /**
     * Create load node by stream node info
     *
     * @param nodeInfo stream node info
     * @param constantFieldMap the constant field map
     * @return the load node
     */
    LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap);

    /**
     * Parse FieldInfos
     *
     * @param sinkFields The stream sink fields
     * @param nodeId The node id
     * @return FieldInfo list
     */
    default List<FieldInfo> parseSinkFieldInfos(List<SinkField> sinkFields, String nodeId) {
        return parseSinkFieldInfos(sinkFields, nodeId, null);
    }

    /**
     * Parse FieldInfos
     *
     * @param sinkFields The stream sink fields
     * @param nodeId The node id
     * @param fieldTypeMappingStrategy The field type mapping operation strategy
     * @return FieldInfo list
     */
    default List<FieldInfo> parseSinkFieldInfos(List<SinkField> sinkFields, String nodeId,
            FieldTypeMappingStrategy fieldTypeMappingStrategy) {
        return sinkFields.stream()
                .map(field -> FieldInfoUtils.parseSinkFieldInfo(field, nodeId, fieldTypeMappingStrategy))
                .collect(Collectors.toList());
    }

    /**
     * Parse information field of data sink.
     */
    default List<FieldRelation> parseSinkFields(List<SinkField> fieldList,
            Map<String, StreamField> constantFieldMap) {
        if (CollectionUtils.isEmpty(fieldList)) {
            return Lists.newArrayList();
        }
        return fieldList.stream()
                .filter(sinkField -> StringUtils.isNotEmpty(sinkField.getSourceFieldName()))
                .map(field -> {
                    FieldInfo outputField = new FieldInfo(field.getFieldName(),
                            FieldInfoUtils.convertFieldFormat(field.getFieldType(), field.getFieldFormat()));
                    FunctionParam inputField;
                    String fieldKey = String.format("%s-%s", field.getOriginNodeName(), field.getSourceFieldName());
                    StreamField constantField = constantFieldMap.get(fieldKey);
                    if (constantField != null) {
                        if (outputField.getFormatInfo() != null
                                && outputField.getFormatInfo().getTypeInfo() == StringTypeInfo.INSTANCE) {
                            inputField = new StringConstantParam(constantField.getFieldValue());
                        } else {
                            inputField = new ConstantParam(constantField.getFieldValue());
                        }
                    } else if (FieldType.FUNCTION.name().equalsIgnoreCase(field.getSourceFieldType())) {
                        inputField = new CustomFunction(field.getSourceFieldName());
                    } else {
                        inputField = new FieldInfo(field.getSourceFieldName(), field.getOriginNodeName(),
                                FieldInfoUtils.convertFieldFormat(field.getSourceFieldType()));
                    }
                    return new FieldRelation(inputField, outputField);
                }).collect(Collectors.toList());
    }

    /**
     * Parse format
     *
     * @param multipleEnable whether to enable multi-write
     * @param multipleFormat data serialization format
     * @return the format for serialized content
     */
    default Format parsingSinkMultipleFormat(Boolean multipleEnable, String multipleFormat) {
        Format format = null;
        if (Boolean.TRUE.equals(multipleEnable) && StringUtils.isNotBlank(multipleFormat)) {
            DataTypeEnum dataType = DataTypeEnum.forType(multipleFormat);
            switch (dataType) {
                case CANAL:
                    format = new CanalJsonFormat();
                    break;
                case DEBEZIUM_JSON:
                    format = new DebeziumJsonFormat();
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Unsupported dataType=%s", dataType));
            }
        }
        return format;
    }
}
