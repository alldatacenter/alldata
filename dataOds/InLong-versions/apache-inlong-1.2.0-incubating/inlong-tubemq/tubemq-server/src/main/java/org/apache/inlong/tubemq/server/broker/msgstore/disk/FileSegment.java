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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.utils.CheckSum;
import org.apache.inlong.tubemq.corebase.utils.ServiceStatusHolder;
import org.apache.inlong.tubemq.server.broker.stats.BrokerSrvStatsHolder;
import org.apache.inlong.tubemq.server.broker.utils.DataStoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Segment file. Topic contains multi FileSegments. Each FileSegment contains data file and index file.
 * It is mini particle of topic expire policy. It will be marked deleted when expired.
 */
public class FileSegment implements Segment {
    private static final Logger logger =
            LoggerFactory.getLogger(FileSegment.class);
    private final long start;
    private final File file;
    private final RandomAccessFile randFile;
    private final FileChannel channel;
    private final AtomicLong cachedSize;
    private final AtomicLong flushedSize;
    private final SegmentType segmentType;
    private volatile boolean mutable = false;
    private long expiredTime = 0;
    private final AtomicBoolean expired = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    // the first record append time
    private final AtomicLong leftAppendTime =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    // the latest record append time
    private final AtomicLong rightAppendTime =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);

    public FileSegment(long start, File file, SegmentType type) throws IOException {
        this(start, file, true, type, Long.MAX_VALUE);
    }

    public FileSegment(long start, File file,
                       boolean mutable, SegmentType type) throws IOException {
        this(start, file, mutable, type, Long.MAX_VALUE);
    }

    public FileSegment(long start, File file,
                       SegmentType type, long checkOffset) throws IOException {
        this(start, file, true, type, checkOffset);
    }

    private FileSegment(long start, File file, boolean mutable,
                        SegmentType type, long checkOffset) throws IOException {
        super();
        this.segmentType = type;
        this.start = start;
        this.file = file;
        this.mutable = mutable;
        this.cachedSize = new AtomicLong(0);
        this.flushedSize = new AtomicLong(0);
        this.randFile = new RandomAccessFile(this.file, "rw");
        this.channel = this.randFile.getChannel();
        if (mutable) {
            final long startMs = System.currentTimeMillis();
            long remaining = checkOffset == Long.MAX_VALUE ? -1 : (checkOffset - this.start);
            if (this.segmentType == SegmentType.DATA) {
                RecoverResult recoverResult = this.recoverData(remaining);
                if (recoverResult.isEqual()) {
                    logger.info(
                            "[File Store] Data Segment recover success, ignore content check!");
                } else {
                    if (recoverResult.getTruncated() > 0) {
                        logger.info(new StringBuilder(512)
                                .append("[File Store] Recover DATA Segment succeeded in ")
                                .append((System.currentTimeMillis() - startMs) / 1000)
                                .append(" seconds. ").append(recoverResult.getTruncated())
                                .append(" bytes truncated.").toString());
                    }
                }
            } else {
                RecoverResult recoverResult = this.recoverIndex(remaining);
                if (recoverResult.isEqual()) {
                    logger.info(
                            "[File Store] Index Segment recover success, ignore content check!");
                } else {
                    if (recoverResult.getTruncated() > 0) {
                        logger.info(new StringBuilder(512)
                                .append("[File Store] Recover Index Segment succeeded in ")
                                .append((System.currentTimeMillis() - startMs) / 1000)
                                .append(" seconds. ").append(recoverResult.getTruncated())
                                .append(" bytes truncated.").toString());
                    }
                }
            }
        } else {
            try {
                this.cachedSize.set(this.channel.size());
                this.flushedSize.set(this.cachedSize.get());
            } catch (final Exception e) {
                if (e instanceof IOException) {
                    ServiceStatusHolder.addReadIOErrCnt();
                    BrokerSrvStatsHolder.incDiskIOExcCnt();
                }
                if (this.segmentType == SegmentType.DATA) {
                    logger.error("[File Store] Set DATA Segment cachedSize error", e);
                } else {
                    logger.error("[File Store] Set INDEX Segment cachedSize error", e);
                }
            }
        }
        if (this.segmentType == SegmentType.INDEX) {
            if (this.cachedSize.get() == 0) {
                if (this.mutable) {
                    this.leftAppendTime.set(System.currentTimeMillis());
                    this.rightAppendTime.set(System.currentTimeMillis());
                }
            } else {
                this.leftAppendTime.set(getRecordTime(this.start));
                this.rightAppendTime.set(getRecordTime(this.start
                        + this.cachedSize.get() - DataStoreUtils.STORE_INDEX_HEAD_LEN));
            }
        }
    }

    @Override
    public void close() {
        if (this.closed.compareAndSet(false, true)) {
            try {
                if (this.channel.isOpen()) {
                    if (this.mutable) {
                        flush(true);
                    }
                    this.channel.close();
                }
                this.randFile.close();
            } catch (Throwable ee) {
                if (ee instanceof IOException) {
                    ServiceStatusHolder.addReadIOErrCnt();
                    BrokerSrvStatsHolder.incDiskIOExcCnt();
                }
                logger.error(new StringBuilder(512).append("[File Store] Close ")
                        .append(this.file.getAbsoluteFile().toString())
                        .append("'s ").append(segmentType).append(" file failure").toString(), ee);
            }
        }
    }

    @Override
    public void deleteFile() {
        this.closed.set(true);
        try {
            if (this.channel.isOpen()) {
                if (this.mutable) {
                    flush(true);
                }
                this.channel.close();
            }
            this.randFile.close();
        } catch (Throwable e1) {
            if (e1 instanceof IOException) {
                ServiceStatusHolder.addReadIOErrCnt();
                BrokerSrvStatsHolder.incDiskIOExcCnt();
            }
            logger.error("[File Store] failure to close channel ", e1);
        }
        try {
            logger.info(new StringBuilder(512)
                    .append("[File Store] delete file ")
                    .append(file.getAbsoluteFile()).toString());
            this.file.delete();
        } catch (Throwable ee) {
            if (ee instanceof IOException) {
                ServiceStatusHolder.addReadIOErrCnt();
                BrokerSrvStatsHolder.incDiskIOExcCnt();
            }
            logger.error("[File Store] failure to delete file ", ee);
        }
    }

    /**
     * Messages can only be appended to the last FileSegment.
     * The last FileSegment is writable, the others are mutable.
     *
     * @param buf            data buffer
     * @param leftTime       the first record timestamp
     * @param rightTime      the latest record timestamp
     * @return               latest writable position
     * @throws IOException   exception while force data to disk
     */
    @Override
    public long append(ByteBuffer buf, long leftTime, long rightTime) throws IOException {
        if (!this.mutable) {
            if (this.segmentType == SegmentType.DATA) {
                throw new UnsupportedOperationException("[File Store] Data Segment is immutable!");
            } else {
                throw new UnsupportedOperationException(
                        "[File Store] Index Segment is immutable!");
            }
        }
        if (this.closed.get()) {
            throw new UnsupportedOperationException("[File Store] Segment is closed!");
        }
        final long offset = this.cachedSize.get();
        int sizeInBytes = 0;
        while (buf.hasRemaining()) {
            sizeInBytes += this.channel.write(buf);
        }
        this.cachedSize.addAndGet(sizeInBytes);
        if (segmentType == SegmentType.INDEX) {
            this.rightAppendTime.set(rightTime);
            if (offset == 0) {
                this.leftAppendTime.set(leftTime);
            }
        }
        return this.start + offset;
    }

    /**
     * Flush file cache to disk.
     *
     * @param force whether to brush
     * @return the latest writable position
     *
     * @throws IOException exception while force data to disk
     */
    @Override
    public long flush(boolean force) throws IOException {
        this.channel.force(force);
        this.flushedSize.set(this.cachedSize.get());
        return this.start + this.flushedSize.get();
    }

    @Override
    public boolean isExpired() {
        return expired.get();
    }

    @Override
    public boolean needDelete() {
        return (expired.get() && (System.currentTimeMillis() - expiredTime > 120000));
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public boolean contains(long offset) {
        return (this.getCachedSize() == 0
                && offset == this.start
                || this.getCachedSize() > 0
                && offset >= this.start
                && offset <= this.start + this.getCachedSize() - 1);
    }

    /**
     * Release reference to this FileSegment.
     * File's channel will be closed when the reference decreased to 0.
     */
    @Override
    public void relViewRef() {

    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getLast() {
        return start + cachedSize.get();
    }

    /**
     * Return the position that have been flushed to disk.
     *
     * @return the position that have been flushed to disk
     */
    @Override
    public long getCommitLast() {
        return start + flushedSize.get();
    }

    @Override
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Set FileSegment to readonly.
     *
     * @param mutable mutable or immutable
     */
    @Override
    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }

    @Override
    public long getLeftAppendTime() {
        return leftAppendTime.get();
    }

    @Override
    public long getRightAppendTime() {
        return rightAppendTime.get();
    }

    @Override
    public boolean containTime(long timestamp) {
        if (this.getCachedSize() == 0) {
            return this.mutable;
        }
        if (timestamp >= this.leftAppendTime.get()) {
            if (this.mutable) {
                return true;
            }
            return timestamp <= this.rightAppendTime.get();
        }
        return false;
    }

    @Override
    public long getCachedSize() {
        return this.cachedSize.get();
    }

    @Override
    public long getCommitSize() {
        return this.flushedSize.get();
    }

    @Override
    public final File getFile() {
        return this.file;
    }

    @Override
    public void read(ByteBuffer bf, long absOffset) throws IOException {
        if (this.isExpired()) {
            //Todo: conduct file closed and expired cases.
        }
        int size = 0;
        long startPos  = absOffset - start;
        while (bf.hasRemaining()) {
            final int l = this.channel.read(bf, startPos + size);
            if (l < 0) {
                break;
            }
            size += l;
        }
    }

    @Override
    public void relRead(final ByteBuffer bf, long relOffset) throws IOException {
        if (this.isExpired()) {
            //Todo: conduct file closed and expired cases.
        }
        int size = 0;
        while (bf.hasRemaining()) {
            final int l = this.channel.read(bf, relOffset + size);
            if (l < 0) {
                break;
            }
            size += l;
        }
    }

    /**
     * read index record's append time.
     * @param reqOffset request offset.
     * @return message append time.
     */
    @Override
    public long getRecordTime(long reqOffset) throws IOException {
        ByteBuffer readUnit = ByteBuffer.allocate(DataStoreUtils.STORE_INDEX_HEAD_LEN);
        int size = 0;
        while (readUnit.hasRemaining()) {
            final int l = this.channel.read(readUnit, reqOffset - start + size);
            if (l < 0) {
                break;
            }
            size += l;
        }
        readUnit.flip();
        return readUnit.getLong(DataStoreUtils.INDEX_POS_TIME_RECV);
    }

    /**
     * Check whether this FileSegment is expired, and set expire status.
     * The last FileSegment cannot be marked expired.
     *
     * @param checkTimestamp check timestamp.
     * @param maxValidTimeMs the max expire interval in milliseconds.
     * @return -1 means already expired, 0 means the last FileSegment, 1 means expired.
     */
    @Override
    public int checkAndSetExpired(long checkTimestamp, long maxValidTimeMs) {
        if (expired.get()) {
            return -1;
        }
        if (closed.get()) {
            return 0;
        }
        if (!mutable) {
            if (checkTimestamp - file.lastModified() > maxValidTimeMs) {
                if (expired.compareAndSet(false, true)) {
                    expiredTime = System.currentTimeMillis();
                }
                return 1;
            }
        }
        return 0;
    }

    private RecoverResult recoverData(long checkOffset) throws IOException {
        if (!this.mutable) {
            throw new UnsupportedOperationException(
                    "[File Store] The Data Segment must be mutable!");
        }
        final long totalBytes = this.channel.size();
        if (totalBytes == checkOffset || checkOffset == -1) {
            this.cachedSize.set(totalBytes);
            this.flushedSize.set(totalBytes);
            this.channel.position(totalBytes);
            return new RecoverResult(0, totalBytes == checkOffset);
        }
        long validBytes = 0L;
        long next = 0L;
        long itemRead = 0L;
        int itemMsglen = 0;
        int itemMsgToken = 0;
        int itemCheckSum = 0;
        long itemNext = 0L;
        final ByteBuffer itemBuf = ByteBuffer.allocate(DataStoreUtils.STORE_DATA_HEADER_LEN);
        do {
            do {
                itemBuf.rewind();
                itemRead = this.channel.read(itemBuf);
                if (itemRead < DataStoreUtils.STORE_DATA_HEADER_LEN) {
                    next = -1;
                    break;
                }
                itemBuf.flip();
                itemMsglen = itemBuf.getInt() - DataStoreUtils.STORE_DATA_PREFX_LEN;
                itemMsgToken = itemBuf.getInt();
                itemCheckSum = itemBuf.getInt();
                itemNext = validBytes + DataStoreUtils.STORE_DATA_HEADER_LEN + itemMsglen;
                if ((itemMsgToken != DataStoreUtils.STORE_DATA_TOKER_BEGIN_VALUE)
                        || (itemMsglen <= 0)
                        || (itemMsglen > DataStoreUtils.MAX_MSG_DATA_STORE_SIZE)
                        || (itemNext > totalBytes)) {
                    next = -1;
                    break;
                }
                final ByteBuffer messageBuffer = ByteBuffer.allocate(itemMsglen);
                while (messageBuffer.hasRemaining()) {
                    itemRead = this.channel.read(messageBuffer);
                    if (itemRead < 0) {
                        throw new IOException(
                                "[File Store] The Data Segment is changing in recover processing!");
                    }
                }
                if (CheckSum.crc32(messageBuffer.array()) != itemCheckSum) {
                    next = -1;
                    break;
                }
                next = itemNext;
            } while (false);
            if (next >= 0) {
                validBytes = next;
            }
        } while (next >= 0);
        if (totalBytes != validBytes) {
            this.channel.truncate(validBytes);
        }
        this.cachedSize.set(validBytes);
        this.flushedSize.set(validBytes);
        this.channel.position(validBytes);
        return new RecoverResult(totalBytes - validBytes, false);
    }

    private RecoverResult recoverIndex(long checkOffset) throws IOException {
        if (!this.mutable) {
            throw new UnsupportedOperationException(
                    "[File Store] The Index Segment must be mutable!");
        }
        final long totalBytes = this.channel.size();
        if (totalBytes == checkOffset) {
            this.cachedSize.set(totalBytes);
            this.flushedSize.set(totalBytes);
            this.channel.position(totalBytes);
            return new RecoverResult(0, true);
        }
        long validBytes = 0L;
        long next = 0L;
        long itemRead = 0L;
        int itemMsgPartId = 0;
        long itemMsgOffset = 0L;
        int itemMsglen = 0;
        int itemKeyCode = 0;
        long itemTimeRecv = 0;
        long itemNext = 0L;
        final ByteBuffer itemBuf = ByteBuffer.allocate(DataStoreUtils.STORE_INDEX_HEAD_LEN);
        do {
            do {
                itemBuf.rewind();
                itemRead = this.channel.read(itemBuf);
                if (itemRead < DataStoreUtils.STORE_INDEX_HEAD_LEN) {
                    next = -1;
                    break;
                }
                itemBuf.flip();
                itemMsgPartId = itemBuf.getInt();
                itemMsgOffset = itemBuf.getLong();
                itemMsglen = itemBuf.getInt();
                itemKeyCode = itemBuf.getInt();
                itemTimeRecv = itemBuf.getLong();
                itemNext = validBytes + DataStoreUtils.STORE_INDEX_HEAD_LEN;
                if ((itemMsgPartId < 0)
                        || (itemMsgOffset < 0)
                        || (itemMsglen <= 0)
                        || (itemMsglen > DataStoreUtils.STORE_MAX_MESSAGE_STORE_LEN)
                        || (itemNext > totalBytes)) {
                    next = -1;
                    break;
                }
                next = itemNext;
            } while (false);
            if (next >= 0) {
                validBytes = next;
            }
        } while (next >= 0);
        if (totalBytes != validBytes) {
            channel.truncate(validBytes);
        }
        this.cachedSize.set(validBytes);
        this.flushedSize.set(validBytes);
        this.channel.position(validBytes);
        return new RecoverResult(totalBytes - validBytes, false);
    }

    private static class RecoverResult {
        private final long truncated;
        private final boolean isEqual;

        public RecoverResult(long truncated, boolean isEqual) {
            this.truncated = truncated;
            this.isEqual = isEqual;
        }

        public long getTruncated() {
            return truncated;
        }

        public boolean isEqual() {
            return isEqual;
        }
    }

}
