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

package org.apache.inlong.manager.client.api.impl;

import com.google.common.collect.Lists;
import org.apache.inlong.manager.client.api.InlongStream;
import org.apache.inlong.manager.client.api.transform.MultiDependencyTransform;
import org.apache.inlong.manager.client.api.transform.SingleDependencyTransform;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseSink;
import org.apache.inlong.manager.pojo.sink.hive.HiveSink;
import org.apache.inlong.manager.pojo.sink.kafka.KafkaSink;
import org.apache.inlong.manager.pojo.source.kafka.KafkaSource;
import org.apache.inlong.manager.pojo.source.mysql.MySQLBinlogSource;
import org.apache.inlong.manager.pojo.stream.StreamPipeline;
import org.apache.inlong.manager.pojo.stream.StreamTransform;
import org.apache.inlong.manager.pojo.transform.filter.FilterDefinition;
import org.apache.inlong.manager.pojo.transform.filter.FilterDefinition.FilterStrategy;
import org.apache.inlong.manager.pojo.transform.joiner.JoinerDefinition;
import org.apache.inlong.manager.pojo.transform.joiner.JoinerDefinition.JoinMode;
import org.apache.inlong.manager.pojo.transform.splitter.SplitterDefinition;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link InlongStreamImpl}
 */
public class InlongStreamImplTest {

    @Test
    public void testCreatePipeline() {
        InlongStream inlongStream = new InlongStreamImpl("group", "stream", null);
        // add stream source
        KafkaSource kafkaSource = new KafkaSource();
        kafkaSource.setSourceName("A");
        MySQLBinlogSource binlogSourceRequest = new MySQLBinlogSource();
        binlogSourceRequest.setSourceName("B");
        inlongStream.addSource(kafkaSource);
        inlongStream.addSource(binlogSourceRequest);
        // add stream sink
        ClickHouseSink clickHouseSink = new ClickHouseSink();
        clickHouseSink.setSinkName("E");
        inlongStream.addSink(clickHouseSink);

        HiveSink hiveSink = new HiveSink();
        hiveSink.setSinkName("F");
        inlongStream.addSink(hiveSink);

        KafkaSink kafkaSink1 = new KafkaSink();
        kafkaSink1.setSinkName("I");
        inlongStream.addSink(kafkaSink1);

        KafkaSink kafkaSink2 = new KafkaSink();
        kafkaSink2.setSinkName("M");
        inlongStream.addSink(kafkaSink2);

        // add stream transform
        StreamTransform multiDependencyTransform = new MultiDependencyTransform(
                "C",
                new JoinerDefinition(kafkaSource, binlogSourceRequest, Lists.newArrayList(), Lists.newArrayList(),
                        JoinMode.INNER_JOIN),
                "A", "B");
        StreamTransform singleDependencyTransform1 = new SingleDependencyTransform(
                "D", new FilterDefinition(FilterStrategy.REMOVE, Lists.newArrayList()), "C", "E", "F");

        StreamTransform singleDependencyTransform2 = new SingleDependencyTransform(
                "G", new SplitterDefinition(Lists.newArrayList()), "C", "I");
        inlongStream.addTransform(multiDependencyTransform);
        inlongStream.addTransform(singleDependencyTransform1);
        inlongStream.addTransform(singleDependencyTransform2);
        StreamPipeline streamPipeline = inlongStream.createPipeline();
        String pipelineView = JsonUtils.toJsonString(streamPipeline);
        Assertions.assertTrue(pipelineView.contains("{\"inputNodes\":[\"C\"],\"outputNodes\":[\"D\",\"G\"]"));
        Assertions.assertTrue(pipelineView.contains("{\"inputNodes\":[\"D\"],\"outputNodes\":[\"E\",\"F\"]}"));
    }
}
