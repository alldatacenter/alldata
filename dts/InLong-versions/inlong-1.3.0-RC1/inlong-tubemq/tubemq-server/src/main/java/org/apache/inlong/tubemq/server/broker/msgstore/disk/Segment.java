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
import java.nio.ByteBuffer;

/**
 * Storage segment, usually implemented in file format.
 */
public interface Segment {

    void close();

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
    long append(ByteBuffer buf, long leftTime, long rightTime) throws IOException;

    long flush(boolean force) throws IOException;

    int checkAndSetExpired(long checkTimestamp, long maxValidTimeMs);

    boolean isClosed();

    boolean needDelete();

    long getStart();

    long getLast();

    long getCommitLast();

    File getFile();

    void deleteFile();

    long getCachedSize();

    long getCommitSize();

    boolean isExpired();

    boolean contains(long offset);

    boolean isMutable();

    void setMutable(boolean mutable);

    void relViewRef();

    /**
     * Read data to buffer from absolute position.
     *
     * @param bf          buffer to store data
     * @param absOffset   absolute read position
     */
    void read(ByteBuffer bf, long absOffset) throws IOException;

    /**
     * Read data to buffer from relative position.
     *
     * @param bf          buffer to store data
     * @param relOffset   relative read position
     */
    void relRead(ByteBuffer bf, long relOffset) throws IOException;

    long getLeftAppendTime();

    long getRightAppendTime();

    boolean containTime(long timestamp);

    long getRecordTime(long reqOffset) throws IOException;
}
