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
import org.apache.inlong.sort.parser.impl.FlinkSqlParser;
import org.apache.inlong.sort.parser.result.FlinkSqlParseResult;
import org.apache.inlong.sort.formats.common.FloatFormatInfo;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.GroupInfo;
import org.apache.inlong.sort.protocol.StreamInfo;
import org.apache.inlong.sort.protocol.constant.IcebergConstant.CatalogType;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.load.IcebergLoadNode;
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
 * Test for Iceberg SQL parser.
 */
public class IcebergNodeSqlParserTest extends AbstractTestBase {

    private MySqlExtractNode buildMySQLExtractNode(String id) {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()),
                new FieldInfo("event_type", new StringFormatInfo()));
        // if you hope hive load mode of append, please add this config
        Map<String, String> map = new HashMap<>();
        map.put("append-mode", "true");
        return new MySqlExtractNode(id, "mysql_input", fields,
                null, map, "id",
                Collections.singletonList("work1"), "localhost", "root", "123456",
                "inlong", null, null,
                null, null);
    }

    private IcebergLoadNode buildIcebergLoadNodeWithHadoopCatalog() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("salary", new StringFormatInfo()),
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

        return new IcebergLoadNode(
                "iceberg",
                "iceberg_output",
                fields,
                relations,
                null,
                null,
                null,
                null,
                "inlong",
                "inlong_iceberg",
                null,
                CatalogType.HADOOP,
                null,
                "hdfs://localhost:9000/iceberg/warehouse");
    }

    private IcebergLoadNode buildIcebergLoadNodeWithHiveCatalog() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
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
                "SYSTEM_TIME=${SYSTEM_TIME}&DIRTY_TYPE=${DIRTY_TYPE}&database=inlong&table=inlong_iceberg");
        // set HIVE_CONF_DIR,or set uri and warehouse
        return new IcebergLoadNode(
                "iceberg",
                "iceberg_output",
                fields,
                relations,
                null,
                null,
                null,
                properties,
                "inlong",
                "inlong_iceberg",
                null,
                CatalogType.HIVE,
                "thrift://localhost:9083",
                "/hive/warehouse");
    }

    /**
     * build node relation
     *
     * @param inputs  extract node
     * @param outputs load node
     * @return node relation
     */
    private NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    @Test
    public void testIceberg() throws Exception {
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
        Node outputNode = buildIcebergLoadNodeWithHiveCatalog();
        StreamInfo streamInfo = new StreamInfo("1L", Arrays.asList(inputNode, outputNode),
                Arrays.asList(buildNodeRelation(Collections.singletonList(inputNode),
                        Collections.singletonList(outputNode))));
        GroupInfo groupInfo = new GroupInfo("group_id", Collections.singletonList(streamInfo));
        FlinkSqlParser parser = FlinkSqlParser.getInstance(tableEnv, groupInfo);
        FlinkSqlParseResult result = (FlinkSqlParseResult) parser.parse();
        Assert.assertTrue(!result.getLoadSqls().isEmpty() && !result.getCreateTableSqls().isEmpty());
    }
}
