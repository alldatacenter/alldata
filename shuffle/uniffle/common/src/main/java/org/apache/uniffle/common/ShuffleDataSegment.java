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

package org.apache.uniffle.common;

import java.util.List;

/**
 * ShuffleDataSegment is a view of a segment of shuffle data file, which is split according to the read buffer size.
 * It contains a list of BufferSegment, they are indices of the block in the data file segment.
 */
public class ShuffleDataSegment {
  private final long offset;
  private final int length;
  private final List<BufferSegment> bufferSegments;

  public ShuffleDataSegment(long offset, int length, List<BufferSegment> bufferSegments) {
    this.offset = offset;
    this.length = length;
    this.bufferSegments = bufferSegments;
  }

  public long getOffset() {
    return offset;
  }

  public int getLength() {
    return length;
  }

  public List<BufferSegment> getBufferSegments() {
    return bufferSegments;
  }
}
