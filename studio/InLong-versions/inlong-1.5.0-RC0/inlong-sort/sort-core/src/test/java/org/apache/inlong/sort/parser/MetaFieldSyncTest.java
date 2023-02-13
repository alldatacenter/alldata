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
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
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
 * Test for meta field sync
 */
public class MetaFieldSyncTest extends AbstractTestBase {

    private Node buildMySQLExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()),
                new MetaFieldInfo("database", MetaField.DATABASE_NAME),
                new MetaFieldInfo("table", MetaField.TABLE_NAME),
                new MetaFieldInfo("pk_names", MetaField.PK_NAMES),
                new MetaFieldInfo("event_time", MetaField.OP_TS),
                new MetaFieldInfo("event_type", MetaField.OP_TYPE),
                new MetaFieldInfo("isddl", MetaField.IS_DDL),
                new MetaFieldInfo("batch_id", MetaField.BATCH_ID),
                new MetaFieldInfo("mysql_type", MetaField.MYSQL_TYPE),
                new MetaFieldInfo("sql_type", MetaField.SQL_TYPE),
                new MetaFieldInfo("meta_ts", MetaField.TS),
                new MetaFieldInfo("up_before", MetaField.UPDATE_BEFORE));
        return new MySqlExtractNode("1", "mysql_input", fields, null, null,
                "id", Collections.singletonList("mysql_table"),
                "localhost", "inlong", "inlong",
                "inlong", null, null, null, null);
    }

    private Node buildKafkaLoadNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()),
                new MetaFieldInfo("database", MetaField.DATABASE_NAME),
                new MetaFieldInfo("table", MetaField.TABLE_NAME),
                new MetaFieldInfo("pk_names", MetaField.PK_NAMES),
                new MetaFieldInfo("event_time", MetaField.OP_TS),
                new MetaFieldInfo("event_type", MetaField.OP_TYPE),
                new MetaFieldInfo("isddl", MetaField.IS_DDL),
                new MetaFieldInfo("batch_id", MetaField.BATCH_ID),
                new MetaFieldInfo("mysql_type", MetaField.MYSQL_TYPE),
                new MetaFieldInfo("sql_type", MetaField.SQL_TYPE),
                new MetaFieldInfo("meta_ts", MetaField.TS),
                new MetaFieldInfo("up_before", MetaField.UPDATE_BEFORE));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("database", new TimestampFormatInfo()),
                                new FieldInfo("database", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("table", new TimestampFormatInfo()),
                                new FieldInfo("table", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("pk_names", new TimestampFormatInfo()),
                                new FieldInfo("pk_names", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("event_time", new TimestampFormatInfo()),
                                new FieldInfo("event_time", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("event_type", new TimestampFormatInfo()),
                                new FieldInfo("event_type", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("isddl", new TimestampFormatInfo()),
                                new FieldInfo("isddl", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("batch_id", new TimestampFormatInfo()),
                                new FieldInfo("batch_id", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("mysql_type", new TimestampFormatInfo()),
                                new FieldInfo("mysql_type", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("sql_type", new TimestampFormatInfo()),
                                new FieldInfo("sql_type", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("meta_ts", new TimestampFormatInfo()),
                                new FieldInfo("meta_ts", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("up_before", new TimestampFormatInfo()),
                                new FieldInfo("up_before", new TimestampFormatInfo())));
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null,
                null, "topic1", "localhost:9092",
                new CanalJsonFormat(), null,
                null, "id");
    }

    private KafkaExtractNode buildKafkaExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()),
                new MetaFieldInfo("database", MetaField.DATABASE_NAME),
                new MetaFieldInfo("table", MetaField.TABLE_NAME),
                new MetaFieldInfo("pk_names", MetaField.PK_NAMES),
                new MetaFieldInfo("event_time", MetaField.OP_TS),
                new MetaFieldInfo("event_type", MetaField.OP_TYPE),
                new MetaFieldInfo("isddl", MetaField.IS_DDL),
                new MetaFieldInfo("batch_id", MetaField.BATCH_ID),
                new MetaFieldInfo("mysql_type", MetaField.MYSQL_TYPE),
                new MetaFieldInfo("sql_type", MetaField.SQL_TYPE),
                new MetaFieldInfo("meta_ts", MetaField.TS),
                new MetaFieldInfo("up_before", MetaField.UPDATE_BEFORE));
        return new KafkaExtractNode("3", "kafka_input", fields,
                null, null, "topic1", "localhost:9092",
                new CanalJsonFormat(), KafkaScanStartupMode.EARLIEST_OFFSET,
                null, "groupId", null, null);
    }

    private Node buildKafkaLoadNode2() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()),
                new MetaFieldInfo("database", MetaField.DATABASE_NAME),
                new MetaFieldInfo("table", MetaField.TABLE_NAME),
                new MetaFieldInfo("pk_names", MetaField.PK_NAMES),
                new MetaFieldInfo("event_time", MetaField.OP_TS),
                new MetaFieldInfo("event_type", MetaField.OP_TYPE),
                new MetaFieldInfo("isddl", MetaField.IS_DDL),
                new MetaFieldInfo("batch_id", MetaField.BATCH_ID),
                new MetaFieldInfo("mysql_type", MetaField.MYSQL_TYPE),
                new MetaFieldInfo("sql_type", MetaField.SQL_TYPE),
                new MetaFieldInfo("meta_ts", MetaField.TS),
                new MetaFieldInfo("up_before", MetaField.UPDATE_BEFORE));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("id", new LongFormatInfo()),
                        new FieldInfo("id", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                                new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("age", new IntFormatInfo())),
                        new FieldRelation(new FieldInfo("ts", new TimestampFormatInfo()),
                                new FieldInfo("ts", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("database", new TimestampFormatInfo()),
                                new FieldInfo("database", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("table", new TimestampFormatInfo()),
                                new FieldInfo("table", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("pk_names", new TimestampFormatInfo()),
                                new FieldInfo("pk_names", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("event_time", new TimestampFormatInfo()),
                                new FieldInfo("event_time", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("event_type", new TimestampFormatInfo()),
                                new FieldInfo("event_type", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("isddl", new TimestampFormatInfo()),
                                new FieldInfo("isddl", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("batch_id", new TimestampFormatInfo()),
                                new FieldInfo("batch_id", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("mysql_type", new TimestampFormatInfo()),
                                new FieldInfo("mysql_type", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("sql_type", new TimestampFormatInfo()),
                                new FieldInfo("sql_type", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("meta_ts", new TimestampFormatInfo()),
                                new FieldInfo("meta_ts", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("up_before", new TimestampFormatInfo()),
                                new FieldInfo("up_before", new TimestampFormatInfo())));
        return new KafkaLoadNode("4", "kafka_output2", fields, relations, null,
                null, "topic2", "localhost:9092",
                new CanalJsonFormat(), null,
                null, "id");
    }

    public NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Test meta field sync test
     * It contains mysql cdc to kafka canal-json, kafka canal-json to kafka canal-json test
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testMetaFieldSyncTest() throws Exception {
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
        Node kafkaOutputNode = buildKafkaLoadNode();
        Node kafkaInputNode = buildKafkaExtractNode();
        Node kafkaOutputNode2 = buildKafkaLoadNode2();
        StreamInfo streamInfo = new StreamInfo("1",
                Arrays.asList(mysqlInputNode, kafkaInputNode, kafkaOutputNode, kafkaOutputNode2),
                Arrays.asList(
                        buildNodeRelation(Collections.singletonList(mysqlInputNode),
                                Collections.singletonList(kafkaOutputNode)),
                        buildNodeRelation(Collections.singletonList(kafkaInputNode),
                                Collections.singletonList(kafkaOutputNode2))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }
}
