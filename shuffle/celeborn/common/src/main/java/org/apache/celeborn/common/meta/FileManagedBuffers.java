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

package org.apache.celeborn.common.meta;

import java.io.File;
import java.util.BitSet;
import java.util.List;

import org.apache.celeborn.common.network.buffer.FileSegmentManagedBuffer;
import org.apache.celeborn.common.network.buffer.ManagedBuffer;
import org.apache.celeborn.common.network.util.TransportConf;

public class FileManagedBuffers {
  private final File file;
  private final long[] offsets;
  private final int numChunks;

  private final BitSet chunkTracker;
  private final TransportConf conf;

  private volatile boolean fullyRead = false;

  public FileManagedBuffers(FileInfo fileInfo, TransportConf conf) {
    file = fileInfo.getFile();
    numChunks = fileInfo.numChunks();
    if (numChunks > 0) {
      offsets = new long[numChunks + 1];
      List<Long> chunkOffsets = fileInfo.getChunkOffsets();
      for (int i = 0; i <= numChunks; i++) {
        offsets[i] = chunkOffsets.get(i);
      }
    } else {
      offsets = new long[1];
      offsets[0] = 0;
    }
    chunkTracker = new BitSet(numChunks);
    chunkTracker.clear();
    this.conf = conf;
  }

  public int numChunks() {
    return numChunks;
  }

  public boolean hasAlreadyRead(int chunkIndex) {
    synchronized (chunkTracker) {
      return chunkTracker.get(chunkIndex);
    }
  }

  public ManagedBuffer chunk(int chunkIndex, int offset, int len) {
    synchronized (chunkTracker) {
      chunkTracker.set(chunkIndex, true);
    }
    // offset of the beginning of the chunk in the file
    final long chunkOffset = offsets[chunkIndex];
    final long chunkLength = offsets[chunkIndex + 1] - chunkOffset;
    assert offset < chunkLength;
    long length = Math.min(chunkLength - offset, len);
    if (len + offset >= chunkLength) {
      synchronized (chunkTracker) {
        chunkTracker.set(chunkIndex);
      }
      if (chunkTracker.cardinality() == numChunks) {
        fullyRead = true;
      }
    }
    return new FileSegmentManagedBuffer(conf, file, chunkOffset + offset, length);
  }

  public boolean isFullyRead() {
    return fullyRead;
  }
}
