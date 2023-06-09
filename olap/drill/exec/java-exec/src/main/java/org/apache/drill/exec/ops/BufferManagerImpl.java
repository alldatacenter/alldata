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
package org.apache.drill.exec.ops;

import io.netty.buffer.DrillBuf;

import org.apache.drill.exec.memory.BufferAllocator;

import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.predicates.LongObjectPredicate;

public class BufferManagerImpl implements BufferManager {
  private LongObjectHashMap<DrillBuf> managedBuffers = new LongObjectHashMap<>();
  private final BufferAllocator allocator;

  public BufferManagerImpl(BufferAllocator allocator) {
    this.allocator = allocator;
  }

  @Override
  public void close() {
    managedBuffers.forEach(new LongObjectPredicate<DrillBuf>() {
      @Override
      public boolean apply(long key, DrillBuf value) {
        value.release();
        return true;
      }
    });
    managedBuffers.clear();
  }

  @Override
  public DrillBuf replace(DrillBuf old, int newSize) {
    if (managedBuffers.remove(old.memoryAddress()) == null) {
      throw new IllegalStateException("Tried to remove unmanaged buffer.");
    }
    old.release(1);
    return getManagedBuffer(newSize);
  }

  @Override
  public DrillBuf getManagedBuffer() {
    return getManagedBuffer(256);
  }

  @Override
  public DrillBuf getManagedBuffer(int size) {
    DrillBuf newBuf = allocator.buffer(size, this);
    managedBuffers.put(newBuf.memoryAddress(), newBuf);
    return newBuf;
  }
}
