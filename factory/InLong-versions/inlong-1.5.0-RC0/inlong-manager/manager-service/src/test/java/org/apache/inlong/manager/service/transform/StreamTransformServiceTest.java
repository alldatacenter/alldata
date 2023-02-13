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

package org.apache.inlong.manager.service.transform;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.TransformType;
import org.apache.inlong.manager.pojo.transform.TransformRequest;
import org.apache.inlong.manager.pojo.transform.TransformResponse;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.StreamTransformEntity;
import org.apache.inlong.manager.dao.mapper.StreamTransformEntityMapper;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * Test class for save stream transform.
 */
public class StreamTransformServiceTest extends ServiceBaseTest {

    public static final String TRANSFORM_NAME = "test_transform";

    @Autowired
    protected StreamTransformService streamTransformService;
    @Autowired
    protected StreamTransformEntityMapper transformEntityMapper;

    @Test
    public void testSaveStreamTransform() {
        TransformRequest request = new TransformRequest();
        request.setTransformName(TRANSFORM_NAME);
        request.setTransformType(TransformType.FILTER.getType());
        request.setTransformDefinition("{}");
        request.setPreNodeNames(TRANSFORM_NAME + "_pre");
        request.setPostNodeNames(TRANSFORM_NAME + "_post");
        request.setInlongStreamId(GLOBAL_STREAM_ID);
        request.setInlongGroupId(GLOBAL_GROUP_ID);
        StreamTransformEntity entity = CommonBeanUtils.copyProperties(request, StreamTransformEntity::new);
        entity.setCreator(GLOBAL_OPERATOR);
        entity.setModifier(GLOBAL_OPERATOR);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        entity.setVersion(InlongConstants.INITIAL_VERSION);
        entity.setIsDeleted(InlongConstants.UN_DELETED);
        int index = transformEntityMapper.insert(entity);
        Assertions.assertEquals(1, index);

        List<TransformResponse> responses = streamTransformService.listTransform(GLOBAL_GROUP_ID, GLOBAL_STREAM_ID);
        Assertions.assertEquals(1, responses.size());
    }

}
