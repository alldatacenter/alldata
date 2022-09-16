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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.metric.impl.ESTHistogram;
import org.apache.inlong.tubemq.corebase.metric.impl.LongStatsCounter;
import org.apache.inlong.tubemq.corebase.metric.impl.SimpleHistogram;
import org.apache.inlong.tubemq.corebase.metric.impl.SinceTime;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;

/**
 * MsgStoreStatsHolder, The statistics set related to the message store,
 * include cache and file store statistics.
 *
 * Through this file storage statistics class, it mainly counts
 * the number of messages refreshed each time, the total data size, the total index size,
 * the total number of times the data file is full, the number of times the index file is full,
 * as well as the conditions that trigger the flush, the data write information such as
 * the number of refreshes that enter the timeout, etc.
 *
 * This part supports index comparison output before and after data collection.
 */
public class MsgStoreStatsHolder {
    // Switchable statistic items
    private final MsgStoreStatsItemSet[] msgStoreStatsSets = new MsgStoreStatsItemSet[2];
    // Current writable index
    private final AtomicInteger writableIndex = new AtomicInteger(0);
    // Last query time
    private final AtomicLong lstQueryTime = new AtomicLong(0);
    // Last snapshot time
    private final AtomicLong lstSnapshotTime = new AtomicLong(0);
    // Whether closed for unused for a long time
    private volatile boolean isClosed;
    // whether the statistic is manual closed
    private volatile boolean isManualClosed = false;

    public MsgStoreStatsHolder() {
        this.isClosed = true;
        this.msgStoreStatsSets[0] = new MsgStoreStatsItemSet();
        this.msgStoreStatsSets[1] = new MsgStoreStatsItemSet();
        this.lstQueryTime.set(System.currentTimeMillis());
        this.lstSnapshotTime.set(System.currentTimeMillis());
    }

    /**
     * Add appended message size statistic.
     *
     * @param msgSize   the message size
     */
    public void addCacheMsgSize(int msgSize) {
        if (isClosed) {
            return;
        }
        msgStoreStatsSets[getIndex()].cacheMsgRcvStats.update(msgSize);
    }

    /**
     * Add write message failure count statistics.
     */
    public void addMsgWriteCacheFail() {
        if (isClosed) {
            return;
        }
        msgStoreStatsSets[getIndex()].cacheWriteFailCnt.incValue();
    }

    /**
     * Add cache pending count statistics.
     */
    public void addCachePending() {
        if (isClosed) {
            return;
        }
        msgStoreStatsSets[getIndex()].cacheFlushPendingCnt.incValue();
    }

    /**
     * Add cache re-alloc count statistics.
     */
    public void addCacheReAlloc() {
        if (isClosed) {
            return;
        }
        msgStoreStatsSets[getIndex()].cacheReAllocCnt.incValue();
    }

    /**
     * Add flush trigger type statistics.
     *
     * @param isDataSizeFull     whether the cached data is full
     * @param isIndexSizeFull    whether the cached index is full
     * @param isMsgCntFull       whether the cached message count is full
     */
    public void addCacheFullType(boolean isDataSizeFull,
                                 boolean isIndexSizeFull,
                                 boolean isMsgCntFull) {
        if (isClosed) {
            return;
        }
        MsgStoreStatsItemSet tmStatsSet = msgStoreStatsSets[getIndex()];
        if (isDataSizeFull) {
            tmStatsSet.cacheDataSizeFullCnt.incValue();
        }
        if (isIndexSizeFull) {
            tmStatsSet.cacheIndexSizeFullCnt.incValue();
        }
        if (isMsgCntFull) {
            tmStatsSet.cacheMsgCountFullCnt.incValue();
        }
    }

    /**
     * Add flush time statistic.
     *
     * @param flushTime          the flush time
     * @param isTimeoutFlush     whether is timeout flush
     */
    public void addCacheFlushTime(long flushTime, boolean isTimeoutFlush) {
        if (isClosed) {
            return;
        }
        MsgStoreStatsItemSet tmStatsSet = msgStoreStatsSets[getIndex()];
        tmStatsSet.cacheSyncStats.update(flushTime);
        if (isTimeoutFlush) {
            tmStatsSet.cacheTimeFullCnt.incValue();
        }
    }

    /**
     * Add message store statistics information.
     *
     * @param msgCnt             the message count written
     * @param msgIndexSize       the message index size written
     * @param msgDataSize        the message data size written
     * @param flushedMsgCnt      the flushed message count
     * @param flushedDataSize    the flushed message size
     * @param isDataSegFlush     whether the data segment flushed
     * @param isIndexSegFlush    whether the index segment flushed
     * @param isDataSizeFull     whether the cached data is full
     * @param isMsgCntFull       whether the cached message count is full
     * @param isCacheTimeFull    whether the cached time is full
     * @param isForceMetadata    whether force push metadata
     */
    public void addFileFlushStatsInfo(int msgCnt, int msgIndexSize, int msgDataSize,
                                      long flushedMsgCnt, long flushedDataSize,
                                      boolean isDataSegFlush, boolean isIndexSegFlush,
                                      boolean isDataSizeFull, boolean isMsgCntFull,
                                      boolean isCacheTimeFull, boolean isForceMetadata) {
        if (isClosed) {
            return;
        }
        MsgStoreStatsItemSet tmStatsSet = msgStoreStatsSets[getIndex()];
        tmStatsSet.fileAccumMsgCnt.addValue(msgCnt);
        tmStatsSet.fileAccumMsgIndexSize.addValue(msgIndexSize);
        tmStatsSet.fileAccumMsgDataSize.addValue(msgDataSize);
        if (flushedDataSize > 0) {
            tmStatsSet.fileFlushedDataSize.update(flushedDataSize);
        }
        if (flushedMsgCnt > 0) {
            tmStatsSet.fileFlushedMsgCnt.update(flushedMsgCnt);
        }
        if (isDataSegFlush) {
            tmStatsSet.fileDataSegAddCnt.incValue();
        }
        if (isIndexSegFlush) {
            tmStatsSet.fileIndexSegAddCnt.incValue();
        }
        if (isDataSizeFull) {
            tmStatsSet.fileDataSizeFullCnt.incValue();
        }
        if (isMsgCntFull) {
            tmStatsSet.fileMsgCountFullCnt.incValue();
        }
        if (isCacheTimeFull) {
            tmStatsSet.fileCachedTimeFullCnt.incValue();
        }
        if (isForceMetadata) {
            tmStatsSet.fileMetaFlushCnt.incValue();
        }
    }

    /**
     * Add flush time timeout statistic.
     *
     * @param flushedMsgCnt      the flushed message count
     * @param flushedDataSize    the flushed message size
     * @param isForceMetadata    whether force push metadata
     */
    public void addFileTimeoutFlushStats(long flushedMsgCnt,
                                         long flushedDataSize,
                                         boolean isForceMetadata) {
        if (isClosed) {
            return;
        }
        MsgStoreStatsItemSet tmStatsSet = msgStoreStatsSets[getIndex()];
        tmStatsSet.fileCachedTimeFullCnt.incValue();
        if (flushedDataSize > 0) {
            tmStatsSet.fileFlushedDataSize.update(flushedDataSize);
        }
        if (flushedMsgCnt > 0) {
            tmStatsSet.fileFlushedMsgCnt.update(flushedMsgCnt);
        }
        if (isForceMetadata) {
            tmStatsSet.fileMetaFlushCnt.incValue();
        }
    }

    /**
     * Check whether has exceeded the maximum self-statistics period.
     *
     * @param checkTime   the check time
     */
    public synchronized void chkStatsExpired(long checkTime) {
        if (!this.isClosed) {
            if ((checkTime - this.lstQueryTime.get())
                    >= TServerConstants.CFG_STORE_STATS_MAX_REFRESH_DURATION) {
                this.isClosed = true;
            }
        }
    }

    /**
     * Set manually the statistic status.
     *
     * @param enableStats  enable or disable the statistic.
     */
    public synchronized void setStatsStatus(boolean enableStats) {
        if (enableStats) {
            this.isManualClosed = false;
        } else {
            this.isManualClosed = true;
            this.isClosed = true;
        }
    }

    /**
     * Query whether the statistic is closed.
     *
     * @return the statistic status
     */
    public boolean isStatsClosed() {
        return (this.isManualClosed || this.isClosed);
    }

    /**
     * Get current writing statistics information.
     *
     * @param statsMap  the return map information
     */
    public void getValue(Map<String, Long> statsMap) {
        activeStatsBaseCall();
        getStatsValue(true, msgStoreStatsSets[getIndex()], statsMap);
    }

    /**
     * Get current writing statistics information.
     *
     * @param strBuff  the return information in json format
     */
    public void getValue(StringBuilder strBuff) {
        activeStatsBaseCall();
        getStatsValue(true, msgStoreStatsSets[getIndex()], strBuff);
    }

    /**
     * Snapshot and get current writing statistics information.
     *
     * @param statsMap  the return map information
     */
    public void snapShort(Map<String, Long> statsMap) {
        activeStatsBaseCall();
        if (switchStatsSets()) {
            getStatsValue(false,
                    msgStoreStatsSets[getIndex(writableIndex.get() - 1)], statsMap);
        } else {
            getStatsValue(true, msgStoreStatsSets[getIndex()], statsMap);
        }
    }

    /**
     * Snapshot and get current writing statistics information.
     *
     * @param strBuff  the return information in json format
     */
    public void snapShort(StringBuilder strBuff) {
        this.activeStatsBaseCall();
        if (switchStatsSets()) {
            getStatsValue(false,
                    msgStoreStatsSets[getIndex(writableIndex.get() - 1)], strBuff);
        } else {
            getStatsValue(true, msgStoreStatsSets[getIndex()], strBuff);
        }
    }

    /**
     * Get current message store statistics information. Contains the data results of
     * the current statistics and the previous snapshot
     *
     * @param isSwitch  whether to switch the writing statistics block
     * @param strBuff   the return information
     */
    public synchronized void getMsgStoreStatsInfo(boolean isSwitch, StringBuilder strBuff) {
        this.activeStatsBaseCall();
        strBuff.append("[");
        getStatsValue(false,
                msgStoreStatsSets[getIndex(writableIndex.get() - 1)], strBuff);
        strBuff.append(",");
        getStatsValue(true, msgStoreStatsSets[getIndex()], strBuff);
        strBuff.append("]");
        if (isSwitch) {
            switchStatsSets();
        }
    }

    /**
     * Active statistic status based on api call
     *
     */
    private void activeStatsBaseCall() {
        if (isManualClosed) {
            return;
        }
        this.lstQueryTime.set(System.currentTimeMillis());
        if (this.isClosed) {
            this.isClosed = false;
        }
    }

    /**
     * Check and switch statistic sets
     *
     * @return  whether the statistic sets has been switched
     */
    private boolean switchStatsSets() {
        long curSwitchTime = lstSnapshotTime.get();
        // Avoid frequent snapshots
        if ((System.currentTimeMillis() - curSwitchTime)
                >= TBaseConstants.CFG_STATS_MIN_SNAPSHOT_PERIOD_MS) {
            if (lstSnapshotTime.compareAndSet(curSwitchTime, System.currentTimeMillis())) {
                msgStoreStatsSets[getIndex(writableIndex.get() - 1)].clear();
                msgStoreStatsSets[getIndex(writableIndex.getAndIncrement())]
                        .setSnapshotTime(lstSnapshotTime.get());
                return true;
            }
        }
        return false;
    }

    /**
     * Get current writable block index.
     *
     * @return the writable block index
     */
    private int getIndex() {
        return getIndex(writableIndex.get());
    }

    /**
     * Gets the metric block index based on the specified value.
     *
     * @param origIndex    the specified value
     * @return the metric block index
     */
    private int getIndex(int origIndex) {
        return Math.abs(origIndex % 2);
    }

    /**
     * Read metric block data into map.
     *
     * @param isWriting    the metric block is writing
     * @param statsSet     the metric block need to read
     * @param statsMap     the read result
     */
    private void getStatsValue(boolean isWriting,
                               MsgStoreStatsItemSet statsSet,
                               Map<String, Long> statsMap) {
        statsMap.put(statsSet.resetTime.getFullName(),
                statsSet.resetTime.getSinceTime());
        statsMap.put("isClosed", (isStatsClosed() ? 1L : 0L));
        // for memory store
        statsSet.cacheMsgRcvStats.getValue(statsMap, false);
        statsMap.put(statsSet.cacheWriteFailCnt.getFullName(),
                statsSet.cacheWriteFailCnt.getValue());
        statsMap.put(statsSet.cacheDataSizeFullCnt.getFullName(),
                statsSet.cacheDataSizeFullCnt.getValue());
        statsMap.put(statsSet.cacheIndexSizeFullCnt.getFullName(),
                statsSet.cacheIndexSizeFullCnt.getValue());
        statsMap.put(statsSet.cacheMsgCountFullCnt.getFullName(),
                statsSet.cacheMsgCountFullCnt.getValue());
        statsMap.put(statsSet.cacheTimeFullCnt.getFullName(),
                statsSet.cacheTimeFullCnt.getValue());
        statsMap.put(statsSet.cacheFlushPendingCnt.getFullName(),
                statsSet.cacheFlushPendingCnt.getValue());
        statsMap.put(statsSet.cacheReAllocCnt.getFullName(),
                statsSet.cacheReAllocCnt.getValue());
        statsSet.cacheSyncStats.getValue(statsMap, false);
        // for file store
        statsMap.put(statsSet.fileAccumMsgCnt.getFullName(),
                statsSet.fileAccumMsgCnt.getValue());
        statsMap.put(statsSet.fileAccumMsgDataSize.getFullName(),
                statsSet.fileAccumMsgDataSize.getValue());
        statsMap.put(statsSet.fileAccumMsgIndexSize.getFullName(),
                statsSet.fileAccumMsgIndexSize.getValue());
        statsSet.fileFlushedDataSize.getValue(statsMap, false);
        statsSet.fileFlushedMsgCnt.getValue(statsMap, false);
        statsMap.put(statsSet.fileDataSegAddCnt.getFullName(),
                statsSet.fileDataSegAddCnt.getValue());
        statsMap.put(statsSet.fileIndexSegAddCnt.getFullName(),
                statsSet.fileIndexSegAddCnt.getValue());
        statsMap.put(statsSet.fileMetaFlushCnt.getFullName(),
                statsSet.fileMetaFlushCnt.getValue());
        statsMap.put(statsSet.fileDataSizeFullCnt.getFullName(),
                statsSet.fileDataSizeFullCnt.getValue());
        statsMap.put(statsSet.fileMsgCountFullCnt.getFullName(),
                statsSet.fileMsgCountFullCnt.getValue());
        statsMap.put(statsSet.fileCachedTimeFullCnt.getFullName(),
                statsSet.fileCachedTimeFullCnt.getValue());
        if (isWriting) {
            statsMap.put(statsSet.snapShotTime.getFullName(),
                    System.currentTimeMillis());
        } else {
            statsMap.put(statsSet.snapShotTime.getFullName(),
                    statsSet.snapShotTime.getSinceTime());
        }
    }

    /**
     * Read metric block data into string format.
     *
     * @param isWriting    the metric block is writing
     * @param statsSet     the metric block need to read
     * @param strBuff     the return buffer
     */
    private void getStatsValue(boolean isWriting,
                               MsgStoreStatsItemSet statsSet,
                               StringBuilder strBuff) {
        strBuff.append("{\"").append(statsSet.resetTime.getFullName())
                .append("\":\"").append(statsSet.resetTime.getStrSinceTime())
                .append("\",\"isClosed\":").append(isStatsClosed()).append(",");
        statsSet.cacheMsgRcvStats.getValue(strBuff, false);
        strBuff.append(",\"").append(statsSet.cacheWriteFailCnt.getFullName())
                .append("\":").append(statsSet.cacheWriteFailCnt.getValue())
                .append(",\"").append(statsSet.cacheDataSizeFullCnt.getFullName())
                .append("\":").append(statsSet.cacheDataSizeFullCnt.getValue())
                .append(",\"").append(statsSet.cacheMsgCountFullCnt.getFullName())
                .append("\":").append(statsSet.cacheMsgCountFullCnt.getValue())
                .append(",\"").append(statsSet.cacheTimeFullCnt.getFullName())
                .append("\":").append(statsSet.cacheTimeFullCnt.getValue())
                .append(",\"").append(statsSet.cacheFlushPendingCnt.getFullName())
                .append("\":").append(statsSet.cacheFlushPendingCnt.getValue())
                .append(",\"").append(statsSet.cacheReAllocCnt.getFullName())
                .append("\":").append(statsSet.cacheReAllocCnt.getValue())
                .append(",\"").append(statsSet.cacheDataSizeFullCnt.getFullName())
                .append("\":").append(statsSet.cacheDataSizeFullCnt.getValue())
                .append(",");
        statsSet.cacheSyncStats.getValue(strBuff, false);
        strBuff.append(",\"").append(statsSet.fileAccumMsgCnt.getFullName())
                .append("\":").append(statsSet.fileAccumMsgCnt.getValue())
                .append(",\"").append(statsSet.fileAccumMsgDataSize.getFullName())
                .append("\":").append(statsSet.fileAccumMsgDataSize.getValue())
                .append(",\"").append(statsSet.fileAccumMsgIndexSize.getFullName())
                .append("\":").append(statsSet.fileAccumMsgIndexSize.getValue())
                .append(",");
        statsSet.fileFlushedDataSize.getValue(strBuff, false);
        strBuff.append(",");
        statsSet.fileFlushedMsgCnt.getValue(strBuff, false);
        strBuff.append(",\"").append(statsSet.fileDataSegAddCnt.getFullName())
                .append("\":").append(statsSet.fileDataSegAddCnt.getValue())
                .append(",\"").append(statsSet.fileIndexSegAddCnt.getFullName())
                .append("\":").append(statsSet.fileIndexSegAddCnt.getValue())
                .append(",\"").append(statsSet.fileMetaFlushCnt.getFullName())
                .append("\":").append(statsSet.fileMetaFlushCnt.getValue())
                .append(",\"").append(statsSet.fileDataSizeFullCnt.getFullName())
                .append("\":").append(statsSet.fileDataSizeFullCnt.getValue())
                .append(",\"").append(statsSet.fileMsgCountFullCnt.getFullName())
                .append("\":").append(statsSet.fileMsgCountFullCnt.getValue())
                .append(",\"").append(statsSet.fileCachedTimeFullCnt.getFullName())
                .append("\":").append(statsSet.fileCachedTimeFullCnt.getValue())
                .append(",\"").append(statsSet.snapShotTime.getFullName())
                .append("\":\"");
        if (isWriting) {
            strBuff.append(DateTimeConvertUtils.ms2yyyyMMddHHmmss(System.currentTimeMillis()));
        } else {
            strBuff.append(statsSet.snapShotTime.getStrSinceTime());
        }
        strBuff.append("\"}");
    }

    /**
     * MsgStoreStatsItemSet, Message store cache related statistics set
     *
     */
    private static class MsgStoreStatsItemSet {
        // The reset time of statistics set
        protected final SinceTime resetTime =
                new SinceTime("reset_time", null);
        // The status of messages received by cache
        protected final SimpleHistogram cacheMsgRcvStats =
                new SimpleHistogram("cache_msg_in", null);
        // The count of message append cache failures
        protected final LongStatsCounter cacheWriteFailCnt =
                new LongStatsCounter("cache_append_fail", null);
        // The cached message data full statistics
        protected final LongStatsCounter cacheDataSizeFullCnt =
                new LongStatsCounter("cache_data_full", null);
        // The cached message index full statistics
        protected final LongStatsCounter cacheIndexSizeFullCnt =
                new LongStatsCounter("cache_index_full", null);
        // The cached message count full statistics
        protected final LongStatsCounter cacheMsgCountFullCnt =
                new LongStatsCounter("cache_count_full", null);
        // The cache timeout refresh amount statistics
        protected final LongStatsCounter cacheTimeFullCnt =
                new LongStatsCounter("cache_time_full", null);
        // The pending count for cache flush operations
        protected final LongStatsCounter cacheFlushPendingCnt =
                new LongStatsCounter("cache_flush_pending", null);
        // The cache re-alloc count
        protected final LongStatsCounter cacheReAllocCnt =
                new LongStatsCounter("cache_realloc", null);
        // The cache persistence duration statistics
        protected final ESTHistogram cacheSyncStats =
                new ESTHistogram("cache_flush_dlt", null);
        // for file store
        // The accumulate message count statistics
        protected final LongStatsCounter fileAccumMsgCnt =
                new LongStatsCounter("file_total_msg_cnt", null);
        // The accumulate message data size statistics
        protected final LongStatsCounter fileAccumMsgDataSize =
                new LongStatsCounter("file_total_data_size", null);
        // The accumulate message index statistics
        protected final LongStatsCounter fileAccumMsgIndexSize =
                new LongStatsCounter("file_total_index_size", null);
        // The data flushed statistics
        protected final SimpleHistogram fileFlushedDataSize =
                new SimpleHistogram("file_flushed_data", null);
        // The message count flushed statistics
        protected final SimpleHistogram fileFlushedMsgCnt =
                new SimpleHistogram("file_flushed_msg", null);
        // The new data segment statistics
        protected final LongStatsCounter fileDataSegAddCnt =
                new LongStatsCounter("file_data_seg", null);
        // The new index segment full statistics
        protected final LongStatsCounter fileIndexSegAddCnt =
                new LongStatsCounter("file_index_seg", null);
        // The flush count statistics of file meta-data
        protected final LongStatsCounter fileMetaFlushCnt =
                new LongStatsCounter("file_meta_flush", null);
        // The cached message data full statistics
        protected final LongStatsCounter fileDataSizeFullCnt =
                new LongStatsCounter("file_data_full", null);
        // The cached message count full statistics
        protected final LongStatsCounter fileMsgCountFullCnt =
                new LongStatsCounter("file_count_full", null);
        // The cache timeout refresh amount statistics
        protected final LongStatsCounter fileCachedTimeFullCnt =
                new LongStatsCounter("file_time_full", null);
        // The snapshot time of statistics set
        protected final SinceTime snapShotTime =
                new SinceTime("end_time", null);

        public MsgStoreStatsItemSet() {
            clear();
        }

        public void setSnapshotTime(long snapshotTime) {
            this.snapShotTime.reset(snapshotTime);
        }

        public void clear() {
            this.snapShotTime.reset();
            // for file metric items
            this.fileAccumMsgCnt.clear();
            this.fileAccumMsgDataSize.clear();
            this.fileFlushedDataSize.clear();
            this.fileAccumMsgIndexSize.clear();
            this.fileFlushedMsgCnt.clear();
            this.fileDataSegAddCnt.clear();
            this.fileIndexSegAddCnt.clear();
            this.fileDataSizeFullCnt.clear();
            this.fileMetaFlushCnt.clear();
            this.fileMsgCountFullCnt.clear();
            this.fileCachedTimeFullCnt.clear();
            // for cache metric items
            this.cacheMsgRcvStats.clear();
            this.cacheWriteFailCnt.clear();
            this.cacheDataSizeFullCnt.clear();
            this.cacheIndexSizeFullCnt.clear();
            this.cacheMsgCountFullCnt.clear();
            this.cacheFlushPendingCnt.clear();
            this.cacheReAllocCnt.clear();
            this.cacheTimeFullCnt.clear();
            this.cacheSyncStats.clear();
            this.resetTime.reset();
        }
    }
}
