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
package org.apache.drill.exec.vector;

import java.io.Closeable;
import java.util.Set;

import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.memory.AllocationManager;
import org.apache.drill.exec.memory.AllocationManager.BufferLedger;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import io.netty.buffer.DrillBuf;

/**
 * An abstraction that is used to store a sequence of values in an individual
 * column.
 *
 * A {@link ValueVector value vector} stores underlying data in-memory in a
 * columnar fashion that is compact and efficient. The column whose data is
 * stored, is referred by {@link #getField()}.
 *
 * A vector when instantiated, relies on a
 * {@link org.apache.drill.exec.record.DeadBuf dead buffer}. It is important
 * that vector is allocated before attempting to read or write.
 *
 * There are a few "rules" around vectors:
 *
 * <ul>
 *   <li>Values need to be written in order (e.g. index 0, 1, 2, 5).</li>
 *   <li>Null vectors start with all values as null before writing anything.</li>
 *   <li>For variable width types, the offset vector should be all zeros before
 *   writing.</li>
 *   <li>You must call setValueCount before a vector can be read.</li>
 *   <li>You should never write to a vector once it has been read.</li>
 *   <li>Vectors may not grow larger than the number of bytes specified in
 *   {@link #MAX_BUFFER_SIZE} to prevent memory fragmentation. Use the
 *   <tt>setBounded()</tt> methods in the mutator to enforce this rule.</li>
 * </ul>
 *
 * Please note that the current implementation doesn't enforce those rules,
 * hence we may find few places that deviate from these rules (e.g. offset
 * vectors in Variable Length and Repeated vector)
 *
 * This interface "should" strive to guarantee this order of operation:
 * <blockquote>
 * allocate > mutate > setvaluecount > access > clear (or allocate
 * to start the process over).
 * </blockquote>
 */

public interface ValueVector extends Closeable, Iterable<ValueVector> {

  /**
   * Maximum allowed size of the buffer backing a value vector.
   * Set to the Netty chunk size to prevent memory fragmentation.
   */

  int MAX_BUFFER_SIZE = AllocationManager.chunkSize();

  /**
   * Maximum allowed row count in a vector. Repeated vectors
   * may have more items, but can have no more than this number
   * or arrays. Limited by 2-byte length in SV2: 65536 = 2<sup>16</sup>.
   */

  int MAX_ROW_COUNT = Character.MAX_VALUE + 1;
  int MIN_ROW_COUNT = 1;

  // Commonly-used internal vector names

  String BITS_VECTOR_NAME = "$bits$";
  String OFFSETS_VECTOR_NAME = "$offsets$";

  @Deprecated
  // See DRILL-6216
  String VALUES_VECTOR_NAME = "$values$";

  /**
   * Allocate new buffers. ValueVector implements logic to determine how much to allocate.
   * @throws OutOfMemoryException Thrown if no memory can be allocated.
   */
  void allocateNew() throws OutOfMemoryException;

  /**
   * Allocates new buffers. ValueVector implements logic to determine how much to allocate.
   * @return Returns true if allocation was successful.
   */
  boolean allocateNewSafe();

  BufferAllocator getAllocator();

  /**
   * Set the initial record capacity
   * @param numRecords
   */
  void setInitialCapacity(int numRecords);

  /**
   * Returns the maximum number of values that can be stored in this vector instance.
   */
  int getValueCapacity();

  /**
   * Alternative to clear(). Allows use as an AutoCloseable in try-with-resources.
   */
  @Override
  void close();

  /**
   * Release the underlying DrillBuf and reset the ValueVector to empty.
   */
  void clear();

  /**
   * Get information about how this field is materialized.
   */
  MaterializedField getField();

  /**
   * Returns a {@link org.apache.drill.exec.record.TransferPair transfer pair}, creating a new target vector of
   * the same type.
   */
  TransferPair getTransferPair(BufferAllocator allocator);

  TransferPair getTransferPair(String ref, BufferAllocator allocator);

  /**
   * Returns a new {@link org.apache.drill.exec.record.TransferPair transfer pair} that is used to transfer underlying
   * buffers into the target vector.
   */
  TransferPair makeTransferPair(ValueVector target);

  /**
   * Returns an {@link org.apache.drill.exec.vector.ValueVector.Accessor accessor} that is used to read from this vector
   * instance.
   */
  Accessor getAccessor();

  /**
   * Returns an {@link org.apache.drill.exec.vector.ValueVector.Mutator mutator} that is used to write to this vector
   * instance.
   */
  Mutator getMutator();

  /**
   * Returns a {@link org.apache.drill.exec.vector.complex.reader.FieldReader field reader} that supports reading values
   * from this vector.
   */
  FieldReader getReader();

  /**
   * Get the metadata for this field. Used in serialization
   *
   * @return FieldMetadata for this field.
   */
  SerializedField getMetadata();

  /**
   * Returns the number of bytes that is used by this vector instance.
   * This is a bit of a misnomer. Returns the number of bytes used by
   * data in this instance.
   */
  int getBufferSize();

  /**
   * Returns the total size of buffers allocated by this vector. Has
   * meaning only when vectors are directly allocated and each vector
   * has its own buffer. Does not have meaning for vectors deserialized
   * from the network or disk in which multiple vectors share the
   * same vector.
   *
   * @return allocated buffer size, in bytes
   */

  int getAllocatedSize();

  /**
   * Returns the number of bytes that is used by this vector if it holds the given number
   * of values. The result will be the same as if Mutator.setValueCount() were called, followed
   * by calling getBufferSize(), but without any of the closing side-effects that setValueCount()
   * implies wrt finishing off the population of a vector. Some operations might wish to use
   * this to determine how much memory has been used by a vector so far, even though it is
   * not finished being populated.
   *
   * @param valueCount the number of values to assume this vector contains
   * @return the buffer size if this vector is holding valueCount values
   */
  int getBufferSizeFor(int valueCount);

  /**
   * Return the underlying buffers associated with this vector. Note that this doesn't impact the reference counts for
   * this buffer so it only should be used for in-context access. Also note that this buffer changes regularly thus
   * external classes shouldn't hold a reference to it (unless they change it).
   * @param clear Whether to clear vector before returning; the buffers will still be refcounted;
   *   but the returned array will be the only reference to them
   *
   * @return The underlying {@link io.netty.buffer.DrillBuf buffers} that is used by this vector instance.
   */
  DrillBuf[] getBuffers(boolean clear);

  /**
   * Load the data provided in the buffer. Typically used when deserializing from the wire.
   *
   * @param metadata
   *          Metadata used to decode the incoming buffer.
   * @param buffer
   *          The buffer that contains the ValueVector.
   */
  void load(SerializedField metadata, DrillBuf buffer);

  void copyEntry(int toIndex, ValueVector from, int fromIndex);

  /**
   * Add the ledgers underlying the buffers underlying the components of the
   * vector to the set provided. Used to determine actual memory allocation.
   *
   * @param ledgers set of ledgers to which to add ledgers for this vector
   */

  void collectLedgers(Set<BufferLedger> ledgers);

  /**
   * Return the number of value bytes consumed by actual data.
   */

  int getPayloadByteCount(int valueCount);

  /**
   * Exchange state with another value vector of the same type.
   * Used to implement look-ahead writers.
   */

  void exchange(ValueVector other);

  /**
   * Convert a non-nullable vector to nullable by shuffling the data from
   * one to the other. Avoids the need to generate copy code just to change
   * mode. If this vector is non-nullable, accepts a nullable dual (same
   * minor type, different mode.) If the vector is non-nullable, or non-scalar,
   * then throws an exception.
   *
   * @param nullableVector nullable vector of the same minor type as
   * this vector
   */

  void toNullable(ValueVector nullableVector);

  /**
   * Reads from this vector instance.
   */
  interface Accessor {
    /**
     * Get the Java Object representation of the element at the specified position. Useful for testing.
     *
     * @param index
     *          Index of the value to get
     */
    Object getObject(int index);

    /**
     * Returns the number of values that is stored in this vector.
     */
    int getValueCount();

    /**
     * Returns true if the value at the given index is null, false otherwise.
     */
    boolean isNull(int index);
  }

  /**
   * Writes into this vector instance.
   */
  interface Mutator {
    /**
     * Sets the number of values that is stored in this vector to the given value count. <b>WARNING!</b> Once the
     * valueCount is set, the vector should be considered immutable.
     *
     * @param valueCount  value count to set.
     */
    void setValueCount(int valueCount);

    /**
     * Resets the mutator to pristine state.
     */
    void reset();

    /**
     * @deprecated  this has nothing to do with value vector abstraction and should be removed.
     */
    @Deprecated
    void generateTestData(int values);

    /**
     * Exchanges state with the mutator of another mutator. Used when exchanging
     * state with another vector.
     */

    void exchange(Mutator other);
  }
}
