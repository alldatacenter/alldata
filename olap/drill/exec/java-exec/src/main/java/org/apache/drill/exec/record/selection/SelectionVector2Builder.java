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
package org.apache.drill.exec.record.selection;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.VectorContainer;

public class SelectionVector2Builder {

  private final SelectionVector2 sv2;
  private int index;

  public SelectionVector2Builder(BufferAllocator allocator, int maxSize) {
    sv2 = new SelectionVector2(allocator);
    sv2.allocateNew(maxSize);
  }

  public void setNext(int value) {
    sv2.setIndex(index++, value);
  }

  public void set(int posn, int value) {
    sv2.setIndex(posn, value);
    index = Math.max(index, posn + 1);
  }

  public SelectionVector2 harvest(VectorContainer batch) {
    sv2.setRecordCount(index);
    sv2.setBatchActualRecordCount(batch.getRecordCount());
    return sv2;
  }
}
