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

package org.apache.inlong.tubemq.server.broker.msgstore.mem;

import java.nio.ByteBuffer;
import org.apache.inlong.tubemq.server.broker.stats.MsgStoreStatsHolder;
import org.apache.inlong.tubemq.server.broker.utils.DataStoreUtils;
import org.apache.inlong.tubemq.server.common.utils.AppendResult;
import org.junit.Test;

/**
 * MsgMemStore test.
 */
public class MsgMemStoreTest {

    @Test
    public void appendMsg() {

        byte[] testData = "abcabdcdsdsdasdfasdfasdfsadfasdfasdfasdfasdfaaaaaaaaaaa".getBytes();
        // build data buffer
        final ByteBuffer dataBuffer =
                ByteBuffer.allocate(DataStoreUtils.STORE_DATA_HEADER_LEN + testData.length);
        dataBuffer.putInt(DataStoreUtils.STORE_DATA_PREFX_LEN + testData.length);
        dataBuffer.putInt(DataStoreUtils.STORE_DATA_TOKER_BEGIN_VALUE);
        dataBuffer.putInt(33);
        dataBuffer.putInt(0);
        dataBuffer.putLong(-1L);
        dataBuffer.putLong(2222L);
        dataBuffer.putInt(255555);
        dataBuffer.putInt(11);
        dataBuffer.putLong(222L);
        dataBuffer.putInt(1);
        dataBuffer.put(testData);
        dataBuffer.flip();
        // build index buffer
        ByteBuffer indexBuffer =
                ByteBuffer.allocate(DataStoreUtils.STORE_INDEX_HEAD_LEN);
        indexBuffer.putInt(0);
        indexBuffer.putLong(-1L);
        indexBuffer.putInt(3);
        indexBuffer.putInt(32);
        indexBuffer.putLong(System.currentTimeMillis());
        indexBuffer.flip();
        AppendResult appendResult = new AppendResult();
        // append data
        int maxCacheSize = 2 * 1024 * 1024;
        int maxMsgCount = 10000;
        MsgMemStore msgMemStore = new MsgMemStore(maxCacheSize, maxMsgCount, 0, 0);
        MsgStoreStatsHolder memStatsHolder = new MsgStoreStatsHolder();
        msgMemStore.appendMsg(memStatsHolder, 0, 0,
                System.currentTimeMillis(), indexBuffer, 3, dataBuffer, appendResult);
    }

    @Test
    public void getMessages() {
        byte[] testData = "abcabdcdsdsdasdfasdfasdfsadfasdfasdfasdfasdfaaaaaaaaaaa".getBytes();
        // build data buffer
        final ByteBuffer dataBuffer =
                ByteBuffer.allocate(DataStoreUtils.STORE_DATA_HEADER_LEN + testData.length);
        dataBuffer.putInt(DataStoreUtils.STORE_DATA_PREFX_LEN + testData.length);
        dataBuffer.putInt(DataStoreUtils.STORE_DATA_TOKER_BEGIN_VALUE);
        dataBuffer.putInt(33);
        dataBuffer.putInt(0);
        dataBuffer.putLong(-1L);
        dataBuffer.putLong(2222L);
        dataBuffer.putInt(255555);
        dataBuffer.putInt(11);
        dataBuffer.putLong(222L);
        dataBuffer.putInt(1);
        dataBuffer.put(testData);
        dataBuffer.flip();
        // build index buffer
        ByteBuffer indexBuffer =
                ByteBuffer.allocate(DataStoreUtils.STORE_INDEX_HEAD_LEN);
        indexBuffer.putInt(0);
        indexBuffer.putLong(-1L);
        indexBuffer.putInt(3);
        indexBuffer.putInt(32);
        indexBuffer.putLong(System.currentTimeMillis());
        indexBuffer.flip();
        AppendResult appendResult = new AppendResult();
        int maxCacheSize = 2 * 1024 * 1024;
        int maxMsgCount = 10000;
        MsgMemStore msgMemStore = new MsgMemStore(maxCacheSize, maxMsgCount, 0, 0);
        MsgStoreStatsHolder memStatsHolder = new MsgStoreStatsHolder();
        msgMemStore.appendMsg(memStatsHolder, 0, 0,
                System.currentTimeMillis(), indexBuffer, 3, dataBuffer, appendResult);
        // get messages
        GetCacheMsgResult getCacheMsgResult = msgMemStore.getMessages(0, 2, 1024, 1000, 0, false, false, null, 0);
    }
}
