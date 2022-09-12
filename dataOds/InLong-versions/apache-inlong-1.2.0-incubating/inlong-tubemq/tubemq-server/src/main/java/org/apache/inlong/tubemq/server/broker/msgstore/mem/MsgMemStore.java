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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.server.broker.metadata.ClusterConfigHolder;
import org.apache.inlong.tubemq.server.broker.msgstore.disk.MsgFileStore;
import org.apache.inlong.tubemq.server.broker.stats.BrokerSrvStatsHolder;
import org.apache.inlong.tubemq.server.broker.stats.MsgStoreStatsHolder;
import org.apache.inlong.tubemq.server.broker.utils.DataStoreUtils;
import org.apache.inlong.tubemq.server.common.utils.AppendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.DirectBuffer;

/**
 * Message's memory storage. It use direct memory store messages that received but not have been flushed to disk.
 */
public class MsgMemStore implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(MsgMemStore.class);
    // statistics of memory store
    private final AtomicInteger cacheDataOffset = new AtomicInteger(0);
    private final AtomicInteger cacheIndexOffset = new AtomicInteger(0);
    private final AtomicInteger curMessageCount = new AtomicInteger(0);
    private final ReentrantLock writeLock = new ReentrantLock();
    // partitionId to index position, accelerate query
    private final Map<Integer, Integer> queuesMap =
            new HashMap<>(20);
    // key to index position, used for filter consume
    private final Map<Integer, Integer> keysMap =
            new HashMap<>(100);
    // where messages in memory will sink to disk
    private final int maxDataCacheSize;
    private long writeDataStartPos = -1;
    private final ByteBuffer cacheDataSegment;
    private final int maxIndexCacheSize;
    private long writeIndexStartPos = -1;
    private final ByteBuffer cachedIndexSegment;
    private final int maxAllowedMsgCount;
    private final AtomicLong leftAppendTime =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private final AtomicLong rightAppendTime =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);

    /**
     * MsgMemStore, initial message memory cache store block
     *
     * @param maxCacheSize     the allowed max cache data size
     * @param maxMsgCount      the allowed max cache data size
     * @param writeDataStartPos     the data start position
     * @param writeIndexStartPos    the data start position
     */
    public MsgMemStore(int maxCacheSize, int maxMsgCount,
                       long writeDataStartPos, long writeIndexStartPos) {
        this.maxDataCacheSize = maxCacheSize;
        this.maxAllowedMsgCount = maxMsgCount;
        this.maxIndexCacheSize = this.maxAllowedMsgCount * DataStoreUtils.STORE_INDEX_HEAD_LEN;
        this.cacheDataSegment = ByteBuffer.allocateDirect(this.maxDataCacheSize);
        this.cachedIndexSegment = ByteBuffer.allocateDirect(this.maxIndexCacheSize);
        this.leftAppendTime.set(System.currentTimeMillis());
        this.rightAppendTime.set(System.currentTimeMillis());
        this.writeDataStartPos = writeDataStartPos;
        this.writeIndexStartPos = writeIndexStartPos;
    }

    /**
     * Reset the memory store status values.
     *
     * @param writeDataStartPos     the data start position
     * @param writeIndexStartPos    the data start position
     */
    public void resetMemStoreStatus(long writeDataStartPos, long writeIndexStartPos) {
        this.keysMap.clear();
        this.queuesMap.clear();
        this.cacheDataOffset.set(0);
        this.cacheIndexOffset.set(0);
        this.curMessageCount.set(0);
        this.cacheDataSegment.rewind();
        this.cachedIndexSegment.rewind();
        this.writeDataStartPos = writeDataStartPos;
        this.writeIndexStartPos = writeIndexStartPos;
        this.leftAppendTime.set(System.currentTimeMillis());
        this.rightAppendTime.set(System.currentTimeMillis());
    }

    /**
     * Append message to memory cache
     *
     * @param memStatsHolder    statistical information object
     * @param partitionId       the partitionId for append messages
     * @param keyCode           the filter item hash code
     * @param timeRecv          the received timestamp
     * @param indexEntry        the stored index entry
     * @param dataEntryLength   the stored data entry length
     * @param dataEntry         the stored data entry
     * @param appendResult      the append result
     *
     * @return    the process result
     */
    public boolean appendMsg(MsgStoreStatsHolder memStatsHolder,
                             int partitionId, int keyCode, long timeRecv,
                             ByteBuffer indexEntry, int dataEntryLength,
                             ByteBuffer dataEntry, AppendResult appendResult) {
        long dataOffset;
        long indexOffset;
        int indexSizePos;
        boolean isAppended = true;
        boolean fullDataSize = false;
        boolean fullIndexSize = false;
        boolean fullCount = false;
        this.writeLock.lock();
        try {
            // judge whether can write to memory or not.
            if ((fullDataSize = (this.cacheDataOffset.get() + dataEntryLength > this.maxDataCacheSize))
                    || (fullCount = (this.curMessageCount.get() + 1 > maxAllowedMsgCount))
                    || (fullIndexSize =
                    (this.cacheIndexOffset.get() + DataStoreUtils.STORE_INDEX_HEAD_LEN > this.maxIndexCacheSize))) {
                isAppended = false;
                return false;
            }
            // conduct message with filling process
            indexOffset = this.writeIndexStartPos + this.cacheIndexOffset.get();
            dataOffset = this.writeDataStartPos + this.cacheDataOffset.get();
            indexEntry.putLong(DataStoreUtils.INDEX_POS_DATAOFFSET, dataOffset);
            dataEntry.putLong(DataStoreUtils.STORE_HEADER_POS_QUEUE_LOGICOFF, indexOffset);
            this.cacheDataSegment.put(dataEntry.array());
            this.cachedIndexSegment.put(indexEntry.array());
            this.cacheDataOffset.getAndAdd(dataEntryLength);
            indexSizePos = cacheIndexOffset.getAndAdd(DataStoreUtils.STORE_INDEX_HEAD_LEN);
            this.queuesMap.put(partitionId, indexSizePos);
            this.keysMap.put(keyCode, indexSizePos);
            this.curMessageCount.getAndIncrement();
            this.rightAppendTime.set(timeRecv);
            if (indexSizePos == 0) {
                this.leftAppendTime.set(timeRecv);
            }
        } finally {
            this.writeLock.unlock();
            if (isAppended) {
                memStatsHolder.addCacheMsgSize(dataEntryLength);
            } else {
                memStatsHolder.addCacheFullType(fullDataSize, fullIndexSize, fullCount);
            }
        }
        appendResult.putAppendResult(indexOffset, dataOffset);
        return true;
    }

    /**
     * Read from memory, read index, then data.
     *
     * @param lstRdDataOffset       the recent data offset read before
     * @param lstRdIndexOffset      the recent index offset read before
     * @param maxReadSize           the max read size
     * @param maxReadCount          the max read count
     * @param partitionId           the partitionId for reading messages
     * @param isSecond              whether read from secondary cache
     * @param isFilterConsume       whether to filter consumption
     * @param filterKeySet          filter item set
     * @param reqRcvTime            the timestamp of the record to be checked
     *
     * @return                      read result
     */
    public GetCacheMsgResult getMessages(long lstRdDataOffset, long lstRdIndexOffset,
                                         int maxReadSize, int maxReadCount,
                                         int partitionId, boolean isSecond,
                                         boolean isFilterConsume, Set<Integer> filterKeySet,
                                         long reqRcvTime) {
        // #lizard forgives
        Integer lastWritePos = 0;
        boolean hasMsg = false;
        // judge memory contains the given offset or not.
        List<ByteBuffer> cacheMsgList = new ArrayList<>();
        if (lstRdIndexOffset < this.writeIndexStartPos) {
            return new GetCacheMsgResult(false, TErrCodeConstants.MOVED,
                    lstRdIndexOffset, "Request offset lower than cache minOffset");
        }
        if (lstRdIndexOffset >= this.writeIndexStartPos + this.cacheIndexOffset.get()) {
            return new GetCacheMsgResult(false, TErrCodeConstants.NOT_FOUND,
                    lstRdIndexOffset, "Request offset reached cache maxOffset");
        }
        int totalReadSize = 0;
        int currIndexOffset;
        int currDataOffset;
        long lastDataRdOff = lstRdDataOffset;
        int startReadOff = (int) (lstRdIndexOffset - this.writeIndexStartPos);
        this.writeLock.lock();
        try {
            if (isFilterConsume) {
                // filter conduct. accelerate by keysMap.
                for (Integer keyCode : filterKeySet) {
                    if (keyCode != null) {
                        lastWritePos = this.keysMap.get(keyCode);
                        if ((lastWritePos != null) && (lastWritePos >= startReadOff)) {
                            hasMsg = true;
                            break;
                        }
                    }
                }
            } else {
                // orderly consume by partition id.
                lastWritePos = this.queuesMap.get(partitionId);
                if ((lastWritePos != null) && (lastWritePos >= startReadOff)) {
                    hasMsg = true;
                }
            }
            currDataOffset = this.cacheDataOffset.get();
            currIndexOffset = this.cacheIndexOffset.get();
            lastDataRdOff = this.writeDataStartPos + currDataOffset;
        } finally {
            this.writeLock.unlock();
        }
        int limitReadSize = currIndexOffset - startReadOff;
        // cannot find message, return not found
        if (!hasMsg) {
            if (isSecond && !isFilterConsume) {
                return new GetCacheMsgResult(true, 0, "Ok2",
                        lstRdIndexOffset, limitReadSize, lastDataRdOff, totalReadSize, cacheMsgList);
            } else {
                return new GetCacheMsgResult(false, TErrCodeConstants.NOT_FOUND,
                        "Can't found Message by index!", lstRdIndexOffset,
                        limitReadSize, lastDataRdOff, totalReadSize, cacheMsgList);
            }
        }
        // fetch data by index.
        int readedSize = 0;
        int cPartitionId = 0;
        long cDataPos = 0L;
        int cDataSize = 0;
        int cKeyCode = 0;
        long cTimeRecv = 0L;
        int cDataOffset = 0;
        ByteBuffer tmpIndexRdBuf = this.cachedIndexSegment.asReadOnlyBuffer();
        ByteBuffer tmpDataRdBuf = this.cacheDataSegment.asReadOnlyBuffer();
        // loop read by index
        for (int count = 0; count < maxReadCount;
             count++, startReadOff += DataStoreUtils.STORE_INDEX_HEAD_LEN) {
            // cannot find matched message, return
            if ((startReadOff >= currIndexOffset)
                || (startReadOff + DataStoreUtils.STORE_INDEX_HEAD_LEN > currIndexOffset)) {
                break;
            }
            // read index content.
            tmpIndexRdBuf.position(startReadOff);
            cPartitionId = tmpIndexRdBuf.getInt();
            cDataPos = tmpIndexRdBuf.getLong();
            cDataSize = tmpIndexRdBuf.getInt();
            cKeyCode = tmpIndexRdBuf.getInt();
            cTimeRecv = tmpIndexRdBuf.getLong();
            cDataOffset = (int) (cDataPos - this.writeDataStartPos);
            // skip when mismatch condition
            if ((cDataOffset < 0)
                    || (cDataSize <= 0)
                    || (cDataOffset >= currDataOffset)
                    || (cDataSize > ClusterConfigHolder.getMaxMsgSize())
                    || (cDataOffset + cDataSize > currDataOffset)) {
                readedSize += DataStoreUtils.STORE_INDEX_HEAD_LEN;
                continue;
            }
            if ((cPartitionId != partitionId)
                    || (isFilterConsume && (!filterKeySet.contains(cKeyCode)))) {
                readedSize += DataStoreUtils.STORE_INDEX_HEAD_LEN;
                continue;
            }
            if (reqRcvTime != 0 && cTimeRecv < reqRcvTime) {
                continue;
            }
            // read data file.
            byte[] tmpArray = new byte[cDataSize];
            final ByteBuffer buffer = ByteBuffer.wrap(tmpArray);
            tmpDataRdBuf.position(cDataOffset);
            tmpDataRdBuf.get(tmpArray);
            buffer.rewind();
            cacheMsgList.add(buffer);
            lastDataRdOff = cDataPos + cDataSize;
            readedSize += DataStoreUtils.STORE_INDEX_HEAD_LEN;
            totalReadSize += cDataSize;
            // break when exceed the max transfer size.
            if (totalReadSize >= maxReadSize) {
                break;
            }
        }
        // return result
        return new GetCacheMsgResult(true, 0, "Ok1",
                lstRdIndexOffset, readedSize, lastDataRdOff, totalReadSize, cacheMsgList);
    }

    /**
     * Batch flush memory data to disk.
     *
     * @param msgFileStore    the file storage
     * @param strBuffer       the message buffer
     * @throws IOException    the exception during processing
     */
    public void batchFlush(MsgFileStore msgFileStore,
                           StringBuilder strBuffer) throws Throwable {
        if (this.curMessageCount.get() == 0) {
            return;
        }
        ByteBuffer tmpIndexBuffer = this.cachedIndexSegment.asReadOnlyBuffer();
        final ByteBuffer tmpDataReadBuf = this.cacheDataSegment.asReadOnlyBuffer();
        tmpIndexBuffer.flip();
        tmpDataReadBuf.flip();
        long startTime = System.currentTimeMillis();
        msgFileStore.appendMsg(strBuffer, curMessageCount.get(),
            cacheIndexOffset.get(), tmpIndexBuffer, cacheDataOffset.get(),
                tmpDataReadBuf, leftAppendTime.get(), rightAppendTime.get());
        BrokerSrvStatsHolder.updDiskSyncDataDlt(System.currentTimeMillis() - startTime);
    }

    public int getCurMsgCount() {
        return this.curMessageCount.get();
    }

    public int getCurDataCacheSize() {
        return this.cacheDataOffset.get();
    }

    public int getIndexCacheSize() {
        return this.cacheIndexOffset.get();
    }

    public int getMaxDataCacheSize() {
        return this.maxDataCacheSize;
    }

    public int getMaxAllowedMsgCount() {
        return this.maxAllowedMsgCount;
    }

    public int isOffsetInHold(long requestOffset) {
        if (requestOffset < this.writeIndexStartPos) {
            return -1;
        } else if (requestOffset >= this.writeIndexStartPos + this.cacheIndexOffset.get()) {
            return 1;
        }
        return 0;
    }

    public long getDataLastWritePos() {
        return this.writeDataStartPos + this.cacheDataOffset.get();
    }

    public long getIndexLastWritePos() {
        return this.writeIndexStartPos + this.cacheIndexOffset.get();
    }

    public long getIndexStartWritePos() {
        return writeIndexStartPos;
    }

    public long getLeftAppendTime() {
        return leftAppendTime.get();
    }

    public long getRightAppendTime() {
        return rightAppendTime.get();
    }

    public int isTimestampInHold(long timestamp) {
        if (timestamp < this.leftAppendTime.get()) {
            return -1;
        } else if (timestamp > rightAppendTime.get()) {
            return 1;
        }
        return 0;
    }

    public void clear() {
        this.writeDataStartPos = -1;
        this.writeIndexStartPos = -1;
        this.cacheDataOffset.set(0);
        this.cacheIndexOffset.set(0);
        this.curMessageCount.set(0);
        this.queuesMap.clear();
        this.keysMap.clear();
        this.cacheDataSegment.rewind();
        this.cachedIndexSegment.rewind();
        this.leftAppendTime.set(System.currentTimeMillis());
        this.rightAppendTime.set(System.currentTimeMillis());
    }

    @Override
    public void close() {
        ((DirectBuffer) this.cacheDataSegment).cleaner().clean();
        ((DirectBuffer) this.cachedIndexSegment).cleaner().clean();
    }

}
