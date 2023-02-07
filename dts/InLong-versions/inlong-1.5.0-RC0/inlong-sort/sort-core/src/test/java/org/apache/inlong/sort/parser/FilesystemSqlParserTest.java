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
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.ParseResult;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.load.FileSystemLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test for {@link FileSystemLoadNode}
 */
public class FilesystemSqlParserTest {

    /**
     * build mysql extract node
     *
     * @return Mysql extract node
     */
    private MySqlExtractNode buildMySQLExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("age", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()));
        Map<String, String> map = new HashMap<>();
        return new MySqlExtractNode("1", "mysql_input", fields,
                null, map, null,
                Collections.singletonList("user"), "localhost", "root", "inlong",
                "test", null, null,
                false, null);
    }

    /**
     * build filesystem load node
     *
     * @return filesystem load node
     */
    private FileSystemLoadNode buildFileSystemLoadNode() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new LongFormatInfo()));
        List<FieldRelation> relations = Arrays
                .asList(new FieldRelation(new FieldInfo("name", new StringFormatInfo()),
                        new FieldInfo("name", new StringFormatInfo())),
                        new FieldRelation(new FieldInfo("age", new IntFormatInfo()),
                                new FieldInfo("age", new LongFormatInfo())));
        Map<String, String> map = new HashMap<>();
        map.put("sink.ignore.changelog", "true");
        return new FileSystemLoadNode("2", "filesystem_node", fields, relations, null, "hdfs://127.0.0.1:8020/", "csv",
                1, map, null, null);
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
     * Test flink sql task for extract is mysql {@link MySqlExtractNode} and load is hbase {@link FileSystemLoadNode}
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
        Node inputNode = buildMySQLExtractNode();
        Node outputNode = buildFileSystemLoadNode();
        StreamInfo streamInfo = new StreamInfo("1", Arrays.asList(inputNode, outputNode),
                Collections.singletonList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("1", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        ParseResult result = parser.parse();
        Assert.assertTrue(result.tryExecute());
    }
}
