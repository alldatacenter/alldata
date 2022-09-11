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

package org.apache.inlong.sdk.sort.impl.decode;

import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MapFieldEntry;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObj;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObjs;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.entity.CacheZoneCluster;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.impl.ClientContextImpl;
import org.apache.inlong.sdk.sort.stat.SortClientStateCounter;
import org.apache.inlong.sdk.sort.stat.StatManager;
import org.apache.inlong.sdk.sort.util.Utils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class MessageDeserializerTest {

    private MessageDeserializer messageDeserializer;
    private Map<String, String> headers;
    private ClientContext context;
    private InLongTopic inLongTopic;
    private String testData;
    private MessageObjs messageObjs;
    private SortClientConfig sortClientConfig;
    private StatManager statManager;

    private void setUp() throws Exception {
        System.setProperty("log4j2.disable.jmx", Boolean.TRUE.toString());
        messageDeserializer = new MessageDeserializer();
        headers = new HashMap<>();
        context = PowerMockito.mock(ClientContextImpl.class);
        sortClientConfig = PowerMockito.mock(SortClientConfig.class);
        statManager = PowerMockito.mock(StatManager.class);

        inLongTopic = new InLongTopic();
        inLongTopic.setTopic("testTopic");
        CacheZoneCluster cacheZoneCluster = new CacheZoneCluster("clusterId", "bootstraps", "token");
        inLongTopic.setInLongCluster(cacheZoneCluster);

        when(context.getConfig()).thenReturn(sortClientConfig);
        when(context.getStatManager()).thenReturn(statManager);
        SortClientStateCounter sortClientStateCounter = new SortClientStateCounter("sortTaskId",
                cacheZoneCluster.getClusterId(),
                inLongTopic.getTopic(), 0);
        when(statManager.getStatistics(anyString(), anyString(), anyString())).thenReturn(sortClientStateCounter);
        when(sortClientConfig.getSortTaskId()).thenReturn("sortTaskId");
    }

    @Test
    public void testDeserialize() {
        //1. setUp
        try {
            setUp();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //2. testDeserializeVersion0
        testDeserializeVersion0();

        //3. testDeserializeVersion1CompressionType0
        testDeserializeVersion1CompressionType0();

        //4. testDeserializeVersion1CompressionType1
        testDeserializeVersion1CompressionType1();

        //5. testDeserializeVersion1CompressionType2
        testDeserializeVersion1CompressionType2();
    }

    private void testDeserializeVersion0() {
        try {
            // test version == 0
            headers.put("version", "0");
            testData = "test data";
            List<InLongMessage> deserialize = messageDeserializer
                    .deserialize(context, inLongTopic, headers, testData.getBytes());
            Assert.assertEquals(1, deserialize.size());
            Assert.assertEquals(testData, new String(deserialize.get(0).getBody()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testDeserializeVersion1CompressionType0() {
        try {
            // test version == 1
            prepareTestMessageObjs();
            // non compression
            headers.put("compressType", "0");

            List<InLongMessage> deserialize = messageDeserializer
                    .deserialize(context, inLongTopic, headers, messageObjs.toByteArray());
            Assert.assertEquals(2, deserialize.size());
            Assert.assertEquals(testData, new String(deserialize.get(0).getBody()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testDeserializeVersion1CompressionType1() {
        try {
            // test version == 1
            prepareTestMessageObjs();
            // compression gzip
            headers.put("compressType", "1");

            byte[] testDataByteArray = Utils.compressGZip(messageObjs.toByteArray());

            List<InLongMessage> deserialize = messageDeserializer
                    .deserialize(context, inLongTopic, headers, testDataByteArray);
            Assert.assertEquals(2, deserialize.size());
            Assert.assertEquals(testData, new String(deserialize.get(0).getBody()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testDeserializeVersion1CompressionType2() {
        try {
            // test version == 1
            prepareTestMessageObjs();
            // compression snappy
            headers.put("compressType", "2");

            byte[] testDataByteArray = Utils.snappyCompress(messageObjs.toByteArray());

            List<InLongMessage> deserialize = messageDeserializer
                    .deserialize(context, inLongTopic, headers, testDataByteArray);
            Assert.assertEquals(2, deserialize.size());
            Assert.assertEquals(testData, new String(deserialize.get(0).getBody()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareTestMessageObjs() {
        headers.put("version", "1");
        testData = "test data";
        long messageTime = System.currentTimeMillis();
        MapFieldEntry mapFieldEntry = MapFieldEntry.newBuilder().setKey("key").setValue("val").build();
        MessageObj messageObj1 = MessageObj.newBuilder().setBody(ByteString.copyFrom(testData.getBytes()))
                .setMsgTime(messageTime)
                .setSourceIp("ip1")
                .addParams(mapFieldEntry)
                .build();
        MessageObj messageObj2 = MessageObj.newBuilder().setBody(ByteString.copyFrom(testData.getBytes()))
                .setMsgTime(messageTime)
                .setSourceIp("ip2")
                .addParams(mapFieldEntry)
                .build();

        messageObjs = MessageObjs.newBuilder().addMsgs(messageObj1).addMsgs(messageObj2).build();
    }
}