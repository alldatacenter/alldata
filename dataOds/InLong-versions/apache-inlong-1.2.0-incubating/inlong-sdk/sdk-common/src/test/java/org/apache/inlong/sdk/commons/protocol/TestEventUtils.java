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

package org.apache.inlong.sdk.commons.protocol;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.inlong.sdk.commons.protocol.ProxySdk.INLONG_COMPRESSED_TYPE;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessagePack;
import org.junit.Test;

/**
 * 
 * TestEventUtils
 */
public class TestEventUtils {

    public static final String INLONG_GROUP_ID = "inlongGroupId";
    public static final String INLONG_STREAM_ID = "inlongStreamId";
    public static final String BODY = "body";
    public static final String SOURCE_IP = "127.0.0.1";

    @Test
    public void testEncodeSdkEvents() {
        try {
            SdkEvent event = new SdkEvent(INLONG_GROUP_ID, INLONG_STREAM_ID, BODY);
            List<SdkEvent> eventList = new ArrayList<>();
            eventList.add(event);
            INLONG_COMPRESSED_TYPE compressedType = INLONG_COMPRESSED_TYPE.INLONG_SNAPPY;
            MessagePack packObj = EventUtils.encodeSdkEvents(INLONG_GROUP_ID, INLONG_STREAM_ID, compressedType,
                    eventList);
            assertEquals(true, (packObj != null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDecodeSdkPack() {
        try {
            SdkEvent event = new SdkEvent(INLONG_GROUP_ID, INLONG_STREAM_ID, BODY);
            event.setSourceIp(SOURCE_IP);
            List<SdkEvent> eventList = new ArrayList<>();
            eventList.add(event);
            INLONG_COMPRESSED_TYPE compressedType = INLONG_COMPRESSED_TYPE.INLONG_SNAPPY;
            MessagePack packObj = EventUtils.encodeSdkEvents(INLONG_GROUP_ID, INLONG_STREAM_ID, compressedType,
                    eventList);
            byte[] packBytes = packObj.toByteArray();
            MessagePack packObject = MessagePack.parseFrom(new ByteArrayInputStream(packBytes, 0, packBytes.length));
            List<ProxyEvent> proxyEventList = EventUtils.decodeSdkPack(packObject);
            assertEquals(1, proxyEventList.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEncodeCacheMessageBody() {
        try {
            ProxyEvent event = new ProxyEvent(INLONG_GROUP_ID, INLONG_STREAM_ID, BODY.getBytes(),
                    System.currentTimeMillis(), SOURCE_IP);
            List<ProxyEvent> eventList = new ArrayList<>();
            eventList.add(event);
            INLONG_COMPRESSED_TYPE compressedType = INLONG_COMPRESSED_TYPE.INLONG_SNAPPY;
            byte[] bodyBytes = EventUtils.encodeCacheMessageBody(compressedType, eventList);
            assertEquals(true, (bodyBytes.length > 0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDecodeCacheMessageBody() {
        try {
            ProxyEvent event = new ProxyEvent(INLONG_GROUP_ID, INLONG_STREAM_ID, BODY.getBytes(),
                    System.currentTimeMillis(), SOURCE_IP);
            List<ProxyEvent> eventList = new ArrayList<>();
            eventList.add(event);
            INLONG_COMPRESSED_TYPE compressedType = INLONG_COMPRESSED_TYPE.INLONG_SNAPPY;
            byte[] bodyBytes = EventUtils.encodeCacheMessageBody(compressedType, eventList);
            List<SortEvent> sortEventList = EventUtils.decodeCacheMessageBody(INLONG_GROUP_ID, INLONG_STREAM_ID,
                    compressedType, bodyBytes);
            assertEquals(1, sortEventList.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
