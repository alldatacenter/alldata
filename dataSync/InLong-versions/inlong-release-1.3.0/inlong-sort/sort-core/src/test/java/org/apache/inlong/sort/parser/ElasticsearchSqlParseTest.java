/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.flink.test.util.AbstractTestBase;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.load.ElasticsearchLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;

/**
 * test elastic search sql parse
 */
public abstract class ElasticsearchSqlParseTest extends AbstractTestBase {

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

    /**
     * Build elasticsearch node
     *
     * @param version version number
     * @return ElasticsearchLoadNode
     */
    ElasticsearchLoadNode buildElasticsearchLoadNode(int version) {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("age", new StringFormatInfo()),
            new FieldInfo("name", new StringFormatInfo()));
        List<FieldRelation> relations = Arrays
            .asList(new FieldRelation(new FieldInfo("age", new StringFormatInfo()),
                    new FieldInfo("age", new StringFormatInfo())),
                new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                    new FieldInfo("name", new StringFormatInfo())));
        CsvFormat csvFormat = new CsvFormat();
        csvFormat.setDisableQuoteCharacter(true);
        return new ElasticsearchLoadNode("2", "kafka_output", fields, relations, null, null,
            2, null,
            "test", "http://localhost:9200",
            "elastic", "my_password", null, "age", version);
    }

    private NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Test mysql to elasticsearch
     *
     * @throws Exception The exception may throws when execute the case
     */
    public void testMysqlToElasticsearch(Node node) throws Exception {
        EnvironmentSettings settings = EnvironmentSettings
            .newInstance()
            .useBlinkPlanner()
            .inStreamingMode()
            .build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(10000);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);
        Node inputNode = buildMysqlExtractNode();
        Node outputNode = node;
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
            Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }

}
