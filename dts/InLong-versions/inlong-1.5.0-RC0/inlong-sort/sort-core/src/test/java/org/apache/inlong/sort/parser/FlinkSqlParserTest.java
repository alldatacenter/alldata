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
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.load.FileSystemLoadNode;
import org.apache.inlong.sort.protocol.node.load.HiveLoadNode;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;
import org.apache.inlong.sort.protocol.transformation.TimeUnitConstantParam;
import org.apache.inlong.sort.protocol.transformation.TimeUnitConstantParam.TimeUnit;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;
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
 * Flink sql parser unit test class
 */
public class FlinkSqlParserTest extends AbstractTestBase {

    private MySqlExtractNode buildMySQLExtractNode(String id) {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()),
                new FieldInfo("event_type", new StringFormatInfo()));
        // if you hope hive load mode of append,please add this config.
        Map<String, String> map = new HashMap<>();
        map.put("append-mode", "true");
        return new MySqlExtractNode(id, "mysql_input", fields,
                null, map, "id",
                Collections.singletonList("work1"), "localhost", "root", "password",
                "inlong", null, null,
                null, null);
    }

    private KafkaExtractNode buildKafkaExtractNode(String id) {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        WatermarkField wk = new WatermarkField(new FieldInfo("ts", new TimestampFormatInfo()),
                new StringConstantParam("5"),
                new TimeUnitConstantParam(TimeUnit.SECOND));
        return new KafkaExtractNode(id, "kafka_input", fields, wk, null, "workerJson",
                "localhost:9092", new JsonFormat(), KafkaScanStartupMode.EARLIEST_OFFSET, null, "groupId", null, null);
    }

    private KafkaLoadNode buildKafkaNode(String id) {
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
        return new KafkaLoadNode(id, "kafka_output", fields, relations, null, null,
                "workerJson", "localhost:9092",
                new JsonFormat(), null,
                null, null);
    }

    private NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    private HiveLoadNode buildHiveNode(String id) {
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
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("dirty.side-output.connector", "log");
        properties.put("dirty.ignore", "true");
        properties.put("dirty.side-output.enable", "true");
        properties.put("dirty.side-output.format", "csv");
        properties.put("dirty.side-output.labels",
                "SYSTEM_TIME=${SYSTEM_TIME}&DIRTY_TYPE=${DIRTY_TYPE}&database=default&table=work2");
        return new HiveLoadNode(id, "hive_output",
                fields, relations, null, null, 1,
                properties, "myCatalog", "default", "work2",
                "/opt/hive/conf", "3.1.3",
                null, null);
    }

    private FileSystemLoadNode buildFileSystemNode(String id) {
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
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("dirty.side-output.connector", "log");
        properties.put("dirty.ignore", "true");
        properties.put("dirty.side-output.enable", "true");
        properties.put("dirty.side-output.format", "csv");
        properties.put("dirty.side-output.labels",
                "SYSTEM_TIME=${SYSTEM_TIME}&DIRTY_TYPE=${DIRTY_TYPE}&database=inlong&table=student");
        return new FileSystemLoadNode(id, "hdfs_output", fields, relations,
                null, "hdfs://localhost:9000/inlong/student", "json",
                1, properties, null, null);
    }

    /**
     * Test flink sql mysql cdc to hive
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testMysqlToHive() {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node mysqlExtractNode = buildMySQLExtractNode("1_1");
        Node kafkaExtractNode = buildKafkaExtractNode("1_2");
        Node kafkaNode = buildKafkaNode("2_1");
        Node hiveNode = buildHiveNode("2_2");
        // mysql-->hive
        StreamInfo streamInfoMySqlToHive = new StreamInfo("1L", Arrays.asList(mysqlExtractNode, hiveNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(mysqlExtractNode),
                        Collections.singletonList(hiveNode))));
        GroupInfo groupInfoMySqlToHive = new GroupInfo("1", Collections.singletonList(streamInfoMySqlToHive));
        // mysql-->kafka--kafka-->hive
        StreamInfo streamInfoMySqlToKafkaToHive1 = new StreamInfo("1L", Arrays.asList(mysqlExtractNode, kafkaNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(mysqlExtractNode),
                        Collections.singletonList(kafkaNode))));
        StreamInfo streamInfoMySqlToKafkaToHive2 = new StreamInfo("2L", Arrays.asList(kafkaExtractNode, hiveNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(kafkaExtractNode),
                        Collections.singletonList(hiveNode))));
        GroupInfo groupInfoMySqlToKafkaToHive = new GroupInfo("1",
                Arrays.asList(streamInfoMySqlToKafkaToHive1, streamInfoMySqlToKafkaToHive2));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfoMySqlToKafkaToHive);
        parser.parse();
    }

    /**
     * Test flink sql mysql cdc to file system
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testToFileSystem() {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node mysqlExtractNode = buildMySQLExtractNode("1");
        Node fileSystemNode = buildFileSystemNode("2");
        // mysql-->HDFS OR LOCAL FILE
        StreamInfo streamInfoToHDFS = new StreamInfo("1L", Arrays.asList(mysqlExtractNode, fileSystemNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(mysqlExtractNode),
                        Collections.singletonList(fileSystemNode))));
        GroupInfo groupInfoToHDFS = new GroupInfo("1", Collections.singletonList(streamInfoToHDFS));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfoToHDFS);
        parser.parse();
    }

    /**
     * Test flink sql parse
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testFlinkSqlParse() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildMySQLExtractNode("1");
        Node outputNode = buildKafkaNode("2");
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }
}
