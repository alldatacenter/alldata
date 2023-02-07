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
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.MetaFieldInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.OracleExtractNode;
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
 * Test for {@link OracleExtractNode}
 */
public class OracleExtractSqlParseTest extends AbstractTestBase {

    /**
     * Build oracle extract node
     *
     * @return The oracle extract node
     */
    private OracleExtractNode buildOracleExtractNode() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("ID", new LongFormatInfo()),
                new FieldInfo("NAME", new StringFormatInfo()),
                new FieldInfo("AGE", new IntFormatInfo()),
                new MetaFieldInfo("proctime", MetaField.PROCESS_TIME),
                new MetaFieldInfo("database_name", MetaField.DATABASE_NAME),
                new MetaFieldInfo("table_name", MetaField.TABLE_NAME),
                new MetaFieldInfo("op_ts", MetaField.OP_TS),
                new MetaFieldInfo("schema_name", MetaField.SCHEMA_NAME));
        return new OracleExtractNode("1", "oracle_input", fields,
                null, null, "ID", "localhost",
                "flinkuser", "flinkpw", "xE",
                "flinkuser", "table", 1521, null);
    }

    private Node buildKafkaLoadNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("proctime", new TimestampFormatInfo()),
                new FieldInfo("database_name", new StringFormatInfo()),
                new FieldInfo("table_name", new StringFormatInfo()),
                new FieldInfo("op_ts", new TimestampFormatInfo()),
                new FieldInfo("schema_name", new StringFormatInfo()));
        List<FieldRelation> relations = Arrays.asList(
                new FieldRelation(new FieldInfo("ID", new LongFormatInfo()),
                        new FieldInfo("id", new LongFormatInfo())),
                new FieldRelation(new FieldInfo("NAME", new StringFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("AGE", new IntFormatInfo()),
                        new FieldInfo("age", new IntFormatInfo())),
                new FieldRelation(new FieldInfo("proctime", new TimestampFormatInfo()),
                        new FieldInfo("proctime", new TimestampFormatInfo())),
                new FieldRelation(new FieldInfo("database_name", new StringFormatInfo()),
                        new FieldInfo("database_name", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("table_name", new StringFormatInfo()),
                        new FieldInfo("table_name", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("op_ts", new TimestampFormatInfo()),
                        new FieldInfo("op_ts", new TimestampFormatInfo())),
                new FieldRelation(new FieldInfo("schema_name", new StringFormatInfo()),
                        new FieldInfo("schema_name", new StringFormatInfo())));
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null,
                null, "topic", "localhost:9092",
                new CanalJsonFormat(), null,
                null, null);
    }

    /**
     * Build node relation
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
     * Test flink sql task for extract is oracle {@link OracleExtractNode} and load is kafka {@link KafkaLoadNode}
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testOracleExtractSqlParse() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(60000);
        env.disableOperatorChaining();
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildOracleExtractNode();
        Node outputNode = buildKafkaLoadNode();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

}
