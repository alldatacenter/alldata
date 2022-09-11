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

package org.apache.inlong.tubemq.server.broker.msgstore.disk;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientBroker;
import org.apache.inlong.tubemq.corebase.utils.ServiceStatusHolder;
import org.apache.inlong.tubemq.corebase.utils.Tuple3;
import org.apache.inlong.tubemq.server.broker.BrokerConfig;
import org.apache.inlong.tubemq.server.broker.msgstore.MessageStore;
import org.apache.inlong.tubemq.server.broker.stats.MsgStoreStatsHolder;
import org.apache.inlong.tubemq.server.broker.stats.BrokerSrvStatsHolder;
import org.apache.inlong.tubemq.server.broker.stats.TrafficInfo;
import org.apache.inlong.tubemq.server.broker.utils.DataStoreUtils;
import org.apache.inlong.tubemq.server.broker.utils.DiskSamplePrint;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message file's storage. Contains data file and index file.
 */
public class MsgFileStore implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(MsgFileStore.class);
    private static final int MAX_META_REFRESH_DUR = 1000 * 60 * 60;
    private static final DiskSamplePrint samplePrintCtrl =
        new DiskSamplePrint(logger);
    // storage ID
    private final String storeKey;
    // data file storage directory
    private final File dataDir;
    // index file storage directory
    private final File indexDir;
    // disk flush parameters: current unflushed message count
    private final AtomicInteger curUnflushed = new AtomicInteger(0);
    // current unflushed message size
    private final AtomicLong curUnflushSize = new AtomicLong(0);
    // time of data's last flush operation
    private final AtomicLong lastFlushTime = new AtomicLong(System.currentTimeMillis());
    // time of meta's last flush operation
    private final AtomicLong lastMetaFlushTime = new AtomicLong(0);
    private final BrokerConfig tubeConfig;
    // file store stats holder
    private final MsgStoreStatsHolder msgStoreStatsHolder;
    // lock used for append message to storage
    private final ReentrantLock writeLock = new ReentrantLock();
    private final ByteBuffer byteBufferIndex =
            ByteBuffer.allocate(DataStoreUtils.STORE_INDEX_HEAD_LEN);
    // message storage
    private final MessageStore messageStore;
    // data file segment list
    private SegmentList dataSegments;
    // index file segment list
    private SegmentList indexSegments;
    // close status
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * MsgFileStore, initial message file store block
     *
     * @param messageStore     the message control block to which the file store belongs
     * @param tubeConfig       the broker configure
     * @param baseStorePath    the file store path
     * @param offsetIfCreate   the offset if create
     */
    public MsgFileStore(final MessageStore messageStore,
                        final BrokerConfig tubeConfig,
                        final String baseStorePath,
                        final long offsetIfCreate) throws IOException {
        final StringBuilder sBuilder = new StringBuilder(512);
        this.tubeConfig = tubeConfig;
        this.messageStore = messageStore;
        this.msgStoreStatsHolder = messageStore.getMsgStoreStatsHolder();
        this.storeKey = messageStore.getStoreKey();
        this.dataDir = new File(sBuilder.append(baseStorePath)
                .append(File.separator).append(this.storeKey).toString());
        sBuilder.delete(0, sBuilder.length());
        this.indexDir = new File(sBuilder.append(baseStorePath)
                .append(File.separator).append(this.storeKey)
                .append(File.separator).append("index").toString());
        sBuilder.delete(0, sBuilder.length());
        FileUtil.checkDir(this.dataDir);
        FileUtil.checkDir(this.indexDir);
        loadSegments(SegmentType.DATA, offsetIfCreate, sBuilder);
        loadSegments(SegmentType.INDEX, offsetIfCreate, sBuilder);
        this.lastFlushTime.set(System.currentTimeMillis());
    }

    /**
     * Append message to file segment
     *
     * @param sb             string buffer
     * @param msgCnt         the record count to append
     * @param indexSize      the index buffer length
     * @param indexBuffer    the index buffer to append
     * @param dataSize       the data buffer length
     * @param dataBuffer     the data buffer to append
     * @param leftTime       the first record timestamp
     * @param rightTime      the latest record timestamp
     * @return      file storage status, the index and data offsets of the added message
     */
    public Tuple3<Boolean, Long, Long> appendMsg(StringBuilder sb, int msgCnt,
                                                 int indexSize, ByteBuffer indexBuffer,
                                                 int dataSize, ByteBuffer dataBuffer,
                                                 long leftTime, long rightTime) {
        // append message, put in data file first, then index file.
        if (this.closed.get()) {
            throw new IllegalStateException(new StringBuilder(512)
                .append("Closed MessageStore for storeKey ")
                .append(this.storeKey).toString());
        }
        // Various parameters that trigger data refresh
        boolean isDataSegFlushed = false;
        boolean isIndexSegFlushed = false;
        boolean isMsgCntFlushed = false;
        boolean isMsgDataFlushed = false;
        boolean isMsgTimeFlushed = false;
        boolean isForceMetadata = false;
        // flushed message message count and data size info
        long flushedMsgCnt = 0;
        long flushedDataSize = 0;
        // Temporary variables in calculations
        long inIndexOffset;
        Segment curDataSeg;
        long dataOffset = -1;
        long inDataOffset;
        Segment curIndexSeg;
        long indexOffset = -1;
        long currTime;
        // new file paths of creating
        String newDataFilePath = null;
        String newIndexFilePath = null;
        boolean fileStoreOK = false;
        this.writeLock.lock();
        try {
            inIndexOffset = dataBuffer.getLong(DataStoreUtils.STORE_HEADER_POS_QUEUE_LOGICOFF);
            // filling data segment.
            curDataSeg = this.dataSegments.last();
            this.curUnflushSize.addAndGet(dataSize);
            dataOffset = curDataSeg.append(dataBuffer, leftTime, rightTime);
            // judge whether need to create a new data segment.
            if (curDataSeg.getCachedSize() >= this.tubeConfig.getMaxSegmentSize()) {
                isDataSegFlushed = true;
                long newDataOffset = curDataSeg.flush(true);
                File newDataFile =
                    new File(this.dataDir,
                        DataStoreUtils.nameFromOffset(newDataOffset, DataStoreUtils.DATA_FILE_SUFFIX));
                curDataSeg.setMutable(false);
                newDataFilePath = newDataFile.getAbsolutePath();
                this.dataSegments.append(new FileSegment(newDataOffset, newDataFile, SegmentType.DATA));
            }
            // filling index data.
            inDataOffset = indexBuffer.getLong(DataStoreUtils.INDEX_POS_DATAOFFSET);
            curIndexSeg = this.indexSegments.last();
            indexOffset = curIndexSeg.append(indexBuffer, leftTime, rightTime);
            // judge whether need to create a new index segment.
            if (curIndexSeg.getCachedSize()
                >= this.tubeConfig.getMaxIndexSegmentSize()) {
                isIndexSegFlushed = true;
                long newIndexOffset = curIndexSeg.flush(true);
                curIndexSeg.setMutable(false);
                File newIndexFile =
                    new File(this.indexDir,
                        DataStoreUtils.nameFromOffset(newIndexOffset, DataStoreUtils.INDEX_FILE_SUFFIX));
                newIndexFilePath = newIndexFile.getAbsolutePath();
                this.indexSegments.append(new FileSegment(newIndexOffset,
                    newIndexFile, SegmentType.INDEX));
            }
            // check whether need to flush to disk.
            currTime = System.currentTimeMillis();
            isMsgDataFlushed = (messageStore.getUnflushDataHold() > 0)
                    && (curUnflushSize.get() >= messageStore.getUnflushDataHold());
            if ((isMsgCntFlushed =
                    (this.curUnflushed.addAndGet(msgCnt) >= messageStore.getUnflushThreshold()))
                || (isMsgTimeFlushed =
                    (currTime - this.lastFlushTime.get() >= messageStore.getUnflushInterval()))
                || isMsgDataFlushed || isDataSegFlushed || isIndexSegFlushed) {
                isForceMetadata = (isDataSegFlushed || isIndexSegFlushed
                    || (currTime - this.lastMetaFlushTime.get() > MAX_META_REFRESH_DUR));
                if (!isDataSegFlushed) {
                    curDataSeg.flush(isForceMetadata);
                }
                if (!isIndexSegFlushed) {
                    curIndexSeg.flush(isForceMetadata);
                }
                flushedMsgCnt = this.curUnflushed.getAndSet(0);
                flushedDataSize = this.curUnflushSize.getAndSet(0);
                this.lastFlushTime.set(System.currentTimeMillis());
                if (isForceMetadata) {
                    this.lastMetaFlushTime.set(this.lastFlushTime.get());
                }
            }
            // print abnormal information
            if (inIndexOffset != indexOffset || inDataOffset != dataOffset) {
                ServiceStatusHolder.addWriteIOErrCnt();
                BrokerSrvStatsHolder.incDiskIOExcCnt();
                logger.error(sb.append("[File Store]: appendMsg data Error, storekey=")
                    .append(this.storeKey).append(",msgCnt=").append(msgCnt)
                    .append(",indexSize=").append(indexSize)
                    .append(",inIndexOffset=").append(inIndexOffset)
                    .append(",indexOffset=").append(indexOffset)
                    .append(",dataSize=").append(dataSize)
                    .append(",inDataOffset=").append(inDataOffset)
                    .append(",dataOffset=").append(dataOffset).toString());
                sb.delete(0, sb.length());
            } else {
                fileStoreOK = true;
            }
        } catch (Throwable e) {
            if (!closed.get()) {
                ServiceStatusHolder.addWriteIOErrCnt();
                BrokerSrvStatsHolder.incDiskIOExcCnt();
            }
            samplePrintCtrl.printExceptionCaught(e);
        } finally {
            this.writeLock.unlock();
            // add statistics.
            msgStoreStatsHolder.addFileFlushStatsInfo(msgCnt, indexSize, dataSize,
                    flushedMsgCnt, flushedDataSize, isDataSegFlushed, isIndexSegFlushed,
                    isMsgDataFlushed, isMsgCntFlushed, isMsgTimeFlushed, isForceMetadata);
            if (isDataSegFlushed) {
                logger.info(sb.append("[File Store] Created data segment ")
                        .append(newDataFilePath).toString());
                sb.delete(0, sb.length());
            }
            if (isIndexSegFlushed) {
                logger.info(sb.append("[File Store] Created index segment ")
                        .append(newIndexFilePath).toString());
                sb.delete(0, sb.length());
            }
        }
        return new Tuple3<>(fileStoreOK, indexOffset, dataOffset);
    }

    /**
     * Get message from index and data files.
     *
     * @param partitionId           the partitionId for reading messages
     * @param lastRdOffset          the recent data offset read before
     * @param reqOffset             the request index offset
     * @param indexBuffer           the index read buffer
     * @param isFilterConsume       whether to filter consumption
     * @param filterKeySet          filter item set
     * @param statsKeyBase         the statistical key prefix
     * @param maxMsgTransferSize    the max read message size
     * @param reqRcvTime            the timestamp of the record to be checked
     *
     * @return                      read result
     */
    public GetMessageResult getMessages(int partitionId, long lastRdOffset,
                                        long reqOffset, ByteBuffer indexBuffer,
                                        boolean isFilterConsume,
                                        Set<Integer> filterKeySet,
                                        String statsKeyBase,
                                        int maxMsgTransferSize,
                                        long reqRcvTime) {
        // #lizard forgives
        // Orderly read from index file, then random read from data file.
        int retCode = 0;
        int totalSize = 0;
        String errInfo = "Ok";
        boolean result = true;
        int dataRealLimit = 0;
        int curIndexOffset = 0;
        int readedOffset = 0;
        Segment recordSeg = null;
        int curIndexPartitionId = 0;
        long curIndexDataOffset = 0L;
        int curIndexDataSize = 0;
        int curIndexKeyCode = 0;
        long recvTimeInMillsec = 0L;
        long maxDataLimitOffset = 0L;
        long lastRdDataOffset = 0L;
        final StringBuilder sBuilder = new StringBuilder(512);
        final long curDataMaxOffset = getDataMaxOffset();
        final long curDataMinOffset = getDataMinOffset();
        HashMap<String, TrafficInfo> countMap = new HashMap<>();
        ByteBuffer dataBuffer =
                ByteBuffer.allocate(TServerConstants.CFG_STORE_DEFAULT_MSG_READ_UNIT);
        List<ClientBroker.TransferedMessage> transferedMessageList =
                new ArrayList<>();
        // read data file by index.
        for (curIndexOffset = 0; curIndexOffset < indexBuffer.remaining();
             curIndexOffset += DataStoreUtils.STORE_INDEX_HEAD_LEN) {
            curIndexPartitionId = indexBuffer.getInt();
            curIndexDataOffset = indexBuffer.getLong();
            curIndexDataSize = indexBuffer.getInt();
            curIndexKeyCode = indexBuffer.getInt();
            recvTimeInMillsec = indexBuffer.getLong();
            maxDataLimitOffset = curIndexDataOffset + curIndexDataSize;
            // skip when mismatch condition
            if (curIndexDataOffset < 0
                    || curIndexDataSize <= 0
                    || curIndexDataSize > DataStoreUtils.STORE_MAX_MESSAGE_STORE_LEN
                    || curIndexDataOffset < curDataMinOffset) {
                readedOffset = curIndexOffset + DataStoreUtils.STORE_INDEX_HEAD_LEN;
                continue;
            }
            // read finish, then return.
            if (curIndexDataOffset >= curDataMaxOffset
                    || maxDataLimitOffset > curDataMaxOffset) {
                lastRdDataOffset = curIndexDataOffset;
                break;
            }
            // conduct filter operation.
            if (curIndexPartitionId != partitionId
                    || (isFilterConsume
                    && !filterKeySet.contains(curIndexKeyCode))) {
                lastRdDataOffset = maxDataLimitOffset;
                readedOffset = curIndexOffset + DataStoreUtils.STORE_INDEX_HEAD_LEN;
                continue;
            }
            if (reqRcvTime != 0 && recvTimeInMillsec < reqRcvTime) {
                continue;
            }
            try {
                // get data from data file by index one by one.
                if (recordSeg == null
                        || !((curIndexDataOffset >= recordSeg.getStart())
                        && (maxDataLimitOffset <= recordSeg.getStart() + recordSeg.getCommitSize()))) {
                    if (recordSeg != null) {
                        recordSeg.relViewRef();
                        recordSeg = null;
                    }
                    recordSeg = dataSegments.getRecordSeg(curIndexDataOffset);
                    if (recordSeg == null) {
                        continue;
                    }
                    if (this.closed.get()) {
                        throw new Exception("Read Service has closed!");
                    }
                }
                if (dataBuffer.capacity() < curIndexDataSize) {
                    dataBuffer = ByteBuffer.allocate(curIndexDataSize);
                }
                dataBuffer.clear();
                dataBuffer.limit(curIndexDataSize);
                recordSeg.read(dataBuffer, curIndexDataOffset);
                dataBuffer.flip();
                dataRealLimit = dataBuffer.limit();
                if (dataRealLimit < curIndexDataSize) {
                    lastRdDataOffset = curIndexDataOffset;
                    readedOffset = curIndexOffset + DataStoreUtils.STORE_INDEX_HEAD_LEN;
                    continue;
                }
            } catch (Throwable e2) {
                if (e2 instanceof IOException) {
                    ServiceStatusHolder.addReadIOErrCnt();
                    BrokerSrvStatsHolder.incDiskIOExcCnt();
                }
                samplePrintCtrl.printExceptionCaught(e2,
                    messageStore.getStoreKey(), String.valueOf(partitionId));
                retCode = TErrCodeConstants.INTERNAL_SERVER_ERROR;
                sBuilder.delete(0, sBuilder.length());
                errInfo = sBuilder.append("Get message from file failure : ")
                        .append(e2.getCause()).toString();
                sBuilder.delete(0, sBuilder.length());
                result = false;
                break;
            }
            // build query result.
            readedOffset = curIndexOffset + DataStoreUtils.STORE_INDEX_HEAD_LEN;
            lastRdDataOffset = maxDataLimitOffset;
            ClientBroker.TransferedMessage transferedMessage =
                    DataStoreUtils.getTransferMsg(dataBuffer,
                            curIndexDataSize, countMap, statsKeyBase, sBuilder);
            if (transferedMessage == null) {
                continue;
            }
            transferedMessageList.add(transferedMessage);
            totalSize += curIndexDataSize;
            // break when exceed the max transfer size.
            if (totalSize >= maxMsgTransferSize) {
                break;
            }
        }
        // release resource
        if (recordSeg != null) {
            recordSeg.relViewRef();
        }
        if (retCode != 0) {
            if (!transferedMessageList.isEmpty()) {
                retCode = 0;
                errInfo = "Ok";
            }
        }
        if (lastRdDataOffset <= 0L) {
            lastRdDataOffset = lastRdOffset;
        }
        // return result.
        return new GetMessageResult(result, retCode, errInfo,
                reqOffset, readedOffset, lastRdDataOffset,
                totalSize, countMap, transferedMessageList);
    }

    /**
     * Get the segment start Offset that contains the specified timestamp
     *
     * @param timestamp           the specified timestamp
     *
     * @return                    the start offset
     */
    public long getStartOffsetByTimeStamp(long timestamp) {
        Segment recordSeg = indexSegments.findSegmentByTimeStamp(timestamp);
        if (recordSeg == null || this.closed.get()) {
            return -1;
        }
        long endPos = (recordSeg.getCommitLast() - recordSeg.getStart())
                / DataStoreUtils.STORE_INDEX_HEAD_LEN - 1;
        final long curDataMinOffset = getDataMinOffset();
        final ByteBuffer readBuffer =
                ByteBuffer.allocate(DataStoreUtils.STORE_INDEX_HEAD_LEN);
        // check boundaries
        if (endPos <= 0) {
            return recordSeg.getStart();
        }
        long foundTime = getTimeStamp(recordSeg,
                0, curDataMinOffset, readBuffer);
        if (timestamp < foundTime) {
            return recordSeg.getStart();
        }
        foundTime = getTimeStamp(recordSeg,
                endPos * DataStoreUtils.STORE_INDEX_HEAD_LEN,
                curDataMinOffset, readBuffer);
        if (timestamp > foundTime) {
            return recordSeg.getStart() + endPos * DataStoreUtils.STORE_INDEX_HEAD_LEN;
        }
        long midPos = 0;
        long startPos = 0;
        long firstLowPos = 0;
        long firstEqualPos = -1;
        // Dichotomy finds the first offset position less than the specified time
        while (startPos <= endPos) {
            midPos = endPos + startPos >>> 1;
            foundTime = getTimeStamp(recordSeg,
                    midPos * DataStoreUtils.STORE_INDEX_HEAD_LEN,
                    curDataMinOffset, readBuffer);
            if (foundTime < timestamp) {
                firstLowPos = midPos;
                startPos = midPos + 1;
            } else {
                endPos = midPos - 1;
                if (foundTime == timestamp) {
                    firstEqualPos = midPos;
                }
            }
        }
        if (firstEqualPos != -1) {
            return recordSeg.getStart() + firstEqualPos * DataStoreUtils.STORE_INDEX_HEAD_LEN;
        } else {
            return recordSeg.getStart() + firstLowPos * DataStoreUtils.STORE_INDEX_HEAD_LEN;
        }
    }

    @Override
    public void close() throws IOException {
        if (this.closed.compareAndSet(false, true)) {
            this.writeLock.lock();
            try {
                this.indexSegments.close();
                this.dataSegments.close();
            } finally {
                this.writeLock.unlock();
            }
        }
    }

    /**
     * Clean expired data files and index files.
     *
     * @param onlyCheck   whether to check only
     * @return            whether found expired segments
     */
    public boolean runClearupPolicy(boolean onlyCheck) {
        final StringBuilder sBuilder = new StringBuilder(512);
        final long start = System.currentTimeMillis();
        boolean hasExpiredDataSegs =
                dataSegments.checkExpiredSegments(start, messageStore.getMaxFileValidDurMs());
        boolean hasExpiredIndexSegs =
                indexSegments.checkExpiredSegments(start, messageStore.getMaxFileValidDurMs());
        if (onlyCheck) {
            return (hasExpiredDataSegs || hasExpiredIndexSegs);
        }
        if (hasExpiredDataSegs) {
            dataSegments.delExpiredSegments(sBuilder);
        }
        if (hasExpiredIndexSegs) {
            indexSegments.delExpiredSegments(sBuilder);
        }
        return (hasExpiredDataSegs || hasExpiredIndexSegs);
    }

    /**
     * Flush data to disk at interval.
     *
     * @throws IOException the exception during processing
     */
    public void flushDiskFile() throws IOException {
        long checkTimestamp = System.currentTimeMillis();
        if ((curUnflushed.get() > 0)
                && (checkTimestamp - lastFlushTime.get() >= messageStore.getUnflushInterval())) {
            long flushedMsgCnt = 0L;
            long flushedDataSize = 0L;
            boolean forceMetadata = false;
            this.writeLock.lock();
            try {
                checkTimestamp = System.currentTimeMillis();
                if ((curUnflushed.get() >= 0)
                        && (checkTimestamp - lastFlushTime.get() >= messageStore.getUnflushInterval())) {
                    forceMetadata =
                            (checkTimestamp - lastMetaFlushTime.get()) > MAX_META_REFRESH_DUR;
                    dataSegments.flushLast(forceMetadata);
                    indexSegments.flushLast(forceMetadata);
                    if (forceMetadata) {
                        this.lastMetaFlushTime.set(checkTimestamp);
                    }
                    flushedMsgCnt = curUnflushed.getAndSet(0);
                    flushedDataSize = curUnflushSize.getAndSet(0);
                    lastFlushTime.set(checkTimestamp);
                }
            } finally {
                this.writeLock.unlock();
                msgStoreStatsHolder.addFileTimeoutFlushStats(flushedMsgCnt,
                        flushedDataSize, forceMetadata);
            }
        }
        msgStoreStatsHolder.chkStatsExpired(checkTimestamp);
    }

    public long getDataSizeInBytes() {
        return dataSegments.getSizeInBytes();
    }

    public long getIndexSizeInBytes() {
        return indexSegments.getSizeInBytes();
    }

    public long getDataMaxOffset() {
        return dataSegments.getMaxOffset();
    }

    public long getDataHighMaxOffset() {
        return dataSegments.getCommitMaxOffset();
    }

    public long getDataMinOffset() {
        return dataSegments.getMinOffset();
    }

    public long getIndexMaxOffset() {
        return this.indexSegments.getMaxOffset();
    }

    public long getIndexMaxAppendTime() {
        return this.indexSegments.getMaxAppendTime();
    }

    public long getIndexMaxHighOffset() {
        return this.indexSegments.getCommitMaxOffset();
    }

    public long getIndexMinOffset() {
        return this.indexSegments.getMinOffset();
    }

    public Segment indexSlice(final long offset, final int maxSize) throws IOException {
        return indexSegments.getRecordSeg(offset);
    }

    private void loadSegments(SegmentType segType, long offsetIfCreate,
                              StringBuilder sBuilder) throws IOException {
        String segTypeStr = "Data";
        File   segListDir = this.dataDir;
        String fileSuffix = DataStoreUtils.DATA_FILE_SUFFIX;
        if (segType == SegmentType.INDEX) {
            segTypeStr = "Index";
            segListDir = this.indexDir;
            fileSuffix = DataStoreUtils.INDEX_FILE_SUFFIX;
        }
        logger.info(sBuilder.append("[File Store] begin Load ")
                .append(segTypeStr).append(" segments ")
                .append(segListDir.getAbsolutePath()).toString());
        sBuilder.delete(0, sBuilder.length());
        final List<Segment> accum = new ArrayList<>();
        final File[] ls = segListDir.listFiles();
        if (ls != null) {
            for (final File file : ls) {
                if (file == null) {
                    continue;
                }
                if (file.isFile() && file.toString().endsWith(fileSuffix)) {
                    if (!file.canRead()) {
                        throw new IOException(new StringBuilder(512)
                                .append("Could not read ").append(segTypeStr)
                                .append(" file ").append(file).toString());
                    }
                    final String filename = file.getName();
                    final long start =
                            Long.parseLong(filename.substring(0, filename.length() - fileSuffix.length()));
                    accum.add(new FileSegment(start, file, false, segType));
                }
            }
        }
        if (accum.size() == 0) {
            final File newFile =
                    new File(segListDir,
                            DataStoreUtils.nameFromOffset(offsetIfCreate, fileSuffix));
            logger.info(sBuilder.append("[File Store] Created ").append(segTypeStr)
                    .append(" segment ").append(newFile.getAbsolutePath()).toString());
            sBuilder.delete(0, sBuilder.length());
            accum.add(new FileSegment(offsetIfCreate, newFile, segType));
        } else {
            // The list of segments is required to be arranged continuously from low to high
            accum.sort(new Comparator<Segment>() {
                @Override
                public int compare(final Segment o1, final Segment o2) {
                    return Long.compare(o1.getStart(), o2.getStart());
                }
            });
            validateSegments(segTypeStr, accum);
            Segment last = accum.get(accum.size() - 1);
            if ((last.getCachedSize() > 0)
                    && (System.currentTimeMillis() - last.getFile().lastModified()
                    >= DataStoreUtils.MAX_FILE_NO_WRITE_DURATION)) {
                // If the last segment is not written for a long time, it will be aged at startup
                final long newOffset = last.getCommitLast();
                final File newFile =
                        new File(segListDir,
                                DataStoreUtils.nameFromOffset(newOffset, fileSuffix));
                logger.info(sBuilder.append("[File Store] Created time roll").append(segTypeStr)
                        .append(" segment ").append(newFile.getAbsolutePath()).toString());
                sBuilder.delete(0, sBuilder.length());
                accum.add(new FileSegment(newOffset, newFile, segType));
            } else {
                last = accum.remove(accum.size() - 1);
                last.close();
                logger.info(sBuilder
                        .append("[File Store] Loading the last ").append(segTypeStr)
                        .append(" segment in mutable mode and running recover on ")
                        .append(last.getFile().getAbsolutePath()).toString());
                sBuilder.delete(0, sBuilder.length());
                final FileSegment mutable =
                        new FileSegment(last.getStart(), last.getFile(), segType, Long.MAX_VALUE);
                accum.add(mutable);
            }
        }
        if (segType == SegmentType.DATA) {
            this.dataSegments = new FileSegmentList(accum.toArray(new Segment[accum.size()]));
        } else {
            this.indexSegments = new FileSegmentList(accum.toArray(new Segment[accum.size()]));
        }
        logger.info(sBuilder.append("[File Store] Loaded ")
                .append(segTypeStr).append(" ").append(accum.size()).append(" segments from ")
                .append(segListDir.getAbsolutePath()).toString());
        sBuilder.delete(0, sBuilder.length());
    }

    private void validateSegments(String segTypeStr, final List<Segment> segments) {
        // valid segments, continuous
        for (int i = 0; i < segments.size() - 1; i++) {
            final Segment curr = segments.get(i);
            final Segment next = segments.get(i + 1);
            if (curr.getStart() + curr.getCachedSize() != next.getStart()) {
                throw new IllegalStateException(new StringBuilder(512)
                        .append("The following ").append(segTypeStr)
                        .append(" segments don't validate: ")
                        .append(curr.getFile().getAbsolutePath()).append(", ")
                        .append(next.getFile().getAbsolutePath()).toString());
            }
        }
    }

    private long getTimeStamp(Segment recordSeg, long relReadPos,
                              long curDataMinOffset, ByteBuffer readBuffer) {
        int curIndexPartitionId = 0;
        long curIndexDataOffset = 0L;
        int curIndexDataSize = 0;
        int curIndexKeyCode = 0;
        long recvTimeInMillsec = 0L;
        try {
            readBuffer.clear();
            recordSeg.relRead(readBuffer, relReadPos);
            readBuffer.flip();
            curIndexPartitionId = readBuffer.getInt();
            curIndexDataOffset = readBuffer.getLong();
            curIndexDataSize = readBuffer.getInt();
            curIndexKeyCode = readBuffer.getInt();
            recvTimeInMillsec = readBuffer.getLong();
            // skip when mismatch condition
            if (curIndexDataOffset < 0
                    || curIndexDataSize <= 0
                    || curIndexDataSize > DataStoreUtils.STORE_MAX_MESSAGE_STORE_LEN
                    || curIndexDataOffset < curDataMinOffset) {
                return -1;
            }
            return recvTimeInMillsec;
        } catch (Throwable ex) {
            samplePrintCtrl.printExceptionCaught(ex);
            return -1;
        }
    }
}
