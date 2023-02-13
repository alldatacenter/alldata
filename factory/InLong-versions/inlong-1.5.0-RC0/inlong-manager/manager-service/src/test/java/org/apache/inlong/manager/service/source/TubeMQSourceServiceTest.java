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

package org.apache.inlong.manager.service.source;

import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.tubemq.TubeMQSource;
import org.apache.inlong.manager.pojo.source.tubemq.TubeMQSourceRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TubeMQ source service test.
 */
public class TubeMQSourceServiceTest extends ServiceBaseTest {

    @Autowired
    private StreamSourceService sourceService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save source info.
     */
    public Integer saveSource() {
        streamServiceTest.saveInlongStream(GLOBAL_GROUP_ID, GLOBAL_STREAM_ID, GLOBAL_OPERATOR);
        TubeMQSourceRequest sourceInfo = new TubeMQSourceRequest();
        sourceInfo.setInlongGroupId(GLOBAL_GROUP_ID);
        sourceInfo.setInlongStreamId(GLOBAL_STREAM_ID);
        sourceInfo.setSourceName("test_tube_mq");
        sourceInfo.setMasterRpc("127.0.0.1:8715");
        sourceInfo.setTopic("inlong");
        sourceInfo.setSerializationType("json");
        sourceInfo.setGroupId("test1");
        sourceInfo.setSourceType(SourceType.TUBEMQ);
        return sourceService.save(sourceInfo, GLOBAL_OPERATOR);
    }

    @Test
    public void testSaveAndDelete() {
        Integer id = this.saveSource();
        Assertions.assertNotNull(id);
        boolean result = sourceService.delete(id, GLOBAL_OPERATOR);
        Assertions.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer id = this.saveSource();
        StreamSource source = sourceService.get(id);
        Assertions.assertEquals(GLOBAL_GROUP_ID, source.getInlongGroupId());
        sourceService.delete(id, GLOBAL_OPERATOR);
    }

    @Test
    public void testGetAndUpdate() {
        Integer id = this.saveSource();
        StreamSource response = sourceService.get(id);
        Assertions.assertEquals(GLOBAL_GROUP_ID, response.getInlongGroupId());
        TubeMQSource tubeMQSource = (TubeMQSource) response;
        TubeMQSourceRequest request = CommonBeanUtils.copyProperties(tubeMQSource, TubeMQSourceRequest::new);
        boolean result = sourceService.update(request, GLOBAL_OPERATOR);
        Assertions.assertTrue(result);
        sourceService.delete(id, GLOBAL_OPERATOR);
    }

}
