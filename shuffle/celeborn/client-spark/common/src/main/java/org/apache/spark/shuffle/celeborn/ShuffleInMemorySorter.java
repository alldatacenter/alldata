/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.shuffle.celeborn;

import org.apache.spark.memory.MemoryConsumer;
import org.apache.spark.memory.SparkOutOfMemoryError;
import org.apache.spark.unsafe.Platform;
import org.apache.spark.unsafe.array.LongArray;
import org.apache.spark.util.collection.unsafe.sort.RadixSort;

public class ShuffleInMemorySorter {

  private final MemoryConsumer consumer;

  /**
   * An array of record pointers and partition ids that have been encoded by {@link
   * PackedRecordPointer}. The sort operates on this array instead of directly manipulating records.
   *
   * <p>Only part of the array will be used to store the pointers, the rest part is preserved as
   * temporary buffer for sorting.
   */
  private LongArray array;

  /** The position in the pointer array where new records can be inserted. */
  private int pos = 0;

  /** How many records could be inserted, because part of the array should be left for sorting. */
  private int usableCapacity = 0;

  private final int initialSize;

  public ShuffleInMemorySorter(MemoryConsumer consumer, int initialSize) {
    this.consumer = consumer;
    assert (initialSize > 0);
    this.initialSize = initialSize;
    this.array = consumer.allocateArray(initialSize);
    this.usableCapacity = getUsableCapacity();
  }

  private int getUsableCapacity() {
    // Radix sort requires same amount of used memory as buffer, Tim sort requires
    // half of the used memory as buffer.
    return (int) (array.size() / 2);
  }

  public long getInitialSize() {
    return initialSize;
  }

  public void freeMemory() {
    if (consumer != null) {
      if (array != null) {
        consumer.freeArray(array);
      }

      // Set the array to null instead of allocating a new array. Allocating an array could have
      // triggered another spill and this method already is called from UnsafeExternalSorter when
      // spilling. Attempting to allocate while spilling is dangerous, as we could be holding onto
      // a large partially complete allocation, which may prevent other memory from being allocated.
      // Instead we will allocate the new array when it is necessary.
      array = null;
      usableCapacity = 0;
    }
    pos = 0;
  }

  /** @return the number of records that have been inserted into this sorter. */
  public int numRecords() {
    return pos;
  }

  public void expandPointerArray(LongArray newArray) {
    if (array != null) {
      if (newArray.size() < array.size()) {
        // checkstyle.off: RegexpSinglelineJava
        throw new SparkOutOfMemoryError("Not enough memory to grow pointer array");
        // checkstyle.on: RegexpSinglelineJava
      }
      Platform.copyMemory(
          array.getBaseObject(),
          array.getBaseOffset(),
          newArray.getBaseObject(),
          newArray.getBaseOffset(),
          pos * 8L);
      consumer.freeArray(array);
    }
    array = newArray;
    usableCapacity = getUsableCapacity();
  }

  public boolean hasSpaceForAnotherRecord() {
    return pos < usableCapacity;
  }

  public long getMemoryUsage() {
    if (array == null) {
      return 0L;
    }

    return array.size() * 8;
  }

  /**
   * Inserts a record to be sorted.
   *
   * @param recordPointer a pointer to the record, encoded by the task memory manager. Due to
   *     certain pointer compression techniques used by the sorter, the sort can only operate on
   *     pointers that point to locations in the first {@link
   *     PackedRecordPointer#MAXIMUM_PAGE_SIZE_BYTES} bytes of a data page.
   * @param partitionId the partition id, which must be less than or equal to {@link
   *     PackedRecordPointer#MAXIMUM_PARTITION_ID}.
   */
  public void insertRecord(long recordPointer, int partitionId) {
    if (!hasSpaceForAnotherRecord()) {
      throw new IllegalStateException("There is no space for new record");
    }
    array.set(pos, PackedRecordPointer.packPointer(recordPointer, partitionId));
    pos++;
  }

  /**
   * An iterator-like class that's used instead of Java's Iterator in order to facilitate inlining.
   */
  public static final class ShuffleSorterIterator {

    private final LongArray pointerArray;
    private final int limit;
    final PackedRecordPointer packedRecordPointer = new PackedRecordPointer();
    private int position = 0;

    ShuffleSorterIterator(int numRecords, LongArray pointerArray, int startingPosition) {
      this.limit = numRecords + startingPosition;
      this.pointerArray = pointerArray;
      this.position = startingPosition;
    }

    public boolean hasNext() {
      return position < limit;
    }

    public void loadNext() {
      packedRecordPointer.set(pointerArray.get(position));
      position++;
    }
  }

  /** Return an iterator over record pointers in sorted order. */
  public ShuffleSorterIterator getSortedIterator() {
    if (numRecords() == 0) {
      // `array` might be null, so make sure that it is not accessed by returning early.
      return new ShuffleSorterIterator(0, array, 0);
    }

    int offset = 0;
    offset =
        RadixSort.sort(
            array,
            pos,
            PackedRecordPointer.PARTITION_ID_START_BYTE_INDEX,
            PackedRecordPointer.PARTITION_ID_END_BYTE_INDEX,
            false,
            false);
    return new ShuffleSorterIterator(pos, array, offset);
  }
}
