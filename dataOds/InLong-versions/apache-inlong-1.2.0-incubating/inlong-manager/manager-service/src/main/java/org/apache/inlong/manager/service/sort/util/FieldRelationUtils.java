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

package org.apache.inlong.manager.service.sort.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.common.enums.TransformType;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.inlong.manager.common.pojo.transform.TransformDefinition;
import org.apache.inlong.manager.common.pojo.transform.TransformResponse;
import org.apache.inlong.manager.common.pojo.transform.replacer.StringReplacerDefinition;
import org.apache.inlong.manager.common.pojo.transform.replacer.StringReplacerDefinition.ReplaceMode;
import org.apache.inlong.manager.common.pojo.transform.replacer.StringReplacerDefinition.ReplaceRule;
import org.apache.inlong.manager.common.pojo.transform.splitter.SplitterDefinition;
import org.apache.inlong.manager.common.pojo.transform.splitter.SplitterDefinition.SplitRule;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.common.util.StreamParseUtils;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.transformation.CascadeFunction;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;
import org.apache.inlong.sort.protocol.transformation.function.CascadeFunctionWrapper;
import org.apache.inlong.sort.protocol.transformation.function.RegexpReplaceFirstFunction;
import org.apache.inlong.sort.protocol.transformation.function.RegexpReplaceFunction;
import org.apache.inlong.sort.protocol.transformation.function.SplitIndexFunction;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Util for creat field relation.
 */
public class FieldRelationUtils {

    /**
     * Create relation of fields.
     */
    public static List<FieldRelation> createFieldRelations(TransformResponse transformResponse) {
        TransformType transformType = TransformType.forType(transformResponse.getTransformType());
        TransformDefinition transformDefinition = StreamParseUtils.parseTransformDefinition(
                transformResponse.getTransformDefinition(), transformType);
        List<StreamField> fieldList = transformResponse.getFieldList();
        String transformName = transformResponse.getTransformName();
        String preNodes = transformResponse.getPreNodeNames();
        switch (transformType) {
            case SPLITTER:
                SplitterDefinition splitterDefinition = (SplitterDefinition) transformDefinition;
                return createSplitterFieldRelations(fieldList, transformName, splitterDefinition, preNodes);
            case STRING_REPLACER:
                StringReplacerDefinition replacerDefinition = (StringReplacerDefinition) transformDefinition;
                return createReplacerFieldRelations(fieldList, transformName, replacerDefinition, preNodes);
            case DE_DUPLICATION:
            case FILTER:
                return createFieldRelations(fieldList, transformName);
            case JOINER:
                return createJoinerFieldRelations(fieldList, transformName);
            default:
                throw new UnsupportedOperationException(
                        String.format("Unsupported transformType=%s", transformType));
        }
    }

    /**
     * Create relation of fields.
     */
    private static List<FieldRelation> createFieldRelations(List<StreamField> fieldList, String transformName) {
        return fieldList.stream()
                .map(FieldInfoUtils::parseStreamField)
                .map(fieldInfo -> {
                    FieldInfo inputField = new FieldInfo(fieldInfo.getName(), fieldInfo.getNodeId(),
                            fieldInfo.getFormatInfo());
                    FieldInfo outputField = new FieldInfo(fieldInfo.getName(), transformName,
                            fieldInfo.getFormatInfo());
                    return new FieldRelation(inputField, outputField);
                }).collect(Collectors.toList());
    }

    /**
     * Create relation of fields in join function.
     */
    private static List<FieldRelation> createJoinerFieldRelations(List<StreamField> fieldList, String transformName) {
        return fieldList.stream()
                .map(streamField -> {
                    FormatInfo formatInfo = FieldInfoUtils.convertFieldFormat(
                            streamField.getFieldType(), streamField.getFieldFormat());
                    FieldInfo inputField = new FieldInfo(streamField.getOriginFieldName(),
                            streamField.getOriginNodeName(), formatInfo);
                    FieldInfo outputField = new FieldInfo(streamField.getFieldName(),
                            transformName, formatInfo);
                    return new FieldRelation(inputField, outputField);
                }).collect(Collectors.toList());
    }

    /**
     * Create relation of fields in split function.
     */
    private static List<FieldRelation> createSplitterFieldRelations(
            List<StreamField> fieldList, String transformName,
            SplitterDefinition splitterDefinition, String preNodes) {
        Preconditions.checkNotEmpty(preNodes, "PreNodes of splitter should not be null");
        String preNode = preNodes.split(",")[0];
        List<SplitRule> splitRules = splitterDefinition.getSplitRules();
        Set<String> splitFields = Sets.newHashSet();
        List<FieldRelation> fieldRelations = splitRules.stream()
                .map(splitRule -> parseSplitRule(splitRule, splitFields, transformName, preNode))
                .reduce(Lists.newArrayList(), (list1, list2) -> {
                    list1.addAll(list2);
                    return list1;
                });
        List<StreamField> filteredFieldList = fieldList.stream()
                .filter(streamFieldInfo -> !splitFields.contains(streamFieldInfo.getFieldName()))
                .collect(Collectors.toList());
        fieldRelations.addAll(createFieldRelations(filteredFieldList, transformName));
        return fieldRelations;
    }

    /**
     * Create relation of fields in replace function.
     */
    private static List<FieldRelation> createReplacerFieldRelations(List<StreamField> fieldList, String transformName,
            StringReplacerDefinition replacerDefinition, String preNodes) {
        Preconditions.checkNotEmpty(preNodes, "PreNodes of splitter should not be null");
        String preNode = preNodes.split(",")[0];
        List<ReplaceRule> replaceRules = replacerDefinition.getReplaceRules();
        Set<String> replaceFields = Sets.newHashSet();
        List<FieldRelation> fieldRelations = replaceRules.stream()
                .map(replaceRule -> parseReplaceRule(replaceRule, replaceFields, transformName, preNode))
                .collect(Collectors.toList());
        fieldRelations = cascadeFunctionRelations(fieldRelations);
        List<StreamField> filteredFieldList = fieldList.stream()
                .filter(streamFieldInfo -> !replaceFields.contains(streamFieldInfo.getFieldName()))
                .collect(Collectors.toList());
        fieldRelations.addAll(createFieldRelations(filteredFieldList, transformName));
        return fieldRelations;
    }

    /**
     * Create relation of fields in cascade function.
     */
    private static List<FieldRelation> cascadeFunctionRelations(List<FieldRelation> fieldRelations) {
        Map<String, List<CascadeFunction>> cascadeFunctions = Maps.newHashMap();
        Map<String, FieldInfo> targetFields = Maps.newHashMap();
        for (FieldRelation fieldRelation : fieldRelations) {
            CascadeFunction cascadeFunction = (CascadeFunction) fieldRelation.getInputField();
            String targetField = fieldRelation.getOutputField().getName();
            cascadeFunctions.computeIfAbsent(targetField, k -> Lists.newArrayList()).add(cascadeFunction);
            targetFields.put(targetField, fieldRelation.getOutputField());
        }
        List<FieldRelation> cascadeRelations = Lists.newArrayList();
        for (Map.Entry<String, List<CascadeFunction>> entry : cascadeFunctions.entrySet()) {
            String targetField = entry.getKey();
            CascadeFunctionWrapper functionWrapper = new CascadeFunctionWrapper(entry.getValue());
            FieldInfo targetFieldInfo = targetFields.get(targetField);
            cascadeRelations.add(new FieldRelation(functionWrapper, targetFieldInfo));
        }
        return cascadeRelations;
    }

    /**
     * Parse rule of replacer.
     */
    private static FieldRelation parseReplaceRule(ReplaceRule replaceRule, Set<String> replaceFields,
            String transformName, String preNode) {
        StreamField sourceField = replaceRule.getSourceField();
        final String fieldName = sourceField.getFieldName();
        String regex = replaceRule.getRegex();
        String targetValue = replaceRule.getTargetValue();
        ReplaceMode replaceMode = replaceRule.getMode();
        FieldInfo fieldInfo = FieldInfoUtils.parseStreamField(sourceField);
        fieldInfo.setNodeId(preNode);
        FieldInfo targetFieldInfo = new FieldInfo(fieldName, transformName,
                FieldInfoUtils.convertFieldFormat(FieldType.STRING.name()));
        replaceFields.add(fieldName);
        if (replaceMode == ReplaceMode.RELACE_ALL) {
            RegexpReplaceFunction regexpReplaceFunction = new RegexpReplaceFunction(fieldInfo,
                    new StringConstantParam(regex), new StringConstantParam(targetValue));
            return new FieldRelation(regexpReplaceFunction, targetFieldInfo);
        } else {
            RegexpReplaceFirstFunction regexpReplaceFirstFunction = new RegexpReplaceFirstFunction(fieldInfo,
                    new StringConstantParam(regex), new StringConstantParam(targetValue));
            return new FieldRelation(regexpReplaceFirstFunction, targetFieldInfo);
        }
    }

    /**
     * Parse rule of split.
     */
    private static List<FieldRelation> parseSplitRule(SplitRule splitRule, Set<String> splitFields,
            String transformName, String preNode) {
        StreamField sourceField = splitRule.getSourceField();
        FieldInfo fieldInfo = FieldInfoUtils.parseStreamField(sourceField);
        fieldInfo.setNodeId(preNode);
        String separator = splitRule.getSeparator();
        List<String> targetSources = splitRule.getTargetFields();
        List<FieldRelation> splitRelations = Lists.newArrayList();
        for (int index = 0; index < targetSources.size(); index++) {
            SplitIndexFunction splitIndexFunction = new SplitIndexFunction(
                    fieldInfo, new StringConstantParam(separator), new ConstantParam(index));
            FieldInfo targetFieldInfo = new FieldInfo(
                    targetSources.get(index), transformName, FieldInfoUtils.convertFieldFormat(FieldType.STRING.name())
            );
            splitFields.add(targetSources.get(index));
            splitRelations.add(new FieldRelation(splitIndexFunction, targetFieldInfo));
        }
        return splitRelations;
    }
}
