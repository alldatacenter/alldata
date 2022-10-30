/*
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

package org.apache.inlong.tubemq.corebase;

import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.junit.Assert;
import org.junit.Test;

public class MessagesTest {

    @Test
    public void testPutSystemHeader() {
        // case 1
        Throwable exVal = null;
        Message message1 = new Message("test", "test case".getBytes());
        try {
            message1.putSystemHeader("streamId1", "12345667");
        } catch (Throwable ex) {
            exVal = ex;
        }
        Assert.assertNotNull(exVal);
        Assert.assertEquals(exVal.getClass(), IllegalArgumentException.class);
        Assert.assertTrue(exVal.getMessage().contains("format and length must equal"));
        // case 2
        exVal = null;
        try {
            message1.putSystemHeader("streamId1", "20220115170022");
        } catch (Throwable ex) {
            exVal = ex;
        }
        Assert.assertNotNull(exVal);
        Assert.assertEquals(exVal.getClass(), IllegalArgumentException.class);
        Assert.assertTrue(exVal.getMessage().contains("format and length must equal"));
        // case 3
        exVal = null;
        try {
            message1.putSystemHeader("streamId1", "202201151799");
        } catch (Throwable ex) {
            exVal = ex;
        }
        Assert.assertNotNull(exVal);
        Assert.assertEquals(exVal.getClass(), IllegalArgumentException.class);
        Assert.assertTrue(exVal.getMessage().contains("could not be parsed"));
        // case 4
        message1.putSystemHeader("streamId1", "202201151700");
        message1.setAttrKeyVal("key", "value");
        System.out.println(message1.getAttribute());
        Assert.assertTrue(message1.getAttribute().contains("202201151700"));
        // case 5
        message1.putSystemHeader("streamId2", "202201151300");
        message1.setAttrKeyVal("key2", "value2");
        System.out.println(message1.getAttribute());
        Assert.assertTrue(message1.getAttribute().contains("202201151300"));
        // case 6
        message1.putSystemHeader(null, null);
        System.out.println(message1.getAttribute());
        Assert.assertFalse(message1.getAttribute().contains("202201151300"));
        // case 7
        Message message2 = new Message("test", "test case".getBytes());
        message2.setAttrKeyVal("key2", "value2");
        message2.putSystemHeader("streamId1", "202201151715");
        System.out.println(message2.getAttribute());
        Assert.assertTrue(message2.getAttribute().contains("202201151715"));
        // case 8
        Message message3 = new Message("test", "test case".getBytes());
        message3.setAttrKeyVal("key2", "value2");
        message3.putSystemHeader(null, "202201151715");
        System.out.println(message3.getAttribute());
        Assert.assertTrue(message3.getAttribute().contains("202201151715"));
        // case 8
        Message message4 = new Message("test", "test case".getBytes());
        message4.putSystemHeader("streamId1", null);
        System.out.println(message4.getAttribute());
        Assert.assertFalse(message4.getAttribute().contains("202201151715"));
    }

    @Test
    public void testParseSystemHeader() {
        // case 1
        Throwable exVal = null;
        String msgTime;
        Message message1 = new MessageExt(0, "test", "test case".getBytes(),
                "$msgType$=streamId1,$msgTime$=202201151799,key2=value2", 1);
        msgTime = message1.getMsgTime();
        Assert.assertEquals(msgTime, TStringUtils.EMPTY);

    }
}
