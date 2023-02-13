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
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.VarBinaryFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.enums.KafkaScanStartupMode;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.KafkaExtractNode;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.format.RawFormat;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test for {@link KafkaLoadNode} sql parse
 * and raw hash partitioner {@link org.apache.inlong.sort.kafka.partitioner.RawDataHashPartitioner}
 */
public class KafkaLoadSqlParseTest extends AbstractTestBase {

    private MySqlExtractNode buildMysqlExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("age", new StringFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()));
        Map<String, String> map = new HashMap<>();
        return new MySqlExtractNode("1", "mysql_input", fields,
                null, map, "age",
                Collections.singletonList("user"), "localhost", "root", "888888",
                "test", null, null,
                true, null);
    }

    private KafkaLoadNode buildKafkaLoadNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("age", new StringFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("age", new StringFormatInfo()),
                        new FieldInfo("age", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())));
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null,
                null, "topic_output", "localhost:9092", new JsonFormat(),
                null, null, null);
    }

    private KafkaExtractNode buildKafkaExtractNode() {
        List<FieldInfo> fields = Collections.singletonList(new FieldInfo("raw", new VarBinaryFormatInfo()));
        return new KafkaExtractNode("1", "kafka_input", fields,
                null, null, "kafka_raw", "localhost:9092",
                new RawFormat(), KafkaScanStartupMode.EARLIEST_OFFSET, null,
                "test_group", null, null);
    }

    private KafkaLoadNode buildKafkaLoadNodeWithDynamicTopic() {
        List<FieldInfo> fields = Collections.singletonList(new FieldInfo("raw", new VarBinaryFormatInfo()));
        List<FieldRelation> relations = Collections
                .singletonList(new FieldRelation(new FieldInfo("raw", new VarBinaryFormatInfo()),
                        new FieldInfo("raw", new VarBinaryFormatInfo())));
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null, null,
                "topic_output", "localhost:9092", new RawFormat(), null,
                null, null, new CanalJsonFormat(), "${database}_${table}",
                null, null);
    }

    private KafkaLoadNode buildKafkaLoadNodeWithDynamicPartition(String pattern) {
        List<FieldInfo> fields = Collections.singletonList(new FieldInfo("raw", new VarBinaryFormatInfo()));
        List<FieldRelation> relations = Collections
                .singletonList(new FieldRelation(new FieldInfo("raw", new VarBinaryFormatInfo()),
                        new FieldInfo("raw", new VarBinaryFormatInfo())));
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null, null,
                "topic_output", "localhost:9092", new RawFormat(), null,
                null, null, new CanalJsonFormat(), null,
                "raw-hash", pattern);
    }

    private KafkaExtractNode buildDirtyKafkaExtractNode() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()));
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("dirty.side-output.connector", "log");
        properties.put("dirty.ignore", "true");
        properties.put("dirty.side-output.enable", "true");
        properties.put("dirty.side-output.format", "csv");
        properties.put("dirty.side-output.labels",
                "SYSTEM_TIME=${SYSTEM_TIME}&DIRTY_TYPE=${DIRTY_TYPE}&database=inlong&table=student");
        JsonFormat jsonFormat = new JsonFormat();
        jsonFormat.setIgnoreParseErrors(false);
        return new KafkaExtractNode("1", "kafka_input", fields,
                null, properties, "topic_dirty_input", "localhost:9092",
                jsonFormat, KafkaScanStartupMode.EARLIEST_OFFSET, null,
                "test_group", null, null);
    }

    private KafkaLoadNode buildDirtyKafkaLoadNode() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()));
        List<FieldRelation> relations = Arrays.asList(
                new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("id", new LongFormatInfo())),
                new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                        new FieldInfo("age", new IntFormatInfo())),
                new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo())));
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("dirty.side-output.connector", "s3");
        properties.put("dirty.ignore", "true");
        properties.put("dirty.side-output.enable", "true");
        properties.put("dirty.side-output.format", "csv");
        properties.put("dirty.side-output.labels",
                "SYSTEM_TIME=${SYSTEM_TIME}&DIRTY_TYPE=${DIRTY_TYPE}&database=inlong&table=student");
        properties.put("dirty.side-output.s3.bucket", "s3-test-bucket");
        properties.put("dirty.side-output.s3.endpoint", "s3.test.endpoint");
        properties.put("dirty.side-output.s3.key", "dirty/test");
        properties.put("dirty.side-output.s3.region", "region");
        properties.put("dirty.side-output.s3.access-key-id", "access_key_id");
        properties.put("dirty.side-output.s3.secret-key-id", "secret_key_id");
        properties.put("dirty.identifier", "inlong-student-${SYSTEM_TIME}");
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null,
                null, "topic_dirty_output", "localhost:9092", new JsonFormat(),
                null, properties, null);
    }

    private KafkaExtractNode buildDirtyKafkaExtractNodeWithRawFormat() {
        List<FieldInfo> fields = Collections.singletonList(new FieldInfo("raw", new VarBinaryFormatInfo()));
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("dirty.side-output.connector", "log");
        properties.put("dirty.ignore", "true");
        properties.put("dirty.side-output.enable", "true");
        properties.put("dirty.side-output.format", "csv");
        properties.put("dirty.side-output.labels",
                "SYSTEM_TIME=${SYSTEM_TIME}&DIRTY_TYPE=${DIRTY_TYPE}&database=inlong&table=student");
        return new KafkaExtractNode("1", "kafka_input", fields,
                null, properties, "topic_dirty_input", "localhost:9092",
                new RawFormat(), KafkaScanStartupMode.EARLIEST_OFFSET, null,
                "test_group", null, null);
    }

    private KafkaLoadNode buildDirtyKafkaLoadNodeWithDynamicTopic() {
        List<FieldInfo> fields = Collections.singletonList(new FieldInfo("raw", new VarBinaryFormatInfo()));
        List<FieldRelation> relations = Collections.singletonList(
                new FieldRelation(new FieldInfo("raw", new StringFormatInfo()),
                        new FieldInfo("raw", new StringFormatInfo())));
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("dirty.side-output.connector", "log");
        properties.put("dirty.ignore", "true");
        properties.put("dirty.side-output.enable", "true");
        properties.put("dirty.side-output.format", "csv");
        properties.put("dirty.side-output.labels",
                "SYSTEM_TIME=${SYSTEM_TIME}&DIRTY_TYPE=${DIRTY_TYPE}&database=${database}&table=${table}");
        properties.put("dirty.identifier", "${database}-${table}-${SYSTEM_TIME}");
        properties.put("dirty.side-output.s3.bucket", "s3-test-bucket");
        properties.put("dirty.side-output.s3.endpoint", "s3.test.endpoint");
        properties.put("dirty.side-output.s3.key", "dirty/test");
        properties.put("dirty.side-output.s3.region", "region");
        properties.put("dirty.side-output.s3.access-key-id", "access_key_id");
        properties.put("dirty.side-output.s3.secret-key-id", "secret_key_id");
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null,
                null, "topic_dirty_output", "localhost:9092", new RawFormat(),
                null, properties, null, new CanalJsonFormat(), "topic_dirty_output",
                null, null);
    }

    /**
     * build node relation
     *
     * @param inputs extract node
     * @param outputs load node
     * @return node relation
     */
    private NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Test mysql to kafka
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testKafkaSourceSqlParse() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildMysqlExtractNode();
        Node outputNode = buildKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test kafka to kafka with dynamic topic
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testKafkaDynamicTopicParse() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildKafkaExtractNode();
        Node outputNode = buildKafkaLoadNodeWithDynamicTopic();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test kafka to kafka with dynamic partition
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testKafkaDynamicPartitionParse() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildKafkaExtractNode();
        Node outputNode = buildKafkaLoadNodeWithDynamicPartition("${database}_${table}");
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test kafka to kafka with dynamic partition based on hash of primary key
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testKafkaDynamicPartitionWithPrimaryKey() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildKafkaExtractNode();
        Node outputNode = buildKafkaLoadNodeWithDynamicPartition("PRIMARY_KEY");
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test dirty handle of kafka source and kafka sink
     * In this part it uses 'log' side-output for kafka source and 's3' side-output for kafka sink
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testKafkaDirtyHandleSqlParse() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildDirtyKafkaExtractNode();
        Node outputNode = buildDirtyKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * Test dirty handle of kafka source and kafka sink with dynamic topic
     * In this part it uses 'log' side-output for kafka source and 's3' side-output for kafka sink
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testKafkaDirtyHandleWithDynamicTopicSqlParse() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        env.disableOperatorChaining();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildDirtyKafkaExtractNodeWithRawFormat();
        Node outputNode = buildDirtyKafkaLoadNodeWithDynamicTopic();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }
}
