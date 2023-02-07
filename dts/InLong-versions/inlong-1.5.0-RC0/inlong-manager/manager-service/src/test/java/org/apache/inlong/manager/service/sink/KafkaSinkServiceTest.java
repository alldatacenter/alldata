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

package org.apache.inlong.manager.service.sink;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.kafka.KafkaSink;
import org.apache.inlong.manager.pojo.sink.kafka.KafkaSinkRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Kafka sink service test
 */
public class KafkaSinkServiceTest extends ServiceBaseTest {

    private static final String bootstrapServers = "127.0.0.1:9092";
    private static final String serializationType = "Json";
    private static final String topicName = "kafka_topic_name";

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save sink info.
     */
    public Integer saveSink(String sinkName) {
        streamServiceTest.saveInlongStream(GLOBAL_GROUP_ID, GLOBAL_STREAM_ID, GLOBAL_OPERATOR);
        KafkaSinkRequest sinkInfo = new KafkaSinkRequest();
        sinkInfo.setInlongGroupId(GLOBAL_GROUP_ID);
        sinkInfo.setInlongStreamId(GLOBAL_STREAM_ID);
        sinkInfo.setSinkType(SinkType.KAFKA);
        sinkInfo.setSinkName(sinkName);
        sinkInfo.setSerializationType(serializationType);
        sinkInfo.setBootstrapServers(bootstrapServers);
        sinkInfo.setTopicName(topicName);
        sinkInfo.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
        return sinkService.save(sinkInfo, GLOBAL_OPERATOR);
    }

    /**
     * Delete sink info by sink id.
     */
    public void deleteSink(Integer sinkId) {
        boolean result = sinkService.delete(sinkId, false, GLOBAL_OPERATOR);
        Assertions.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer sinkId = this.saveSink("default1");
        StreamSink sink = sinkService.get(sinkId);
        Assertions.assertEquals(GLOBAL_GROUP_ID, sink.getInlongGroupId());
        deleteSink(sinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("default2");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(GLOBAL_GROUP_ID, streamSink.getInlongGroupId());

        KafkaSink sink = (KafkaSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        SinkRequest request = sink.genSinkRequest();
        boolean result = sinkService.update(request, GLOBAL_OPERATOR);
        Assertions.assertTrue(result);

        deleteSink(sinkId);
    }

}
