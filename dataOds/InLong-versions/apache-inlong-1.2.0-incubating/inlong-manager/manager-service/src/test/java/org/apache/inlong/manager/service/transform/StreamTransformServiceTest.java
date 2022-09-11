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

import org.apache.inlong.manager.common.enums.TransformType;
import org.apache.inlong.manager.common.pojo.transform.TransformRequest;
import org.apache.inlong.manager.common.pojo.transform.TransformResponse;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.StreamTransformEntity;
import org.apache.inlong.manager.dao.mapper.StreamTransformEntityMapper;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import springfox.boot.starter.autoconfigure.OpenApiAutoConfiguration;

import java.util.Date;
import java.util.List;

/**
 * Test class for save stream transform.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@EnableAutoConfiguration(exclude = OpenApiAutoConfiguration.class)
public class StreamTransformServiceTest extends ServiceBaseTest {

    public static final String TRANSFORM_NAME = "test_transform";

    @Autowired
    protected StreamTransformService streamTransformService;
    @Autowired
    protected StreamTransformEntityMapper transformEntityMapper;

    @Test
    public void testSaveStreamTransform() {
        TransformRequest transformRequest = new TransformRequest();
        transformRequest.setTransformName(TRANSFORM_NAME);
        transformRequest.setTransformType(TransformType.FILTER.getType());
        transformRequest.setTransformDefinition("{}");
        transformRequest.setInlongStreamId(GLOBAL_STREAM_ID);
        transformRequest.setInlongGroupId(GLOBAL_GROUP_ID);
        StreamTransformEntity transformEntity = CommonBeanUtils.copyProperties(transformRequest,
                StreamTransformEntity::new);
        transformEntity.setCreator(GLOBAL_OPERATOR);
        transformEntity.setModifier(GLOBAL_OPERATOR);
        Date now = new Date();
        transformEntity.setCreateTime(now);
        transformEntity.setModifyTime(now);
        int index = transformEntityMapper.insertSelective(transformEntity);
        Assert.assertTrue(index == 1);

        List<TransformResponse> transformResponses = streamTransformService.listTransform(GLOBAL_GROUP_ID,
                GLOBAL_STREAM_ID);
        Assert.assertTrue(transformResponses.size() == 1);

    }

}
