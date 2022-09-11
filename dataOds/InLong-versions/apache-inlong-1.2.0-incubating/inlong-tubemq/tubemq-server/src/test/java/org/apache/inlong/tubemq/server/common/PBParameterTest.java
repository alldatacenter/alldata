/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common;

import java.util.ArrayList;
import java.util.List;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.server.common.paramcheck.PBParameterUtils;
import org.apache.inlong.tubemq.server.common.paramcheck.ParamCheckResult;
import org.junit.Assert;
import org.junit.Test;

public class PBParameterTest {
    @Test
    public void checkProducerTopicTest() {
        ParamCheckResult result = PBParameterUtils.checkProducerTopicList(null, null);
        Assert.assertEquals(result.errCode, TErrCodeConstants.BAD_REQUEST);
        final List<String> topicList = new ArrayList<>();
        topicList.add("test1");
        result = PBParameterUtils.checkProducerTopicList(topicList, new StringBuilder(128));
        Assert.assertEquals(result.errCode, TErrCodeConstants.SUCCESS);
        for (int i = 0; i < 1025; i++) {
            topicList.add("test" + i);
        }
        result = PBParameterUtils.checkProducerTopicList(topicList, new StringBuilder(128));
        Assert.assertEquals(result.errCode, TErrCodeConstants.BAD_REQUEST);
    }

    @Test
    public void checkConsumerTopicTest() {
        ParamCheckResult result = PBParameterUtils.checkConsumerTopicList(null, null);
        Assert.assertEquals(result.errCode, TErrCodeConstants.BAD_REQUEST);
        final List<String> topicList = new ArrayList<>();
        topicList.add("test1");
        result = PBParameterUtils.checkConsumerTopicList(topicList, new StringBuilder(128));
        Assert.assertEquals(result.errCode, TErrCodeConstants.SUCCESS);
        for (int i = 0; i < 1025; i++) {
            topicList.add("test" + i);
        }
        result = PBParameterUtils.checkConsumerTopicList(topicList, new StringBuilder(128));
        Assert.assertEquals(result.errCode, TErrCodeConstants.BAD_REQUEST);
    }

    @Test
    public void checkIdTest() {
        ParamCheckResult result = PBParameterUtils.checkClientId("100", new StringBuilder(128));
        Assert.assertEquals(result.errCode, TErrCodeConstants.SUCCESS);
        result = PBParameterUtils.checkClientId("", new StringBuilder(128));
        Assert.assertEquals(result.errCode, TErrCodeConstants.BAD_REQUEST);
        result = PBParameterUtils.checkBrokerId("100", new StringBuilder(128));
        Assert.assertEquals(result.errCode, TErrCodeConstants.SUCCESS);
        result = PBParameterUtils.checkBrokerId("", new StringBuilder(128));
        Assert.assertEquals(result.errCode, TErrCodeConstants.BAD_REQUEST);
    }
}
