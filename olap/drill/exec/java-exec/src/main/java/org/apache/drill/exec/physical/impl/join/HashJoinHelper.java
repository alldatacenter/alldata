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
package org.apache.drill.exec.physical.impl.join;

import com.carrotsearch.hppc.IntArrayList;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.impl.common.HashTable;
import org.apache.drill.exec.record.selection.SelectionVector4;


/*
 * Helper class for hash join. Keeps track of information about the build side batches.
 *
 * Hash join is a blocking operator, so we consume all the batches on the build side and
 * store them in a hyper container. The way we can retrieve records from the hyper container
 * is by providing the record index and batch index in the hyper container. When we invoke put()
 * for a given row, hash table returns a global index. We store the current row's record index
 * and batch index in this global index of the startIndices structure.
 *
 * Since there can be many rows with the same key on the build side, we store the first
 * index in the startIndices list and the remaining are stored as a logical linked list using
 * the 'links' field in the BuildInfo structures.
 *
 * Apart from the indexes into the hyper container, this class also stores information about
 * which records of the build side had a matching record on the probe side. Stored in a bitvector
 * keyMatchBitVector, it is used to retrieve all records that did not match a record on probe side
 * for right outer and full outer joins
 */
public class HashJoinHelper {

  /* List of start indexes. Stores the record and batch index of the first record
   * with a give key.
   */
  List<SelectionVector4> startIndices = new ArrayList<>();

  // List of BuildInfo structures. Used to maintain auxiliary information about the build batches
  List<BuildInfo> buildInfoList = new ArrayList<>();

  // Fragment context
  FragmentContext context;
  BufferAllocator allocator;

  // Constant to indicate index is empty.
  static final int INDEX_EMPTY = -1;

  // bits to shift while obtaining batch index from SV4
  static final int SHIFT_SIZE = 16;

  public static final int LEFT_INPUT = 0;
  public static final int RIGHT_INPUT = 1;

  public HashJoinHelper(FragmentContext context, BufferAllocator allocator) {
    this.context = context;
    this.allocator = allocator;
}

  public void addStartIndexBatch() throws SchemaChangeException {
    startIndices.add(getNewSV4(HashTable.BATCH_SIZE));
  }

  public class BuildInfo {
    // List of links. Logically it helps maintain a linked list of records with the same key value
    private SelectionVector4 links;

    // List of bitvectors. Keeps track of records on the build side that matched a record on the probe side
    private BitSet keyMatchBitVector;

    // number of records in this batch
    private int recordCount;

    public BuildInfo(SelectionVector4 links, BitSet keyMatchBitVector, int recordCount) {
      this.links = links;
      this.keyMatchBitVector = keyMatchBitVector;
      this.recordCount = recordCount;
    }

    public SelectionVector4 getLinks() {
      return links;
    }

    public BitSet getKeyMatchBitVector() {
      return keyMatchBitVector;
    }
    public void clear() {
      keyMatchBitVector.clear();
    }
  }

  public SelectionVector4 getNewSV4(int recordCount) throws SchemaChangeException {

    ByteBuf vector = allocator.buffer((recordCount * 4));

    SelectionVector4 sv4 = new SelectionVector4(vector, recordCount, recordCount);

    // Initialize the vector
    for (int i = 0; i < recordCount; i++) {
      sv4.set(i, INDEX_EMPTY);
    }

    return sv4;
  }

  public void addNewBatch(int recordCount) throws SchemaChangeException {
    // Add a node to the list of BuildInfo's
    BuildInfo info = new BuildInfo(getNewSV4(recordCount), new BitSet(recordCount), recordCount);
    buildInfoList.add(info);
  }

  /**
   * Takes a composite index for a key produced by {@link HashTable#probeForKey(int, int)}, and uses it to look up the
   * index of the first original key in the original data.
   * @param keyIndex A composite index for a key produced by {@link HashTable#probeForKey(int, int)}
   * @return The composite index for the first added key record in the original data.
   */
  public int getStartIndex(int keyIndex) {
    int batchIdx  = keyIndex / HashTable.BATCH_SIZE;
    int offsetIdx = keyIndex % HashTable.BATCH_SIZE;

    assert batchIdx < startIndices.size();

    SelectionVector4 sv4 = startIndices.get(batchIdx);

    return sv4.get(offsetIdx);
  }

  /**
   * Takes a composite index for a key produced by {@link HashJoinHelper#getStartIndex(int)}, and returns the composite index for the
   * next record in the list of records that match a key. The result is a composite index for a record within the original data set.
   * @param currentIdx A composite index for a key produced by {@link HashJoinHelper#getStartIndex(int)}.
   * @return The composite index for the next record in the list of records that match a key. The result is a composite index for a record within the original data set.
   */
  public int getNextIndex(int currentIdx) {
    // Get to the links field of the current index to get the next index
    int batchIdx = currentIdx >>> SHIFT_SIZE;
    int recordIdx = currentIdx & HashTable.BATCH_MASK;

    assert batchIdx < buildInfoList.size();

    // Get the corresponding BuildInfo node
    BuildInfo info = buildInfoList.get(batchIdx);
    return info.getLinks().get(recordIdx);
  }

  public IntArrayList getNextUnmatchedIndex() {
    IntArrayList compositeIndexes = new IntArrayList();

    for (int i = 0; i < buildInfoList.size(); i++) {
      BuildInfo info = buildInfoList.get(i);
      int fromIndex = 0;

      while (((fromIndex = info.getKeyMatchBitVector().nextClearBit(fromIndex)) != -1) && (fromIndex < info.recordCount)) {
          compositeIndexes.add((i << SHIFT_SIZE) | (fromIndex & HashTable.BATCH_MASK));
          fromIndex++;
      }
    }
    return compositeIndexes;
  }

  public boolean setRecordMatched(int index) {
    int batchIdx  = index >>> SHIFT_SIZE;
    int recordIdx = index & HashTable.BATCH_MASK;

    // Get the BitVector for the appropriate batch and set the bit to indicate the record matched
    BuildInfo info = buildInfoList.get(batchIdx);
    BitSet bitVector = info.getKeyMatchBitVector();

    if(bitVector.get(recordIdx)) {
      return true;
    }
    bitVector.set(recordIdx);
    return false;
  }

  public void setCurrentIndex(int keyIndex, int batchIndex, int recordIndex) throws SchemaChangeException {

    /* set the current record batch index and the index
     * within the batch at the specified keyIndex. The keyIndex
     * denotes the global index where the key for this record is
     * stored in the hash table
     */
    if (keyIndex < 0) {
      //receive a negative index, meaning we are not going to add this index (in distinct case when key already present)
      return;
    }

    int batchIdx  = keyIndex / HashTable.BATCH_SIZE;
    int offsetIdx = keyIndex % HashTable.BATCH_SIZE;

    if (keyIndex >= (HashTable.BATCH_SIZE * startIndices.size())) {
        // allocate a new batch
      addStartIndexBatch();
    }

    SelectionVector4 startIndex = startIndices.get(batchIdx);
    int linkIndex;

    // If head of the list is empty, insert current index at this position
    if ((linkIndex = (startIndex.get(offsetIdx))) == INDEX_EMPTY) {
      startIndex.set(offsetIdx, batchIndex, recordIndex);
    } else {
      /* Head of this list is not empty, if the first link
       * is empty insert there
       */
      batchIdx = linkIndex >>> SHIFT_SIZE;
      offsetIdx = linkIndex & Character.MAX_VALUE;

      SelectionVector4 link = buildInfoList.get(batchIdx).getLinks();
      int firstLink = link.get(offsetIdx);

      if (firstLink == INDEX_EMPTY) {
        link.set(offsetIdx, batchIndex, recordIndex);
      } else {
        /* Insert the current value as the first link and
         * make the current first link as its next
         */
        int firstLinkBatchIdx  = firstLink >>> SHIFT_SIZE;
        int firstLinkOffsetIDx = firstLink & Character.MAX_VALUE;

        SelectionVector4 nextLink = buildInfoList.get(batchIndex).getLinks();
        nextLink.set(recordIndex, firstLinkBatchIdx, firstLinkOffsetIDx);

        link.set(offsetIdx, batchIndex, recordIndex);
      }
    }
  }

  public void clear() {
    // Clear the SV4 used for start indices
    for (SelectionVector4 sv4: startIndices) {
      sv4.clear();
    }

    for (BuildInfo info : buildInfoList) {
      info.getLinks().clear();
    }
    buildInfoList.clear();
  }
}
