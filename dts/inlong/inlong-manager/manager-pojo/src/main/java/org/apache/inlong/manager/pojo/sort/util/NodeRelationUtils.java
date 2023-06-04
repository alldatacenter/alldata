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
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.manager.common.enums.TransformType;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.manager.pojo.stream.StreamPipeline;
import org.apache.inlong.manager.pojo.stream.StreamTransform;
import org.apache.inlong.manager.pojo.transform.TransformDefinition;
import org.apache.inlong.manager.pojo.transform.TransformResponse;
import org.apache.inlong.manager.pojo.transform.joiner.IntervalJoinerDefinition;
import org.apache.inlong.manager.pojo.transform.joiner.JoinerDefinition;
import org.apache.inlong.manager.pojo.transform.joiner.JoinerDefinition.JoinMode;
import org.apache.inlong.manager.pojo.transform.joiner.LookUpJoinerDefinition;
import org.apache.inlong.manager.pojo.transform.joiner.TemporalJoinerDefinition;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.transform.TransformNode;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.LogicOperator;
import org.apache.inlong.sort.protocol.transformation.function.SingleValueFilterFunction;
import org.apache.inlong.sort.protocol.transformation.operator.AndOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EmptyOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EqualOperator;
import org.apache.inlong.sort.protocol.transformation.relation.InnerJoinNodeRelation;
import org.apache.inlong.sort.protocol.transformation.relation.IntervalJoinRelation;
import org.apache.inlong.sort.protocol.transformation.relation.LeftOuterJoinNodeRelation;
import org.apache.inlong.sort.protocol.transformation.relation.LeftOuterTemporalJoinRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.apache.inlong.sort.protocol.transformation.relation.RightOuterJoinNodeRelation;
import org.apache.inlong.sort.protocol.transformation.relation.UnionNodeRelation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Util for create node relation.
 */
@Slf4j
public class NodeRelationUtils {

    private static final Set<TransformType> JOIN_NODES = new HashSet<>();

    static {

        JOIN_NODES.add(TransformType.JOINER);
        JOIN_NODES.add(TransformType.LOOKUP_JOINER);
        JOIN_NODES.add(TransformType.INTERVAL_JOINER);
        JOIN_NODES.add(TransformType.TEMPORAL_JOINER);

    }

    /**
     * Create node relation for the given stream
     */
    public static List<NodeRelation> createNodeRelations(InlongStreamInfo streamInfo) {
        if (StringUtils.isEmpty(streamInfo.getExtParams())) {
            log.warn("stream node relation is empty for {}", streamInfo);
            return Lists.newArrayList();
        }
        StreamPipeline pipeline = StreamParseUtils.parseStreamPipeline(streamInfo.getExtParams(),
                streamInfo.getInlongStreamId());
        return pipeline.getPipeline().stream()
                .map(nodeRelation -> {
                    if (nodeRelation.getInputNodes().size() > 1) {
                        return new UnionNodeRelation(
                                Lists.newArrayList(nodeRelation.getInputNodes()),
                                Lists.newArrayList(nodeRelation.getOutputNodes()));
                    } else {
                        return new NodeRelation(
                                Lists.newArrayList(nodeRelation.getInputNodes()),
                                Lists.newArrayList(nodeRelation.getOutputNodes()));
                    }
                }).collect(Collectors.toList());
    }

    /**
     * Create node relation from the given sources and sinks
     */
    public static List<NodeRelation> createNodeRelations(List<StreamSource> sources, List<StreamSink> sinks) {
        NodeRelation relation = new NodeRelation();
        List<String> inputs = sources.stream().map(StreamSource::getSourceName).collect(Collectors.toList());
        List<String> outputs = sinks.stream().map(StreamSink::getSinkName).collect(Collectors.toList());
        relation.setInputs(inputs);
        relation.setOutputs(outputs);
        return Lists.newArrayList(relation);
    }

    /**
     * Optimize relation of node, JoinerRelation must be rebuilt.
     */
    public static void optimizeNodeRelation(StreamInfo streamInfo, List<TransformResponse> transformResponses) {
        if (CollectionUtils.isEmpty(transformResponses)) {
            return;
        }
        Map<String, TransformDefinition> transformTypeMap = transformResponses.stream().collect(
                Collectors.toMap(TransformResponse::getTransformName, transformResponse -> {
                    TransformType transformType = TransformType.forType(transformResponse.getTransformType());
                    return StreamParseUtils.parseTransformDefinition(transformResponse.getTransformDefinition(),
                            transformType);
                }));
        List<Node> nodes = streamInfo.getNodes();
        Map<String, TransformNode> joinNodes = nodes.stream().filter(node -> node instanceof TransformNode)
                .map(node -> (TransformNode) node)
                .filter(transformNode -> {
                    TransformDefinition transformDefinition = transformTypeMap.get(transformNode.getName());
                    return JOIN_NODES.contains(transformDefinition.getTransformType());
                }).collect(Collectors.toMap(TransformNode::getName, transformNode -> transformNode));

        List<NodeRelation> relations = streamInfo.getRelations();
        Iterator<NodeRelation> shipIterator = relations.listIterator();
        List<NodeRelation> joinRelations = Lists.newArrayList();
        while (shipIterator.hasNext()) {
            NodeRelation relation = shipIterator.next();
            List<String> outputs = relation.getOutputs();
            if (outputs.size() == 1) {
                String nodeName = outputs.get(0);
                if (joinNodes.get(nodeName) != null) {
                    TransformDefinition transformDefinition = transformTypeMap.get(nodeName);
                    TransformType transformType = transformDefinition.getTransformType();
                    switch (transformType) {
                        case JOINER:
                            joinRelations.add(getNodeRelation((JoinerDefinition) transformDefinition, relation));
                            shipIterator.remove();
                            break;
                        case LOOKUP_JOINER:
                            assert transformDefinition instanceof LookUpJoinerDefinition;
                            joinRelations.add(getNodeRelation((LookUpJoinerDefinition) transformDefinition, relation));
                            shipIterator.remove();
                            break;
                        case INTERVAL_JOINER:
                            if (transformDefinition instanceof IntervalJoinerDefinition) {
                                joinRelations
                                        .add(getNodeRelation((IntervalJoinerDefinition) transformDefinition, relation));
                            }
                            shipIterator.remove();
                            break;
                        case TEMPORAL_JOINER:
                            assert transformDefinition instanceof TemporalJoinerDefinition;
                            joinRelations
                                    .add(getNodeRelation((TemporalJoinerDefinition) transformDefinition, relation));
                            shipIterator.remove();
                            break;
                        default:
                            throw new IllegalArgumentException(
                                    String.format("Unsupported transformType for %s", transformType));
                    }
                }
            }
        }
        relations.addAll(joinRelations);
    }

    private static NodeRelation getNodeRelation(JoinerDefinition joinerDefinition, NodeRelation nodeRelation) {
        JoinMode joinMode = joinerDefinition.getJoinMode();
        String leftNode = getNodeName(joinerDefinition.getLeftNode());
        String rightNode = getNodeName(joinerDefinition.getRightNode());
        List<String> preNodes = Lists.newArrayList(leftNode, rightNode);
        List<StreamField> leftJoinFields = joinerDefinition.getLeftJoinFields();
        List<StreamField> rightJoinFields = joinerDefinition.getRightJoinFields();
        List<FilterFunction> filterFunctions = Lists.newArrayList();
        for (int index = 0; index < leftJoinFields.size(); index++) {
            StreamField leftField = leftJoinFields.get(index);
            StreamField rightField = rightJoinFields.get(index);
            LogicOperator operator;
            if (index != 0) {
                operator = AndOperator.getInstance();
            } else {
                operator = EmptyOperator.getInstance();
            }
            filterFunctions.add(createFilterFunction(leftField, rightField, operator));
        }
        Map<String, List<FilterFunction>> joinConditions = new HashMap<>();
        joinConditions.put(rightNode, filterFunctions);
        switch (joinMode) {
            case LEFT_JOIN:
                return new LeftOuterJoinNodeRelation(preNodes, nodeRelation.getOutputs(), joinConditions);
            case INNER_JOIN:
                return new InnerJoinNodeRelation(preNodes, nodeRelation.getOutputs(), joinConditions);
            case RIGHT_JOIN:
                return new RightOuterJoinNodeRelation(preNodes, nodeRelation.getOutputs(), joinConditions);
            default:
                throw new IllegalArgumentException(String.format("Unsupported join mode=%s for inlong", joinMode));
        }
    }

    private static NodeRelation getNodeRelation(LookUpJoinerDefinition joinerDefinition, NodeRelation nodeRelation) {
        String leftNode = getNodeName(joinerDefinition.getLeftNode());
        String rightNode = getNodeName(joinerDefinition.getRightNode());
        List<String> preNodes = Lists.newArrayList(leftNode, rightNode);
        List<StreamField> leftJoinFields = joinerDefinition.getLeftJoinFields();
        List<StreamField> rightJoinFields = joinerDefinition.getRightJoinFields();
        List<FilterFunction> filterFunctions = Lists.newArrayList();
        for (int index = 0; index < leftJoinFields.size(); index++) {
            StreamField leftField = leftJoinFields.get(index);
            StreamField rightField = rightJoinFields.get(index);
            LogicOperator operator;
            if (index != 0) {
                operator = AndOperator.getInstance();
            } else {
                operator = EmptyOperator.getInstance();
            }
            filterFunctions.add(createFilterFunction(leftField, rightField, operator));
        }
        Map<String, List<FilterFunction>> joinConditions = new HashMap<>();
        joinConditions.put(rightNode, filterFunctions);
        FieldInfo systemTime = new FieldInfo(MetaField.PROCESS_TIME.name());
        return new LeftOuterTemporalJoinRelation(preNodes, nodeRelation.getOutputs(), joinConditions, systemTime);
    }

    private static NodeRelation getNodeRelation(IntervalJoinerDefinition joinerDefinition, NodeRelation nodeRelation) {
        String leftNode = getNodeName(joinerDefinition.getLeftNode());
        String rightNode = getNodeName(joinerDefinition.getRightNode());
        List<String> preNodes = Lists.newArrayList(leftNode, rightNode);
        List<StreamField> leftJoinFields = joinerDefinition.getLeftJoinFields();
        List<StreamField> rightJoinFields = joinerDefinition.getRightJoinFields();
        List<FilterFunction> filterFunctions = Lists.newArrayList();
        for (int index = 0; index < leftJoinFields.size(); index++) {
            StreamField leftField = leftJoinFields.get(index);
            StreamField rightField = rightJoinFields.get(index);
            LogicOperator operator;
            if (index != 0) {
                operator = AndOperator.getInstance();
            } else {
                operator = EmptyOperator.getInstance();
            }
            filterFunctions.add(createFilterFunction(leftField, rightField, operator));
        }
        LinkedHashMap<String, List<FilterFunction>> joinConditions = new LinkedHashMap<>();
        joinConditions.put(rightNode, filterFunctions);

        return new IntervalJoinRelation(preNodes, nodeRelation.getOutputs(), joinConditions);

    }

    private static NodeRelation getNodeRelation(TemporalJoinerDefinition joinerDefinition, NodeRelation nodeRelation) {
        JoinMode joinMode = joinerDefinition.getJoinMode();
        String leftNode = getNodeName(joinerDefinition.getLeftNode());
        String rightNode = getNodeName(joinerDefinition.getRightNode());
        List<String> preNodes = Lists.newArrayList(leftNode, rightNode);
        List<StreamField> leftJoinFields = joinerDefinition.getLeftJoinFields();
        List<StreamField> rightJoinFields = joinerDefinition.getRightJoinFields();
        List<FilterFunction> filterFunctions = Lists.newArrayList();
        for (int index = 0; index < leftJoinFields.size(); index++) {
            StreamField leftField = leftJoinFields.get(index);
            StreamField rightField = rightJoinFields.get(index);
            LogicOperator operator;
            if (index != 0) {
                operator = AndOperator.getInstance();
            } else {
                operator = EmptyOperator.getInstance();
            }
            filterFunctions.add(createFilterFunction(leftField, rightField, operator));
        }
        Map<String, List<FilterFunction>> joinConditions = new HashMap<>();
        joinConditions.put(rightNode, filterFunctions);
        switch (joinMode) {
            case LEFT_JOIN:
                return new LeftOuterJoinNodeRelation(preNodes, nodeRelation.getOutputs(), joinConditions);
            case INNER_JOIN:
                return new InnerJoinNodeRelation(preNodes, nodeRelation.getOutputs(), joinConditions);
            case RIGHT_JOIN:
                return new RightOuterJoinNodeRelation(preNodes, nodeRelation.getOutputs(), joinConditions);
            default:
                throw new IllegalArgumentException(String.format("Unsupported join mode=%s for inlong", joinMode));
        }
    }

    private static SingleValueFilterFunction createFilterFunction(StreamField leftField, StreamField rightField,
            LogicOperator operator) {
        FieldInfo sourceField = new FieldInfo(leftField.getOriginFieldName(), leftField.getOriginNodeName(),
                FieldInfoUtils.convertFieldFormat(leftField.getFieldType(), leftField.getFieldFormat()));
        FieldInfo targetField = new FieldInfo(rightField.getOriginFieldName(), rightField.getOriginNodeName(),
                FieldInfoUtils.convertFieldFormat(rightField.getFieldType(), rightField.getFieldFormat()));
        return new SingleValueFilterFunction(operator, sourceField, EqualOperator.getInstance(), targetField);
    }

    private static String getNodeName(StreamNode node) {
        if (node instanceof StreamSource) {
            return ((StreamSource) node).getSourceName();
        } else if (node instanceof StreamSink) {
            return ((StreamSink) node).getSinkName();
        } else {
            return ((StreamTransform) node).getTransformName();
        }
    }

}
