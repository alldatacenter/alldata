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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileSegments management. Contains two types FileSegment: data and index.
 */
public class FileSegmentList implements SegmentList {
    private static final Logger logger =
            LoggerFactory.getLogger(FileSegmentList.class);
    // list of segments.
    private final AtomicReference<Segment[]> segmentList =
            new AtomicReference<>();

    public FileSegmentList(final Segment[] s) {
        super();
        this.segmentList.set(s);
    }

    public FileSegmentList() {
        super();
        this.segmentList.set(new Segment[0]);
    }

    @Override
    public void close() {
        for (Segment segment : segmentList.get()) {
            if (segment != null) {
                segment.close();
            }
        }
    }

    @Override
    public Segment[] getView() {
        return segmentList.get();
    }

    /**
     * Return segment by the given offset.
     *
     * @param offset     the position to search
     * @return           the segment included the position
     * @throws IOException  the exception while searching
     */
    @Override
    public Segment getRecordSeg(final long offset) throws IOException {
        Segment tmpSeg = this.findSegment(offset);
        if (tmpSeg != null && tmpSeg.isExpired()) {
            return null;
        }
        return tmpSeg;
    }

    @Override
    public void append(final Segment segment) {
        while (true) {
            final Segment[] curr = segmentList.get();
            final Segment[] update = new Segment[curr.length + 1];
            System.arraycopy(curr, 0, update, 0, curr.length);
            update[curr.length] = segment;
            if (segmentList.compareAndSet(curr, update)) {
                return;
            }
        }
    }

    /**
     * Check each FileSegment whether is expired, and set expire status.
     *
     * @param checkTimestamp   current check timestamp
     * @param fileValidTimeMs  the max expire interval
     * @return                 whether is expired
     */
    @Override
    public boolean checkExpiredSegments(long checkTimestamp, long fileValidTimeMs) {
        boolean hasExpired = false;
        for (Segment segment : segmentList.get()) {
            if (segment == null) {
                continue;
            }
            if (segment.checkAndSetExpired(checkTimestamp, fileValidTimeMs) == 0) {
                break;
            }
            hasExpired = true;
        }
        return hasExpired;
    }

    /**
     * Check FileSegments whether is expired, close all expired FileSegments, and then delete these files.
     *
     * @param sb   string buffer
     */
    @Override
    public void delExpiredSegments(final StringBuilder sb) {
        // delete expired segment
        for (Segment segment : segmentList.get()) {
            if (segment == null) {
                continue;
            }
            if (!segment.needDelete()) {
                break;
            }
            delete(segment);
            segment.deleteFile();
        }
    }

    @Override
    public void delete(final Segment segment) {
        while (true) {
            int index = -1;
            final Segment[] curViews = segmentList.get();
            for (int i = 0; i < curViews.length; i++) {
                if (curViews[i] == segment) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                return;
            }
            final Segment[] update = new Segment[curViews.length - 1];
            System.arraycopy(curViews, 0, update, 0, index);
            if (index + 1 < curViews.length) {
                System.arraycopy(curViews, index + 1, update, index, curViews.length - index - 1);
            }
            if (this.segmentList.compareAndSet(curViews, update)) {
                return;
            }
        }
    }

    @Override
    public void flushLast(boolean force) throws IOException {
        final Segment[] curViews = segmentList.get();
        if (curViews.length == 0) {
            return;
        }
        Segment last = curViews[curViews.length - 1];
        if (last == null) {
            return;
        }
        last.flush(force);
    }

    @Override
    public Segment last() {
        final Segment[] curViews = segmentList.get();
        if (curViews.length == 0) {
            return null;
        }
        return curViews[curViews.length - 1];
    }

    /**
     * Return the start position of these FileSegments.
     *
     * @return  the first position
     */
    @Override
    public long getMinOffset() {
        long last = 0L;
        final Segment[] curViews = segmentList.get();
        if (curViews.length == 0) {
            return last;
        }
        for (int i = 0; i < curViews.length; i++) {
            if (curViews[i] == null) {
                continue;
            }
            if (curViews[i].isExpired()) {
                last = curViews[i].getCommitLast();
                continue;
            }
            return curViews[i].getStart();
        }
        return  last;
    }

    /**
     * Return the max position of these FileSegments.
     *
     * @return   the latest position
     */
    @Override
    public long getMaxOffset() {
        final Segment[] curViews = segmentList.get();
        if (curViews.length == 0) {
            return 0L;
        }
        Segment last = curViews[curViews.length - 1];
        if (last == null) {
            return 0L;
        }
        return last.getLast();
    }

    @Override
    public long getMaxAppendTime() {
        final Segment[] curViews = segmentList.get();
        if (curViews.length == 0) {
            return 0L;
        }
        Segment last = curViews[curViews.length - 1];
        if (last == null) {
            return 0L;
        }
        return last.getRightAppendTime();
    }

    /**
     * Return the max position that have been flushed to disk.
     *
     * @return  the latest committed offset
     */
    @Override
    public long getCommitMaxOffset() {
        final Segment[] curViews = segmentList.get();
        if (curViews.length == 0) {
            return 0L;
        }
        Segment last = curViews[curViews.length - 1];
        if (last == null) {
            return 0L;
        }
        return last.getCommitLast();
    }

    @Override
    public long getSizeInBytes() {
        long sum = 0L;
        final Segment[] curViews = segmentList.get();
        if (curViews.length == 0) {
            return sum;
        }
        for (int i = 0; i < curViews.length; i++) {
            if (curViews[i] == null
                    || curViews[i].isExpired()) {
                continue;
            }
            sum += curViews[i].getCachedSize();
        }
        return sum;
    }

    /**
     *  Binary search the segment that contains the offset
     *
     * @param offset    offset to search
     * @return          the segment includes the searched offset
     */
    @Override
    public Segment findSegment(final long offset) {
        final Segment[] curViews = segmentList.get();
        if (curViews.length == 0) {
            return null;
        }
        int minStart  = 0;
        for (minStart = 0; minStart < curViews.length; minStart++) {
            if (curViews[minStart] == null
                    || curViews[minStart].isExpired()) {
                continue;
            }
            break;
        }
        if (minStart >= curViews.length) {
            minStart = curViews.length - 1;
        }
        final Segment startSeg = curViews[minStart];
        if (offset < startSeg.getStart()) {
            throw new ArrayIndexOutOfBoundsException(new StringBuilder(512)
                    .append("Request offsets is ").append(offset)
                    .append(", the start is ").append(startSeg.getStart()).toString());
        }
        final Segment last = curViews[curViews.length - 1];
        if (offset >= last.getStart() + last.getCachedSize()) {
            return null;
        }
        int high = curViews.length - 1;
        while (minStart <= high) {
            final int mid = high + minStart >>> 1;
            final Segment found = curViews[mid];
            if (found.contains(offset)) {
                return found;
            } else if (offset < found.getStart()) {
                high = mid - 1;
            } else {
                minStart = mid + 1;
            }
        }
        return null;
    }

    @Override
    public Segment findSegmentByTimeStamp(long timestamp) {
        final Segment[] curViews = segmentList.get();
        if (curViews.length == 0) {
            return null;
        }
        int minStart  = 0;
        for (minStart = 0; minStart < curViews.length; minStart++) {
            if (curViews[minStart] == null
                    || curViews[minStart].isExpired()) {
                continue;
            }
            break;
        }
        // Check boundaries
        if (minStart >= curViews.length) {
            minStart = curViews.length - 1;
        }
        int hight = curViews.length - 1;
        final Segment startSeg = curViews[minStart];
        if ((minStart == hight)
                || (timestamp <= startSeg.getLeftAppendTime())) {
            return startSeg;
        }
        Segment last = curViews[hight];
        Segment before = curViews[hight - 1];
        if (last.getCachedSize() > 0) {
            if (timestamp > last.getLeftAppendTime()) {
                return last;
            }
        }
        if (timestamp > before.getRightAppendTime()) {
            return last;
        } else if (timestamp > before.getLeftAppendTime()) {
            return before;
        }
        int mid = 0;
        Segment found = null;
        // Use dichotomy to find the first segment that contains the specified timestamp
        while (minStart <= hight) {
            mid = hight + minStart >>> 1;
            found = curViews[mid];
            if (found.containTime(timestamp)) {
                before = curViews[mid - 1];
                if (timestamp > before.getRightAppendTime()) {
                    return found;
                } else if (timestamp > before.getLeftAppendTime()) {
                    return before;
                }
                hight = mid - 1;
            } else if (timestamp < found.getLeftAppendTime()) {
                hight = mid - 1;
            } else {
                minStart = mid + 1;
            }
        }
        return null;
    }

}
