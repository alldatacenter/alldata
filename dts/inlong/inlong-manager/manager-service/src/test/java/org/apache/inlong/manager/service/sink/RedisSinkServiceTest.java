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

import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.redis.RedisClusterMode;
import org.apache.inlong.manager.pojo.sink.redis.RedisSink;
import org.apache.inlong.manager.pojo.sink.redis.RedisSinkRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Redis stream sink service test.
 */
public class RedisSinkServiceTest extends ServiceBaseTest {

    private final String globalGroupId = "b_group1";
    private final String globalStreamId = "stream1_hudi";
    private final String globalOperator = "admin";

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save sink info.
     */
    public Integer saveSink(String sinkName) {
        streamServiceTest.saveInlongStream(globalGroupId, globalStreamId, globalOperator);
        RedisSinkRequest sinkRequest = new RedisSinkRequest();
        sinkRequest.setInlongGroupId(globalGroupId);
        sinkRequest.setInlongStreamId(globalStreamId);
        sinkRequest.setSinkType(SinkType.REDIS);
        sinkRequest.setClusterMode(RedisClusterMode.STANDALONE.name());
        sinkRequest.setHost("demo-host");
        sinkRequest.setPort(6300);
        sinkRequest.setDataType("HASH");
        sinkRequest.setSchemaMapMode("DYNAMIC");
        sinkRequest.setSinkName(sinkName);
        sinkRequest.setId((int) (Math.random() * 100000 + 1));
        return sinkService.save(sinkRequest, globalOperator);
    }

    @Test
    public void testSaveAndDelete() {
        Integer id = this.saveSink("default1");
        Assertions.assertNotNull(id);
        boolean result = sinkService.delete(id, false, globalOperator);
        Assertions.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer id = this.saveSink("default2");
        StreamSink sink = sinkService.get(id);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        sinkService.delete(id, false, globalOperator);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("default3");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        RedisSink sink = (RedisSink) streamSink;
        SinkRequest request = sink.genSinkRequest();
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);

        sinkService.delete(sinkId, false, globalOperator);
    }

}
