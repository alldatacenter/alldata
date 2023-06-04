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
import org.apache.inlong.sort.protocol.enums.KafkaScanStartupMode;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.KafkaExtractNode;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.node.transform.DistinctNode;
import org.apache.inlong.sort.protocol.node.transform.TransformNode;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.OrderDirection;
import org.apache.inlong.sort.protocol.transformation.function.SingleValueFilterFunction;
import org.apache.inlong.sort.protocol.transformation.operator.AndOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EmptyOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.MoreThanOrEqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.NotEqualOperator;
import org.apache.inlong.sort.protocol.transformation.relation.InnerJoinNodeRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Test for {@link InnerJoinNodeRelation}
 */
public class InnerJoinRelationSqlParseTest extends AbstractTestBase {

    /**
     * Build the first kafka extract node
     *
     * @return A kafka extract node
     */
    private KafkaExtractNode buildKafkaExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()));
        return new KafkaExtractNode("1", "kafka_input_1", fields, null,
                null, "topic_input_1", "localhost:9092",
                new JsonFormat(), KafkaScanStartupMode.EARLIEST_OFFSET,
                null, "groupId", null, null);
    }

    /**
     * Build the second kafka extract node
     *
     * @return A kafka extract node
     */
    private KafkaExtractNode buildKafkaExtractNode2() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()));
        return new KafkaExtractNode("2", "kafka_input_2", fields, null,
                null, "topic_input_2", "localhost:9092",
                new JsonFormat(), KafkaScanStartupMode.EARLIEST_OFFSET,
                null, "groupId", null, null);
    }

    /**
     * Build the third kafka extract node
     *
     * @return A kafka extract node
     */
    private KafkaExtractNode buildKafkaExtractNode3() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        return new KafkaExtractNode("3", "kafka_input_3", fields, null,
                null, "topic_input_3", "localhost:9092",
                new JsonFormat(), KafkaScanStartupMode.EARLIEST_OFFSET, null, "groupId", null, null);
    }

    /**
     * Build a kafka load node
     *
     * @return A kafka load node
     */
    private KafkaLoadNode buildKafkaLoadNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("id", "1", new LongFormatInfo()),
                        new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", "1", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", "2", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", "3", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("salary", "3", new TimestampFormatInfo()),
                                new FieldInfo("salary", new TimestampFormatInfo())));
        return new KafkaLoadNode("5", "kafka_output", fields, relations, null,
                null, "topic_output", "localhost:9092",
                new JsonFormat(), null,
                null, null);
    }

    /**
     * Build a upsert kafka load node
     *
     * @return A kafka load node
     */
    private KafkaLoadNode buildKafkaLoadNode2() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("id", "1", new LongFormatInfo()),
                        new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", "1", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", "2", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", "3", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("salary", "3", new TimestampFormatInfo()),
                                new FieldInfo("salary", new TimestampFormatInfo())));
        return new KafkaLoadNode("5", "kafka_output", fields, relations, null,
                null, "topic_output", "localhost:9092",
                new JsonFormat(), null,
                null, "id");
    }

    /**
     * Build a transform node
     *
     * @return A transform node
     */
    private Node buildTransformNode() {
        return new TransformNode("4", "transform_node",
                Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo()),
                        new FieldInfo("age", new IntFormatInfo()),
                        new FieldInfo("salary", new FloatFormatInfo()),
                        new FieldInfo("ts", new TimestampFormatInfo())),
                Arrays.asList(
                        new FieldRelation(new FieldInfo("id", "1", new LongFormatInfo()),
                                new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", "1", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", "2", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", "3", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", "3", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo()))),
                null, null);
    }

    /**
     * Build a distinct node
     *
     * @return A distinct node
     */
    private Node buildDistinctNode() {
        List<FilterFunction> filters = Arrays.asList(
                new SingleValueFilterFunction(EmptyOperator.getInstance(),
                        new FieldInfo("id", "1", new LongFormatInfo()),
                        NotEqualOperator.getInstance(), new ConstantParam("1")),
                new SingleValueFilterFunction(AndOperator.getInstance(),
                        new FieldInfo("age", "2", new IntFormatInfo()),
                        MoreThanOrEqualOperator.getInstance(), new ConstantParam("18")));
        return new DistinctNode("4", "transform_node",
                Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo()),
                        new FieldInfo("age", new IntFormatInfo()),
                        new FieldInfo("salary", new FloatFormatInfo()),
                        new FieldInfo("ts", new TimestampFormatInfo())),
                Arrays.asList(
                        new FieldRelation(new FieldInfo("id", "1", new LongFormatInfo()),
                                new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", "1", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", "2", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", "3", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", "3", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("salary", "3", new TimestampFormatInfo()),
                                new FieldInfo("salary", new TimestampFormatInfo()))),
                filters, null,
                Collections.singletonList(new FieldInfo("name", "1", new StringFormatInfo())),
                new FieldInfo("ts", "3", new TimestampFormatInfo()), OrderDirection.ASC);
    }

    /**
     * Build the node relation
     *
     * @param inputs  The inputs that is the id list of input nodes
     * @param outputs The outputs that is the id list of output nodes
     * @return A NodeRelation
     */
    private NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Build the inner join node relation
     *
     * @param inputs  The inputs that is the id list of input nodes
     * @param outputs The outputs that is the id list of output nodes
     * @return A InnerJoinNodeRelation
     */
    private NodeRelation buildInnerJoinNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        Map<String, List<FilterFunction>> conditionMap = new TreeMap<>();
        conditionMap.put("2", Collections.singletonList(new SingleValueFilterFunction(EmptyOperator.getInstance(),
                new FieldInfo("id", "1", new LongFormatInfo()), EqualOperator.getInstance(),
                new FieldInfo("id", "2", new LongFormatInfo()))));
        conditionMap.put("3", Collections.singletonList(new SingleValueFilterFunction(EmptyOperator.getInstance(),
                new FieldInfo("id", "1", new LongFormatInfo()), EqualOperator.getInstance(),
                new FieldInfo("id", "3", new LongFormatInfo()))));
        return new InnerJoinNodeRelation(inputIds, outputIds, conditionMap);
    }

    /**
     * Test innser join
     * The use case is that three input nodes generate a transformation node through inner join,
     * and then another transformation node is synchronized to the output node
     *
     * @throws Exception The exception may throws when executing
     */
    @Test
    public void testInnerJoin() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildKafkaExtractNode();
        Node inputNode2 = buildKafkaExtractNode2();
        Node inputNode3 = buildKafkaExtractNode3();
        Node tansformNode = buildTransformNode();
        Node outputNode = buildKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(inputNode, inputNode2, inputNode3, tansformNode, outputNode),
                Arrays.asList(
                        buildInnerJoinNodeRelation(Arrays.asList(inputNode, inputNode2, inputNode3),
                                Collections.singletonList(tansformNode)),
                        buildNodeRelation(Collections.singletonList(tansformNode),
                                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("group_id", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test inner join with upsert kafka
     * The same as {@link #testInnerJoin()} , it's just the difference that the output node is upsert-kafka
     *
     * @throws Exception The exception may throws when executing
     * @see #testInnerJoin()
     */
    @Test
    public void testInnerJoinWithUpsertKafka() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildKafkaExtractNode();
        Node inputNode2 = buildKafkaExtractNode2();
        Node inputNode3 = buildKafkaExtractNode3();
        Node tansformNode = buildTransformNode();
        Node outputNode = buildKafkaLoadNode2();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(inputNode, inputNode2, inputNode3, tansformNode, outputNode),
                Arrays.asList(
                        buildInnerJoinNodeRelation(Arrays.asList(inputNode, inputNode2, inputNode3),
                                Collections.singletonList(tansformNode)),
                        buildNodeRelation(Collections.singletonList(tansformNode),
                                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("group_id", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test inner join with distinct and filter
     * The same as {@link #testInnerJoinWithUpsertKafka()} ,
     * it's just the difference that the transform node is {@link DistinctNode} and have filters
     *
     * @throws Exception The exception may throws when executing
     * @see #testInnerJoinWithUpsertKafka()
     */
    @Test
    public void testInnerJoinWithDistinctAndFilter() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildKafkaExtractNode();
        Node inputNode2 = buildKafkaExtractNode2();
        Node inputNode3 = buildKafkaExtractNode3();
        Node tansformNode = buildDistinctNode();
        Node outputNode = buildKafkaLoadNode2();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(inputNode, inputNode2, inputNode3, tansformNode, outputNode),
                Arrays.asList(
                        buildInnerJoinNodeRelation(Arrays.asList(inputNode, inputNode2, inputNode3),
                                Collections.singletonList(tansformNode)),
                        buildNodeRelation(Collections.singletonList(tansformNode),
                                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("group_id", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

}
