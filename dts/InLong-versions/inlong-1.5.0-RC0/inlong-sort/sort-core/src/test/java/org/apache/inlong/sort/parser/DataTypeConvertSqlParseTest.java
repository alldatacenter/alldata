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
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.MetaFieldInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.enums.KafkaScanStartupMode;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.KafkaExtractNode;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.load.HbaseLoadNode;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.node.transform.TransformNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test data type convert implicitly {@link FlinkSqlParser}
 */
public class DataTypeConvertSqlParseTest extends AbstractTestBase {

    private KafkaExtractNode buildKafkaExtractNode() {
        MetaFieldInfo metaFieldInfo = new MetaFieldInfo("PROCESS_TIME", MetaField.PROCESS_TIME);
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                metaFieldInfo);
        return new KafkaExtractNode("1", "kafka_input", fields, null,
                null, "topic_input", "localhost:9092",
                new CanalJsonFormat(), KafkaScanStartupMode.EARLIEST_OFFSET,
                null, "group_1", null, null);
    }

    /**
     * Build a transform node
     *
     * @return A transform node
     */
    private Node buildTransformNode() {
        return new TransformNode("4", "transform_node",
                Arrays.asList(
                        new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("age", new IntFormatInfo()),
                        new FieldInfo("PROCESS_TIME", null)),
                Arrays.asList(
                        new FieldRelation(new FieldInfo("id", "1", new LongFormatInfo()),
                                new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("age", "2", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("PROCESS_TIME", "3", null),
                                new FieldInfo("PROCESS_TIME", null))),
                null, null);
    }

    private KafkaLoadNode buildKafkaLoadNode() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("id", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("id", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())));
        return new KafkaLoadNode("3", "kafka_output", fields, relations, null,
                null, "topic_output", "localhost:9092",
                new CanalJsonFormat(), 1,
                null, null);
    }

    public NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Test data type convert implicitly
     *
     * @throws Exception The exception may throws when executing
     */
    @Test
    public void testDataTypeConvertSqlParse() throws Exception {
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
        Node transformNode = buildTransformNode();
        Node outputNode = buildKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(inputNode, transformNode, outputNode),
                Arrays.asList(
                        buildNodeRelation(Collections.singletonList(inputNode),
                                Collections.singletonList(transformNode)),
                        buildNodeRelation(Collections.singletonList(transformNode),
                                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    /**
     * build hbase load node
     *
     * @return hbase load node
     */
    private HbaseLoadNode buildHbaseLoadNode() {
        return new HbaseLoadNode("2", "test_hbase",
                Arrays.asList(new FieldInfo("cf:id", new StringFormatInfo()), new FieldInfo("cf:age",
                        new StringFormatInfo())),
                Arrays.asList(new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("cf:id", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("cf:age", new StringFormatInfo()))),
                null, null, 1, null, "mytable",
                "default",
                "localhost:2181", "MD5(CAST(`id` as String))", null, null, null, null);
    }

    /**
     * Test data type convert implicitly for sinking data into HBase
     *
     * @throws Exception The exception may throws when executing
     */
    @Test
    public void testHBaseDataTypeConvertSqlParse() throws Exception {
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
        Node outputNode = buildHbaseLoadNode();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(inputNode, outputNode),
                Arrays.asList(
                        buildNodeRelation(Collections.singletonList(inputNode),
                                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }
}
