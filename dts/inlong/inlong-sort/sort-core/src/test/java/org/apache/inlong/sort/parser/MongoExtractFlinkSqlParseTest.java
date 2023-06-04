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
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.MetaFieldInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.MongoExtractNode;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test for mongodb extract node
 */
public class MongoExtractFlinkSqlParseTest extends AbstractTestBase {

    private MongoExtractNode buildMongoNode() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new MetaFieldInfo("proctime", MetaField.PROCESS_TIME),
                new MetaFieldInfo("database_name", MetaField.DATABASE_NAME),
                new MetaFieldInfo("collection_name", MetaField.COLLECTION_NAME),
                new MetaFieldInfo("op_ts", MetaField.OP_TS));
        return new MongoExtractNode("1", "mysql_input", fields,
                null, null, "test", "localhost:27017",
                "root", "inlong", "test");
    }

    private KafkaLoadNode buildKafkaLoadNode() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("_id", new StringFormatInfo()),
                new FieldInfo("proctime", new TimestampFormatInfo()),
                new FieldInfo("database_name", new StringFormatInfo()),
                new FieldInfo("collection_name", new StringFormatInfo()),
                new FieldInfo("op_ts", new TimestampFormatInfo()));
        List<FieldRelation> relations = Arrays.asList(
                new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("_id", new StringFormatInfo()),
                        new FieldInfo("_id", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("proctime", new TimestampFormatInfo()),
                        new FieldInfo("proctime", new TimestampFormatInfo())),
                new FieldRelation(new FieldInfo("database_name", new StringFormatInfo()),
                        new FieldInfo("database_name", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("collection_name", new StringFormatInfo()),
                        new FieldInfo("collection_name", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("op_ts", new TimestampFormatInfo()),
                        new FieldInfo("op_ts", new TimestampFormatInfo())));
        CsvFormat csvFormat = new CsvFormat();
        csvFormat.setDisableQuoteCharacter(true);
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null, null,
                "test", "localhost:9092",
                csvFormat, null,
                null, "_id");
    }

    private NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Test mongodb to kafka
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testMongoDbToKafka() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildMongoNode();
        Node outputNode = buildKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

    private MongoExtractNode buildComplexTypeMongoNode() {
        String[] fieldNames = new String[]{"name", "age"};
        FormatInfo[] fieldFormatInfos = new FormatInfo[]{new StringFormatInfo(), new StringFormatInfo()};
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("info", new RowFormatInfo(fieldNames, fieldFormatInfos)),
                new MetaFieldInfo("proctime", MetaField.PROCESS_TIME),
                new MetaFieldInfo("database_name", MetaField.DATABASE_NAME),
                new MetaFieldInfo("collection_name", MetaField.COLLECTION_NAME),
                new MetaFieldInfo("op_ts", MetaField.OP_TS));
        return new MongoExtractNode("1", "mysql_input", fields,
                null, null, "test", "localhost:27017",
                "root", "inlong", "test");
    }

    private KafkaLoadNode buildComplexTypeKafkaLoadNode() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("_id", new StringFormatInfo()),
                new FieldInfo("proctime", new TimestampFormatInfo()),
                new FieldInfo("database_name", new StringFormatInfo()),
                new FieldInfo("collection_name", new StringFormatInfo()),
                new FieldInfo("op_ts", new TimestampFormatInfo()));
        List<FieldRelation> relations = Arrays.asList(
                new FieldRelation(new FieldInfo("info.name", new StringFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("_id", new StringFormatInfo()),
                        new FieldInfo("_id", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("proctime", new TimestampFormatInfo()),
                        new FieldInfo("proctime", new TimestampFormatInfo())),
                new FieldRelation(new FieldInfo("database_name", new StringFormatInfo()),
                        new FieldInfo("database_name", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("collection_name", new StringFormatInfo()),
                        new FieldInfo("collection_name", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("op_ts", new TimestampFormatInfo()),
                        new FieldInfo("op_ts", new TimestampFormatInfo())));
        CsvFormat csvFormat = new CsvFormat();
        csvFormat.setDisableQuoteCharacter(true);
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null, null,
                "test", "localhost:9092",
                csvFormat, null,
                null, "_id");
    }

    /**
     * Test mongodb to kafka
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testMongoDbComplexTypeToKafka() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildComplexTypeMongoNode();
        Node outputNode = buildComplexTypeKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }
}
