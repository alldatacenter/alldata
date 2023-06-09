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
package org.apache.drill.exec.physical.impl.common;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.exec.compile.TemplateClassDefinition;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.common.exceptions.RetryAfterSpillException;

public interface HashTable {
  TemplateClassDefinition<HashTable> TEMPLATE_DEFINITION =
      new TemplateClassDefinition<>(HashTable.class, HashTableTemplate.class);

  /**
   * The maximum capacity of the hash table (in terms of number of buckets).
   */
  int MAXIMUM_CAPACITY = 1 << 30;

  /**
   * The default load factor of a hash table.
   */
  float DEFAULT_LOAD_FACTOR = 0.75f;

  enum PutStatus {KEY_PRESENT, KEY_ADDED, NEW_BATCH_ADDED, KEY_ADDED_LAST, PUT_FAILED;}

  /**
   * The batch size used for internal batch holders
   */
  int BATCH_SIZE = Character.MAX_VALUE + 1;
  int BATCH_MASK = 0x0000FFFF;

  /**
   * {@link HashTable#setup} must be called before anything can be done to the {@link HashTable}.
   *
   * @param htConfig
   * @param allocator
   * @param incomingBuild
   * @param incomingProbe
   * @param outgoing
   * @param htContainerOrig
   * @param context
   * @param cg
   */
  void setup(HashTableConfig htConfig, BufferAllocator allocator, VectorContainer incomingBuild, RecordBatch incomingProbe, RecordBatch outgoing,
             VectorContainer htContainerOrig, FragmentContext context, ClassGenerator<?> cg);

  /**
   * Updates the incoming (build and probe side) value vectors references in the {@link HashTableTemplate.BatchHolder}s.
   * This is useful on OK_NEW_SCHEMA (need to verify).
   * @throws SchemaChangeException
   */
  void updateBatches() throws SchemaChangeException;

  /**
   * Computes the hash code for the record at the given index in the build side batch.
   * @param incomingRowIdx The index of the build side record of interest.
   * @return The hash code for the record at the given index in the build side batch.
   * @throws SchemaChangeException
   */
  int getBuildHashCode(int incomingRowIdx) throws SchemaChangeException;

  /**
   * Computes the hash code for the record at the given index in the probe side batch.
   * @param incomingRowIdx The index of the probe side record of interest.
   * @return The hash code for the record at the given index in the probe side batch.
   * @throws SchemaChangeException
   */
  int getProbeHashCode(int incomingRowIdx) throws SchemaChangeException;

  PutStatus put(int incomingRowIdx, IndexPointer htIdxHolder, int hashCode, int batchSize) throws SchemaChangeException, RetryAfterSpillException;

  /**
   * @param incomingRowIdx The index of the key in the probe batch.
   * @param hashCode The hashCode of the key.
   * @return Returns -1 if the data in the probe batch at the given incomingRowIdx is not in the hash table. Otherwise returns
   * the composite index of the key in the hash table (index of BatchHolder and record in Batch Holder).
   * @throws SchemaChangeException
   */
  int probeForKey(int incomingRowIdx, int hashCode) throws SchemaChangeException;

  void getStats(HashTableStats stats);

  int size();

  boolean isEmpty();

  /**
   * Frees all the direct memory consumed by the {@link HashTable}.
   */
  void clear();

  /**
   * Update the initial capacity for the hash table. This method will be removed after the key vectors are removed from the hash table. It is used
   * to allocate {@link HashTableTemplate.BatchHolder}s of appropriate size when the final size of the HashTable is known.
   *
   * <b>Warning!</b> Only call this method before you have inserted elements into the HashTable.
   *
   * @param initialCapacity The new initial capacity to use.
   */
  void updateInitialCapacity(int initialCapacity);

  /**
   * Changes the incoming probe and build side batches, and then updates all the value vector references in the {@link HashTableTemplate.BatchHolder}s.
   * @param newIncoming The new build side batch.
   * @param newIncomingProbe The new probe side batch.
   */
  void updateIncoming(VectorContainer newIncoming, RecordBatch newIncomingProbe);

  /**
   * Clears all the memory used by the {@link HashTable} and re-initializes it.
   */
  void reset();

  /**
   * Retrieves the key columns and transfers them to the output container. Note this operation removes the key columns from the {@link HashTable}.
   * @param batchIdx The index of a {@link HashTableTemplate.BatchHolder} in the HashTable.
   * @param outContainer The destination container for the key columns.
   * @param numRecords The number of key recorts to transfer.
   * @return
   */
  boolean outputKeys(int batchIdx, VectorContainer outContainer, int numRecords);

  /**
   * Returns a message containing memory usage statistics. Intended to be used for printing debugging or error messages.
   * @return A debug string.
   */
  String makeDebugString();

  /**
   * The amount of direct memory consumed by the hash table.
   * @return
   */
  long getActualSize();

  void setTargetBatchRowCount(int batchRowCount);

  int getTargetBatchRowCount();

  Pair<VectorContainer, Integer> nextBatch();
}


