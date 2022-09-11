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

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.formats.base.TableFormatUtils;
import org.apache.inlong.sort.function.RegexpReplaceFirstFunction;
import org.apache.inlong.sort.parser.Parser;
import org.apache.inlong.sort.parser.result.FlinkSqlParseResult;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.MetaFieldInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.KafkaExtractNode;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.extract.OracleExtractNode;
import org.apache.inlong.sort.protocol.node.load.HbaseLoadNode;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.node.transform.DistinctNode;
import org.apache.inlong.sort.protocol.node.transform.TransformNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.Function;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.relation.JoinRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.apache.inlong.sort.protocol.transformation.relation.UnionNodeRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Flink sql parse handler
 * It accepts a Tableenv and GroupInfo, and outputs the parsed FlinkSqlParseResult
 */
public class FlinkSqlParser implements Parser {

    private static final Logger log = LoggerFactory.getLogger(FlinkSqlParser.class);

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
     * @param tableEnv  The tableEnv,it is the execution environment of flink sql
     * @param groupInfo The groupInfo,it is the data model abstraction of task execution
     */
    public FlinkSqlParser(TableEnvironment tableEnv, GroupInfo groupInfo) {
        this.tableEnv = tableEnv;
        this.groupInfo = groupInfo;
        registerUDF();
    }

    /**
     * Get a instance of FlinkSqlParser
     *
     * @param tableEnv  The tableEnv,it is the execution environment of flink sql
     * @param groupInfo The groupInfo,it is the data model abstraction of task execution
     * @return FlinkSqlParser The flink sql parse handler
     */
    public static FlinkSqlParser getInstance(TableEnvironment tableEnv, GroupInfo groupInfo) {
        return new FlinkSqlParser(tableEnv, groupInfo);
    }

    /**
     * Register udf
     */
    private void registerUDF() {
        tableEnv.createTemporarySystemFunction("REGEXP_REPLACE_FIRST", RegexpReplaceFirstFunction.class);
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
        Map<String, Node> nodeMap = new HashMap<>(streamInfo.getNodes().size());
        streamInfo.getNodes().forEach(s -> {
            Preconditions.checkNotNull(s.getId(), "node id is null");
            nodeMap.put(s.getId(), s);
        });
        Map<String, NodeRelation> relationMap = new HashMap<String, NodeRelation>();
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
     * parse node relation
     * <p>
     * Here we only parse the output node in the relation,
     * and the input node parsing is achieved by parsing the dependent node parsing of the output node.
     *
     * @param relation    Define relations between nodes, it also shows the data flow
     * @param nodeMap     Store the mapping relation between node id and node
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
            Node node = nodeMap.get(s);
            Preconditions.checkNotNull(node, "can not find any node by node id " + s);
            parseNode(node, relation, nodeMap, relationMap);
        });
        log.info("parse node relation success, relation:{}", relation);
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
     * Parse a node and recursively resolve its dependent nodes
     *
     * @param node        The abstract of extract, transform, load
     * @param relation    Define relations between nodes, it also shows the data flow
     * @param nodeMap     store the mapping relation between node id and node
     * @param relationMap Store the mapping relation between node id and relation
     */
    private void parseNode(Node node, NodeRelation relation, Map<String, Node> nodeMap,
            Map<String, NodeRelation> relationMap) {
        if (hasParsedSet.contains(node.getId())) {
            log.warn("the node has already been parsed, node id:{}", node.getId());
            return;
        }
        if (node instanceof ExtractNode) {
            log.info("start parse node, node id:{}", node.getId());
            String sql = genCreateSql(node);
            log.info("node id:{}, create table sql:\n{}", node.getId(), sql);
            registerTableSql(node, sql);
            hasParsedSet.add(node.getId());
        } else {
            Preconditions.checkNotNull(relation, "relation is null");
            for (String upstreamNodeId : relation.getInputs()) {
                if (!hasParsedSet.contains(upstreamNodeId)) {
                    Node upstreamNode = nodeMap.get(upstreamNodeId);
                    Preconditions.checkNotNull(upstreamNode,
                            "can not find any node by node id " + upstreamNodeId);
                    parseNode(upstreamNode, relationMap.get(upstreamNodeId), nodeMap, relationMap);
                }
            }
            if (node instanceof LoadNode) {
                String createSql = genCreateSql(node);
                log.info("node id:{}, create table sql:\n{}", node.getId(), createSql);
                registerTableSql(node, createSql);
                Preconditions.checkState(relation.getInputs().size() == 1,
                        "load node only support one input node");
                LoadNode loadNode = (LoadNode) node;
                String insertSql = genLoadNodeInsertSql(loadNode, nodeMap.get(relation.getInputs().get(0)));
                log.info("node id:{}, insert sql:\n{}", node.getId(), insertSql);
                insertSqls.add(insertSql);
                hasParsedSet.add(node.getId());
            } else if (node instanceof TransformNode) {
                TransformNode transformNode = (TransformNode) node;
                Preconditions.checkNotNull(transformNode.getFieldRelations(),
                        "field relations is null");
                Preconditions.checkState(!transformNode.getFieldRelations().isEmpty(),
                        "field relations is empty");
                String createSql = genCreateSql(node);
                log.info("node id:{}, create table sql:\n{}", node.getId(), createSql);
                String selectSql;
                if (relation instanceof JoinRelation) {
                    // parse join relation and generate the transform sql
                    Preconditions.checkState(relation.getInputs().size() > 1,
                            "join must have more than one input nodes");
                    Preconditions.checkState(relation.getOutputs().size() == 1,
                            "join node only support one output node");
                    JoinRelation joinRelation = (JoinRelation) relation;
                    selectSql = genJoinSelectSql(transformNode, joinRelation, nodeMap);
                } else if (relation instanceof UnionNodeRelation) {
                    // parse union relation and generate the transform sql
                    Preconditions.checkState(relation.getInputs().size() > 1,
                            "union must have more than one input nodes");
                    Preconditions.checkState(relation.getOutputs().size() == 1,
                            "join node only support one output node");
                    UnionNodeRelation unionRelation = (UnionNodeRelation) relation;
                    selectSql = genUnionNodeSelectSql(transformNode, unionRelation, nodeMap);
                } else {
                    // parse base relation that one to one and generate the transform sql
                    Preconditions.checkState(relation.getInputs().size() == 1,
                            "simple transform only support one input node");
                    Preconditions.checkState(relation.getOutputs().size() == 1,
                            "join node only support one output node");
                    selectSql = genSimpleTransformSelectSql(transformNode, relation, nodeMap);
                }
                log.info("node id:{}, tansform sql:\n{}", node.getId(), selectSql);
                registerTableSql(node, createSql + " AS\n" + selectSql);
                hasParsedSet.add(node.getId());
            }
        }
        log.info("parse node success, node id:{}", node.getId());
    }

    /**
     * generate transform sql
     *
     * @param transformNode The transform node
     * @param unionRelation The union relation of sql
     * @param nodeMap       Store the mapping relation between node id and node
     * @return Transform sql for this transform logic
     */
    private String genUnionNodeSelectSql(TransformNode transformNode,
            UnionNodeRelation unionRelation, Map<String, Node> nodeMap) {
        throw new UnsupportedOperationException("Union is not currently supported");
    }

    private String genJoinSelectSql(TransformNode node,
            JoinRelation relation, Map<String, Node> nodeMap) {
        // Get tablename alias map by input nodes
        Map<String, String> tableNameAliasMap = new HashMap<>(relation.getInputs().size());
        relation.getInputs().forEach(s -> {
            Node inputNode = nodeMap.get(s);
            Preconditions.checkNotNull(inputNode, String.format("input node is not found by id:%s", s));
            tableNameAliasMap.put(s, String.format("t%s", s));
        });
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        Map<String, FieldRelation> fieldRelationMap = new HashMap<>(node.getFieldRelations().size());
        // Generate mapping for output field to FieldRelation
        node.getFieldRelations().forEach(s -> {
            fillOutTableNameAlias(Collections.singletonList(s.getInputField()), tableNameAliasMap);
            fieldRelationMap.put(s.getOutputField().getName(), s);
        });
        parseFieldRelations(node.getFields(), fieldRelationMap, sb);
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
        String relationFormat = relation.format();
        Map<String, List<FilterFunction>> conditionMap = relation.getJoinConditionMap();
        for (int i = 1; i < relation.getInputs().size(); i++) {
            String inputId = relation.getInputs().get(i);
            sb.append("\n      ").append(relationFormat).append(" ")
                    .append(nodeMap.get(inputId).genTableName()).append(" ")
                    .append(tableNameAliasMap.get(inputId)).append("\n    ON ");
            List<FilterFunction> conditions = conditionMap.get(inputId);
            Preconditions.checkNotNull(conditions, String.format("join condition is null for node id:%s", inputId));
            for (FilterFunction filter : conditions) {
                // Fill out the tablename alias for param
                fillOutTableNameAlias(filter.getParams(), tableNameAliasMap);
                sb.append(" ").append(filter.format());
            }
        }
        if (node.getFilters() != null && !node.getFilters().isEmpty()) {
            // Fill out the tablename alias for param
            fillOutTableNameAlias(new ArrayList<>(node.getFilters()), tableNameAliasMap);
            // Parse filter fields to generate filter sql like 'WHERE 1=1...'
            parseFilterFields(node.getFilterStrategy(), node.getFilters(), sb);
        }
        if (node instanceof DistinctNode) {
            // Generate distinct filter sql like 'WHERE row_num = 1'
            sb = genDistinctFilterSql(node.getFields(), sb);
        }
        return sb.toString();
    }

    /**
     * Fill out the tablename alias
     *
     * @param params            The params used in filter, join condition, transform function etc.
     * @param tableNameAliasMap The tablename alias map,
     *                          contains all tablename alias used in this relation of nodes
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
     * @param sb     Container for storing sql
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
     * @param sb           Container for storing sql
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
     * @param node     The transform node
     * @param relation Define relations between nodes, it also shows the data flow
     * @param nodeMap  Store the mapping relation between node id and node
     * @return Transform sql for this transform logic
     */
    private String genSimpleTransformSelectSql(TransformNode node,
            NodeRelation relation, Map<String, Node> nodeMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        Map<String, FieldRelation> fieldRelationMap = new HashMap<>(node.getFieldRelations().size());
        node.getFieldRelations().forEach(s -> {
            fieldRelationMap.put(s.getOutputField().getName(), s);
        });
        parseFieldRelations(node.getFields(), fieldRelationMap, sb);
        if (node instanceof DistinctNode) {
            genDistinctSql((DistinctNode) node, sb);
        }
        sb.append("\n    FROM `").append(nodeMap.get(relation.getInputs().get(0)).genTableName()).append("` ");
        parseFilterFields(node.getFilterStrategy(), node.getFilters(), sb);
        if (node instanceof DistinctNode) {
            sb = genDistinctFilterSql(node.getFields(), sb);
        }
        return sb.toString();
    }

    /**
     * Parse filter fields to generate filter sql like 'where 1=1...'
     *
     * @param filterStrategy The filter strategy default[RETAIN], it decide whether to retain or remove
     * @param filters        The filter functions
     * @param sb             Container for storing sql
     */
    private void parseFilterFields(FilterStrategy filterStrategy, List<FilterFunction> filters, StringBuilder sb) {
        if (filters != null && !filters.isEmpty()) {
            sb.append("\n    WHERE ");
            String subSql = StringUtils
                    .join(filters.stream().map(FunctionParam::format).collect(Collectors.toList()), " ");
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
     * @param fields           The fields defined in node
     * @param fieldRelationMap The field relation map
     * @param sb               Container for storing sql
     */
    private void parseFieldRelations(List<FieldInfo> fields,
            Map<String, FieldRelation> fieldRelationMap, StringBuilder sb) {
        for (FieldInfo field : fields) {
            FieldRelation fieldRelation = fieldRelationMap.get(field.getName());
            if (fieldRelation != null) {
                sb.append("\n    ").append(fieldRelation.getInputField().format())
                        .append(" AS ").append(field.format()).append(",");
            } else {
                String targetType = TableFormatUtils.deriveLogicalType(field.getFormatInfo()).asSummaryString();
                sb.append("\n    CAST(NULL as ").append(targetType).append(") AS ").append(field.format()).append(",");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
    }

    /**
     * Generate load node insert sql
     *
     * @param loadNode  The real data write node
     * @param inputNode The input node
     * @return Insert sql
     */
    private String genLoadNodeInsertSql(LoadNode loadNode, Node inputNode) {
        Preconditions.checkNotNull(loadNode.getFieldRelations(), "field relations is null");
        Preconditions.checkState(!loadNode.getFieldRelations().isEmpty(),
                "field relations is empty");
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO `").append(loadNode.genTableName()).append("` ");
        sb.append("\n    SELECT ");
        if (loadNode instanceof HbaseLoadNode) {
            parseHbaseLoadFieldRelation((HbaseLoadNode) loadNode, sb);
        } else {
            Map<String, FieldRelation> fieldRelationMap = new HashMap<>(loadNode.getFieldRelations().size());
            loadNode.getFieldRelations().forEach(s -> {
                fieldRelationMap.put(s.getOutputField().getName(), s);
            });
            parseFieldRelations(loadNode.getFields(), fieldRelationMap, sb);
        }
        sb.append("\n    FROM `").append(inputNode.genTableName()).append("`");
        parseFilterFields(loadNode.getFilterStrategy(), loadNode.getFilters(), sb);
        return sb.toString();
    }

    private void parseHbaseLoadFieldRelation(HbaseLoadNode hbaseLoadNode, StringBuilder sb) {
        sb.append(hbaseLoadNode.getRowKey()).append(" as rowkey,\n");
        List<FieldRelation> fieldRelations = hbaseLoadNode.getFieldRelations();
        Map<String, List<FieldRelation>> columnFamilyMapFields = genColumnFamilyMapFieldRelations(
                fieldRelations);
        for (Map.Entry<String, List<FieldRelation>> entry : columnFamilyMapFields.entrySet()) {
            StringBuilder fieldAppend = new StringBuilder(" ROW(");
            for (FieldRelation fieldRelation : entry.getValue()) {
                FieldInfo fieldInfo = (FieldInfo) fieldRelation.getInputField();
                fieldAppend.append(fieldInfo.getName()).append(",");
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
     * @return The create sql pf table
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
        sb.append(genPrimaryKey(node.getPrimaryKey()));
        sb.append(parseFields(node.getFields(), node));
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
     * gen create table DDL for hbase load
     *
     * @param node
     * @return
     */
    private String genCreateHbaseLoadSql(HbaseLoadNode node) {
        StringBuilder sb = new StringBuilder("CREATE TABLE `");
        sb.append(node.genTableName()).append("`(\n");
        sb.append("rowkey STRING,\n");
        List<FieldRelation> fieldRelations = node.getFieldRelations();
        Map<String, List<FieldRelation>> columnFamilyMapFields = genColumnFamilyMapFieldRelations(
                fieldRelations);
        for (Map.Entry<String, List<FieldRelation>> entry : columnFamilyMapFields.entrySet()) {
            sb.append(entry.getKey());
            StringBuilder fieldsAppend = new StringBuilder(" Row<");
            for (FieldRelation fieldRelation : entry.getValue()) {
                FieldInfo fieldInfo = fieldRelation.getOutputField();
                fieldsAppend.append(fieldInfo.getName().split(":")[1]).append(" ")
                        .append(TableFormatUtils.deriveLogicalType(fieldInfo.getFormatInfo()).asSummaryString())
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
            List<FieldRelation> fieldRelations) {
        Map<String, List<FieldRelation>> columnFamilyMapFields = new HashMap<>(16);
        for (FieldRelation fieldRelation : fieldRelations) {
            String columnFamily = fieldRelation.getOutputField().getName().split(":")[0];
            columnFamilyMapFields.computeIfAbsent(columnFamily, v -> new ArrayList<>())
                    .add(fieldRelation);
        }
        return columnFamilyMapFields;
    }

    /**
     * Genrate create transform sql
     *
     * @param node The transform node
     * @return The create sql of transform node
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
     * @return Field format in select sql
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
     * @param node   The abstract of extract, transform, load
     * @return Field format in select sql
     */
    private String parseFields(List<FieldInfo> fields, Node node) {
        StringBuilder sb = new StringBuilder();
        for (FieldInfo field : fields) {
            sb.append("    `").append(field.getName()).append("` ");
            if (field instanceof MetaFieldInfo) {
                MetaFieldInfo metaFieldInfo = (MetaFieldInfo) field;
                parseMetaField(node, metaFieldInfo, sb);
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

    private void parseMetaField(Node node, MetaFieldInfo metaFieldInfo, StringBuilder sb) {
        if (metaFieldInfo.getMetaField() == MetaField.PROCESS_TIME) {
            sb.append(" AS PROCTIME()");
            return;
        }
        if (node instanceof MySqlExtractNode) {
            sb.append(parseMySqlExtractNodeMetaField(metaFieldInfo));
        } else if (node instanceof OracleExtractNode) {
            sb.append(parseOracleExtractNodeMetaField(metaFieldInfo));
        } else if (node instanceof KafkaExtractNode) {
            sb.append(parseKafkaExtractNodeMetaField(metaFieldInfo));
        } else if (node instanceof KafkaLoadNode) {
            sb.append(parseKafkaLoadNodeMetaField(metaFieldInfo));
        } else {
            throw new UnsupportedOperationException(
                    String.format("This node:%s does not currently support metadata fields",
                            node.getClass().getName()));
        }
    }

    private String parseKafkaLoadNodeMetaField(MetaFieldInfo metaFieldInfo) {
        String metaType;
        switch (metaFieldInfo.getMetaField()) {
            case TABLE_NAME:
                metaType = "STRING METADATA FROM 'value.table'";
                break;
            case DATABASE_NAME:
                metaType = "STRING METADATA FROM 'value.database'";
                break;
            case OP_TS:
                metaType = "TIMESTAMP(3) METADATA FROM 'value.event-timestamp'";
                break;
            case OP_TYPE:
                metaType = "STRING METADATA FROM 'value.op-type'";
                break;
            case DATA:
                metaType = "STRING METADATA FROM 'value.data'";
                break;
            case IS_DDL:
                metaType = "BOOLEAN METADATA FROM 'value.is-ddl'";
                break;
            case TS:
                metaType = "TIMESTAMP_LTZ(3) METADATA FROM 'value.ingestion-timestamp'";
                break;
            case SQL_TYPE:
                metaType = "MAP<STRING, INT> METADATA FROM 'value.sql-type'";
                break;
            case MYSQL_TYPE:
                metaType = "MAP<STRING, STRING> METADATA FROM 'value.mysql-type'";
                break;
            case PK_NAMES:
                metaType = "ARRAY<STRING> METADATA FROM 'value.pk-names'";
                break;
            case BATCH_ID:
                metaType = "BIGINT METADATA FROM 'value.batch-id'";
                break;
            case UPDATE_BEFORE:
                metaType = "ARRAY<MAP<STRING, STRING>> METADATA FROM 'value.update-before'";
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupport meta field: %s",
                        metaFieldInfo.getMetaField()));
        }
        return metaType;
    }

    private String parseKafkaExtractNodeMetaField(MetaFieldInfo metaFieldInfo) {
        String metaType;
        switch (metaFieldInfo.getMetaField()) {
            case TABLE_NAME:
                metaType = "STRING METADATA FROM 'value.table'";
                break;
            case DATABASE_NAME:
                metaType = "STRING METADATA FROM 'value.database'";
                break;
            case SQL_TYPE:
                metaType = "MAP<STRING, INT> METADATA FROM 'value.sql-type'";
                break;
            case PK_NAMES:
                metaType = "ARRAY<STRING> METADATA FROM 'value.pk-names'";
                break;
            case TS:
                metaType = "TIMESTAMP_LTZ(3) METADATA FROM 'value.ingestion-timestamp'";
                break;
            case OP_TS:
                metaType = "TIMESTAMP_LTZ(3) METADATA FROM 'value.event-timestamp'";
                break;
            // additional metadata
            case OP_TYPE:
                metaType = "STRING METADATA FROM 'value.op-type'";
                break;
            case IS_DDL:
                metaType = "BOOLEAN METADATA FROM 'value.is-ddl'";
                break;
            case MYSQL_TYPE:
                metaType = "MAP<STRING, STRING> METADATA FROM 'value.mysql-type'";
                break;
            case BATCH_ID:
                metaType = "BIGINT METADATA FROM 'value.batch-id'";
                break;
            case UPDATE_BEFORE:
                metaType = "ARRAY<MAP<STRING, STRING>> METADATA FROM 'value.update-before'";
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupport meta field: %s",
                        metaFieldInfo.getMetaField()));
        }
        return metaType;
    }

    private String parseMySqlExtractNodeMetaField(MetaFieldInfo metaFieldInfo) {
        String metaType;
        switch (metaFieldInfo.getMetaField()) {
            case TABLE_NAME:
                metaType = "STRING METADATA FROM 'meta.table_name' VIRTUAL";
                break;
            case DATABASE_NAME:
                metaType = "STRING METADATA FROM 'meta.database_name' VIRTUAL";
                break;
            case OP_TS:
                metaType = "TIMESTAMP(3) METADATA FROM 'meta.op_ts' VIRTUAL";
                break;
            case OP_TYPE:
                metaType = "STRING METADATA FROM 'meta.op_type' VIRTUAL";
                break;
            case DATA:
                metaType = "STRING METADATA FROM 'meta.data' VIRTUAL";
                break;
            case IS_DDL:
                metaType = "BOOLEAN METADATA FROM 'meta.is_ddl' VIRTUAL";
                break;
            case TS:
                metaType = "TIMESTAMP_LTZ(3) METADATA FROM 'meta.ts' VIRTUAL";
                break;
            case SQL_TYPE:
                metaType = "MAP<STRING, INT> METADATA FROM 'meta.sql_type' VIRTUAL";
                break;
            case MYSQL_TYPE:
                metaType = "MAP<STRING, STRING> METADATA FROM 'meta.mysql_type' VIRTUAL";
                break;
            case PK_NAMES:
                metaType = "ARRAY<STRING> METADATA FROM 'meta.pk_names' VIRTUAL";
                break;
            case BATCH_ID:
                metaType = "BIGINT METADATA FROM 'meta.batch_id' VIRTUAL";
                break;
            case UPDATE_BEFORE:
                metaType = "ARRAY<MAP<STRING, STRING>> METADATA FROM 'meta.update_before' VIRTUAL";
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupport meta field: %s",
                        metaFieldInfo.getMetaField()));
        }
        return metaType;
    }

    private String parseOracleExtractNodeMetaField(MetaFieldInfo metaFieldInfo) {
        String metaType;
        switch (metaFieldInfo.getMetaField()) {
            case TABLE_NAME:
                metaType = "STRING METADATA FROM 'table_name' VIRTUAL";
                break;
            case SCHEMA_NAME:
                metaType = "STRING METADATA FROM 'schema_name' VIRTUAL";
                break;
            case DATABASE_NAME:
                metaType = "STRING METADATA FROM 'database_name' VIRTUAL";
                break;
            case OP_TS:
                metaType = "TIMESTAMP_LTZ(3) METADATA FROM 'op_ts' VIRTUAL";
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupport meta field: %s",
                        metaFieldInfo.getMetaField()));
        }
        return metaType;
    }

    /**
     * Generate primary key format in sql
     *
     * @param primaryKey The primary key of table
     * @return Primary key format in sql
     */
    private String genPrimaryKey(String primaryKey) {
        if (StringUtils.isNotBlank(primaryKey)) {
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
     * @return Field list after format
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
