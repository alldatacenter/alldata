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

import org.apache.drill.exec.record.RawFragmentBatch;
import org.apache.drill.exec.record.RawFragmentBatchProvider;

/**
 * A batch buffer is responsible for queuing incoming batches until a consumer is ready to receive them. It will also
 * inform upstream if the batch cannot be accepted.
 */
public interface RawBatchBuffer extends RawFragmentBatchProvider {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RawBatchBuffer.class);

  /**
   * Add the next new raw fragment batch to the buffer.
   *
   * @param batch
   *          Batch to enqueue
   * @throws IOException
   * @return Whether response should be returned.
   */
  public void enqueue(RawFragmentBatch batch) throws IOException;
}
