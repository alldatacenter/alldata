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
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.function.SingleValueFilterFunction;
import org.apache.inlong.sort.protocol.transformation.operator.AndOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EmptyOperator;
import org.apache.inlong.sort.protocol.transformation.operator.LessThanOperator;
import org.apache.inlong.sort.protocol.transformation.operator.MoreThanOrEqualOperator;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test for filter parse
 */
public class FilterParseTest extends AbstractTestBase {

    private Node buildMySQLExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        return new MySqlExtractNode("1", "mysql_input", fields, null, null,
                "id", Collections.singletonList("mysql_table"),
                "localhost", "inlong", "inlong",
                "inlong", null, null, null, null);
    }

    private Node buildKafkaLoadNode(FilterStrategy filterStrategy) {
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
        List<FilterFunction> filters = Arrays.asList(
                new SingleValueFilterFunction(EmptyOperator.getInstance(),
                        new FieldInfo("age", new IntFormatInfo()),
                        LessThanOperator.getInstance(), new ConstantParam(25)),
                new SingleValueFilterFunction(AndOperator.getInstance(),
                        new FieldInfo("age", new IntFormatInfo()),
                        MoreThanOrEqualOperator.getInstance(), new ConstantParam(18)));
        return new KafkaLoadNode("2", "kafka_output", fields, relations, filters,
                filterStrategy, "topic1", "localhost:9092",
                new CanalJsonFormat(), null,
                null, "id");
    }

    public NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Test filter with the strategy {@link FilterStrategy#RETAIN}
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testFilterWithRetainParse() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node mysqlInputNode = buildMySQLExtractNode();
        Node kafkaOutputNode = buildKafkaLoadNode(FilterStrategy.RETAIN);
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(mysqlInputNode, kafkaOutputNode),
                Collections.singletonList(
                        buildNodeRelation(Collections.singletonList(mysqlInputNode),
                                Collections.singletonList(kafkaOutputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test filter with the strategy {@link FilterStrategy#REMOVE}
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testFilterWithRemoveParse() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node mysqlInputNode = buildMySQLExtractNode();
        Node kafkaOutputNode = buildKafkaLoadNode(FilterStrategy.REMOVE);
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(mysqlInputNode, kafkaOutputNode),
                Collections.singletonList(
                        buildNodeRelation(Collections.singletonList(mysqlInputNode),
                                Collections.singletonList(kafkaOutputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }
}
