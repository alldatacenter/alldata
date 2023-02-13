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

package org.apache.inlong.sort.protocol;

import org.apache.inlong.sort.SerializeBaseTest;
import org.apache.inlong.sort.formats.common.FloatFormatInfo;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.load.HiveLoadNode;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;
import org.apache.inlong.sort.protocol.transformation.TimeUnitConstantParam;
import org.apache.inlong.sort.protocol.transformation.TimeUnitConstantParam.TimeUnit;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StreamInfo unit test class
 */
public class StreamInfoTest extends SerializeBaseTest<StreamInfo> {

    private MySqlExtractNode buildMySqlExtractNode() {
        List<FieldInfo> fields = Arrays.asList(new FieldInfo("id", new LongFormatInfo()),
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()),
                new FieldInfo("salary", new FloatFormatInfo()),
                new FieldInfo("ts", new TimestampFormatInfo()));
        WatermarkField wk = new WatermarkField(new FieldInfo("ts", new TimestampFormatInfo()),
                new StringConstantParam("1"),
                new TimeUnitConstantParam(TimeUnit.MINUTE));
        return new MySqlExtractNode("1", "mysql_input", fields,
                wk, null, "id",
                Collections.singletonList("table"), "localhost", "username", "username",
                "test_database", 3306, 123, true, null);
    }

    private KafkaLoadNode buildKafkaNode() {
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
        return new KafkaLoadNode("2", "kafka_output", fields, relations, null, null,
                "topic", "localhost:9092", new JsonFormat(),
                1, null, "id");
    }

    private HiveLoadNode buildHiveNode() {
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
        return new HiveLoadNode("2", "hive_output", fields, relations, null, null,
                1, null, "myHive", "default", "test", "/opt/hive-conf", "3.1.3",
                null, Arrays.asList(new FieldInfo("day", new LongFormatInfo())));
    }

    private NodeRelation buildNodeRelation(List<Node> inputs, List<Node> outputs) {
        List<String> inputIds = inputs.stream().map(Node::getId).collect(Collectors.toList());
        List<String> outputIds = outputs.stream().map(Node::getId).collect(Collectors.toList());
        return new NodeRelation(inputIds, outputIds);
    }

    /**
     * Get test object
     *
     * @return The test object
     */
    @Override
    public StreamInfo getTestObject() {
        Node input = buildMySqlExtractNode();
        Node output = buildKafkaNode();
        return new StreamInfo("1", Arrays.asList(input, output), Collections.singletonList(
                buildNodeRelation(Collections.singletonList(input), Collections.singletonList(output))));
    }
}
