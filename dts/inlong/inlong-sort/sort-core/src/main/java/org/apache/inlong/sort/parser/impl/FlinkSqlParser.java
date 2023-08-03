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

package org.apache.inlong.sort.parser.impl;

import org.apache.inlong.sort.configuration.Constants;
import org.apache.inlong.sort.formats.base.TableFormatUtils;
import org.apache.inlong.sort.formats.common.ArrayFormatInfo;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.MapFormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.function.EncryptFunction;
import org.apache.inlong.sort.function.JsonGetterFunction;
import org.apache.inlong.sort.function.RegexpReplaceFirstFunction;
import org.apache.inlong.sort.function.RegexpReplaceFunction;
import org.apache.inlong.sort.parser.Parser;
import org.apache.inlong.sort.parser.result.FlinkSqlParseResult;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.MetaFieldInfo;
import org.apache.inlong.sort.protocol.Metadata;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.MongoExtractNode;
import org.apache.inlong.sort.protocol.node.load.HbaseLoadNode;
import org.apache.inlong.sort.protocol.node.transform.DistinctNode;
import org.apache.inlong.sort.protocol.node.transform.TransformNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.Function;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.relation.IntervalJoinRelation;
import org.apache.inlong.sort.protocol.transformation.relation.JoinRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.apache.inlong.sort.protocol.transformation.relation.TemporalJoinRelation;
import org.apache.inlong.sort.protocol.transformation.relation.UnionNodeRelation;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.api.TableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.inlong.common.util.MaskDataUtils.maskSensitiveMessage;

/**
 * Flink sql parse handler
 * It accepts a TableEnvironment and GroupInfo, and outputs the parsed FlinkSqlParseResult
 */
public class FlinkSqlParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(FlinkSqlParser.class);

    public static final String SOURCE_MULTIPLE_ENABLE_KEY = "source.multiple.enable";
    private final TableEnvironment tableEnv;
    private final GroupInfo groupInfo;
    private final Set<String> hasParsedSet = new HashSet<>();
    private final List<String> extractTableSqls = new ArrayList<>();
    private final List<String> transformTableSqls = new ArrayList<>();
    private final List<String> loadTableSqls = new ArrayList<>();
    private final List<String> insertSqls = new ArrayList<>();

    /**
     * Flink sql parse constructor
     *
     * @param tableEnv The tableEnv, it is the execution environment of flink sql
     * @param groupInfo The groupInfo, it is the data model abstraction of task execution
     */
    public FlinkSqlParser(TableEnvironment tableEnv, GroupInfo groupInfo) {
        this.tableEnv = tableEnv;
        this.groupInfo = groupInfo;
        registerUDF();
    }

    /**
     * Get an instance of FlinkSqlParser
     *
     * @param tableEnv The tableEnv, it is the execution environment of flink sql
     * @param groupInfo The groupInfo, it is the data model abstraction of task execution
     * @return FlinkSqlParser The flink sql parse handler
     */
    public static FlinkSqlParser getInstance(TableEnvironment tableEnv, GroupInfo groupInfo) {
        return new FlinkSqlParser(tableEnv, groupInfo);
    }

    /**
     * Register UDF
     */
    private void registerUDF() {
        tableEnv.createTemporarySystemFunction("REGEXP_REPLACE_FIRST", RegexpReplaceFirstFunction.class);
        tableEnv.createTemporarySystemFunction("REGEXP_REPLACE", RegexpReplaceFunction.class);
        tableEnv.createTemporarySystemFunction("ENCRYPT", EncryptFunction.class);
        tableEnv.createTemporarySystemFunction("JSON_GETTER", JsonGetterFunction.class);
    }

    /**
     * Sql parse entrance
     *
     * @return FlinkSqlParseResult the result of sql parsed
     */
    @Override
    public ParseResult parse() {
        Preconditions.checkNotNull(groupInfo, "group info is null");
        Preconditions.checkNotNull(groupInfo.getStreams(), "streams is null");
        Preconditions.checkState(!groupInfo.getStreams().isEmpty(), "streams is empty");
        Preconditions.checkNotNull(tableEnv, "tableEnv is null");
        log.info("start parse group, groupId:{}", groupInfo.getGroupId());
        for (StreamInfo streamInfo : groupInfo.getStreams()) {
            parseStream(streamInfo);
        }
        log.info("parse group success, groupId:{}", groupInfo.getGroupId());
        List<String> createTableSqls = new ArrayList<>(extractTableSqls);
        createTableSqls.addAll(transformTableSqls);
        createTableSqls.addAll(loadTableSqls);
        return new FlinkSqlParseResult(tableEnv, createTableSqls, insertSqls);
    }

    /**
     * Parse stream
     *
     * @param streamInfo The encapsulation of nodes and node relations
     */
    private void parseStream(StreamInfo streamInfo) {
        Preconditions.checkNotNull(streamInfo, "stream is null");
        Preconditions.checkNotNull(streamInfo.getStreamId(), "streamId is null");
        Preconditions.checkNotNull(streamInfo.getNodes(), "nodes is null");
        Preconditions.checkState(!streamInfo.getNodes().isEmpty(), "nodes is empty");
        Preconditions.checkNotNull(streamInfo.getRelations(), "relations is null");
        Preconditions.checkState(!streamInfo.getRelations().isEmpty(), "relations is empty");
        log.info("start parse stream, streamId:{}", streamInfo.getStreamId());
        // Inject the metric option for ExtractNode or LoadNode
        injectInlongMetric(streamInfo);
        Map<String, Node> nodeMap = new HashMap<>(streamInfo.getNodes().size());
        streamInfo.getNodes().forEach(s -> {
            Preconditions.checkNotNull(s.getId(), "node id is null");
            nodeMap.put(s.getId(), s);
        });
        Map<String, NodeRelation> relationMap = new HashMap<>();
        streamInfo.getRelations().forEach(r -> {
            for (String output : r.getOutputs()) {
                relationMap.put(output, r);
            }
        });
        streamInfo.getRelations().forEach(r -> {
            parseNodeRelation(r, nodeMap, relationMap);
        });
        log.info("parse stream success, streamId:{}", streamInfo.getStreamId());
    }

    /**
     * Inject the metric option for ExtractNode or LoadNode
     *
     * @param streamInfo The encapsulation of nodes and node relations
     */
    private void injectInlongMetric(StreamInfo streamInfo) {
        streamInfo.getNodes().stream().filter(node -> node instanceof InlongMetric).forEach(node -> {
            Map<String, String> properties = node.getProperties();
            if (properties == null) {
                properties = new LinkedHashMap<>();
                if (node instanceof LoadNode) {
                    ((LoadNode) node).setProperties(properties);
                } else if (node instanceof ExtractNode) {
                    ((ExtractNode) node).setProperties(properties);
                } else {
                    throw new UnsupportedOperationException(String.format(
                            "Unsupported inlong group stream node for: %s", node.getClass().getSimpleName()));
                }
            }
            properties.put(Constants.METRICS_LABELS.key(),
                    Stream.of(Constants.GROUP_ID + "=" + groupInfo.getGroupId(),
                            Constants.STREAM_ID + "=" + streamInfo.getStreamId(),
                            Constants.NODE_ID + "=" + node.getId())
                            .collect(Collectors.joining("&")));
            // METRICS_AUDIT_PROXY_HOSTS depends on INLONG_GROUP_STREAM_NODE
            if (StringUtils.isNotEmpty(groupInfo.getProperties().get(Constants.METRICS_AUDIT_PROXY_HOSTS.key()))) {
                properties.put(Constants.METRICS_AUDIT_PROXY_HOSTS.key(),
                        groupInfo.getProperties().get(Constants.METRICS_AUDIT_PROXY_HOSTS.key()));
            }
        });
    }

    /**
     * parse node relation
     * Here we only parse the output node in the relation,
     * and the input node parsing is achieved by parsing the dependent node parsing of the output node.
     *
     * @param relation Define relations between nodes, it also shows the data flow
     * @param nodeMap Store the mapping relation between node id and node
     * @param relationMap Store the mapping relation between node id and relation
     */
    private void parseNodeRelation(NodeRelation relation, Map<String, Node> nodeMap,
            Map<String, NodeRelation> relationMap) {
        log.info("start parse node relation, relation:{}", relation);
        Preconditions.checkNotNull(relation, "relation is null");
        Preconditions.checkState(relation.getInputs().size() > 0,
                "relation must have at least one input node");
        Preconditions.checkState(relation.getOutputs().size() > 0,
                "relation must have at least one output node");
        relation.getOutputs().forEach(s -> {
            Preconditions.checkNotNull(s, "node id in outputs is null");
            Node outputNode = nodeMap.get(s);
            Preconditions.checkNotNull(outputNode, "can not find any node by node id " + s);
            parseInputNodes(relation, nodeMap, relationMap);
            parseSingleNode(outputNode, relation, nodeMap);
            // for Load node we need to generate insert sql
            if (outputNode instanceof LoadNode) {
                insertSqls.add(genLoadNodeInsertSql((LoadNode) outputNode, relation, nodeMap));
            }
        });
        log.info("parse node relation success, relation:{}", relation);
    }

    /**
     * parse the input nodes corresponding to the output node
     * @param relation Define relations between nodes, it also shows the data flow
     * @param nodeMap Store the mapping relation between node id and node
     * @param relationMap Store the mapping relation between node id and relation
     */
    private void parseInputNodes(NodeRelation relation, Map<String, Node> nodeMap,
            Map<String, NodeRelation> relationMap) {
        for (String upstreamNodeId : relation.getInputs()) {
            if (!hasParsedSet.contains(upstreamNodeId)) {
                Node upstreamNode = nodeMap.get(upstreamNodeId);
                Preconditions.checkNotNull(upstreamNode,
                        "can not find any node by node id " + upstreamNodeId);
                parseSingleNode(upstreamNode, relationMap.get(upstreamNodeId), nodeMap);
            }
        }
    }

    private void registerTableSql(Node node, String sql) {
        if (node instanceof ExtractNode) {
            extractTableSqls.add(sql);
        } else if (node instanceof TransformNode) {
            transformTableSqls.add(sql);
        } else if (node instanceof LoadNode) {
            loadTableSqls.add(sql);
        } else {
            throw new UnsupportedOperationException("Only support [ExtractNode|TransformNode|LoadNode]");
        }
    }

    /**
     * Parse a single node and generate the corresponding sql
     *
     * @param node The abstract of extract, transform, load
     * @param relation Define relations between nodes, it also shows the data flow
     * @param nodeMap store the mapping relation between node id and node
     */
    private void parseSingleNode(Node node, NodeRelation relation, Map<String, Node> nodeMap) {
        if (hasParsedSet.contains(node.getId())) {
            log.warn("the node has already been parsed, node id:{}", node.getId());
            return;
        }
        if (node instanceof ExtractNode) {
            log.info("start parse node, node id:{}", node.getId());
            String sql = genCreateSql(node);
            log.info("node id:{}, create table sql:\n{}", node.getId(), maskSensitiveMessage(sql));
            registerTableSql(node, sql);
            hasParsedSet.add(node.getId());
        } else {
            Preconditions.checkNotNull(relation, "relation is null");
            if (node instanceof LoadNode) {
                String createSql = genCreateSql(node);
                log.info("node id:{}, create table sql:\n{}", node.getId(), maskSensitiveMessage(createSql));
                registerTableSql(node, createSql);
                hasParsedSet.add(node.getId());
            } else if (node instanceof TransformNode) {
                TransformNode transformNode = (TransformNode) node;
                Preconditions.checkNotNull(transformNode.getFieldRelations(),
                        "field relations is null");
                Preconditions.checkState(!transformNode.getFieldRelations().isEmpty(),
                        "field relations is empty");
                String createSql = genCreateSql(node);
                log.info("node id:{}, create table sql:\n{}", node.getId(), maskSensitiveMessage(createSql));
                String selectSql = genTransformSelectSql(transformNode, relation, nodeMap);
                log.info("node id:{}, transform sql:\n{}", node.getId(), maskSensitiveMessage(selectSql));
                registerTableSql(node, createSql + " AS\n" + selectSql);
                hasParsedSet.add(node.getId());
            }
        }
        log.info("parse node success, node id:{}", node.getId());
    }

    private String genTransformSelectSql(TransformNode transformNode, NodeRelation relation,
            Map<String, Node> nodeMap) {
        String selectSql;
        if (relation instanceof JoinRelation) {
            // parse join relation and generate the transform sql
            JoinRelation joinRelation = (JoinRelation) relation;
            selectSql = genJoinSelectSql(transformNode, transformNode.getFieldRelations(), joinRelation,
                    transformNode.getFilters(), transformNode.getFilterStrategy(), nodeMap);
        } else if (relation instanceof UnionNodeRelation) {
            // parse union relation and generate the transform sql
            Preconditions.checkState(transformNode.getFilters() == null
                    || transformNode.getFilters().isEmpty(), "Filter is not supported when union");
            Preconditions.checkState(transformNode.getClass() == TransformNode.class,
                    String.format("union is not supported for %s", transformNode.getClass().getSimpleName()));
            UnionNodeRelation unionRelation = (UnionNodeRelation) relation;
            selectSql = genUnionNodeSelectSql(transformNode, transformNode.getFieldRelations(), unionRelation, nodeMap);
        } else {
            // parse base relation that one to one and generate the transform sql
            Preconditions.checkState(relation.getInputs().size() == 1,
                    "simple transform only support one input node");
            Preconditions.checkState(relation.getOutputs().size() == 1,
                    "join node only support one output node");
            selectSql = genSimpleSelectSql(transformNode, transformNode.getFieldRelations(), relation,
                    transformNode.getFilters(), transformNode.getFilterStrategy(), nodeMap);
        }
        return selectSql;
    }

    /**
     * Generate select sql for union
     *
     * @param fieldRelations The relation of fieds
     * @param unionRelation The union relation of sql
     * @param nodeMap Store the mapping relation between node id and node
     * @return Transform sql for this node logic
     */
    private String genUnionNodeSelectSql(Node node, List<FieldRelation> fieldRelations,
            UnionNodeRelation unionRelation, Map<String, Node> nodeMap) {
        Preconditions.checkState(unionRelation.getInputs().size() > 1,
                "union must have more than one input nodes");
        Preconditions.checkState(unionRelation.getOutputs().size() == 1,
                "union node only support one output node");
        // Get table name alias map by input nodes
        Map<String, Map<String, FieldRelation>> fieldRelationMap = new HashMap<>(unionRelation.getInputs().size());
        // Generate mapping for output field to FieldRelation
        fieldRelations.forEach(s -> {
            // All field relations of input nodes will be the same if the node id of output field is blank.
            // Currently, the node id in the output field is used to distinguish which field of the node in the
            // upstream of the union the field comes from. A better way is through the upstream input field,
            // but this abstraction does not yet have the ability to set node ids for all upstream input fields.
            // todo optimize the implementation of this block in the future
            String nodeId = s.getOutputField().getNodeId();
            if (StringUtils.isBlank(nodeId)) {
                nodeId = unionRelation.getInputs().get(0);
            }
            fieldRelationMap.computeIfAbsent(nodeId, k -> new HashMap<>()).put(s.getOutputField().getName(), s);
        });
        StringBuilder sb = new StringBuilder();
        sb.append(genUnionSingleSelectSql(unionRelation.getInputs().get(0),
                nodeMap.get(unionRelation.getInputs().get(0)).genTableName(), node.getFields(),
                fieldRelationMap, fieldRelationMap.get(unionRelation.getInputs().get(0)), node));
        String relationFormat = unionRelation.format();
        for (int i = 1; i < unionRelation.getInputs().size(); i++) {
            String inputId = unionRelation.getInputs().get(i);
            sb.append("\n").append(relationFormat).append("\n")
                    .append(genUnionSingleSelectSql(inputId, nodeMap.get(inputId).genTableName(), node.getFields(),
                            fieldRelationMap, fieldRelationMap.get(unionRelation.getInputs().get(0)), node));
        }
        return sb.toString();
    }

    private String genUnionSingleSelectSql(String inputId, String tableName, List<FieldInfo> fields,
            Map<String, Map<String, FieldRelation>> fieldRelationMap,
            Map<String, FieldRelation> defaultFieldRelationMap, Node node) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        Map<String, FieldRelation> fieldRelations = fieldRelationMap.get(inputId);
        if (fieldRelations == null) {
            fieldRelations = defaultFieldRelationMap;
        }
        if (node instanceof HbaseLoadNode) {
            HbaseLoadNode hbaseLoadNode = (HbaseLoadNode) node;
            parseHbaseLoadFieldRelation(hbaseLoadNode.getRowKey(), fieldRelations.values(), sb);
        } else {
            parseFieldRelations(fields, fieldRelations, sb);
        }
        sb.append(" FROM `").append(tableName).append("` ");
        return sb.toString();
    }

    private String genJoinSelectSql(Node node, List<FieldRelation> fieldRelations,
            JoinRelation relation, List<FilterFunction> filters, FilterStrategy filterStrategy,
            Map<String, Node> nodeMap) {
        Preconditions.checkState(relation.getInputs().size() > 1,
                "join must have more than one input nodes");
        Preconditions.checkState(relation.getOutputs().size() == 1,
                "join node only support one output node");
        // Get table name alias map by input nodes
        Map<String, String> tableNameAliasMap = new HashMap<>(relation.getInputs().size());
        relation.getInputs().forEach(s -> {
            Node inputNode = nodeMap.get(s);
            Preconditions.checkNotNull(inputNode, String.format("input node is not found by id:%s", s));
            tableNameAliasMap.put(s, String.format("t%s", s));
        });
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        Map<String, FieldRelation> fieldRelationMap = new HashMap<>(fieldRelations.size());
        // Generate mapping for output field to FieldRelation
        fieldRelations.forEach(s -> {
            fillOutTableNameAlias(Collections.singletonList(s.getInputField()), tableNameAliasMap);
            fieldRelationMap.put(s.getOutputField().getName(), s);
        });

        if (node instanceof HbaseLoadNode) {
            HbaseLoadNode hbaseLoadNode = (HbaseLoadNode) node;
            parseHbaseLoadFieldRelation(hbaseLoadNode.getRowKey(), hbaseLoadNode.getFieldRelations(), sb);
        } else {
            parseFieldRelations(node.getFields(), fieldRelationMap, sb);
        }
        if (node instanceof DistinctNode) {
            DistinctNode distinctNode = (DistinctNode) node;
            // Fill out the tablename alias for param
            List<FunctionParam> params = new ArrayList<>(distinctNode.getDistinctFields());
            params.add(distinctNode.getOrderField());
            fillOutTableNameAlias(params, tableNameAliasMap);
            // Generate distinct sql, such as ROW_NUMBER()...
            genDistinctSql(distinctNode, sb);
        }
        sb.append(" FROM `").append(nodeMap.get(relation.getInputs().get(0)).genTableName()).append("` ")
                .append(tableNameAliasMap.get(relation.getInputs().get(0)));
        // Parse condition map of join and format condition to sql, such as on 1 = 1...
        Map<String, List<FilterFunction>> conditionMap = relation.getJoinConditionMap();
        if (relation instanceof TemporalJoinRelation) {
            parseTemporalJoin((TemporalJoinRelation) relation, nodeMap, tableNameAliasMap, conditionMap, sb);
        } else if (relation instanceof IntervalJoinRelation) {
            Preconditions.checkState(filters == null || filters.isEmpty(),
                    String.format("filters must be empty for %s", relation.getClass().getSimpleName()));
            parseIntervalJoin((IntervalJoinRelation) relation, nodeMap, tableNameAliasMap, sb);
            List<FilterFunction> conditions = conditionMap.values().stream().findFirst().orElse(null);
            Preconditions.checkState(conditions != null && !conditions.isEmpty(),
                    String.format("Join conditions must no be empty for %s", relation.getClass().getSimpleName()));
            fillOutTableNameAlias(new ArrayList<>(conditions), tableNameAliasMap);
            parseFilterFields(FilterStrategy.RETAIN, conditions, sb);
        } else {
            parseRegularJoin(relation, nodeMap, tableNameAliasMap, conditionMap, sb);
        }
        if (filters != null && !filters.isEmpty()) {
            // Fill out the table name alias for param
            fillOutTableNameAlias(new ArrayList<>(filters), tableNameAliasMap);
            // Parse filter fields to generate filter sql like 'WHERE 1=1...'
            parseFilterFields(filterStrategy, filters, sb);
        }
        if (node instanceof DistinctNode) {
            // Generate distinct filter sql like 'WHERE row_num = 1'
            sb = genDistinctFilterSql(node.getFields(), sb);
        }
        return sb.toString();
    }

    private void parseIntervalJoin(IntervalJoinRelation relation, Map<String, Node> nodeMap,
            Map<String, String> tableNameAliasMap, StringBuilder sb) {
        for (int i = 1; i < relation.getInputs().size(); i++) {
            String inputId = relation.getInputs().get(i);
            sb.append(", ").append(nodeMap.get(inputId).genTableName())
                    .append(" ").append(tableNameAliasMap.get(inputId));
        }
    }

    private void parseRegularJoin(JoinRelation relation, Map<String, Node> nodeMap,
            Map<String, String> tableNameAliasMap, Map<String, List<FilterFunction>> conditionMap, StringBuilder sb) {
        for (int i = 1; i < relation.getInputs().size(); i++) {
            String inputId = relation.getInputs().get(i);
            sb.append("\n      ").append(relation.format()).append(" ")
                    .append(nodeMap.get(inputId).genTableName()).append(" ")
                    .append(tableNameAliasMap.get(inputId)).append("\n    ON ");
            parseJoinConditions(inputId, conditionMap, tableNameAliasMap, sb);
        }
    }

    private void parseTemporalJoin(TemporalJoinRelation relation, Map<String, Node> nodeMap,
            Map<String, String> tableNameAliasMap, Map<String, List<FilterFunction>> conditionMap, StringBuilder sb) {
        if (StringUtils.isBlank(relation.getSystemTime().getNodeId())) {
            relation.getSystemTime().setNodeId(relation.getInputs().get(0));
        }
        relation.getSystemTime().setTableNameAlias(tableNameAliasMap.get(relation.getSystemTime().getNodeId()));
        String systemTimeFormat = String.format("FOR SYSTEM_TIME AS OF %s ", relation.getSystemTime().format());
        for (int i = 1; i < relation.getInputs().size(); i++) {
            String inputId = relation.getInputs().get(i);
            sb.append("\n      ").append(relation.format()).append(" ")
                    .append(nodeMap.get(inputId).genTableName()).append(" ");
            sb.append(systemTimeFormat);
            sb.append(tableNameAliasMap.get(inputId)).append("\n    ON ");
            parseJoinConditions(inputId, conditionMap, tableNameAliasMap, sb);
        }
    }

    private void parseJoinConditions(String inputId, Map<String, List<FilterFunction>> conditionMap,
            Map<String, String> tableNameAliasMap, StringBuilder sb) {
        List<FilterFunction> conditions = conditionMap.get(inputId);
        Preconditions.checkNotNull(conditions, String.format("join condition is null for node id:%s", inputId));
        for (FilterFunction filter : conditions) {
            // Fill out the table name alias for param
            fillOutTableNameAlias(filter.getParams(), tableNameAliasMap);
            sb.append(" ").append(filter.format());
        }
    }

    /**
     * Fill out the table name alias
     *
     * @param params The params used in filter, join condition, transform function etc.
     * @param tableNameAliasMap The table name alias map, contains all table name alias used in this relation of
     *         nodes
     */
    private void fillOutTableNameAlias(List<FunctionParam> params, Map<String, String> tableNameAliasMap) {
        for (FunctionParam param : params) {
            if (param instanceof Function) {
                fillOutTableNameAlias(((Function) param).getParams(), tableNameAliasMap);
            } else if (param instanceof FieldInfo) {
                FieldInfo fieldParam = (FieldInfo) param;
                Preconditions.checkNotNull(fieldParam.getNodeId(),
                        "node id of field is null when exists more than two input nodes");
                String tableNameAlias = tableNameAliasMap.get(fieldParam.getNodeId());
                Preconditions.checkNotNull(tableNameAlias,
                        String.format("can not find any node by node id:%s of field:%s",
                                fieldParam.getNodeId(), fieldParam.getName()));
                fieldParam.setTableNameAlias(tableNameAlias);
            }
        }
    }

    /**
     * Generate filter sql of distinct node
     *
     * @param fields The fields of node
     * @param sb Container for storing sql
     * @return A new container for storing sql
     */
    private StringBuilder genDistinctFilterSql(List<FieldInfo> fields, StringBuilder sb) {
        String subSql = sb.toString();
        sb = new StringBuilder("SELECT ");
        for (FieldInfo field : fields) {
            sb.append("\n    `").append(field.getName()).append("`,");
        }
        sb.deleteCharAt(sb.length() - 1).append("\n    FROM (").append(subSql)
                .append(")\nWHERE row_num = 1");
        return sb;
    }

    /**
     * Generate distinct sql according to the deduplication field, the sorting field.
     *
     * @param distinctNode The distinct node
     * @param sb Container for storing sql
     */
    private void genDistinctSql(DistinctNode distinctNode, StringBuilder sb) {
        Preconditions.checkNotNull(distinctNode.getDistinctFields(), "distinctField is null");
        Preconditions.checkState(!distinctNode.getDistinctFields().isEmpty(),
                "distinctField is empty");
        Preconditions.checkNotNull(distinctNode.getOrderField(), "orderField is null");
        sb.append(",\n    ROW_NUMBER() OVER (PARTITION BY ");
        for (FieldInfo distinctField : distinctNode.getDistinctFields()) {
            sb.append(distinctField.format()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(" ORDER BY ").append(distinctNode.getOrderField().format()).append(" ")
                .append(distinctNode.getOrderDirection().name()).append(") AS row_num");
    }

    /**
     * Generate the most basic conversion sql one-to-one
     *
     * @param node The load node
     * @param fieldRelations The relation between fields
     * @param relation Define relations between nodes, it also shows the data flow
     * @param filters The filters
     * @param filterStrategy The filterStrategy
     * @param nodeMap Store the mapping relation between node id and node
     * @return Select sql for this node logic
     */
    private String genSimpleSelectSql(Node node, List<FieldRelation> fieldRelations,
            NodeRelation relation, List<FilterFunction> filters, FilterStrategy filterStrategy,
            Map<String, Node> nodeMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        Map<String, FieldRelation> fieldRelationMap = new HashMap<>(fieldRelations.size());
        fieldRelations.forEach(s -> {
            fieldRelationMap.put(s.getOutputField().getName(), s);
        });
        if (node instanceof HbaseLoadNode) {
            HbaseLoadNode hbaseLoadNode = (HbaseLoadNode) node;
            parseHbaseLoadFieldRelation(hbaseLoadNode.getRowKey(), hbaseLoadNode.getFieldRelations(), sb);
        } else {
            parseFieldRelations(node.getFields(), fieldRelationMap, sb);
        }
        if (node instanceof DistinctNode) {
            genDistinctSql((DistinctNode) node, sb);
        }
        sb.append("\n    FROM `").append(nodeMap.get(relation.getInputs().get(0)).genTableName()).append("` ");
        parseFilterFields(filterStrategy, filters, sb);
        if (node instanceof DistinctNode) {
            sb = genDistinctFilterSql(node.getFields(), sb);
        }
        return sb.toString();
    }

    /**
     * Parse filter fields to generate filter sql like 'where 1=1...'
     *
     * @param filterStrategy The filter strategy default[RETAIN], it decides whether to retain or remove
     * @param filters The filter functions
     * @param sb Container for storing sql
     */
    private void parseFilterFields(FilterStrategy filterStrategy, List<FilterFunction> filters, StringBuilder sb) {
        if (filters != null && !filters.isEmpty()) {
            sb.append("\nWHERE ");
            String subSql = StringUtils
                    .join(filters.stream().map(FunctionParam::format).collect(Collectors.toList()), "\n    ");
            if (filterStrategy == FilterStrategy.REMOVE) {
                sb.append("not (").append(subSql).append(")");
            } else {
                sb.append(subSql);
            }
        }
    }

    /**
     * Parse field relation
     *
     * @param fields The fields defined in node
     * @param fieldRelationMap The field relation map
     * @param sb Container for storing sql
     */
    private void parseFieldRelations(List<FieldInfo> fields,
            Map<String, FieldRelation> fieldRelationMap, StringBuilder sb) {
        for (FieldInfo field : fields) {
            FieldRelation fieldRelation = fieldRelationMap.get(field.getName());
            FormatInfo fieldFormatInfo = field.getFormatInfo();
            if (fieldRelation == null) {
                String targetType = TableFormatUtils.deriveLogicalType(fieldFormatInfo).asSummaryString();
                sb.append("\n    CAST(NULL as ").append(targetType).append(") AS ").append(field.format()).append(",");
                continue;
            }
            boolean complexType = fieldFormatInfo instanceof RowFormatInfo
                    || fieldFormatInfo instanceof ArrayFormatInfo
                    || fieldFormatInfo instanceof MapFormatInfo;
            FunctionParam inputField = fieldRelation.getInputField();
            if (inputField instanceof FieldInfo) {
                FieldInfo fieldInfo = (FieldInfo) inputField;
                FormatInfo formatInfo = fieldInfo.getFormatInfo();
                FieldInfo outputField = fieldRelation.getOutputField();
                boolean sameType = formatInfo != null
                        && outputField != null
                        && outputField.getFormatInfo() != null
                        && outputField.getFormatInfo().getTypeInfo().equals(formatInfo.getTypeInfo());
                if (complexType || sameType || fieldFormatInfo == null) {
                    sb.append("\n    ").append(inputField.format()).append(" AS ").append(field.format()).append(",");
                } else {
                    String targetType = TableFormatUtils.deriveLogicalType(fieldFormatInfo).asSummaryString();
                    sb.append("\n    CAST(").append(inputField.format()).append(" as ")
                            .append(targetType).append(") AS ").append(field.format()).append(",");
                }
            } else {
                String targetType = TableFormatUtils.deriveLogicalType(field.getFormatInfo()).asSummaryString();
                sb.append("\n    CAST(").append(inputField.format()).append(" as ")
                        .append(targetType).append(") AS ").append(field.format()).append(",");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
    }

    /**
     * Generate load node insert sql
     *
     * @param loadNode The real data write node
     * @param relation The relation between nods
     * @param nodeMap The node map
     * @return Insert sql
     */
    private String genLoadNodeInsertSql(LoadNode loadNode, NodeRelation relation, Map<String, Node> nodeMap) {
        Preconditions.checkNotNull(loadNode.getFieldRelations(), "field relations is null");
        Preconditions.checkState(!loadNode.getFieldRelations().isEmpty(),
                "field relations is empty");
        String selectSql = genLoadSelectSql(loadNode, relation, nodeMap);
        return "INSERT INTO `" + loadNode.genTableName() + "`\n    " + selectSql;
    }

    private String genLoadSelectSql(LoadNode loadNode, NodeRelation relation,
            Map<String, Node> nodeMap) {
        String selectSql;
        if (relation instanceof JoinRelation) {
            // parse join relation and generate the select sql
            JoinRelation joinRelation = (JoinRelation) relation;
            selectSql = genJoinSelectSql(loadNode, loadNode.getFieldRelations(), joinRelation,
                    loadNode.getFilters(), loadNode.getFilterStrategy(), nodeMap);
        } else if (relation instanceof UnionNodeRelation) {
            // parse union relation and generate the select sql
            Preconditions.checkState(loadNode.getFilters() == null || loadNode.getFilters().isEmpty(),
                    "Filter is not supported when union");
            UnionNodeRelation unionRelation = (UnionNodeRelation) relation;
            selectSql = genUnionNodeSelectSql(loadNode, loadNode.getFieldRelations(), unionRelation, nodeMap);
        } else {
            // parse base relation that one to one and generate the select sql
            Preconditions.checkState(relation.getInputs().size() == 1,
                    "simple transform only support one input node");
            Preconditions.checkState(relation.getOutputs().size() == 1,
                    "join node only support one output node");
            selectSql = genSimpleSelectSql(loadNode, loadNode.getFieldRelations(), relation,
                    loadNode.getFilters(), loadNode.getFilterStrategy(), nodeMap);
        }
        return selectSql;
    }

    private void parseHbaseLoadFieldRelation(String rowkey, Collection<FieldRelation> fieldRelations,
            StringBuilder sb) {
        sb.append("CAST(").append(rowkey).append(" AS STRING) AS rowkey,\n");
        Map<String, List<FieldRelation>> columnFamilyMapFields = genColumnFamilyMapFieldRelations(
                fieldRelations);
        for (Map.Entry<String, List<FieldRelation>> entry : columnFamilyMapFields.entrySet()) {
            StringBuilder fieldAppend = new StringBuilder(" ROW(");
            for (FieldRelation fieldRelation : entry.getValue()) {
                FieldInfo outputField = fieldRelation.getOutputField();
                String targetType = TableFormatUtils.deriveLogicalType(outputField.getFormatInfo()).asSummaryString();
                fieldAppend.append("CAST(").append(fieldRelation.getInputField().format()).append(" AS ")
                        .append(targetType).append(")").append(", ");
            }
            if (fieldAppend.length() > 0) {
                fieldAppend.delete(fieldAppend.lastIndexOf(","), fieldAppend.length());
            }
            fieldAppend.append("),");
            sb.append(fieldAppend);
        }
        sb.delete(sb.lastIndexOf(","), sb.length());
    }

    /**
     * Generate create sql
     *
     * @param node The abstract of extract, transform, load
     * @return The creation sql pf table
     */
    private String genCreateSql(Node node) {
        if (node instanceof TransformNode) {
            return genCreateTransformSql(node);
        }
        if (node instanceof HbaseLoadNode) {
            return genCreateHbaseLoadSql((HbaseLoadNode) node);
        }
        StringBuilder sb = new StringBuilder("CREATE TABLE `");
        sb.append(node.genTableName()).append("`(\n");
        String filterPrimaryKey = getFilterPrimaryKey(node);
        sb.append(genPrimaryKey(node.getPrimaryKey(), filterPrimaryKey));
        sb.append(parseFields(node.getFields(), node, filterPrimaryKey));
        if (node instanceof ExtractNode) {
            ExtractNode extractNode = (ExtractNode) node;
            if (extractNode.getWatermarkField() != null) {
                sb.append(",\n     ").append(extractNode.getWatermarkField().format());
            }
        }
        sb.append(")");
        if (node.getPartitionFields() != null && !node.getPartitionFields().isEmpty()) {
            sb.append(String.format("\nPARTITIONED BY (%s)",
                    StringUtils.join(formatFields(node.getPartitionFields()), ",")));
        }
        sb.append(parseOptions(node.tableOptions()));
        return sb.toString();
    }

    /**
     * Get filter PrimaryKey for Mongo when multi-sink mode
     */
    private String getFilterPrimaryKey(Node node) {
        if (node instanceof MongoExtractNode) {
            if (null != node.getProperties().get(SOURCE_MULTIPLE_ENABLE_KEY)
                    && node.getProperties().get(SOURCE_MULTIPLE_ENABLE_KEY).equals("true")) {
                return node.getPrimaryKey();
            }
        }
        return null;
    }

    /**
     * Gen create table DDL for hbase load
     */
    private String genCreateHbaseLoadSql(HbaseLoadNode node) {
        StringBuilder sb = new StringBuilder("CREATE TABLE `");
        sb.append(node.genTableName()).append("`(\n");
        sb.append("rowkey STRING,\n");

        Map<String, List<FieldRelation>> columnFamilyMapFields = genColumnFamilyMapFieldRelations(
                node.getFieldRelations());
        for (Map.Entry<String, List<FieldRelation>> entry : columnFamilyMapFields.entrySet()) {
            sb.append(entry.getKey());
            StringBuilder fieldsAppend = new StringBuilder(" Row<");
            for (FieldRelation fieldRelation : entry.getValue()) {
                fieldsAppend.append(fieldRelation.getOutputField().getName().split(":")[1]).append(" ")
                        .append(TableFormatUtils.deriveLogicalType(fieldRelation.getOutputField().getFormatInfo())
                                .asSummaryString())
                        .append(",");
            }
            if (fieldsAppend.length() > 0) {
                fieldsAppend.delete(fieldsAppend.lastIndexOf(","), fieldsAppend.length());
                fieldsAppend.append(">,\n");
            }
            sb.append(fieldsAppend);
        }
        sb.append("PRIMARY KEY (rowkey) NOT ENFORCED\n) ");
        sb.append(parseOptions(node.tableOptions()));
        return sb.toString();
    }

    private Map<String, List<FieldRelation>> genColumnFamilyMapFieldRelations(
            Collection<FieldRelation> fieldRelations) {
        Map<String, List<FieldRelation>> columnFamilyMapFields = new LinkedHashMap<>(16);
        Set<String> nameSet = new HashSet<>();
        for (FieldRelation fieldRelation : fieldRelations) {
            String columnFamily = fieldRelation.getOutputField().getName().split(":")[0];
            if (nameSet.add(fieldRelation.getOutputField().getName())) {
                columnFamilyMapFields.computeIfAbsent(columnFamily, v -> new ArrayList<>())
                        .add(fieldRelation);
            }
        }
        return columnFamilyMapFields;
    }

    /**
     * Generate create transform sql
     *
     * @param node The transform node
     * @return The creation sql of transform node
     */
    private String genCreateTransformSql(Node node) {
        return String.format("CREATE VIEW `%s` (%s)",
                node.genTableName(), parseTransformNodeFields(node.getFields()));
    }

    /**
     * Parse options to generate with options
     *
     * @param options The options defined in node
     * @return The with option string
     */
    private String parseOptions(Map<String, String> options) {
        StringBuilder sb = new StringBuilder();
        if (options != null && !options.isEmpty()) {
            sb.append("\n    WITH (");
            for (Map.Entry<String, String> kv : options.entrySet()) {
                sb.append("\n    '").append(kv.getKey()).append("' = '").append(kv.getValue()).append("'").append(",");
            }
            if (sb.length() > 0) {
                sb.delete(sb.lastIndexOf(","), sb.length());
            }
            sb.append("\n)");
        }
        return sb.toString();
    }

    /**
     * Parse transform node fields
     *
     * @param fields The fields defined in node
     * @return Field formats in select sql
     */
    private String parseTransformNodeFields(List<FieldInfo> fields) {
        StringBuilder sb = new StringBuilder();
        for (FieldInfo field : fields) {
            sb.append("\n    `").append(field.getName()).append("`,");
        }
        if (sb.length() > 0) {
            sb.delete(sb.lastIndexOf(","), sb.length());
        }
        return sb.toString();
    }

    /**
     * Parse fields
     *
     * @param fields The fields defined in node
     * @param node The abstract of extract, transform, load
     * @param filterPrimaryKey filter PrimaryKey, use for mongo
     * @return Field formats in select sql
     */
    private String parseFields(List<FieldInfo> fields, Node node, String filterPrimaryKey) {
        StringBuilder sb = new StringBuilder();
        for (FieldInfo field : fields) {
            if (StringUtils.isNotBlank(filterPrimaryKey) && field.getName().equals(filterPrimaryKey)) {
                continue;
            }
            sb.append("    `").append(field.getName()).append("` ");
            if (field instanceof MetaFieldInfo) {
                if (!(node instanceof Metadata)) {
                    throw new IllegalArgumentException(String.format("Node: %s is not instance of Metadata",
                            node.getClass().getSimpleName()));
                }
                MetaFieldInfo metaFieldInfo = (MetaFieldInfo) field;
                Metadata metadataNode = (Metadata) node;
                if (!metadataNode.supportedMetaFields().contains(metaFieldInfo.getMetaField())) {
                    throw new UnsupportedOperationException(String.format("Unsupported meta field for %s: %s",
                            metadataNode.getClass().getSimpleName(), metaFieldInfo.getMetaField()));
                }
                sb.append(metadataNode.format(metaFieldInfo.getMetaField()));
            } else {
                sb.append(TableFormatUtils.deriveLogicalType(field.getFormatInfo()).asSummaryString());
            }
            sb.append(",\n");
        }
        if (sb.length() > 0) {
            sb.delete(sb.lastIndexOf(","), sb.length());
        }
        return sb.toString();
    }

    /**
     * Generate primary key format in sql
     *
     * @param primaryKey The primary key of table
     * @param filterPrimaryKey filter PrimaryKey, use for mongo
     * @return Primary key format in sql
     */
    private String genPrimaryKey(String primaryKey, String filterPrimaryKey) {
        boolean checkPrimaryKeyFlag = StringUtils.isNotBlank(primaryKey)
                && (StringUtils.isBlank(filterPrimaryKey) || !primaryKey.equals(filterPrimaryKey));
        if (checkPrimaryKeyFlag) {
            primaryKey = String.format("    PRIMARY KEY (%s) NOT ENFORCED,\n",
                    StringUtils.join(formatFields(primaryKey.split(",")), ","));
        } else {
            primaryKey = "";
        }
        return primaryKey;
    }

    /**
     * Format fields with '`'
     *
     * @param fields The fields that need format
     * @return list of field after format
     */
    private List<String> formatFields(String... fields) {
        List<String> formatFields = new ArrayList<>(fields.length);
        for (String field : fields) {
            if (!field.contains("`")) {
                formatFields.add(String.format("`%s`", field.trim()));
            } else {
                formatFields.add(field);
            }
        }
        return formatFields;
    }

    private List<String> formatFields(List<FieldInfo> fields) {
        List<String> formatFields = new ArrayList<>(fields.size());
        for (FieldInfo field : fields) {
            formatFields.add(field.format());
        }
        return formatFields;
    }
}
