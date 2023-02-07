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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.MetaFieldInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.PostgresExtractNode;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.load.DorisLoadNode;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;
import org.junit.Test;

/**
 * A demo of transferring data between PostgreSQL Server and other database, using postgres-cdc-inlong connector.
 */
public class AllMigratePostgreSQLTest {

    private PostgresExtractNode buildAllMigrateExtractNode() {
        List<FieldInfo> fields = Arrays.asList(
                new MetaFieldInfo("data", MetaField.DATA_CANAL));
        Map<String, String> option = new HashMap<>();
        option.put("source.multiple.enable", "true");
        List<String> tableNames = Arrays.asList("table");
        // List<String> tableNames = Arrays.asList("*");
        PostgresExtractNode node = new PostgresExtractNode("1", "pg_input", fields,
                null, option, null, tableNames, "localhost", "user",
                "password", "db", "schema", 1234,
                null, "Asia/Shanghai", "initial");
        return node;
    }

    private KafkaLoadNode buildAllMigrateKafkaNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("data", new StringFormatInfo()));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("data", new StringFormatInfo()),
                        new FieldInfo("data", new StringFormatInfo())));
        CsvFormat csvFormat = new CsvFormat();
        csvFormat.setDisableQuoteCharacter(true);
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null, null,
                "pg_db", "localhost:9092",
                csvFormat, null,
                null, null);
    }

    private DorisLoadNode buildAllMigrateDorisNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("data", new StringFormatInfo()));
        List<FieldRelation> relations = Arrays.asList(new FieldRelation(new FieldInfo("data", new StringFormatInfo()),
                new FieldInfo("data", new StringFormatInfo())));
        return new DorisLoadNode("2", "doris_output", fields, relations, null, null,
                null, null, "localhost:1234", "user", "password",
                null, null, true, new CanalJsonFormat(),
                "${database}", "${table}");
    }

    private NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Test flink sql parse
     *
     * @throws Exception The exception may throws when execute the case
     */
    @Test
    public void testAllMigrate() throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildAllMigrateExtractNode();
        Node outputNode = buildAllMigrateKafkaNode();
        // Node outputNode = buildAllMigrateDorisNode();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

}
