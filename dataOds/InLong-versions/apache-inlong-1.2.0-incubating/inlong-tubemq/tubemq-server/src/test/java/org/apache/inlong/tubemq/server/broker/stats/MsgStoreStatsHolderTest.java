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
        msgStoreStatsHolder.addCacheMsgSize(50);
        msgStoreStatsHolder.addCacheFullType(true, false, false);
        msgStoreStatsHolder.addCacheFullType(false, true, false);
        msgStoreStatsHolder.addCacheFullType(false, false, true);
        msgStoreStatsHolder.addCacheFlushTime(50, false);
        msgStoreStatsHolder.addCacheFlushTime(10, true);
        msgStoreStatsHolder.addMsgWriteCacheFail();
        Map<String, Long> retMap = new LinkedHashMap<>();
        msgStoreStatsHolder.getValue(retMap);
        Assert.assertNotNull(retMap.get("reset_time"));
        Assert.assertEquals(0, retMap.get("cache_msg_in_count").longValue());
        Assert.assertEquals(Long.MIN_VALUE, retMap.get("cache_msg_in_max").longValue());
        Assert.assertEquals(Long.MAX_VALUE, retMap.get("cache_msg_in_min").longValue());
        Assert.assertEquals(0, retMap.get("cache_append_fail").longValue());
        Assert.assertEquals(0, retMap.get("cache_data_full").longValue());
        Assert.assertEquals(0, retMap.get("cache_index_full").longValue());
        Assert.assertEquals(0, retMap.get("cache_count_full").longValue());
        Assert.assertEquals(0, retMap.get("cache_time_full").longValue());
        Assert.assertEquals(0, retMap.get("cache_flush_pending").longValue());
        Assert.assertEquals(0, retMap.get("cache_realloc").longValue());
        Assert.assertEquals(0, retMap.get("cache_flush_dlt_count").longValue());
        Assert.assertEquals(Long.MIN_VALUE, retMap.get("cache_flush_dlt_max").longValue());
        Assert.assertEquals(Long.MAX_VALUE, retMap.get("cache_flush_dlt_min").longValue());
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
        msgStoreStatsHolder.addCacheMsgSize(50);
        msgStoreStatsHolder.addCacheMsgSize(500);
        msgStoreStatsHolder.addCacheMsgSize(5);
        msgStoreStatsHolder.addCacheFullType(true, false, false);
        msgStoreStatsHolder.addCacheFullType(false, true, false);
        msgStoreStatsHolder.addCacheFullType(false, false, true);
        msgStoreStatsHolder.addCacheFlushTime(50, false);
        msgStoreStatsHolder.addCacheFlushTime(10, true);
        msgStoreStatsHolder.addCacheFlushTime(100, true);
        msgStoreStatsHolder.addCacheFlushTime(1, false);
        msgStoreStatsHolder.addMsgWriteCacheFail();
        msgStoreStatsHolder.addMsgWriteCacheFail();
        msgStoreStatsHolder.addCacheReAlloc();
        msgStoreStatsHolder.addCacheReAlloc();
        msgStoreStatsHolder.addCachePending();
        msgStoreStatsHolder.addCachePending();
        msgStoreStatsHolder.addCachePending();
        msgStoreStatsHolder.getValue(retMap);
        Assert.assertNotNull(retMap.get("reset_time"));
        Assert.assertEquals(3, retMap.get("cache_msg_in_count").longValue());
        Assert.assertEquals(500, retMap.get("cache_msg_in_max").longValue());
        Assert.assertEquals(5, retMap.get("cache_msg_in_min").longValue());
        Assert.assertEquals(2, retMap.get("cache_append_fail").longValue());
        Assert.assertEquals(1, retMap.get("cache_data_full").longValue());
        Assert.assertEquals(1, retMap.get("cache_index_full").longValue());
        Assert.assertEquals(1, retMap.get("cache_count_full").longValue());
        Assert.assertEquals(2, retMap.get("cache_time_full").longValue());
        Assert.assertEquals(3, retMap.get("cache_flush_pending").longValue());
        Assert.assertEquals(2, retMap.get("cache_realloc").longValue());
        Assert.assertEquals(4, retMap.get("cache_flush_dlt_count").longValue());
        Assert.assertEquals(100, retMap.get("cache_flush_dlt_max").longValue());
        Assert.assertEquals(1, retMap.get("cache_flush_dlt_min").longValue());
        Assert.assertEquals(1, retMap.get("cache_flush_dlt_cell_0t2").longValue());
        Assert.assertEquals(1, retMap.get("cache_flush_dlt_cell_8t16").longValue());
        Assert.assertEquals(1, retMap.get("cache_flush_dlt_cell_32t64").longValue());
        Assert.assertEquals(1, retMap.get("cache_flush_dlt_cell_64t128").longValue());
        Assert.assertNotNull(retMap.get("end_time"));
        msgStoreStatsHolder.getMsgStoreStatsInfo(false, strBuff);
        // System.out.println("\n the second is : " + strBuff.toString());
        strBuff.delete(0, strBuff.length());
    }

    @Test
    public void testFilePartStats() {
        MsgStoreStatsHolder msgStoreStatsHolder = new MsgStoreStatsHolder();
        // case 1, not started
        msgStoreStatsHolder.addFileFlushStatsInfo(2, 30, 500,
                0, 0, true, true,
                true, true, true, true);
        msgStoreStatsHolder.addFileTimeoutFlushStats(1, 500, false);
        Map<String, Long> retMap = new LinkedHashMap<>();
        msgStoreStatsHolder.getValue(retMap);
        Assert.assertNotNull(retMap.get("reset_time"));
        Assert.assertEquals(0, retMap.get("file_total_msg_cnt").longValue());
        Assert.assertEquals(0, retMap.get("file_total_data_size").longValue());
        Assert.assertEquals(0, retMap.get("file_total_index_size").longValue());
        Assert.assertEquals(0, retMap.get("file_flushed_data_count").longValue());
        Assert.assertEquals(Long.MIN_VALUE, retMap.get("file_flushed_data_max").longValue());
        Assert.assertEquals(Long.MAX_VALUE, retMap.get("file_flushed_data_min").longValue());
        Assert.assertEquals(0, retMap.get("file_flushed_msg_count").longValue());
        Assert.assertEquals(Long.MIN_VALUE, retMap.get("file_flushed_msg_max").longValue());
        Assert.assertEquals(Long.MAX_VALUE, retMap.get("file_flushed_msg_min").longValue());
        Assert.assertEquals(0, retMap.get("file_index_seg").longValue());
        Assert.assertEquals(0, retMap.get("file_meta_flush").longValue());
        Assert.assertEquals(0, retMap.get("file_data_full").longValue());
        Assert.assertEquals(0, retMap.get("file_count_full").longValue());
        Assert.assertEquals(0, retMap.get("file_time_full").longValue());
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
        Assert.assertEquals(1, retMap.get("file_flushed_data_count").longValue());
        Assert.assertEquals(1, retMap.get("file_flushed_data_max").longValue());
        Assert.assertEquals(1, retMap.get("file_flushed_data_min").longValue());
        Assert.assertEquals(1, retMap.get("file_flushed_msg_count").longValue());
        Assert.assertEquals(1, retMap.get("file_flushed_msg_max").longValue());
        Assert.assertEquals(1, retMap.get("file_flushed_msg_min").longValue());
        Assert.assertEquals(0, retMap.get("file_data_seg").longValue());
        Assert.assertEquals(0, retMap.get("file_index_seg").longValue());
        Assert.assertEquals(1, retMap.get("file_meta_flush").longValue());
        Assert.assertEquals(0, retMap.get("file_data_full").longValue());
        Assert.assertEquals(0, retMap.get("file_count_full").longValue());
        Assert.assertEquals(1, retMap.get("file_time_full").longValue());
        Assert.assertNotNull(retMap.get("end_time"));
        retMap.clear();
        // get value when started
        msgStoreStatsHolder.addFileFlushStatsInfo(1, 1, 1,
                1, 1, true, false,
                false, false, false, false);
        msgStoreStatsHolder.addFileFlushStatsInfo(6, 6, 6,
                6, 6, false, false,
                false, false, false, true);
        msgStoreStatsHolder.addFileFlushStatsInfo(2, 2, 2,
                2, 2, false, true,
                false, false, false, false);
        msgStoreStatsHolder.addFileFlushStatsInfo(5, 5, 5,
                5, 5, false, false,
                false, false, true, false);
        msgStoreStatsHolder.addFileFlushStatsInfo(4, 4, 4,
                4, 4, false, false,
                false, true, false, false);
        msgStoreStatsHolder.addFileFlushStatsInfo(3, 3, 3,
                3, 3, false, false,
                true, false, false, false);
        msgStoreStatsHolder.snapShort(retMap);
        Assert.assertNotNull(retMap.get("reset_time"));
        Assert.assertEquals(21, retMap.get("file_total_msg_cnt").longValue());
        Assert.assertEquals(21, retMap.get("file_total_data_size").longValue());
        Assert.assertEquals(21, retMap.get("file_total_index_size").longValue());
        Assert.assertEquals(7, retMap.get("file_flushed_data_count").longValue());
        Assert.assertEquals(6, retMap.get("file_flushed_data_max").longValue());
        Assert.assertEquals(1, retMap.get("file_flushed_data_min").longValue());
        Assert.assertEquals(7, retMap.get("file_flushed_msg_count").longValue());
        Assert.assertEquals(6, retMap.get("file_flushed_msg_max").longValue());
        Assert.assertEquals(1, retMap.get("file_flushed_msg_min").longValue());
        Assert.assertEquals(1, retMap.get("file_data_seg").longValue());
        Assert.assertEquals(1, retMap.get("file_index_seg").longValue());
        Assert.assertEquals(1, retMap.get("file_data_full").longValue());
        Assert.assertEquals(2, retMap.get("file_meta_flush").longValue());
        Assert.assertEquals(1, retMap.get("file_count_full").longValue());
        Assert.assertEquals(2, retMap.get("file_time_full").longValue());
        Assert.assertNotNull(retMap.get("end_time"));
        retMap.clear();
        msgStoreStatsHolder.getMsgStoreStatsInfo(true, strBuff);
        // System.out.println(strBuff.toString());
        strBuff.delete(0, strBuff.length());
    }
}
