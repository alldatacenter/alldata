/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.segment;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.List;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.BufferSegment;
import org.apache.uniffle.common.ShuffleDataSegment;
import org.apache.uniffle.common.ShuffleIndexResult;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.Constants;

public class FixedSizeSegmentSplitter implements SegmentSplitter {
  private static final Logger LOGGER = LoggerFactory.getLogger(FixedSizeSegmentSplitter.class);

  private int readBufferSize;

  public FixedSizeSegmentSplitter(int readBufferSize) {
    this.readBufferSize = readBufferSize;
  }

  @Override
  public List<ShuffleDataSegment> split(ShuffleIndexResult shuffleIndexResult) {
    if (shuffleIndexResult == null || shuffleIndexResult.isEmpty()) {
      return Lists.newArrayList();
    }

    byte[] indexData = shuffleIndexResult.getIndexData();
    long dataFileLen = shuffleIndexResult.getDataFileLen();
    return transIndexDataToSegments(indexData, readBufferSize, dataFileLen);
  }

  private static List<ShuffleDataSegment> transIndexDataToSegments(byte[] indexData,
      int readBufferSize, long dataFileLen) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(indexData);
    List<BufferSegment> bufferSegments = Lists.newArrayList();
    List<ShuffleDataSegment> dataFileSegments = Lists.newArrayList();
    int bufferOffset = 0;
    long fileOffset = -1;
    long totalLength = 0;

    while (byteBuffer.hasRemaining()) {
      try {
        final long offset = byteBuffer.getLong();
        final int length = byteBuffer.getInt();
        final int uncompressLength = byteBuffer.getInt();
        final long crc = byteBuffer.getLong();
        final long blockId = byteBuffer.getLong();
        final long taskAttemptId = byteBuffer.getLong();

        // The index file is written, read and parsed sequentially, so these parsed index segments
        // index a continuous shuffle data in the corresponding data file and the first segment's
        // offset field is the offset of these shuffle data in the data file.
        if (fileOffset == -1) {
          fileOffset = offset;
        }

        totalLength += length;

        // If ShuffleServer is flushing the file at this time, the length in the index file record may be greater
        // than the length in the actual data file, and it needs to be returned at this time to avoid EOFException
        if (dataFileLen != -1 && totalLength > dataFileLen) {
          long mask = (1L << Constants.PARTITION_ID_MAX_LENGTH) - 1;
          LOGGER.info("Abort inconsistent data, the data length: {}(bytes) recorded in index file is greater than "
                  + "the real data file length: {}(bytes). Partition id: {}. "
                  + "This may happen when the data is flushing, please ignore.",
              totalLength, dataFileLen, Math.toIntExact((blockId >> Constants.TASK_ATTEMPT_ID_MAX_LENGTH) & mask));
          break;
        }

        bufferSegments.add(new BufferSegment(blockId, bufferOffset, length, uncompressLength, crc, taskAttemptId));
        bufferOffset += length;

        if (bufferOffset >= readBufferSize) {
          ShuffleDataSegment sds = new ShuffleDataSegment(fileOffset, bufferOffset, bufferSegments);
          dataFileSegments.add(sds);
          bufferSegments = Lists.newArrayList();
          bufferOffset = 0;
          fileOffset = -1;
        }
      } catch (BufferUnderflowException ue) {
        throw new RssException("Read index data under flow", ue);
      }
    }

    if (bufferOffset > 0) {
      ShuffleDataSegment sds = new ShuffleDataSegment(fileOffset, bufferOffset, bufferSegments);
      dataFileSegments.add(sds);
    }

    return dataFileSegments;
  }
}
