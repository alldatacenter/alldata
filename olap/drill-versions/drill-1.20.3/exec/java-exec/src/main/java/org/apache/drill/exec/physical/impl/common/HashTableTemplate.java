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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Named;

import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.compile.sig.RuntimeOverridden;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.AllocationManager;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.impl.join.HashJoinMemoryCalculator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.FixedWidthVector;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.common.exceptions.RetryAfterSpillException;
import org.apache.drill.exec.vector.VariableWidthVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HashTableTemplate implements HashTable {

  public static final int MAX_VARCHAR_SIZE = 8; // This is a bad heuristic which will be eliminated when the keys are removed from the HashTable.

  private static final Logger logger = LoggerFactory.getLogger(HashTableTemplate.class);
  private static final boolean EXTRA_DEBUG = false;

  private static final int EMPTY_SLOT = -1;

  // A hash 'bucket' consists of the start index to indicate start of a hash chain

  // Array of start indexes. start index is a global index across all batch holders
  //  This is the "classic hash table", where  Hash-Value % size-of-table  yields
  // the offset/position (in the startIndices) of the beginning of the hash chain.
  private IntVector startIndices;

  // Array of batch holders..each batch holder can hold up to BATCH_SIZE entries
  private ArrayList<BatchHolder> batchHolders;

  private int totalIndexSize; // index size of all batchHolders including current batch
  private int prevIndexSize; // index size of all batchHolders not including current batch
  private int currentIndexSize; // prevIndexSize + current batch count.

  // Current size of the hash table in terms of number of buckets
  private int tableSize = 0;

  // Original size of the hash table (needed when re-initializing)
  private int originalTableSize;

  // Threshold after which we rehash; It must be the tableSize * loadFactor
  private int threshold;

  // Actual number of entries in the hash table
  private int numEntries = 0;

  // current available (free) slot globally across all batch holders
  private int freeIndex = 0;

  private BufferAllocator allocator;

  // The incoming build side record batch
  private VectorContainer incomingBuild;

  // The incoming probe side record batch (may be null)
  private RecordBatch incomingProbe;

  // The outgoing record batch
  private RecordBatch outgoing;

  // Hash table configuration parameters
  private HashTableConfig htConfig;

  // Allocation tracker
  private HashTableAllocationTracker allocationTracker;

  // The original container from which others may be cloned
  private VectorContainer htContainerOrig;

  private MaterializedField dummyIntField;

  protected FragmentContext context;

  protected ClassGenerator<?> cg;

  private int numResizing = 0;

  private int resizingTime = 0;

  private Iterator<BatchHolder> htIter = null;

  // This class encapsulates the links, keys and values for up to BATCH_SIZE
  // *unique* records. Thus, suppose there are N incoming record batches, each
  // of size BATCH_SIZE..but they have M unique keys altogether, the number of
  // BatchHolders will be (M/BATCH_SIZE) + 1
  public class BatchHolder {

    // Container of vectors to hold type-specific keys
    private VectorContainer htContainer;

    // Array of 'link' values
    private IntVector links;

    // Array of hash values - this is useful when resizing the hash table
    private IntVector hashValues;

    private int maxOccupiedIdx = -1;
    private int targetBatchRowCount;
    private int batchIndex = 0;

    public void setTargetBatchRowCount(int targetBatchRowCount) {
      this.targetBatchRowCount = targetBatchRowCount;
    }

    public int getTargetBatchRowCount() {
      return targetBatchRowCount;
    }

    public BatchHolder(int idx, int newBatchHolderSize) {

      this.batchIndex = idx;
      this.targetBatchRowCount = newBatchHolderSize;

      htContainer = new VectorContainer();
      boolean success = false;
      try {
        for (VectorWrapper<?> w : htContainerOrig) {
          ValueVector vv = TypeHelper.getNewVector(w.getField(), allocator);
          htContainer.add(vv); // add to container before actual allocation (to allow clearing in case of an OOM)

          // Capacity for "hashValues" and "links" vectors is newBatchHolderSize records. It is better to allocate space for
          // "key" vectors to store as close to as newBatchHolderSize records. A new BatchHolder is created when either newBatchHolderSize
          // records are inserted or "key" vectors ran out of space. Allocating too less space for "key" vectors will
          // result in unused space in "hashValues" and "links" vectors in the BatchHolder. Also for each new
          // BatchHolder we create a SV4 vector of newBatchHolderSize in HashJoinHelper.
          if (vv instanceof FixedWidthVector) {
            ((FixedWidthVector) vv).allocateNew(newBatchHolderSize);
          } else if (vv instanceof VariableWidthVector) {
            long beforeMem = allocator.getAllocatedMemory();
            ((VariableWidthVector) vv).allocateNew(MAX_VARCHAR_SIZE * newBatchHolderSize, newBatchHolderSize);
            logger.trace("HT allocated {} for varchar of max width {}", allocator.getAllocatedMemory() - beforeMem, MAX_VARCHAR_SIZE);
          } else {
            vv.allocateNew();
          }
        }

        links = allocMetadataVector(newBatchHolderSize, EMPTY_SLOT);
        hashValues = allocMetadataVector(newBatchHolderSize, 0);
        success = true;
      } finally {
        if (!success) {
          htContainer.clear();
          if (links != null) {
            links.clear();
          }
        }
      }
    }

    @SuppressWarnings("unused")
    private void init(IntVector links, IntVector hashValues, int size) {
      for (int i = 0; i < size; i++) {
        links.getMutator().set(i, EMPTY_SLOT);
      }
      for (int i = 0; i < size; i++) {
        hashValues.getMutator().set(i, 0);
      }
      links.getMutator().setValueCount(size);
      hashValues.getMutator().setValueCount(size);
    }

    protected void setup() throws SchemaChangeException {
      setupInterior(incomingBuild, incomingProbe, outgoing, htContainer);
    }

    // Check if the key at the current Index position in hash table matches the key
    // at the incomingRowIdx.
    private boolean isKeyMatch(int incomingRowIdx,
        int currentIndex,
        boolean isProbe) throws SchemaChangeException {
      int currentIdxWithinBatch = currentIndex & BATCH_MASK;

      if (currentIdxWithinBatch >= batchHolders.get((currentIndex >>> 16) & BATCH_MASK).getTargetBatchRowCount()) {
        logger.debug("Batch size = {}, incomingRowIdx = {}, currentIdxWithinBatch = {}.",
          batchHolders.get((currentIndex >>> 16) & BATCH_MASK).getTargetBatchRowCount(), incomingRowIdx, currentIdxWithinBatch);
      }
      assert (currentIdxWithinBatch < batchHolders.get((currentIndex >>> 16) & BATCH_MASK).getTargetBatchRowCount());
      assert (incomingRowIdx < HashTable.BATCH_SIZE);

      if (isProbe) {
        return isKeyMatchInternalProbe(incomingRowIdx, currentIdxWithinBatch);
      }

      // in case of a hash-join build, where both the new incoming key and the current key are null, treat them as
      // a match; i.e. the new would be added into the helper (but not the Hash-Table !), though it would never
      // be used (not putting it into the helper would take a bigger code change, and some performance cost, hence
      // not worth it).  In the past such a new null key was added into the Hash-Table (i.e., no match), which
      // created long costly chains - SEE DRILL-6880)
      if ( areBothKeysNull(incomingRowIdx, currentIdxWithinBatch) ) { return true; }

      return isKeyMatchInternalBuild(incomingRowIdx, currentIdxWithinBatch);
    }

    // This method should only be used in an "iterator like" next() fashion, to traverse a hash table chain looking for a match.
    // Starting from the first element (i.e., index) in the chain, _isKeyMatch()_ should be called on that element; if "false" is returned,
    // then this method should be called to return the (index to the) next element in the chain (or an EMPTY_SLOT to terminate), and then
    // _isKeyMatch()_ should be called on that next element; and so on until a match is found - where the loop is exited with the found result.
    // (This was not implemented as a real Java iterator as each index may point to another BatchHolder).
    private int nextLinkInHashChain(int currentIndex) {
      return links.getAccessor().get(currentIndex & BATCH_MASK);
    }

    // Insert a new <key1, key2...keyN> entry coming from the incoming batch into the hash table
    // container at the specified index
    private void insertEntry(int incomingRowIdx, int currentIdx, int hashValue, BatchHolder lastEntryBatch, int lastEntryIdxWithinBatch) throws SchemaChangeException {
      int currentIdxWithinBatch = currentIdx & BATCH_MASK;
      setValue(incomingRowIdx, currentIdxWithinBatch);
      // setValue may OOM when doubling of one of the VarChar Key Value Vectors
      // This would be caught and retried later (setValue() is idempotent)

      // the previous entry in this hash chain should now point to the entry in this currentIdx
      if (lastEntryBatch != null) {
        lastEntryBatch.updateLinks(lastEntryIdxWithinBatch, currentIdx);
      }

      // since this is the last entry in the hash chain, the links array at position currentIdx
      // will point to a null (empty) slot
      links.getMutator().set(currentIdxWithinBatch, EMPTY_SLOT);
      hashValues.getMutator().set(currentIdxWithinBatch, hashValue);

      maxOccupiedIdx = Math.max(maxOccupiedIdx, currentIdxWithinBatch);

      if (EXTRA_DEBUG) {
        logger.debug("BatchHolder: inserted key at incomingRowIdx = {}, currentIdx = {}, hash value = {}.",
            incomingRowIdx, currentIdx, hashValue);
      }
    }

    private void updateLinks(int lastEntryIdxWithinBatch, int currentIdx) {
      links.getMutator().set(lastEntryIdxWithinBatch, currentIdx);
    }

    private void rehash(int numbuckets, IntVector newStartIndices, int batchStartIdx) {

      logger.debug("Rehashing entries within the batch: {}; batchStartIdx = {}, total numBuckets in hash table = {}.", batchIndex, batchStartIdx, numbuckets);

      int size = links.getAccessor().getValueCount();
      IntVector newLinks = allocMetadataVector(size, EMPTY_SLOT);
      IntVector newHashValues = allocMetadataVector(size, 0);

      for (int i = 0; i <= maxOccupiedIdx; i++) {
        int entryIdxWithinBatch = i;
        int entryIdx = entryIdxWithinBatch + batchStartIdx;
        int hash = hashValues.getAccessor().get(entryIdxWithinBatch); // get the already saved hash value
        int bucketIdx = getBucketIndex(hash, numbuckets);
        int newStartIdx = newStartIndices.getAccessor().get(bucketIdx);

        if (newStartIdx == EMPTY_SLOT) { // new bucket was empty
          newStartIndices.getMutator().set(bucketIdx, entryIdx); // update the start index to point to entry
          newLinks.getMutator().set(entryIdxWithinBatch, EMPTY_SLOT);
          newHashValues.getMutator().set(entryIdxWithinBatch, hash);

          if (EXTRA_DEBUG) {
            logger.debug("New bucket was empty. bucketIdx = {}, newStartIndices[ {} ] = {}, newLinks[ {} ] = {}, " +
                "hash value = {}.", bucketIdx, bucketIdx, newStartIndices.getAccessor().get(bucketIdx),
                entryIdxWithinBatch, newLinks.getAccessor().get(entryIdxWithinBatch),
                newHashValues.getAccessor().get(entryIdxWithinBatch));
          }

        } else {
          // follow the new table's hash chain until we encounter empty slot. Note that the hash chain could
          // traverse multiple batch holders, so make sure we are accessing the right batch holder.
          int idx = newStartIdx;
          int idxWithinBatch = 0;
          BatchHolder bh = this;
          while (true) {
            if (idx != EMPTY_SLOT) {
              idxWithinBatch = idx & BATCH_MASK;
              bh = batchHolders.get((idx >>> 16) & BATCH_MASK);
            }

            if (bh == this && newLinks.getAccessor().get(idxWithinBatch) == EMPTY_SLOT) {
              newLinks.getMutator().set(idxWithinBatch, entryIdx);
              newLinks.getMutator().set(entryIdxWithinBatch, EMPTY_SLOT);
              newHashValues.getMutator().set(entryIdxWithinBatch, hash);

              if (EXTRA_DEBUG) {
                logger.debug("Followed hash chain in new bucket. bucketIdx = {}, newLinks[ {} ] = {}, " +
                    "newLinks[ {} ] = {}, hash value = {}.", bucketIdx, idxWithinBatch,
                    newLinks.getAccessor().get(idxWithinBatch), entryIdxWithinBatch,
                    newLinks.getAccessor().get(entryIdxWithinBatch), newHashValues.getAccessor().get
                        (entryIdxWithinBatch));
              }

              break;
            } else if (bh != this && bh.links.getAccessor().get(idxWithinBatch) == EMPTY_SLOT) {
              bh.links.getMutator().set(idxWithinBatch, entryIdx); // update the link in the other batch
              newLinks.getMutator().set(entryIdxWithinBatch, EMPTY_SLOT); // update the newLink entry in this
              // batch to mark end of the hash chain
              newHashValues.getMutator().set(entryIdxWithinBatch, hash);

              if (EXTRA_DEBUG) {
                logger.debug("Followed hash chain in new bucket. bucketIdx = {}, newLinks[ {} ] = {}, " +
                    "newLinks[ {} ] = {}, hash value = {}.", bucketIdx, idxWithinBatch,
                    newLinks.getAccessor().get(idxWithinBatch), entryIdxWithinBatch,
                    newLinks.getAccessor().get(entryIdxWithinBatch),
                    newHashValues.getAccessor().get(entryIdxWithinBatch));
              }

              break;
            }
            if (bh == this) {
              idx = newLinks.getAccessor().get(idxWithinBatch);
            } else {
              idx = bh.links.getAccessor().get(idxWithinBatch);
            }
          }

        }

      }

      links.clear();
      hashValues.clear();

      links = newLinks;
      hashValues = newHashValues;
    }

    private boolean outputKeys(VectorContainer outContainer, int numRecords) {
      // set the value count for htContainer's value vectors before the transfer ..
      setValueCount();

      Iterator<VectorWrapper<?>> outgoingIter = outContainer.iterator();

      for (VectorWrapper<?> sourceWrapper : htContainer) {
        ValueVector sourceVV = sourceWrapper.getValueVector();
        ValueVector targetVV = outgoingIter.next().getValueVector();
        TransferPair tp = sourceVV.makeTransferPair(targetVV);
        // The normal case: The whole column key(s) are transfered as is
        tp.transfer();
      }
      return true;
    }

    private void setValueCount() {
      for (VectorWrapper<?> vw : htContainer) {
        ValueVector vv = vw.getValueVector();
        vv.getMutator().setValueCount(maxOccupiedIdx + 1);
      }
      htContainer.setRecordCount(maxOccupiedIdx+1);
    }

    private void dump(int idx) {
      while (true) {
        int idxWithinBatch = idx & BATCH_MASK;
        if (idxWithinBatch == EMPTY_SLOT) {
          break;
        } else {
          logger.debug("links[ {} ] = {}, hashValues[ {} ] = {}.", idxWithinBatch,
              links.getAccessor().get(idxWithinBatch), idxWithinBatch, hashValues.getAccessor().get(idxWithinBatch));
          idx = links.getAccessor().get(idxWithinBatch);
        }
      }
    }

    private void clear() {
      htContainer.clear();
      if ( links != null ) { links.clear(); }
      if ( hashValues != null ) { hashValues.clear(); }
    }

    // Only used for internal debugging. Get the value vector at a particular index from the htContainer.
    // By default this assumes the VV is a BigIntVector.
    @SuppressWarnings("unused")
    private ValueVector getValueVector(int index) {
      Object tmp = (htContainer).getValueAccessorById(BigIntVector.class, index).getValueVector();
      if (tmp != null) {
        BigIntVector vv0 = ((BigIntVector) tmp);
        return vv0;
      }
      return null;
    }

    // These methods will be code-generated

    @RuntimeOverridden
    protected void setupInterior(
        @Named("incomingBuild") VectorContainer incomingBuild,
        @Named("incomingProbe") RecordBatch incomingProbe,
        @Named("outgoing") RecordBatch outgoing,
        @Named("htContainer") VectorContainer htContainer) throws SchemaChangeException {
    }

    @RuntimeOverridden
    protected boolean isKeyMatchInternalBuild(
        @Named("incomingRowIdx") int incomingRowIdx, @Named("htRowIdx") int htRowIdx) throws SchemaChangeException {
      return false;
    }

    @RuntimeOverridden
    protected boolean areBothKeysNull(
      @Named("incomingRowIdx") int incomingRowIdx, @Named("htRowIdx") int htRowIdx) throws SchemaChangeException {
      return false;
    }

    @RuntimeOverridden
    protected boolean isKeyMatchInternalProbe(
        @Named("incomingRowIdx") int incomingRowIdx, @Named("htRowIdx") int htRowIdx) throws SchemaChangeException {
      return false;
    }

    @RuntimeOverridden
    protected void setValue(@Named("incomingRowIdx") int incomingRowIdx, @Named("htRowIdx") int htRowIdx) throws SchemaChangeException {
    }

    @RuntimeOverridden
    protected void outputRecordKeys(@Named("htRowIdx") int htRowIdx, @Named("outRowIdx") int outRowIdx) throws SchemaChangeException {
    }

    public long getActualSize() {
      Set<AllocationManager.BufferLedger> ledgers = Sets.newHashSet();
      links.collectLedgers(ledgers);
      hashValues.collectLedgers(ledgers);

      long size = 0L;

      for (AllocationManager.BufferLedger ledger: ledgers) {
        size += ledger.getAccountedSize();
      }

      // In some rare cases (e.g., making a detailed debug msg after an OOM) the container
      // was not initialized; ignore such cases
      if ( htContainer.hasRecordCount() ) {
        size += new RecordBatchSizer(htContainer).getActualSize();
      }
      return size;
    }
  }

  @Override
  public void setup(HashTableConfig htConfig, BufferAllocator allocator, VectorContainer incomingBuild,
                    RecordBatch incomingProbe, RecordBatch outgoing, VectorContainer htContainerOrig,
                    FragmentContext context, ClassGenerator<?> cg) {
    float loadf = htConfig.getLoadFactor();
    int initialCap = htConfig.getInitialCapacity();

    if (loadf <= 0 || Float.isNaN(loadf)) {
      throw new IllegalArgumentException("Load factor must be a valid number greater than 0");
    }
    if (initialCap <= 0) {
      throw new IllegalArgumentException("The initial capacity must be greater than 0");
    }
    if (initialCap > MAXIMUM_CAPACITY) {
      throw new IllegalArgumentException("The initial capacity must be less than maximum capacity allowed");
    }

    if (htConfig.getKeyExprsBuild() == null || htConfig.getKeyExprsBuild().size() == 0) {
      throw new IllegalArgumentException("Hash table must have at least 1 key expression");
    }

    this.htConfig = htConfig;
    this.allocator = allocator;
    this.incomingBuild = incomingBuild;
    this.incomingProbe = incomingProbe;
    this.outgoing = outgoing;
    this.htContainerOrig = htContainerOrig;
    this.context = context;
    this.cg = cg;
    this.allocationTracker = new HashTableAllocationTracker(htConfig);

    // round up the initial capacity to nearest highest power of 2
    tableSize = roundUpToPowerOf2(initialCap);
    if (tableSize > MAXIMUM_CAPACITY) {
      tableSize = MAXIMUM_CAPACITY;
    }
    originalTableSize = tableSize; // retain original size

    threshold = (int) Math.ceil(tableSize * loadf);

    dummyIntField = MaterializedField.create("dummy", Types.required(MinorType.INT));

    startIndices = allocMetadataVector(tableSize, EMPTY_SLOT);

    // Create the first batch holder
    batchHolders = new ArrayList<BatchHolder>();
    // First BatchHolder is created when the first put request is received.

    prevIndexSize = 0;
    currentIndexSize = 0;
    totalIndexSize = 0;

    try {
      doSetup(incomingBuild, incomingProbe);
    } catch (SchemaChangeException e) {
      throw new IllegalStateException("Unexpected schema change", e);
    }

  }

  @Override
  public void updateInitialCapacity(int initialCapacity) {
    htConfig = htConfig.withInitialCapacity(initialCapacity);
    allocationTracker = new HashTableAllocationTracker(htConfig);
    enlargeEmptyHashTableIfNeeded(initialCapacity);
  }

  @Override
  public void updateBatches() throws SchemaChangeException {
    doSetup(incomingBuild, incomingProbe);
    for (BatchHolder batchHolder : batchHolders) {
      batchHolder.setup();
    }
  }

  public int numBuckets() {
    return startIndices.getAccessor().getValueCount();
  }

  public int numResizing() {
    return numResizing;
  }

  @Override
  public int size() {
    return numEntries;
  }

  @Override
  public void getStats(HashTableStats stats) {
    assert stats != null;
    stats.numBuckets = numBuckets();
    stats.numEntries = numEntries;
    stats.numResizing = numResizing;
    stats.resizingTime = resizingTime;
  }

  @Override
  public boolean isEmpty() {
    return numEntries == 0;
  }

  @Override
  public void clear() {
    clear(true);
  }

  private void clear(boolean close) {
    if (close) {
      // If we are closing, we need to clear the htContainerOrig as well.
      htContainerOrig.clear();
    }

    if (batchHolders != null) {
      for (BatchHolder bh : batchHolders) {
        bh.clear();
      }
      batchHolders.clear();
      batchHolders = null;
      prevIndexSize = 0;
      currentIndexSize = 0;
      totalIndexSize = 0;
    }
    startIndices.clear();
    // currentIdxHolder = null; // keep IndexPointer in case HT is reused
    numEntries = 0;
  }

  private int getBucketIndex(int hash, int numBuckets) {
    return hash & (numBuckets - 1);
  }

  private static int roundUpToPowerOf2(int number) {
    int rounded = number >= MAXIMUM_CAPACITY
        ? MAXIMUM_CAPACITY
        : (rounded = Integer.highestOneBit(number)) != 0
        ? (Integer.bitCount(number) > 1) ? rounded << 1 : rounded
        : 1;

    return rounded;
  }

  private void retryAfterOOM(boolean batchAdded) throws RetryAfterSpillException {
    // If a batch was added then undo; otherwise when retrying this put() we'd miss a NEW_BATCH_ADDED
    if ( batchAdded ) {
      logger.trace("OOM - Removing index {} from the batch holders list",batchHolders.size() - 1);
      BatchHolder bh = batchHolders.remove(batchHolders.size() - 1);
      prevIndexSize = batchHolders.size() > 1 ? (batchHolders.size()-1) * BATCH_SIZE : 0;
      currentIndexSize = prevIndexSize + (batchHolders.size() > 0 ? batchHolders.get(batchHolders.size()-1).getTargetBatchRowCount() : 0);
      totalIndexSize = batchHolders.size() * BATCH_SIZE;
      // update freeIndex to point to end of last batch + 1
      freeIndex = totalIndexSize + 1;
      bh.clear();
    } else {
      freeIndex--;
    }
    throw new RetryAfterSpillException();
  }

  /**
   *   Return the Hash Value for the row in the Build incoming batch at index:
   *   (For Hash Aggregate there's no "Build" side -- only one batch - this one)
   *
   * @param incomingRowIdx
   * @return
   * @throws SchemaChangeException
   */
  @Override
  public int getBuildHashCode(int incomingRowIdx) throws SchemaChangeException {
    return getHashBuild(incomingRowIdx, 0);
  }

  /**
   *   Return the Hash Value for the row in the Probe incoming batch at index:
   *
   * @param incomingRowIdx
   * @return
   * @throws SchemaChangeException
   */
  @Override
  public int getProbeHashCode(int incomingRowIdx) throws SchemaChangeException {
    return getHashProbe(incomingRowIdx, 0);
  }

  /** put() uses the hash code (from gethashCode() above) to insert the key(s) from the incoming
   * row into the hash table. The code selects the bucket in the startIndices, then the keys are
   * placed into the chained list - by storing the key values into a batch, and updating its
   * "links" member. Last it modifies the index holder to the batch offset so that the caller
   * can store the remaining parts of the row into a matching batch (outside the hash table).
   * Returning
   *
   * @param incomingRowIdx - position of the incoming row
   * @param htIdxHolder - to return batch + batch-offset (for caller to manage a matching batch)
   * @param hashCode - computed over the key(s) by calling getBuildHashCode()
   * @return Status - the key(s) was ADDED or was already PRESENT
   */
  @Override
  public PutStatus put(int incomingRowIdx, IndexPointer htIdxHolder, int hashCode, int targetBatchRowCount) throws SchemaChangeException, RetryAfterSpillException {

    int bucketIndex = getBucketIndex(hashCode, numBuckets());
    int startIdx = startIndices.getAccessor().get(bucketIndex);
    int currentIdx;
    BatchHolder lastEntryBatch = null;
    int lastEntryIdxWithinBatch = EMPTY_SLOT;

    // if startIdx is non-empty, follow the hash chain links until we find a matching
    // key or reach the end of the chain (and remember the last link there)
    for ( int currentIndex = startIdx;
          currentIndex != EMPTY_SLOT;
          currentIndex = lastEntryBatch.nextLinkInHashChain(currentIndex)) {
      // remember the current link, which would be the last when the next link is empty
      lastEntryBatch = batchHolders.get((currentIndex >>> 16) & BATCH_MASK);
      lastEntryIdxWithinBatch = currentIndex & BATCH_MASK;

      if (lastEntryBatch.isKeyMatch(incomingRowIdx, currentIndex, false)) {
        htIdxHolder.value = currentIndex;
        return PutStatus.KEY_PRESENT;
      }
    }

    // no match was found, so insert a new entry
    currentIdx = freeIndex++;
    boolean addedBatch = false;
    try {  // ADD A BATCH
      addedBatch = addBatchIfNeeded(currentIdx, targetBatchRowCount);
      if (addedBatch) {
        // If we just added the batch, update the current index to point to beginning of new batch.
        currentIdx = (batchHolders.size() - 1) * BATCH_SIZE;
        freeIndex = currentIdx + 1;
      }
    } catch (OutOfMemoryException OOME) {
      retryAfterOOM( currentIdx < totalIndexSize);
    }

    try { // INSERT ENTRY
      BatchHolder bh = batchHolders.get((currentIdx >>> 16) & BATCH_MASK);
      bh.insertEntry(incomingRowIdx, currentIdx, hashCode, lastEntryBatch, lastEntryIdxWithinBatch);
      numEntries++;
    } catch (OutOfMemoryException OOME) { retryAfterOOM( addedBatch ); }

    try {  // RESIZE HT
      /* Resize hash table if needed and transfer the metadata
       * Resize only after inserting the current entry into the hash table
       * Otherwise our calculated lastEntryBatch and lastEntryIdx
       * becomes invalid after resize.
       */
      resizeAndRehashIfNeeded();
    } catch (OutOfMemoryException OOME) {
      numEntries--; // undo - insert entry
      if (lastEntryBatch != null) { // undo last added link in chain (if any)
        lastEntryBatch.updateLinks(lastEntryIdxWithinBatch, EMPTY_SLOT);
      }
      retryAfterOOM( addedBatch );
    }

    if (EXTRA_DEBUG) {
      logger.debug("No match was found for incomingRowIdx = {}; inserting new entry at currentIdx = {}.", incomingRowIdx, currentIdx);
    }

    // if there was no hash chain at this bucket, need to update the start index array
    if (startIdx == EMPTY_SLOT) {
      startIndices.getMutator().set(getBucketIndex(hashCode, numBuckets()), currentIdx);
    }
    htIdxHolder.value = currentIdx;
    return  addedBatch ? PutStatus.NEW_BATCH_ADDED :
        (freeIndex + 1 > currentIndexSize) ?
        PutStatus.KEY_ADDED_LAST : // the last key in the batch
        PutStatus.KEY_ADDED;     // otherwise
  }

  /**
   * Return -1 if Probe-side key is not found in the (build-side) hash table.
   * Otherwise, return the global index of the key
   *
   *
   * @param incomingRowIdx
   * @param hashCode - The hash code for the Probe-side key
   * @return -1 if key is not found, else return the global index of the key
   * @throws SchemaChangeException
   */
   @Override
  public int probeForKey(int incomingRowIdx, int hashCode) throws SchemaChangeException {
    int bucketIndex = getBucketIndex(hashCode, numBuckets());
     int startIdx = startIndices.getAccessor().get(bucketIndex);
     BatchHolder lastEntryBatch = null;

     for ( int currentIndex = startIdx;
           currentIndex != EMPTY_SLOT;
           currentIndex = lastEntryBatch.nextLinkInHashChain(currentIndex)) {
      lastEntryBatch = batchHolders.get((currentIndex >>> 16) & BATCH_MASK);
      if (lastEntryBatch.isKeyMatch(incomingRowIdx, currentIndex, true /* isProbe */)) {
        return currentIndex;
      }
    }
    return -1;
  }

  // Add a new BatchHolder to the list of batch holders if needed. This is based on the supplied
  // currentIdx; since each BatchHolder can hold up to BATCH_SIZE entries, if the currentIdx exceeds
  // the capacity, we will add a new BatchHolder. Return true if a new batch was added.
  private boolean addBatchIfNeeded(int currentIdx, int batchRowCount) throws SchemaChangeException {
     // Add a new batch if this is the first batch or
     // index is greater than current batch target count i.e. we reached the limit of current batch.
     if (batchHolders.size() == 0 || (currentIdx >= currentIndexSize)) {
      final int allocationSize = allocationTracker.getNextBatchHolderSize(batchRowCount);
      final BatchHolder bh = newBatchHolder(batchHolders.size(), allocationSize);
      batchHolders.add(bh);
      prevIndexSize = batchHolders.size() > 1 ? (batchHolders.size()-1)*BATCH_SIZE : 0;
      currentIndexSize = prevIndexSize + batchHolders.get(batchHolders.size()-1).getTargetBatchRowCount();
      totalIndexSize = batchHolders.size() * BATCH_SIZE;
      bh.setup();
      if (EXTRA_DEBUG) {
        logger.debug("HashTable: Added new batch. Num batches = {}.", batchHolders.size());
      }

      allocationTracker.commit(allocationSize);
      return true;
    }
    return false;
  }

  protected BatchHolder newBatchHolder(int index, int newBatchHolderSize) { // special method to allow debugging of gen code
    return this.injectMembers(new BatchHolder(index, newBatchHolderSize));
  }

  protected BatchHolder injectMembers(BatchHolder batchHolder) {
    CodeGenMemberInjector.injectMembers(cg, batchHolder, context);
    return batchHolder;
  }

  // Resize the hash table if needed by creating a new one with double the number of buckets.
  // For each entry in the old hash table, re-hash it to the new table and update the metadata
  // in the new table.. the metadata consists of the startIndices, links and hashValues.
  // Note that the keys stored in the BatchHolders are not moved around.
  private void resizeAndRehashIfNeeded() {
    if (numEntries < threshold) {
      return;
    }

    if (EXTRA_DEBUG) {
      logger.debug("Hash table numEntries = {}, threshold = {}; resizing the table...", numEntries, threshold);
    }

    // If the table size is already MAXIMUM_CAPACITY, don't resize
    // the table, but set the threshold to Integer.MAX_VALUE such that
    // future attempts to resize will return immediately.
    if (tableSize == MAXIMUM_CAPACITY) {
      threshold = Integer.MAX_VALUE;
      return;
    }

    int newTableSize = 2 * tableSize;
    newTableSize = roundUpToPowerOf2(newTableSize);

    // if not enough memory available to allocate the new hash-table, plus the new links and
    // the new hash-values (to replace the existing ones - inside rehash() ), then OOM
    if ( 4 /* sizeof(int) */ * ( newTableSize + 2 * HashTable.BATCH_SIZE /* links + hashValues */)
        >= allocator.getLimit() - allocator.getAllocatedMemory()) {
      throw new OutOfMemoryException("Resize Hash Table");
    }

    tableSize = newTableSize;
    if (tableSize > MAXIMUM_CAPACITY) {
      tableSize = MAXIMUM_CAPACITY;
    }

    long t0 = System.currentTimeMillis();

    // set the new threshold based on the new table size and load factor
    threshold = (int) Math.ceil(tableSize * htConfig.getLoadFactor());

    IntVector newStartIndices = allocMetadataVector(tableSize, EMPTY_SLOT);

    for (int i = 0; i < batchHolders.size(); i++) {
      BatchHolder bh = batchHolders.get(i);
      int batchStartIdx = i * BATCH_SIZE;
      bh.rehash(tableSize, newStartIndices, batchStartIdx);
    }

    startIndices.clear();
    startIndices = newStartIndices;

    if (EXTRA_DEBUG) {
      logger.debug("After resizing and rehashing, dumping the hash table...");
      logger.debug("Number of buckets = {}.", startIndices.getAccessor().getValueCount());
      for (int i = 0; i < startIndices.getAccessor().getValueCount(); i++) {
        logger.debug("Bucket: {}, startIdx[ {} ] = {}.", i, i, startIndices.getAccessor().get(i));
        int startIdx = startIndices.getAccessor().get(i);
        BatchHolder bh = batchHolders.get((startIdx >>> 16) & BATCH_MASK);
        bh.dump(startIdx);
      }
    }
    resizingTime += Math.toIntExact(System.currentTimeMillis() - t0);
    numResizing++;
  }

  /**
   *  Resize up the Hash Table if needed (to hold newNum entries)
   */
  public void enlargeEmptyHashTableIfNeeded(int newNum) {
    assert numEntries == 0;
    if ( newNum < threshold )  { return; } // no need to resize

    while ( tableSize * 2 < MAXIMUM_CAPACITY && newNum > threshold ) {
      tableSize *= 2;
      threshold = (int) Math.ceil(tableSize * htConfig.getLoadFactor());
    }
    startIndices.clear();
    startIndices = allocMetadataVector(tableSize, EMPTY_SLOT);
  }


  /**
   * Reinit the hash table to its original size, and clear up all its prior batch holder
   *
   */
  @Override
  public void reset() {
    this.clear(false); // Clear all current batch holders and hash table (i.e. free their memory)

    freeIndex = 0; // all batch holders are gone
    // reallocate batch holders, and the hash table to the original size
    batchHolders = new ArrayList<BatchHolder>();
    prevIndexSize = 0;
    currentIndexSize = 0;
    totalIndexSize = 0;
    startIndices = allocMetadataVector(originalTableSize, EMPTY_SLOT);
  }

  @Override
  public void updateIncoming(VectorContainer newIncoming, RecordBatch newIncomingProbe) {
    incomingBuild = newIncoming;
    incomingProbe = newIncomingProbe;
    // reset();
    try {
      updateBatches();  // Needed to update the value vectors in the generated code with the new incoming
    } catch (SchemaChangeException e) {
      throw new IllegalStateException("Unexpected schema change", e);
    }
  }

  @Override
  public boolean outputKeys(int batchIdx, VectorContainer outContainer, int numRecords) {
    assert batchIdx < batchHolders.size();
    return batchHolders.get(batchIdx).outputKeys(outContainer, numRecords);
  }

  private IntVector allocMetadataVector(int size, int initialValue) {
    IntVector vector = (IntVector) TypeHelper.getNewVector(dummyIntField, allocator);
    vector.allocateNew(size);
    for (int i = 0; i < size; i++) {
      vector.getMutator().set(i, initialValue);
    }
    vector.getMutator().setValueCount(size);
    return vector;
  }

  @Override
  public Pair<VectorContainer, Integer> nextBatch() {
    if (batchHolders == null || batchHolders.size() == 0) {
      return null;
    }
    if (htIter == null) {
      htIter = batchHolders.iterator();
    }
    if (htIter.hasNext()) {
      BatchHolder bh = htIter.next();
      // set the value count for the vectors in the batch
      // TODO: investigate why the value count is not already set in the
      // batch.. it seems even outputKeys() sets the value count explicitly
      if (bh != null) {
        bh.setValueCount();
        return Pair.of(bh.htContainer, bh.maxOccupiedIdx);
      }
    }
    return null;
  }

  // These methods will be code-generated in the context of the outer class
  protected abstract void doSetup(@Named("incomingBuild") VectorContainer incomingBuild, @Named("incomingProbe") RecordBatch incomingProbe) throws SchemaChangeException;

  protected abstract int getHashBuild(@Named("incomingRowIdx") int incomingRowIdx, @Named("seedValue") int seedValue) throws SchemaChangeException;

  protected abstract int getHashProbe(@Named("incomingRowIdx") int incomingRowIdx, @Named("seedValue") int seedValue) throws SchemaChangeException;

  @Override
  public long getActualSize() {
    Set<AllocationManager.BufferLedger> ledgers = Sets.newHashSet();
    startIndices.collectLedgers(ledgers);

    long size = 0L;

    for (AllocationManager.BufferLedger ledger: ledgers) {
      size += ledger.getAccountedSize();
    }

    for (BatchHolder batchHolder: batchHolders) {
      size += batchHolder.getActualSize();
    }

    return size;
  }

  @Override
  public String makeDebugString() {
    return String.format("[numBuckets = %d, numEntries = %d, numBatchHolders = %d, actualSize = %s]",
      numBuckets(), numEntries, batchHolders.size(), HashJoinMemoryCalculator.PartitionStatSet.prettyPrintBytes(getActualSize()));
  }

  @Override
  public void setTargetBatchRowCount(int batchRowCount) {
    batchHolders.get(batchHolders.size()-1).targetBatchRowCount = batchRowCount;
  }

  @Override
  public int getTargetBatchRowCount() {
    return batchHolders.get(batchHolders.size()-1).targetBatchRowCount;
  }
}
