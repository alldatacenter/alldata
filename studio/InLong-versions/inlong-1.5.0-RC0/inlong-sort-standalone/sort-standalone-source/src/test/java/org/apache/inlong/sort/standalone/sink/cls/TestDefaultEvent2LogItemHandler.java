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

package org.apache.inlong.sort.standalone.sink.cls;

import com.tencentcloudapi.cls.producer.common.LogItem;

import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.utils.Constants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({ClsSinkContext.class, LogItem.class})
public class TestDefaultEvent2LogItemHandler {

    private ClsIdConfig idConfig;
    private ProfileEvent event;
    private DefaultEvent2LogItemHandler handler;
    private ClsSinkContext mockContext;

    @Before
    public void setUp() {
        idConfig = prepareIdConfig();
        event = prepareEvent();
        mockContext = PowerMockito.mock(ClsSinkContext.class);
        handler = new DefaultEvent2LogItemHandler();
    }

    @Test
    public void testNoIdConfig() {
        Assert.assertNull(handler.parse(mockContext, event));
    }

    // @Test
    public void testNormal() {
        PowerMockito.when(mockContext.getIdConfig(Mockito.anyString())).thenReturn(idConfig);
        PowerMockito.when(mockContext.getKeywordMaxLength()).thenReturn(8 * 1024);
        List<LogItem> itemList = handler.parse(mockContext, event);
        System.out.println(itemList.size());
    }

    private ClsIdConfig prepareIdConfig() {
        ClsIdConfig config = new ClsIdConfig();
        config.setFieldNames("f1 f2 f3 f4 f5 f6 f7 f8");
        config.setInlongGroupId("testGroup");
        config.setInlongStreamId("testStream");
        config.setSecretId("testSecretId");
        config.setSecretKey("testSecretKey");
        config.setEndpoint("testEndPoint");
        config.setTopicId("testTopicId");
        return config;
    }

    private ProfileEvent prepareEvent() {
        String str = "v1|v2|v3|v4|v5|v6|v7|v8";
        final byte[] body = str.getBytes(StandardCharsets.UTF_8);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.INLONG_GROUP_ID, "testGroup");
        headers.put(Constants.INLONG_STREAM_ID, "testStream");
        headers.put(Constants.HEADER_KEY_MSG_TIME, "1234456");
        return new ProfileEvent(headers, body);
    }

}