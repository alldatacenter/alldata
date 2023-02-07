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
import org.apache.inlong.sort.formats.common.DoubleFormatInfo;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.LookupOptions;
import org.apache.inlong.sort.protocol.MetaFieldInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.enums.RedisCommand;
import org.apache.inlong.sort.protocol.enums.RedisMode;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.extract.RedisExtractNode;
import org.apache.inlong.sort.protocol.node.load.MySqlLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
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
 * Test for {@link RedisExtractNode} and temporal join {@link FlinkSqlParser}
 */
public class RedisTemporalJoinRelationSqlParseTest extends AbstractTestBase {

    private MySqlExtractNode buildMySQLExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new StringFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new MetaFieldInfo("proc_time", MetaField.PROCESS_TIME));
        Map<String, String> map = new HashMap<>();
        return new MySqlExtractNode("1", "mysql_input", fields,
                null, map, "id",
                Collections.singletonList("mysql_input"), "localhost", "inlong", "inlong",
                "inlong", null, null, null, null);
    }

    private Node buildRedisExtractNode(String id, RedisCommand redisCommand, String additionalKey) {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("k", new StringFormatInfo()),
                new FieldInfo("v", new StringFormatInfo()));
        if (redisCommand == RedisCommand.ZREVRANK) {
            fields.get(1).setFormatInfo(new LongFormatInfo());
        } else if (redisCommand == RedisCommand.ZSCORE) {
            fields.get(1).setFormatInfo(new DoubleFormatInfo());
        }
        return new RedisExtractNode(id, id, fields, null, null, null,
                RedisMode.STANDALONE, redisCommand, null, null, null,
                "localhost", 6379, "inlong", additionalKey, null, null,
                null, null, null, null,
                new LookupOptions(10L, 10000L, 1, false));
    }

    private Node buildMysqlLoadNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new StringFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("nickname", new StringFormatInfo()),
                new FieldInfo("rank", new LongFormatInfo()),
                new FieldInfo("score", new DoubleFormatInfo()));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("id", "1", new StringFormatInfo()),
                        new FieldInfo("id", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("name", "1", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", "1", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("v", "2", new StringFormatInfo()),
                                new FieldInfo("nickname", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("v", "3", new LongFormatInfo()),
                                new FieldInfo("rank", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("v", "4", new DoubleFormatInfo()),
                                new FieldInfo("score", new DoubleFormatInfo())),
                        new FieldRelation(new FieldInfo("v", "5", new StringFormatInfo()),
                                new FieldInfo("address", new StringFormatInfo())));
        return new MySqlLoadNode("6", "mysql_output", fields, relations, null,
                null, null, null, "jdbc:mysql://localhost:3306/inlong",
                "inlong", "inlong", "mysql_output", "id");
    }

    /**
     * build node relation
     *
     * @param inputs extract node
     * @param outputs load node
     * @return node relation
     */
    private TemporalJoinRelation buildNodeRelation(List<Node> inputs, List<Node> outputs, boolean left) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        Map<String, List<FilterFunction>> conditionMap = new TreeMap<>();
        conditionMap.put("2", Collections.singletonList(new SingleValueFilterFunction(EmptyOperator.getInstance(),
                new FieldInfo("id", "1", new LongFormatInfo()),
                EqualOperator.getInstance(), new FieldInfo("k", "2", new StringFormatInfo()))));
        conditionMap.put("3", Collections.singletonList(new SingleValueFilterFunction(EmptyOperator.getInstance(),
                new FieldInfo("id", "1", new StringFormatInfo()),
                EqualOperator.getInstance(), new FieldInfo("k", "3", new StringFormatInfo()))));
        conditionMap.put("4", Collections.singletonList(new SingleValueFilterFunction(EmptyOperator.getInstance(),
                new FieldInfo("id", "1", new LongFormatInfo()),
                EqualOperator.getInstance(), new FieldInfo("k", "4", new StringFormatInfo()))));
        conditionMap.put("5", Collections.singletonList(new SingleValueFilterFunction(EmptyOperator.getInstance(),
                new FieldInfo("id", "1", new LongFormatInfo()),
                EqualOperator.getInstance(), new FieldInfo("k", "5", new StringFormatInfo()))));
        if (left) {
            return new LeftOuterTemporalJoinRelation(inputIds, outputIds, conditionMap,
                    new FieldInfo("proc_time"));
        }
        return new InnerTemporalJoinRelation(inputIds, outputIds, conditionMap, new FieldInfo("proc_time"));
    }

    /**
     * Test left temporal join for extract is mysql {@link MySqlExtractNode}, {@link RedisExtractNode}
     * and load is mysql {@link MySqlLoadNode}
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testLeftTemporalJoinParse() throws Exception {
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
        Node inputNode = buildMySQLExtractNode();
        Node redisDim1 = buildRedisExtractNode("2", RedisCommand.HGET, "student");
        Node redisDim2 = buildRedisExtractNode("3", RedisCommand.ZREVRANK, "student_rank");
        Node redisDim3 = buildRedisExtractNode("4", RedisCommand.ZSCORE, "student_rank");
        Node redisDim4 = buildRedisExtractNode("5", RedisCommand.GET, null);
        Node outputNode = buildMysqlLoadNode();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(inputNode, redisDim1, redisDim2, redisDim3, redisDim4, outputNode),
                Collections.singletonList(
                        buildNodeRelation(Arrays.asList(inputNode, redisDim1, redisDim2, redisDim3, redisDim4),
                                Collections.singletonList(outputNode), true)));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test inner temporal join for extract is mysql {@link MySqlExtractNode}, {@link RedisExtractNode}
     * and load is mysql {@link MySqlLoadNode}
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testInnerTemporalJoinParse() throws Exception {
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
        Node inputNode = buildMySQLExtractNode();
        Node redisDim1 = buildRedisExtractNode("2", RedisCommand.HGET, "student");
        Node redisDim2 = buildRedisExtractNode("3", RedisCommand.ZREVRANK, "student_rank");
        Node redisDim3 = buildRedisExtractNode("4", RedisCommand.ZSCORE, "student_rank");
        Node redisDim4 = buildRedisExtractNode("5", RedisCommand.GET, null);
        Node outputNode = buildMysqlLoadNode();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(inputNode, redisDim1, redisDim2, redisDim3, redisDim4, outputNode),
                Collections.singletonList(
                        buildNodeRelation(Arrays.asList(inputNode, redisDim1, redisDim2, redisDim3, redisDim4),
                                Collections.singletonList(outputNode), false)));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }
}
