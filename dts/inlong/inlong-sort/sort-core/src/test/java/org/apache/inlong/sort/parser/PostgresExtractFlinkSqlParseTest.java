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
import org.apache.inlong.sort.protocol.node.extract.PostgresExtractNode;
import org.apache.inlong.sort.protocol.node.load.HbaseLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test for {@link PostgresExtractNode}
 */
public class PostgresExtractFlinkSqlParseTest extends AbstractTestBase {

    /**
     * build postgres extract node
     *
     * @return
     */
    private PostgresExtractNode buildPostgresNode() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new MetaFieldInfo("proctime", MetaField.PROCESS_TIME),
                new MetaFieldInfo("database_name", MetaField.DATABASE_NAME),
                new MetaFieldInfo("table_name", MetaField.TABLE_NAME),
                new MetaFieldInfo("op_ts", MetaField.OP_TS),
                new MetaFieldInfo("schema_name", MetaField.SCHEMA_NAME));
        return new PostgresExtractNode("1", "postgres_input", fields, null, null, null, Arrays.asList("user"),
                "localhost", "postgres", "inlong", "postgres", "public", 5432, null, null, "initial");
    }

    /**
     * build hbase load node
     *
     * @return hbase load node
     */
    private HbaseLoadNode buildHbaseLoadNode() {
        return new HbaseLoadNode("2", "hbase_output",
                Arrays.asList(
                        new FieldInfo("cf:age", new LongFormatInfo()),
                        new FieldInfo("cf:name", new StringFormatInfo()),
                        new FieldInfo("cf:proctime", new TimestampFormatInfo()),
                        new FieldInfo("cf:database_name", new StringFormatInfo()),
                        new FieldInfo("cf:table_name", new StringFormatInfo()),
                        new FieldInfo("cf:op_ts", new TimestampFormatInfo()),
                        new FieldInfo("cf:schema_name", new StringFormatInfo())),
                Arrays.asList(
                        new FieldRelation(new FieldInfo("age", new LongFormatInfo()),
                                new FieldInfo("cf:age", new LongFormatInfo())),
                        new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                                new FieldInfo("cf:name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("proctime", new TimestampFormatInfo()),
                                new FieldInfo("cf:proctime", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("database_name", new StringFormatInfo()),
                                new FieldInfo("cf:database_name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("table_name", new StringFormatInfo()),
                                new FieldInfo("cf:table_name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("op_ts", new TimestampFormatInfo()),
                                new FieldInfo("cf:op_ts", new TimestampFormatInfo())),
                        new FieldRelation(new FieldInfo("schema_name", new StringFormatInfo()),
                                new FieldInfo("cf:schema_name", new StringFormatInfo()))),
                null, null, 1, null, "user",
                "default",
                "localhost:2181", "MD5(`name`)", null, "/hbase", null, null);
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
     * Test flink sql task for extract is postgres {@link PostgresExtractNode} and load is hbase {@link HbaseLoadNode}
     *
     * @throws Exception The exception may be thrown when executing
     */
    @Test
    public void testFlinkSqlParse() throws Exception {
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
        Node inputNode = buildPostgresNode();
        Node outputNode = buildHbaseLoadNode();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

}
