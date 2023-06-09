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
package org.apache.drill.exec.physical.impl.xsort;

import java.io.IOException;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector2;

/**
 * The input batch group gathers batches buffered in memory before
 * spilling. The structure of the data is:
 * <ul>
 * <li>Contains a single batch received from the upstream (input)
 * operator.</li>
 * <li>Associated selection vector that provides a sorted
 * indirection to the values in the batch.</li>
 * </ul>
 */
public class InputBatch extends BatchGroup {
  private final SelectionVector2 sv2;
  private final long dataSizeBytes;

  public InputBatch(VectorContainer container, SelectionVector2 sv2, BufferAllocator allocator, long dataSize) {
    super(container, allocator);
    this.sv2 = sv2;
    this.dataSizeBytes = dataSize;
  }

  public SelectionVector2 getSv2() { return sv2; }

  public long getDataSize() { return dataSizeBytes; }

  @Override
  public int getRecordCount() {
    if (sv2 != null) {
      return sv2.getCount();
    } else {
      return super.getRecordCount();
    }
  }

  @Override
  public int getNextIndex() {
    int val = super.getNextIndex();
    if (val == -1) {
      return val;
    }
    return sv2.getIndex(val);
  }

  @Override
  public void close() throws IOException {
    if (sv2 != null) {
      sv2.clear();
    }
    super.close();
  }
}