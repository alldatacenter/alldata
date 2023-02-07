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

package org.apache.inlong.sort.standalone.sink.elasticsearch;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.sort.standalone.channel.BufferQueueChannel;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.sink.SinkContext;
import org.apache.inlong.sort.standalone.utils.BufferQueue;
import org.apache.inlong.sort.standalone.utils.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * 
 * TestEsSinkContext
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({MetricRegister.class})
public class TestEsSinkContext {

    public static final String TEST_INLONG_GROUP_ID = "0fc00000046";
    public static final String TEST_INLONG_STREAM_ID = "";
    public static final String TEST_CONTENT = "field1|field2|field3|field4";

    /**
     * mock
     * 
     * @param dispatchQueue
     * @return EsSinkContext
     * @throws Exception exception
     */
    public static EsSinkContext mock(BufferQueue<EsIndexRequest> dispatchQueue) throws Exception {
        PowerMockito.mockStatic(MetricRegister.class);
        PowerMockito.doNothing().when(MetricRegister.class, "register", any());
        Context context = CommonPropertiesHolder.getContext();
        String sinkName = CommonPropertiesHolder.getClusterId() + "Sink";
        context.put("taskName", "sid_es_es-rmrv7g7a_v3");
        Channel channel = new BufferQueueChannel();
        EsSinkContext esSinkContext = new EsSinkContext(sinkName, context, channel, dispatchQueue);
        esSinkContext.reload();
        return esSinkContext;
    }

    /**
     * mockProfileEvent
     * 
     * @param  inlongGroupId
     * @param  inlongStreamId
     * @param  content
     * @return
     */
    public static ProfileEvent mockProfileEvent(String inlongGroupId, String inlongStreamId, String content) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.INLONG_GROUP_ID, inlongGroupId);
        headers.put(Constants.INLONG_STREAM_ID, inlongStreamId);
        headers.put(Constants.HEADER_KEY_MSG_TIME, String.valueOf(System.currentTimeMillis()));
        headers.put(Constants.HEADER_KEY_SOURCE_IP, "127.0.0.1");
        byte[] body = content.getBytes(Charset.defaultCharset());
        return new ProfileEvent(headers, body);
    }

    /**
     * mockProfileEvent
     * 
     * @return
     */
    public static ProfileEvent mockProfileEvent() {
        return mockProfileEvent(TEST_INLONG_STREAM_ID, TEST_INLONG_GROUP_ID, TEST_CONTENT);
    }

    /**
     * test
     * 
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        BufferQueue<EsIndexRequest> dispatchQueue = SinkContext.createBufferQueue();
        EsSinkContext context = mock(dispatchQueue);
        assertEquals(10, context.getBulkSizeMb());
    }
}
