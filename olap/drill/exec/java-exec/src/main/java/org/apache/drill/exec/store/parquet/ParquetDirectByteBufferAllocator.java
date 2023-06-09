/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.parquet;

import io.netty.buffer.DrillBuf;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.OperatorContext;

import org.apache.parquet.bytes.ByteBufferAllocator;

/**
 * {@link ByteBufferAllocator} implementation that uses Drill's {@link BufferAllocator} to allocate and release
 * {@link ByteBuffer} objects.<br>
 * To properly release an allocated {@link DrillBuf}, this class keeps track of it's corresponding {@link ByteBuffer}
 * that was passed to the Parquet library.
 */
public class ParquetDirectByteBufferAllocator implements ByteBufferAllocator {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ParquetDirectByteBufferAllocator.class);

  private final BufferAllocator allocator;
  private final Map<ByteBuffer, DrillBuf> allocatedBuffers = new IdentityHashMap<>();

  public ParquetDirectByteBufferAllocator(OperatorContext o) {
    this(o.getAllocator());
  }

  public ParquetDirectByteBufferAllocator(BufferAllocator allocator) {
    this.allocator = allocator;
  }

  @Override
  public ByteBuffer allocate(int sz) {
    DrillBuf drillBuf = allocator.buffer(sz);
    ByteBuffer byteBuffer = drillBuf.nioBuffer(0, sz);
    allocatedBuffers.put(byteBuffer, drillBuf);
    logger.debug("{}: Allocated {} bytes. Allocated DrillBuf with id {} and ByteBuffer {}", this, sz, drillBuf.getId(), System.identityHashCode(byteBuffer));
    return byteBuffer;
  }

  @Override
  public void release(ByteBuffer byteBuffer) {
    final DrillBuf drillBuf = allocatedBuffers.remove(byteBuffer);
    // The ByteBuffer passed in may already have been freed or not allocated by this allocator.
    // If it is not found in the allocated buffers, do nothing
    if (drillBuf != null) {
      logger.debug("{}: Freed DrillBuf with id {} and ByteBuffer {}", this, drillBuf.getId(), System.identityHashCode(byteBuffer));
      drillBuf.release();
    } else {
      logger.warn("{}: ByteBuffer {} is not present", this, System.identityHashCode(byteBuffer));
    }
  }

  @Override
  public boolean isDirect() {
    return true;
  }
}
