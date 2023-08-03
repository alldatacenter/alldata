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

import com.google.common.collect.Lists;

public class ShuffleDataResult {

  private final byte[] data;
  private final List<BufferSegment> bufferSegments;

  public ShuffleDataResult() {
    this(new byte[0]);
  }

  public ShuffleDataResult(byte[] data) {
    this(data, Lists.newArrayList());
  }

  public ShuffleDataResult(byte[] data, List<BufferSegment> bufferSegments) {
    this.data = data;
    this.bufferSegments = bufferSegments;
  }

  public byte[] getData() {
    return data;
  }

  public List<BufferSegment> getBufferSegments() {
    return bufferSegments;
  }

  public boolean isEmpty() {
    return bufferSegments == null || bufferSegments.isEmpty() || data == null || data.length == 0;
  }

}
