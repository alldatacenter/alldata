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

package org.apache.inlong.sort.parser;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.test.util.AbstractTestBase;
import org.apache.inlong.sort.formats.common.FloatFormatInfo;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.load.HbaseLoadNode;
import org.apache.inlong.sort.protocol.node.transform.TransformNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.apache.inlong.sort.protocol.transformation.relation.UnionNodeRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test for union sql parse {@link UnionNodeRelation}
 */
public class UnionSqlParseTest extends AbstractTestBase {

    private Node buildMySQLExtractNode(String nodeId) {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        return new MySqlExtractNode(nodeId, "mysql_input_" + nodeId, fields, null, null,
                "id", Collections.singletonList("mysql_table"),
                "localhost", "inlong", "inlong",
                "inlong", null, null, null, null);
    }

    private Node buildTransformNode() {
        List<FieldRelation> relations = new ArrayList<>();
        buildRelations("1", relations);
        buildRelations("2", relations);
        buildRelations("3", relations);
        return new TransformNode("4", "transform_node",
                Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo()),
                        new FieldInfo("age", new IntFormatInfo()),
                        new FieldInfo("salary", new FloatFormatInfo()),
                        new FieldInfo("ts", new TimestampFormatInfo())),
                relations, null, null);
    }

    private Node buildHbaseNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("cf:id", new LongFormatInfo()),
                new FieldInfo("cf1:name", new StringFormatInfo()),
                new FieldInfo("cf1:age", new IntFormatInfo()),
                new FieldInfo("cf2:salary", new FloatFormatInfo()),
                new FieldInfo("cf2:ts", new TimestampFormatInfo()));
        List<FieldRelation> relations = Arrays.asList(
                new FieldRelation(new FieldInfo("id", "1", new LongFormatInfo()),
                        new FieldInfo("cf1:id", new LongFormatInfo())),
                new FieldRelation(new FieldInfo("name", "1", new StringFormatInfo()),
                        new FieldInfo("cf1:name", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("age", "2", new IntFormatInfo()),
                        new FieldInfo("cf2:age", new IntFormatInfo())),
                new FieldRelation(new FieldInfo("ts", "3", new TimestampFormatInfo()),
                        new FieldInfo("cf2:ts", new TimestampFormatInfo())));
        return new HbaseLoadNode("5", "test_hbase",
                fields, relations, null, null, 1, null, "mytable",
                "default", "localhost:2181", "MD5(`name`)", null,
                null, null, null);
    }

    private Node buildHbaseNode2() {
        List<FieldRelation> relations = new ArrayList<>();
        buildRelations("1", relations);
        buildRelations("2", relations);
        buildRelations("3", relations);
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("cf1:id", new LongFormatInfo()),
                new FieldInfo("cf1:name", new StringFormatInfo()),
                new FieldInfo("cf1:age", new IntFormatInfo()),
                new FieldInfo("cf2:salary", new FloatFormatInfo()),
                new FieldInfo("cf2:ts", new TimestampFormatInfo()));
        return new HbaseLoadNode("4", "test_hbase",
                fields, relations, null, null, 1, null, "mytable",
                "default", "localhost:2181", "MD5(`name`)", null,
                null, null, null);
    }

    private void buildRelations(String nodeId, List<FieldRelation> relations) {
        relations.add(new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("cf1:id", nodeId, new LongFormatInfo())));
        relations.add(new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("cf1:name", nodeId, new StringFormatInfo())));
        relations.add(new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("cf2:age", nodeId, new IntFormatInfo())));
        relations.add(new FieldRelation(new FieldInfo("ts", new TimestampFormatInfo()),
                new FieldInfo("cf2:ts", nodeId, new TimestampFormatInfo())));
    }

    public NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    public NodeRelation buildUnionNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new UnionNodeRelation(inputIds, outputIds);
    }

    /**
     * Test union sql parse
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testUnionSqlParse() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode1 = buildMySQLExtractNode("1");
        Node inputNode2 = buildMySQLExtractNode("2");
        Node inputNode3 = buildMySQLExtractNode("3");
        Node transformNode = buildTransformNode();
        Node outputNode = buildHbaseNode();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(inputNode1, inputNode2, inputNode3, transformNode, outputNode),
                Arrays.asList(
                        buildUnionNodeRelation(Arrays.asList(inputNode1, inputNode2, inputNode3),
                                Collections.singletonList(transformNode)),
                        buildNodeRelation(Collections.singletonList(transformNode),
                                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test union sql parse without transform
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testUnionSqlParseWithoutTransform() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode1 = buildMySQLExtractNode("1");
        Node inputNode2 = buildMySQLExtractNode("2");
        Node inputNode3 = buildMySQLExtractNode("3");
        Node outputNode = buildHbaseNode2();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(inputNode1, inputNode2, inputNode3, outputNode),
                Collections.singletonList(
                        buildUnionNodeRelation(Arrays.asList(inputNode1, inputNode2, inputNode3),
                                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }
}
