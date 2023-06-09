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
package org.apache.drill.exec.cache;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.WritableBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedVectorContainer extends LoopedAbstractDrillSerializable {
  private static final Logger logger = LoggerFactory.getLogger(CachedVectorContainer.class);

  private byte[] data;
  private final BufferAllocator allocator;
  private VectorContainer container;

  public CachedVectorContainer(WritableBatch batch, BufferAllocator allocator) {
    VectorAccessibleSerializable va = new VectorAccessibleSerializable(batch, allocator);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      va.writeToStream(baos);
    } catch (IOException e) {
      throw UserException.dataWriteError(e)
          .addContext("Failed to write a cached batch to storage")
          .build(logger);
    }
    this.allocator = allocator;
    this.data = baos.toByteArray();
    va.clear();
  }

  public CachedVectorContainer(BufferAllocator allocator) {
    this.allocator = allocator;
  }

  public CachedVectorContainer(byte[] data, BufferAllocator allocator) {
    this.data = data;
    this.allocator = allocator;
  }

  private void construct() {
    try {
      VectorAccessibleSerializable va = new VectorAccessibleSerializable(allocator);
      va.readFromStream(new ByteArrayInputStream(data));
      this.container = va.get();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void read(DataInput input) throws IOException {
    int len = input.readInt();
    this.data = new byte[len];
    input.readFully(data);
  }

  @Override
  public void write(DataOutput output) throws IOException {
    output.writeInt(data.length);
    output.write(data);
  }

  public VectorAccessible get() {
    if (container == null) {
      construct();
    }
    return container;
  }

  public void clear() {
    if (container != null) {
      container.clear();
    }
    container = null;
  }

  public byte[] getData() {
    return data;
  }
}
