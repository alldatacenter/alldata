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
package org.apache.drill.exec.work.batch;

import java.io.IOException;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.RawFragmentBatch;

public interface DataCollector extends AutoCloseable {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataCollector.class);
  public boolean batchArrived(int minorFragmentId, RawFragmentBatch batch) throws IOException;
  public int getOppositeMajorFragmentId();
  public RawBatchBuffer[] getBuffers();
  public int getTotalIncomingFragments();
  public void close() throws Exception;
  /**
   * Enables caller (e.g., receiver) to attach its buffer allocator to this Data Collector in order
   * to claim ownership of incoming batches; by default, the fragment allocator owns these batches.
   *
   * @param allocator operator buffer allocator
   */
  void setAllocator(BufferAllocator allocator);
  /**
   * @return allocator
   */
  BufferAllocator getAllocator();
}
