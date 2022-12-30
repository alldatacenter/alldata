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

package org.apache.inlong.manager.pojo.sort.util;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.TransformType;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.transform.TransformDefinition;
import org.apache.inlong.manager.pojo.transform.TransformResponse;
import org.apache.inlong.manager.pojo.transform.deduplication.DeDuplicationDefinition;
import org.apache.inlong.manager.pojo.transform.deduplication.DeDuplicationDefinition.DeDuplicationStrategy;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.transform.DistinctNode;
import org.apache.inlong.sort.protocol.node.transform.TransformNode;
import org.apache.inlong.sort.protocol.transformation.OrderDirection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parse TransformResponse to TransformNode which sort needed
 */
@Slf4j
public class TransformNodeUtils {

    public static List<TransformNode> createTransformNodes(List<TransformResponse> transformResponses,
            Map<String, StreamField> constantFieldMap) {
        if (CollectionUtils.isEmpty(transformResponses)) {
            return Lists.newArrayList();
        }
        return transformResponses.stream()
                .map(s -> TransformNodeUtils.createTransformNode(s, constantFieldMap)).collect(Collectors.toList());
    }

    public static TransformNode createTransformNode(TransformResponse transformResponse,
            Map<String, StreamField> constantFieldMap) {
        TransformType transformType = TransformType.forType(transformResponse.getTransformType());
        if (transformType == TransformType.DE_DUPLICATION) {
            TransformDefinition transformDefinition = StreamParseUtils.parseTransformDefinition(
                    transformResponse.getTransformDefinition(), transformType);
            return createDistinctNode((DeDuplicationDefinition) transformDefinition,
                    transformResponse, constantFieldMap);
        } else {
            return createNormalTransformNode(transformResponse, constantFieldMap);
        }
    }

    /**
     * Create distinct node based on deDuplicationDefinition
     */
    public static DistinctNode createDistinctNode(DeDuplicationDefinition deDuplicationDefinition,
            TransformResponse transformResponse, Map<String, StreamField> constantFieldMap) {
        List<StreamField> streamFields = deDuplicationDefinition.getDupFields();
        List<FieldInfo> distinctFields = streamFields.stream()
                .map(FieldInfoUtils::parseStreamField)
                .collect(Collectors.toList());
        StreamField timingField = deDuplicationDefinition.getTimingField();
        FieldInfo orderField = FieldInfoUtils.parseStreamField(timingField);
        DeDuplicationStrategy deDuplicationStrategy = deDuplicationDefinition.getDeDuplicationStrategy();
        OrderDirection orderDirection;
        switch (deDuplicationStrategy) {
            case RESERVE_LAST:
                orderDirection = OrderDirection.DESC;
                break;
            case RESERVE_FIRST:
                orderDirection = OrderDirection.ASC;
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Unsupported deduplication strategy=%s for inlong", deDuplicationStrategy));
        }
        TransformNode transformNode = createNormalTransformNode(transformResponse, constantFieldMap);
        return new DistinctNode(transformNode.getId(),
                transformNode.getName(),
                transformNode.getFields(),
                transformNode.getFieldRelations(),
                transformNode.getFilters(),
                transformNode.getFilterStrategy(),
                distinctFields,
                orderField,
                orderDirection);

    }

    /**
     * Create transform node based on transformResponse
     */
    public static TransformNode createNormalTransformNode(TransformResponse transformResponse,
            Map<String, StreamField> constantFieldMap) {
        TransformNode transformNode = new TransformNode();
        transformNode.setId(transformResponse.getTransformName());
        transformNode.setName(transformResponse.getTransformName());
        // Filter constant fields
        List<FieldInfo> fieldInfos = transformResponse.getFieldList().stream()
                .filter(s -> s.getFieldValue() == null)
                .map(FieldInfoUtils::parseStreamField).collect(Collectors.toList());
        transformNode.setFields(fieldInfos);
        transformNode.setFieldRelations(FieldRelationUtils.createFieldRelations(transformResponse, constantFieldMap));
        transformNode.setFilters(
                FilterFunctionUtils.createFilterFunctions(transformResponse));
        transformNode.setFilterStrategy(FilterFunctionUtils.parseFilterStrategy(transformResponse));
        return transformNode;
    }
}
