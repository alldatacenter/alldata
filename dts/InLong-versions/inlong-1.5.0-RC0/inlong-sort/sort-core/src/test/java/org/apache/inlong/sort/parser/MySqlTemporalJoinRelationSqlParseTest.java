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
import org.apache.inlong.sort.formats.common.DecimalFormatInfo;
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
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;
import org.apache.inlong.sort.protocol.transformation.function.SingleValueFilterFunction;
import org.apache.inlong.sort.protocol.transformation.operator.EmptyOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EqualOperator;
import org.apache.inlong.sort.protocol.transformation.relation.InnerTemporalJoinRelation;
import org.apache.inlong.sort.protocol.transformation.relation.LeftOuterTemporalJoinRelation;
import org.apache.inlong.sort.protocol.transformation.relation.TemporalJoinRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Test for temporal join for with {@link FlinkSqlParser} with {@link MySqlExtractNode}
 */
public class MySqlTemporalJoinRelationSqlParseTest extends AbstractTestBase {

    private KafkaExtractNode buildKafkaExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("price", new DecimalFormatInfo(32, 2)),
                new FieldInfo("currency", new StringFormatInfo()),
                new FieldInfo("order_time", new TimestampFormatInfo(3)),
                new MetaFieldInfo("proc_time", MetaField.PROCESS_TIME));
        return new KafkaExtractNode("1", "kafka_input", fields,
                new WatermarkField(new FieldInfo("order_time", new TimestampFormatInfo(3))),
                null, "orders", "localhost:9092",
                new JsonFormat(), KafkaScanStartupMode.EARLIEST_OFFSET, null,
                "groupId", null, null);
    }

    private MySqlExtractNode buildMySQLExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("currency", new LongFormatInfo()),
                new FieldInfo("conversion_rate", new DecimalFormatInfo(32, 2)),
                new FieldInfo("currency", new StringFormatInfo()),
                new FieldInfo("update_time", new TimestampFormatInfo(3)));
        Map<String, String> map = new HashMap<>();
        return new MySqlExtractNode("2", "mysql_input", fields,
                new WatermarkField(new FieldInfo("update_time", new TimestampFormatInfo(3))), map,
                "currency", Collections.singletonList("currency_rates"), "localhost",
                "inlong", "inlong", "inlong", null,
                null, null, null);
    }

    private MySqlExtractNode buildMySQLExtractNode2() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("currency", new LongFormatInfo()),
                new FieldInfo("conversion_rate", new DecimalFormatInfo(32, 2)),
                new FieldInfo("currency", new StringFormatInfo()),
                new FieldInfo("update_time", new TimestampFormatInfo(3)),
                new MetaFieldInfo("proc_time", MetaField.PROCESS_TIME));
        Map<String, String> map = new HashMap<>();
        return new MySqlExtractNode("2", "mysql_input", fields, map, "currency",
                Collections.singletonList("currency_rates"), "inlong", "inlong",
                "jdbc:mysql://localhost:3306/inlong");
    }

    private KafkaLoadNode buildKafkaLoadNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("price", new DecimalFormatInfo(32, 2)),
                new FieldInfo("currency", new StringFormatInfo()),
                new FieldInfo("order_time", new TimestampFormatInfo(3)),
                new FieldInfo("conversion_rate", new DecimalFormatInfo(32, 2)));
        List<FieldRelation> relations = Arrays.asList(
                new FieldRelation(new FieldInfo("id", "1", new LongFormatInfo()),
                        new FieldInfo("id", new LongFormatInfo())),
                new FieldRelation(new FieldInfo("price", "1", new DecimalFormatInfo(32, 2)),
                        new FieldInfo("price", new DecimalFormatInfo(32, 2))),
                new FieldRelation(new FieldInfo("currency", "1", new StringFormatInfo()),
                        new FieldInfo("currency", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("order_time", "1", new TimestampFormatInfo(3)),
                        new FieldInfo("order_time", new TimestampFormatInfo(3))),
                new FieldRelation(new FieldInfo("conversion_rate", "2", new DecimalFormatInfo(32, 2)),
                        new FieldInfo("conversion_rate", new DecimalFormatInfo(32, 2))));
        return new KafkaLoadNode("3", "kafka_output", fields, relations, null,
                null, "orders_output", "localhost:9092", new CanalJsonFormat(),
                null, null, null);
    }

    /**
     * build node relation
     *
     * @param inputs extract node
     * @param outputs load node
     * @return node relation
     */
    private TemporalJoinRelation buildNodeRelation(List<Node> inputs, List<Node> outputs, boolean left,
            FieldInfo systemTime) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        Map<String, List<FilterFunction>> conditionMap = new TreeMap<>();
        conditionMap.put("2", Collections.singletonList(new SingleValueFilterFunction(EmptyOperator.getInstance(),
                new FieldInfo("currency", "1", new LongFormatInfo()),
                EqualOperator.getInstance(), new FieldInfo("currency", "2", new StringFormatInfo()))));
        if (left) {
            return new LeftOuterTemporalJoinRelation(inputIds, outputIds, conditionMap, systemTime);
        }
        return new InnerTemporalJoinRelation(inputIds, outputIds, conditionMap, systemTime);
    }

    /**
     * Test inner temporal join with event time for extract is mysql {@link MySqlExtractNode},
     * {@link KafkaExtractNode} and load is mysql {@link KafkaLoadNode}
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testInnerTemporalJoinWithEventTimeParse() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node kafkaExtractNode = buildKafkaExtractNode();
        Node mySqlExtractNode2 = buildMySQLExtractNode();
        Node kafkaLoadNode = buildKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(kafkaExtractNode, mySqlExtractNode2, kafkaLoadNode),
                Collections.singletonList(
                        buildNodeRelation(Arrays.asList(kafkaExtractNode, mySqlExtractNode2),
                                Collections.singletonList(kafkaLoadNode), false, new FieldInfo("order_time"))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test left temporal join with event time for extract is mysql {@link MySqlExtractNode},
     * {@link KafkaExtractNode} and load is mysql {@link KafkaLoadNode}
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testLeftTemporalJoinWithEventTimeParse() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node kafkaExtractNode = buildKafkaExtractNode();
        Node mySqlExtractNode2 = buildMySQLExtractNode();
        Node kafkaLoadNode = buildKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(kafkaExtractNode, mySqlExtractNode2, kafkaLoadNode),
                Collections.singletonList(
                        buildNodeRelation(Arrays.asList(kafkaExtractNode, mySqlExtractNode2),
                                Collections.singletonList(kafkaLoadNode), true, new FieldInfo("order_time"))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test inner temporal join with process time for extract is mysql {@link MySqlExtractNode},
     * {@link KafkaExtractNode} and load is mysql {@link KafkaLoadNode}
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testInnerTemporalJoinWithProcessTimeParse() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node kafkaExtractNode = buildKafkaExtractNode();
        Node mySqlExtractNode2 = buildMySQLExtractNode2();
        Node kafkaLoadNode = buildKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(kafkaExtractNode, mySqlExtractNode2, kafkaLoadNode),
                Collections.singletonList(
                        buildNodeRelation(Arrays.asList(kafkaExtractNode, mySqlExtractNode2),
                                Collections.singletonList(kafkaLoadNode), false, new FieldInfo("proc_time"))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test left temporal join with process time for extract is mysql {@link MySqlExtractNode},
     * {@link KafkaExtractNode} and load is mysql {@link KafkaLoadNode}
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testLeftTemporalJoinWithProcessTimeParse() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node kafkaExtractNode = buildKafkaExtractNode();
        Node mySqlExtractNode2 = buildMySQLExtractNode2();
        Node kafkaLoadNode = buildKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(kafkaExtractNode, mySqlExtractNode2, kafkaLoadNode),
                Collections.singletonList(
                        buildNodeRelation(Arrays.asList(kafkaExtractNode, mySqlExtractNode2),
                                Collections.singletonList(kafkaLoadNode), true, new FieldInfo("proc_time"))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }
}
