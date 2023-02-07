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

package org.apache.inlong.tubemq.server.broker.stats;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.junit.Assert;
import org.junit.Test;

/**
 * MsgStoreStatsHolder test.
 */
public class MsgStoreStatsHolderTest {

    @Test
    public void testMemPartStats() {
        MsgStoreStatsHolder msgStoreStatsHolder = new MsgStoreStatsHolder();
        // case 1, not started
        msgStoreStatsHolder.addMsgWriteSuccess(50, 2);
        msgStoreStatsHolder.addCacheFullType(true, false, false);
        msgStoreStatsHolder.addCacheFullType(false, true, false);
        msgStoreStatsHolder.addCacheFullType(false, false, true);
        msgStoreStatsHolder.addCacheTimeoutFlush();
        msgStoreStatsHolder.addMsgWriteFailure();
        Map<String, Long> retMap = new LinkedHashMap<>();
        msgStoreStatsHolder.getValue(retMap);
        Assert.assertNotNull(retMap.get("reset_time"));
        Assert.assertEquals(0, retMap.get("msg_append_size_count").longValue());
        Assert.assertEquals(Long.MIN_VALUE, retMap.get("msg_append_size_max").longValue());
        Assert.assertEquals(Long.MAX_VALUE, retMap.get("msg_append_size_min").longValue());
        Assert.assertEquals(0, retMap.get("msg_append_fail").longValue());
        Assert.assertEquals(0, retMap.get("cache_data_full").longValue());
        Assert.assertEquals(0, retMap.get("cache_index_full").longValue());
        Assert.assertEquals(0, retMap.get("cache_count_full").longValue());
        Assert.assertEquals(0, retMap.get("cache_time_full").longValue());
        Assert.assertEquals(0, retMap.get("cache_flush_pending").longValue());
        Assert.assertEquals(0, retMap.get("cache_realloc").longValue());
        Assert.assertNotNull(retMap.get("end_time"));
        retMap.clear();
        // get content by StringBuilder
        StringBuilder strBuff = new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE);
        msgStoreStatsHolder.getValue(strBuff);
        // System.out.println(strBuff.toString());
        strBuff.delete(0, strBuff.length());
        msgStoreStatsHolder.getMsgStoreStatsInfo(true, strBuff);
        // System.out.println("getAllMemStatsInfo : " + strBuff);
        strBuff.delete(0, strBuff.length());
        // case 2 started
        msgStoreStatsHolder.addMsgWriteSuccess(50, 10);
        msgStoreStatsHolder.addMsgWriteSuccess(500, 20);
        msgStoreStatsHolder.addMsgWriteSuccess(5, 3);
        msgStoreStatsHolder.addCacheFullType(true, false, false);
        msgStoreStatsHolder.addCacheFullType(false, true, false);
        msgStoreStatsHolder.addCacheFullType(false, false, true);
        msgStoreStatsHolder.addCacheTimeoutFlush();
        msgStoreStatsHolder.addCacheTimeoutFlush();
        msgStoreStatsHolder.addMsgWriteFailure();
        msgStoreStatsHolder.addMsgWriteFailure();
        msgStoreStatsHolder.addCacheReAlloc();
        msgStoreStatsHolder.addCacheReAlloc();
        msgStoreStatsHolder.addCachePending();
        msgStoreStatsHolder.addCachePending();
        msgStoreStatsHolder.addCachePending();
        msgStoreStatsHolder.getValue(retMap);
        Assert.assertNotNull(retMap.get("reset_time"));
        Assert.assertEquals(3, retMap.get("msg_append_size_count").longValue());
        Assert.assertEquals(500, retMap.get("msg_append_size_max").longValue());
        Assert.assertEquals(5, retMap.get("msg_append_size_min").longValue());
        Assert.assertEquals(2, retMap.get("msg_append_fail").longValue());
        Assert.assertEquals(1, retMap.get("cache_data_full").longValue());
        Assert.assertEquals(1, retMap.get("cache_index_full").longValue());
        Assert.assertEquals(1, retMap.get("cache_count_full").longValue());
        Assert.assertEquals(2, retMap.get("cache_time_full").longValue());
        Assert.assertEquals(3, retMap.get("cache_flush_pending").longValue());
        Assert.assertEquals(2, retMap.get("cache_realloc").longValue());
        Assert.assertNotNull(retMap.get("end_time"));
        msgStoreStatsHolder.getMsgStoreStatsInfo(false, strBuff);
        System.out.println("\n the second is : " + strBuff.toString());
        strBuff.delete(0, strBuff.length());
    }

    @Test
    public void testFilePartStats() {
        MsgStoreStatsHolder msgStoreStatsHolder = new MsgStoreStatsHolder();
        // case 1, not started
        msgStoreStatsHolder.addFileFlushStatsInfo(2, 30, 500,
                0, 0, true, true,
                true, true, true, true, 5);
        msgStoreStatsHolder.addFileTimeoutFlushStats(1, 500, false);
        Map<String, Long> retMap = new LinkedHashMap<>();
        msgStoreStatsHolder.getValue(retMap);
        Assert.assertNotNull(retMap.get("reset_time"));
        Assert.assertEquals(0, retMap.get("file_total_msg_cnt").longValue());
        Assert.assertEquals(0, retMap.get("file_total_data_size").longValue());
        Assert.assertEquals(0, retMap.get("file_total_index_size").longValue());
        Assert.assertEquals(0, retMap.get("file_flush_data_size_count").longValue());
        Assert.assertEquals(Long.MIN_VALUE, retMap.get("file_flush_data_size_max").longValue());
        Assert.assertEquals(Long.MAX_VALUE, retMap.get("file_flush_data_size_min").longValue());
        Assert.assertEquals(0, retMap.get("file_flush_msg_cnt_count").longValue());
        Assert.assertEquals(Long.MIN_VALUE, retMap.get("file_flush_msg_cnt_max").longValue());
        Assert.assertEquals(Long.MAX_VALUE, retMap.get("file_flush_msg_cnt_min").longValue());
        Assert.assertEquals(0, retMap.get("file_index_seg").longValue());
        Assert.assertEquals(0, retMap.get("file_meta_flush").longValue());
        Assert.assertEquals(0, retMap.get("file_data_full").longValue());
        Assert.assertEquals(0, retMap.get("file_count_full").longValue());
        Assert.assertEquals(0, retMap.get("file_time_full").longValue());
        Assert.assertEquals(0, retMap.get("file_flush_dlt_count").longValue());
        Assert.assertEquals(Long.MAX_VALUE, retMap.get("file_flush_dlt_min").longValue());
        Assert.assertEquals(Long.MIN_VALUE, retMap.get("file_flush_dlt_max").longValue());
        Assert.assertNotNull(retMap.get("end_time"));
        retMap.clear();
        // get content by StringBuilder
        StringBuilder strBuff = new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE);
        msgStoreStatsHolder.getValue(strBuff);
        // System.out.println(strBuff.toString());
        strBuff.delete(0, strBuff.length());
        // test timeout
        msgStoreStatsHolder.addFileTimeoutFlushStats(1, 1, true);
        msgStoreStatsHolder.getValue(retMap);
        Assert.assertNotNull(retMap.get("reset_time"));
        Assert.assertEquals(0, retMap.get("file_total_msg_cnt").longValue());
        Assert.assertEquals(0, retMap.get("file_total_data_size").longValue());
        Assert.assertEquals(0, retMap.get("file_total_index_size").longValue());
        Assert.assertEquals(1, retMap.get("file_flush_data_size_count").longValue());
        Assert.assertEquals(1, retMap.get("file_flush_data_size_max").longValue());
        Assert.assertEquals(1, retMap.get("file_flush_data_size_min").longValue());
        Assert.assertEquals(1, retMap.get("file_flush_msg_cnt_count").longValue());
        Assert.assertEquals(1, retMap.get("file_flush_msg_cnt_max").longValue());
        Assert.assertEquals(1, retMap.get("file_flush_msg_cnt_min").longValue());
        Assert.assertEquals(0, retMap.get("file_data_seg").longValue());
        Assert.assertEquals(0, retMap.get("file_index_seg").longValue());
        Assert.assertEquals(1, retMap.get("file_meta_flush").longValue());
        Assert.assertEquals(0, retMap.get("file_data_full").longValue());
        Assert.assertEquals(0, retMap.get("file_count_full").longValue());
        Assert.assertEquals(1, retMap.get("file_time_full").longValue());
        Assert.assertEquals(0, retMap.get("file_flush_dlt_count").longValue());
        Assert.assertEquals(Long.MAX_VALUE, retMap.get("file_flush_dlt_min").longValue());
        Assert.assertEquals(Long.MIN_VALUE, retMap.get("file_flush_dlt_max").longValue());
        Assert.assertNotNull(retMap.get("end_time"));
        retMap.clear();
        // get value when started
        msgStoreStatsHolder.addFileFlushStatsInfo(1, 1, 1,
                1, 1, true, false,
                false, false, false, false,
                6);
        msgStoreStatsHolder.addFileFlushStatsInfo(6, 6, 6,
                6, 6, false, false,
                false, false, false, true,
                100);
        msgStoreStatsHolder.addFileFlushStatsInfo(2, 2, 2,
                2, 2, false, true,
                false, false, false, false,
                10);
        msgStoreStatsHolder.addFileFlushStatsInfo(5, 5, 5,
                5, 5, false, false,
                false, false, true, false,
                200);
        msgStoreStatsHolder.addFileFlushStatsInfo(4, 4, 4,
                4, 4, false, false,
                false, true, false, false,
                50);
        msgStoreStatsHolder.addFileFlushStatsInfo(3, 3, 3,
                3, 3, false, false,
                true, false, false, false,
                150);
        msgStoreStatsHolder.snapShort(retMap);
        Assert.assertNotNull(retMap.get("reset_time"));
        Assert.assertEquals(21, retMap.get("file_total_msg_cnt").longValue());
        Assert.assertEquals(21, retMap.get("file_total_data_size").longValue());
        Assert.assertEquals(21, retMap.get("file_total_index_size").longValue());
        Assert.assertEquals(7, retMap.get("file_flush_data_size_count").longValue());
        Assert.assertEquals(6, retMap.get("file_flush_data_size_max").longValue());
        Assert.assertEquals(1, retMap.get("file_flush_data_size_min").longValue());
        Assert.assertEquals(7, retMap.get("file_flush_msg_cnt_count").longValue());
        Assert.assertEquals(6, retMap.get("file_flush_msg_cnt_max").longValue());
        Assert.assertEquals(1, retMap.get("file_flush_msg_cnt_min").longValue());
        Assert.assertEquals(1, retMap.get("file_data_seg").longValue());
        Assert.assertEquals(1, retMap.get("file_index_seg").longValue());
        Assert.assertEquals(1, retMap.get("file_data_full").longValue());
        Assert.assertEquals(2, retMap.get("file_meta_flush").longValue());
        Assert.assertEquals(1, retMap.get("file_count_full").longValue());
        Assert.assertEquals(2, retMap.get("file_time_full").longValue());
        Assert.assertEquals(6, retMap.get("file_flush_dlt_count").longValue());
        Assert.assertEquals(6, retMap.get("file_flush_dlt_min").longValue());
        Assert.assertEquals(200, retMap.get("file_flush_dlt_max").longValue());
        Assert.assertNotNull(retMap.get("end_time"));
        retMap.clear();
        msgStoreStatsHolder.getMsgStoreStatsInfo(true, strBuff);
        // System.out.println(strBuff.toString());
        strBuff.delete(0, strBuff.length());
    }
}
