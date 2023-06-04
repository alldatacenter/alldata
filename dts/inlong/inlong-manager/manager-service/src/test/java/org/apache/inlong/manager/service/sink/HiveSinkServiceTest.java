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
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.hive.HiveSink;
import org.apache.inlong.manager.pojo.sink.hive.HiveSinkRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Hive sink service test
 */
public class HiveSinkServiceTest extends ServiceBaseTest {

    private final String globalGroupId = "b_group1";
    private final String globalStreamId = "stream1";
    private final String globalOperator = "admin";
    private final String sinkName = "default";
    private final String jdbcUrl = "127.0.0.1:8080";

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save sink info.
     */
    public Integer saveSink() {
        streamServiceTest.saveInlongStream(globalGroupId, globalStreamId, globalOperator);

        HiveSinkRequest sinkInfo = new HiveSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.HIVE);
        sinkInfo.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
        sinkInfo.setSinkName(sinkName);
        sinkInfo.setJdbcUrl(jdbcUrl);
        return sinkService.save(sinkInfo, globalOperator);
    }

    @Test
    public void testSaveAndDelete() {
        Integer id = this.saveSink();
        Assertions.assertNotNull(id);

        boolean result = sinkService.delete(id, false, globalOperator);
        Assertions.assertTrue(result);
    }

    @Test
    public void testSaveAndDeleteByKey() {
        Integer id = this.saveSink();
        Assertions.assertNotNull(id);

        boolean result = sinkService.deleteByKey(globalGroupId, globalStreamId, sinkName, false, globalOperator);
        Assertions.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer id = this.saveSink();

        StreamSink sink = sinkService.get(id);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());

        sinkService.delete(id, false, globalOperator);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink();
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        HiveSink sink = (HiveSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
        HiveSinkRequest request = CommonBeanUtils.copyProperties(sink, HiveSinkRequest::new);
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);

        sinkService.delete(sinkId, false, globalOperator);
    }

    @Test
    public void testGetAndUpdateByKey() {
        Integer sinkId = this.saveSink();
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        HiveSink sink = (HiveSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
        HiveSinkRequest request = CommonBeanUtils.copyProperties(sink, HiveSinkRequest::new);
        UpdateResult result = sinkService.updateByKey(request, globalOperator);
        Assertions.assertTrue(result.getSuccess());
        Assertions.assertEquals(request.getVersion() + 1, result.getVersion().intValue());

        sinkService.delete(sinkId, false, globalOperator);
    }

}
