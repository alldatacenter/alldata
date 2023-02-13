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
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.formats.common.FloatFormatInfo;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.MetaFieldInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.enums.KafkaScanStartupMode;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.KafkaExtractNode;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.node.transform.DistinctNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.OrderDirection;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;
import org.apache.inlong.sort.protocol.transformation.TimeUnitConstantParam;
import org.apache.inlong.sort.protocol.transformation.TimeUnitConstantParam.TimeUnit;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test for {@link DistinctNode}
 */
public class DistinctNodeSqlParseTest extends AbstractTestBase {

    private KafkaExtractNode buildKafkaExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()),
                new MetaFieldInfo("proctime", MetaField.PROCESS_TIME));
        return new KafkaExtractNode("1", "kafka_input", fields, null,
                null, "topic_input", "localhost:9092",
                new JsonFormat(), KafkaScanStartupMode.EARLIEST_OFFSET,
                null, "groupId", null, null);
    }

    private KafkaExtractNode buildKafkaExtractNode2() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()),
                new MetaFieldInfo("proctime", MetaField.PROCESS_TIME));
        WatermarkField wk = new WatermarkField(new FieldInfo("ts", new TimestampFormatInfo()),
                new StringConstantParam("1"),
                new TimeUnitConstantParam(TimeUnit.SECOND));
        return new KafkaExtractNode("1", "kafka_input", fields, wk,
                null, "topic_input", "localhost:9092",
                new JsonFormat(), KafkaScanStartupMode.EARLIEST_OFFSET,
                null, "groupId", null, null);
    }

    private KafkaExtractNode buildKafkaExtractNode3() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        return new KafkaExtractNode("1", "kafka_input", fields, null,
                null, "topic_input", "localhost:9092",
                new JsonFormat(), KafkaScanStartupMode.EARLIEST_OFFSET, null, "groupId", null, null);
    }

    private KafkaLoadNode buildKafkaLoadNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo())));
        return new KafkaLoadNode("3", "kafka_output", fields, relations, null,
                null, "topic_output", "localhost:9092",
                new JsonFormat(), null,
                null, null);
    }

    private KafkaLoadNode buildKafkaLoadNode2() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo())));
        return new KafkaLoadNode("3", "kafka_output", fields, relations, null,
                null, "topic_output", "localhost:9092",
                new JsonFormat(), null,
                null, "id");
    }

    private KafkaLoadNode buildKafkaLoadNode3() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo())));
        return new KafkaLoadNode("3", "kafka_output", fields, relations, null,
                null, "topic_output", "localhost:9092",
                new JsonFormat(), null,
                null, "id");
    }

    private Node buildTransformNode() {
        return new DistinctNode("2", null,
                Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo()),
                        new FieldInfo("age", new IntFormatInfo()),
                        new FieldInfo("salary", new FloatFormatInfo()),
                        new FieldInfo("ts", new TimestampFormatInfo())),
                Arrays.asList(
                        new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                                new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo()))),
                null, null,
                Collections.singletonList(new FieldInfo("name", new StringFormatInfo())),
                new FieldInfo("proctime", new TimestampFormatInfo()),
                OrderDirection.ASC);
    }

    private Node buildTransformNode2() {
        return new DistinctNode("2", null,
                Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo()),
                        new FieldInfo("age", new IntFormatInfo()),
                        new FieldInfo("salary", new FloatFormatInfo()),
                        new FieldInfo("ts", new TimestampFormatInfo())),
                Arrays.asList(
                        new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                                new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo()))),
                null, null,
                Collections.singletonList(new FieldInfo("name", new StringFormatInfo())),
                new FieldInfo("ts", new TimestampFormatInfo()),
                OrderDirection.ASC);
    }

    private Node buildTransformNode3() {
        return new DistinctNode("2", null,
                Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo()),
                        new FieldInfo("age", new IntFormatInfo()),
                        new FieldInfo("salary", new FloatFormatInfo()),
                        new FieldInfo("ts", new TimestampFormatInfo())),
                Arrays.asList(
                        new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                                new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo()))),
                null, null,
                Collections.singletonList(new FieldInfo("name", new StringFormatInfo())),
                new FieldInfo("ts", new TimestampFormatInfo()),
                OrderDirection.ASC);
    }

    public NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Test distinct based process time
     *
     * @throws Exception The exception may throws when executing
     */
    @Test
    public void testDistinctBasedProcessTime() throws Exception {
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
        Node tansformNode = buildTransformNode();
        Node outputNode = buildKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, tansformNode, outputNode),
                Arrays.asList(
                        buildNodeRelation(Collections.singletonList(inputNode),
                                Collections.singletonList(tansformNode)),
                        buildNodeRelation(Collections.singletonList(tansformNode),
                                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("group_id", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test distinct based a time field
     *
     * @throws Exception The exception may throws when executing
     */
    @Test
    public void testDistinctBasedTimeField() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildKafkaExtractNode3();
        Node tansformNode = buildTransformNode3();
        Node outputNode = buildKafkaLoadNode3();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, tansformNode, outputNode),
                Arrays.asList(
                        buildNodeRelation(Collections.singletonList(inputNode),
                                Collections.singletonList(tansformNode)),
                        buildNodeRelation(Collections.singletonList(tansformNode),
                                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("group_id", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test distinct based event time
     *
     * @throws Exception The exception may throws when executing
     */
    @Test
    public void testDistinctBasedEventTime() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildKafkaExtractNode2();
        Node tansformNode = buildTransformNode2();
        Node outputNode = buildKafkaLoadNode2();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, tansformNode, outputNode),
                Arrays.asList(
                        buildNodeRelation(Collections.singletonList(inputNode),
                                Collections.singletonList(tansformNode)),
                        buildNodeRelation(Collections.singletonList(tansformNode),
                                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("group_id", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

}
