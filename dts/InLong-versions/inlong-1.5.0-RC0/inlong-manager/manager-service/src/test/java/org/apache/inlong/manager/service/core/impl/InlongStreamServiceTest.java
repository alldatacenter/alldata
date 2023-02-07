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

import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamRequest;
import org.apache.inlong.manager.service.group.InlongGroupServiceTest;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import java.util.List;

/**
 * Inlong stream service test
 */
@TestComponent
public class InlongStreamServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongStreamServiceTest.class);

    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private InlongGroupServiceTest groupServiceTest;
    @Autowired
    private InlongGroupEntityMapper groupMapper;

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
        InlongGroupPageRequest groupRequest = InlongGroupPageRequest.builder().build();
        List<InlongGroupBriefInfo> groupList = groupMapper.selectBriefList(groupRequest);

        InlongStreamRequest request = new InlongStreamRequest();
        request.setInlongGroupId(groupId);
        request.setInlongStreamId(streamId);
        request.setDataEncoding("UTF-8");

        return streamService.save(request, operator);
    }

    /**
     * Delete one inlong stream
     */
    public Boolean deleteStream(String groupId, String streamId, String operator) {
        return streamService.delete(groupId, streamId, operator);
    }

    @Test
    public void test() {
        LOGGER.info("Blank test for inlong stream service");
    }

}
