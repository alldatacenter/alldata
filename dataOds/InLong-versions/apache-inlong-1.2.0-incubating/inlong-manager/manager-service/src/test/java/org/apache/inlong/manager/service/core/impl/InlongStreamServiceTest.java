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

package org.apache.inlong.manager.service.core.impl;

import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamRequest;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.group.InlongGroupServiceTest;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

/**
 * Inlong stream service test
 */
@TestComponent
public class InlongStreamServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongStreamServiceTest.class);

    private final String globalOperator = "admin";

    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private InlongGroupServiceTest groupServiceTest;

    /**
     * Test save inlong stream
     */
    public Integer saveInlongStream(String groupId, String streamId, String operator) {
        try {
            InlongStreamInfo response = streamService.get(groupId, streamId);
            if (response != null) {
                return response.getId();
            }
        } catch (Exception e) {
            // ignore
        }

        groupServiceTest.saveGroup(groupId, operator);

        InlongStreamRequest request = new InlongStreamRequest();
        request.setInlongGroupId(groupId);
        request.setInlongStreamId(streamId);
        request.setDataEncoding("UTF-8");

        return streamService.save(request, operator);
    }

    // @Test
    public void testSaveAndDelete() {
        String groupId = "stream_service_test_group";
        String streamId = "stream_service_test_stream";
        Integer id = this.saveInlongStream(groupId, streamId, globalOperator);
        Assert.assertNotNull(id);

        boolean result = streamService.delete(groupId, streamId, globalOperator);
        Assert.assertTrue(result);
    }

    @Test
    public void test() {
        LOGGER.info("If you don't add test, UnusedImports: Unused import: org.junit.Test.");
    }

}
