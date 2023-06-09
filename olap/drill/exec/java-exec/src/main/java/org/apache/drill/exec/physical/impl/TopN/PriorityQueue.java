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
package org.apache.drill.exec.physical.impl.TopN;

import org.apache.drill.exec.compile.TemplateClassDefinition;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.impl.sort.RecordBatchData;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector4;

public interface PriorityQueue {
  /**
   * The elements in the given batch are added to the priority queue. Note that the priority queue
   * only retains the top elements that fit within the size specified by the {@link #init(int, BufferAllocator, boolean)}
   * method.
   * @param batch The batch containing elements we want to add.
   * @throws SchemaChangeException
   */
  void add(RecordBatchData batch) throws SchemaChangeException;

  /**
   * Initializes the priority queue. This method must be called before any other methods on the priority
   * queue are called.
   * @param limit The size of the priority queue.
   * @param allocator The {@link BufferAllocator} to use when creating the priority queue.
   * @param hasSv2 True when incoming batches have 2 byte selection vectors. False otherwise.
   * @throws SchemaChangeException
   */
  void init(int limit, BufferAllocator allocator, boolean hasSv2) throws SchemaChangeException;

  /**
   * This method must be called before fetching the final priority queue hyper batch and final Sv4 vector.
   * @throws SchemaChangeException
   */
  void generate();

  /**
   * Retrieves the final priority queue HyperBatch containing the results. <b>Note:</b> this should be called
   * after {@link #generate()}.
   * @return The final priority queue HyperBatch containing the results.
   */
  VectorContainer getHyperBatch();

  SelectionVector4 getSv4();

  /**
   * Retrieves the selection vector used to select the elements in the priority queue from the hyper batch
   * provided by the {@link #getHyperBatch()} method. <b>Note:</b> this should be called after {@link #generate()}.
   * @return The selection vector used to select the elements in the priority queue.
   */
  SelectionVector4 getFinalSv4();

  /**
   * Cleanup the old state of queue and recreate a new one with HyperContainer containing vectors in input container
   * and the corresponding indexes (in SV4 format) from input SelectionVector4
   * @param container
   * @param vector4
   * @throws SchemaChangeException
   */
  void resetQueue(VectorContainer container, SelectionVector4 vector4) throws SchemaChangeException;

  /**
   * Releases all the memory consumed by the priority queue.
   */
  void cleanup();

  boolean isInitialized();

  TemplateClassDefinition<PriorityQueue> TEMPLATE_DEFINITION = new TemplateClassDefinition<>(PriorityQueue.class, PriorityQueueTemplate.class);
}
