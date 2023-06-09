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

import static org.apache.drill.exec.physical.impl.common.HashTable.BATCH_SIZE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.exceptions.RetryAfterSpillException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.cache.VectorSerializer;
import org.apache.drill.exec.exception.ClassTransformationException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.impl.join.HashJoinHelper;
import org.apache.drill.exec.physical.impl.join.HashJoinMemoryCalculator;
import org.apache.drill.exec.physical.impl.spill.SpillSet;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.TransferPair;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.vector.FixedWidthVector;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.ObjectVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VariableWidthVector;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.IntArrayList;

/**
 * <h2>Overview</h2>
 * <p>
 *    Created to represent an active partition for the Hash-Join operator
 *  (active means: currently receiving data, or its data is being probed; as opposed to fully
 *   spilled partitions).
 *    After all the build/inner data is read for this partition - if all its data is in memory, then
 *  a hash table and a helper are created, and later this data would be probed.
 *    If all this partition's build/inner data was spilled, then it begins to work as an outer
 *  partition (see the flag "processingOuter") -- reusing some of the fields (e.g., currentBatch,
 *  currHVVector, writer, spillFile, partitionBatchesCount) for the outer.
 *  </p>
 */
public class HashPartition implements HashJoinMemoryCalculator.PartitionStat {
  static final Logger logger = LoggerFactory.getLogger(HashPartition.class);

  public static final String HASH_VALUE_COLUMN_NAME = "$Hash_Values$";

  private int partitionNum = -1; // the current number of this partition, as used by the operator

  private static final int VARIABLE_MIN_WIDTH_VALUE_SIZE = 8;
  private final int maxColumnWidth = VARIABLE_MIN_WIDTH_VALUE_SIZE; // to control memory allocation for varchars

  public static final MajorType HVtype = MajorType.newBuilder()
    .setMinorType(MinorType.INT /* dataType */)
    .setMode(DataMode.REQUIRED /* mode */)
    .build();

  // The vector containers storing all the inner rows
  // * Records are retrieved from these containers when there is a matching record
  // * on the probe side
  private ArrayList<VectorContainer> containers;

  // While build data is incoming - temporarily keep the list of in-memory
  // incoming batches, per each partition (these may be spilled at some point)
  private final List<VectorContainer> tmpBatchesList;
  // A batch and HV vector to hold incoming rows - per each partition
  private VectorContainer currentBatch; // The current (newest) batch
  private IntVector currHVVector; // The HV vectors for the currentBatches

  /* Helper class
   * Maintains linked list of build side records with the same key
   * Keeps information about which build records have a corresponding
   * matching key in the probe side (for outer, right joins)
   */
  private HashJoinHelper hjHelper;

  // Underlying hashtable used by the hash join
  private HashTable hashTable;

  private VectorSerializer.Writer writer; // a vector writer for each spilled partition
  private int partitionBatchesCount; // count number of batches spilled
  private String spillFile;

  private final BufferAllocator allocator;
  private int recordsPerBatch;
  private final SpillSet spillSet;
  private boolean isSpilled; // is this partition spilled ?
  private boolean processingOuter; // is (inner done spilling and) now the outer is processed?
  private boolean outerBatchAllocNotNeeded; // when the inner is whole in memory
  private final RecordBatch buildBatch;
  private final RecordBatch probeBatch;
  private final int cycleNum;
  private final int numPartitions;
  private final List<HashJoinMemoryCalculator.BatchStat> inMemoryBatchStats = Lists.newArrayList();
  private long partitionInMemorySize;
  private long numInMemoryRecords;
  private boolean updatedRecordsPerBatch;
  private final boolean semiJoin;

  public HashPartition(FragmentContext context, BufferAllocator allocator, ChainedHashTable baseHashTable,
                       RecordBatch buildBatch, RecordBatch probeBatch, boolean semiJoin,
                       int recordsPerBatch, SpillSet spillSet, int partNum, int cycleNum, int numPartitions) {
    this.allocator = allocator;
    this.buildBatch = buildBatch;
    this.probeBatch = probeBatch;
    this.recordsPerBatch = recordsPerBatch;
    this.spillSet = spillSet;
    this.partitionNum = partNum;
    this.cycleNum = cycleNum;
    this.numPartitions = numPartitions;
    this.semiJoin = semiJoin;

    try {
      this.hashTable = baseHashTable.createAndSetupHashTable(null);
    } catch (ClassTransformationException e) {
      throw UserException.unsupportedError(e)
        .message("Code generation error - likely an error in the code.")
        .build(logger);
    } catch (IOException e) {
      throw UserException.resourceError(e)
        .message("IO Error while creating a hash table.")
        .build(logger);
    } catch (SchemaChangeException sce) {
      throw new IllegalStateException("Unexpected Schema Change while creating a hash table",sce);
    }
    this.hjHelper = semiJoin ? null : new HashJoinHelper(context, allocator);
    tmpBatchesList = new ArrayList<>();
    if (numPartitions > 1) {
      allocateNewCurrentBatchAndHV();
    }
  }

  /**
   * Configure a different temporary batch size when spilling probe batches.
   * @param newRecordsPerBatch The new temporary batch size to use.
   */
  public void updateProbeRecordsPerBatch(int newRecordsPerBatch) {
    Preconditions.checkArgument(newRecordsPerBatch > 0);
    Preconditions.checkState(!updatedRecordsPerBatch); // Only allow updating once
    Preconditions.checkState(processingOuter); // We can only update the records per batch when probing.

    recordsPerBatch = newRecordsPerBatch;
  }

  /**
   * Allocate a new vector container for either right or left record batch
   * Add an additional special vector for the hash values
   * Note: this call may OOM !!
   * @param rb - either the right or the left record batch
   * @return the new vector container
   */
  private VectorContainer allocateNewVectorContainer(RecordBatch rb) {
    VectorContainer newVC = new VectorContainer();
    VectorContainer fromVC = rb.getContainer();
    Iterator<VectorWrapper<?>> vci = fromVC.iterator();
    boolean success = false;

    try {
      while (vci.hasNext()) {
        VectorWrapper<?> vw = vci.next();
        // If processing a spilled container, skip the last column (HV)
        if (cycleNum > 0 && ! vci.hasNext()) { break; }
        ValueVector vv = vw.getValueVector();
        ValueVector newVV = TypeHelper.getNewVector(vv.getField(), allocator);
        newVC.add(newVV); // add first to allow dealloc in case of an OOM

        if (newVV instanceof FixedWidthVector) {
          ((FixedWidthVector) newVV).allocateNew(recordsPerBatch);
        } else if (newVV instanceof VariableWidthVector) {
          ((VariableWidthVector) newVV).allocateNew(maxColumnWidth * recordsPerBatch, recordsPerBatch);
        } else if (newVV instanceof ObjectVector) {
          ((ObjectVector) newVV).allocateNew(recordsPerBatch);
        } else {
          newVV.allocateNew();
        }
      }

      newVC.setRecordCount(0);
      success = true;
    } finally {
      if (!success) {
        newVC.clear(); // in case of an OOM
      }
    }
    return newVC;
  }

  /**
   *  Allocate a new current Vector Container and current HV vector
   */
  public void allocateNewCurrentBatchAndHV() {
    if (outerBatchAllocNotNeeded) { return; } // skip when the inner is whole in memory
    currentBatch = allocateNewVectorContainer(processingOuter ? probeBatch : buildBatch);
    currHVVector = new IntVector(MaterializedField.create(HASH_VALUE_COLUMN_NAME, HVtype), allocator);
    currHVVector.allocateNew(recordsPerBatch);
  }

  /**
   *  Spills if needed
   */
  public void appendInnerRow(VectorContainer buildContainer, int ind,
      int hashCode, HashJoinMemoryCalculator.BuildSidePartitioning calc) {

    int pos = currentBatch.appendRow(buildContainer, ind);
    currHVVector.getMutator().set(pos - 1, hashCode);   // store the hash value in the new column
    if (pos == recordsPerBatch) {
      boolean needsSpill = isSpilled || calc.shouldSpill();
      completeAnInnerBatch(true, needsSpill);
    }
  }

  /**
   *  Outer always spills when batch is full
   */
  public void appendOuterRow(int hashCode, int recordsProcessed) {
    int pos = currentBatch.appendRow(probeBatch.getContainer(),recordsProcessed);
    currHVVector.getMutator().set(pos - 1, hashCode);   // store the hash value in the new column
    if (pos == recordsPerBatch) {
      completeAnOuterBatch(true);
    }
  }

  public void completeAnOuterBatch(boolean toInitialize) {
    completeABatch(toInitialize, true);
  }

  public void completeAnInnerBatch(boolean toInitialize, boolean needsSpill) {
    completeABatch(toInitialize, needsSpill);
  }

  /**
   *     A current batch is full (or no more rows incoming) - complete processing this batch
   * I.e., add it to its partition's tmp list, if needed - spill that list, and if needed -
   * (that is, more rows are coming) - initialize with a new current batch for that partition
   * */
  private void completeABatch(boolean toInitialize, boolean needsSpill) {
    if (currentBatch.hasRecordCount() && currentBatch.getRecordCount() > 0) {
      currentBatch.add(currHVVector);
      currentBatch.buildSchema(BatchSchema.SelectionVectorMode.NONE);
      tmpBatchesList.add(currentBatch);
      partitionBatchesCount++;

      long batchSize = new RecordBatchSizer(currentBatch).getActualSize();
      inMemoryBatchStats.add(new HashJoinMemoryCalculator.BatchStat(currentBatch.getRecordCount(), batchSize));

      partitionInMemorySize += batchSize;
      numInMemoryRecords += currentBatch.getRecordCount();
    } else {
      freeCurrentBatchAndHVVector();
    }
    if (needsSpill) { // spill this batch/partition and free its memory
      spillThisPartition();
    }
    if (toInitialize) { // allocate a new batch and HV vector
      allocateNewCurrentBatchAndHV();
    } else {
      currentBatch = null;
      currHVVector = null;
    }
  }

  /**
   *  Append the incoming batch (actually only the vectors of that batch) into the tmp list
   */
  public void appendBatch(VectorAccessible batch) {
    assert numPartitions == 1;
    int recordCount = batch.getRecordCount();
    currHVVector = new IntVector(MaterializedField.create(HASH_VALUE_COLUMN_NAME, HVtype), allocator);
    currHVVector.allocateNew(recordCount /* recordsPerBatch */);
    try {
      // For every record in the build batch, hash the key columns and keep the result
      for (int ind = 0; ind < recordCount; ind++) {
        int hashCode = getBuildHashCode(ind);
        currHVVector.getMutator().set(ind, hashCode);   // store the hash value in the new HV column
      }
    } catch(SchemaChangeException sce) {}

    VectorContainer container = new VectorContainer();
    List<ValueVector> vectors = Lists.newArrayList();

    for (VectorWrapper<?> v : batch) {
      TransferPair tp = v.getValueVector().getTransferPair(allocator);
      tp.transfer();
      vectors.add(tp.getTo());
    }

    container.addCollection(vectors);
    container.add(currHVVector); // the HV vector is added as an extra "column"
    container.setRecordCount(recordCount);
    container.buildSchema(BatchSchema.SelectionVectorMode.NONE);

    tmpBatchesList.add(container);
    partitionBatchesCount++;
    currHVVector = null;
    numInMemoryRecords += recordCount;
  }

  public void spillThisPartition() {
    if (tmpBatchesList.size() == 0) { return; } // in case empty - nothing to spill
    logger.debug("HashJoin: Spilling partition {}, current cycle {}, part size {} batches", partitionNum, cycleNum, tmpBatchesList.size());

    // If this is the first spill for this partition, create an output stream
    if (writer == null) {
      final String side = processingOuter ? "outer" : "inner";
      final String suffix = cycleNum > 0 ? side + "_" + Integer.toString(cycleNum) : side;
      spillFile = spillSet.getNextSpillFile(suffix);

      try {
        writer = spillSet.writer(spillFile);
      } catch (IOException ioe) {
        throw UserException.resourceError(ioe)
          .message("Hash Join failed to open spill file: " + spillFile)
          .build(logger);
      }

      isSpilled = true;
    }

    partitionInMemorySize = 0L;
    numInMemoryRecords = 0L;
    inMemoryBatchStats.clear();

    while (tmpBatchesList.size() > 0) {
      VectorContainer vc = tmpBatchesList.remove(0);

      int numRecords = vc.getRecordCount();

      // set the value count for outgoing batch value vectors
      vc.setValueCount(numRecords);

      WritableBatch wBatch = WritableBatch.getBatchNoHVWrap(numRecords, vc, false);
      try {
        writer.write(wBatch, null);
      } catch (IOException ioe) {
        throw UserException.dataWriteError(ioe)
          .message("Hash Join failed to write to output file: " + spillFile)
          .build(logger);
      } finally {
        wBatch.clear();
      }
      vc.zeroVectors();
      logger.trace("HASH JOIN: Took {} us to spill {} records", writer.time(TimeUnit.MICROSECONDS), numRecords);
    }
  }

  //
  // ===== Methods to probe the hash table and to get indices out of the helper =======
  //

  public int probeForKey(int recordsProcessed, int hashCode) throws SchemaChangeException {
    return hashTable.probeForKey(recordsProcessed, hashCode);
  }

  public Pair<Integer, Boolean> getStartIndex(int probeIndex) {
    /* The current probe record has a key that matches. Get the index
     * of the first row in the build side that matches the current key
     */
    int compositeIndex = hjHelper.getStartIndex(probeIndex);
    /* Record in the build side at currentCompositeIdx has a matching record in the probe
     * side. Set the bit corresponding to this index so if we are doing a FULL or RIGHT
     * join we keep track of which records we need to project at the end
     */
    boolean matchExists = hjHelper.setRecordMatched(compositeIndex);
    return Pair.of(compositeIndex, matchExists);
  }

  public int getNextIndex(int compositeIndex) {
    // in case of inner rows with duplicate keys, get the next one
    return hjHelper.getNextIndex(compositeIndex);
  }

  public boolean setRecordMatched(int compositeIndex) {
    return hjHelper.setRecordMatched(compositeIndex);
  }

  public IntArrayList getNextUnmatchedIndex() {
    return hjHelper.getNextUnmatchedIndex();
  }

  //
  // =====================================================================================
  //

  public int getBuildHashCode(int ind) throws SchemaChangeException {
    return hashTable.getBuildHashCode(ind);
  }

  public int getProbeHashCode(int ind) throws SchemaChangeException {
    return hashTable.getProbeHashCode(ind);
  }

  public ArrayList<VectorContainer> getContainers() {
    return containers;
  }

  public void updateBatches() throws SchemaChangeException {
    hashTable.updateBatches();
  }

  public Pair<VectorContainer, Integer> nextBatch() {
    return hashTable.nextBatch();
  }

  @Override
  public List<HashJoinMemoryCalculator.BatchStat> getInMemoryBatches() {
    return inMemoryBatchStats;
  }

  @Override
  public int getNumInMemoryBatches() {
    return inMemoryBatchStats.size();
  }

  @Override
  public boolean isSpilled() {
    return isSpilled;
  }

  @Override
  public long getNumInMemoryRecords() {
    return numInMemoryRecords;
  }

  @Override
  public long getInMemorySize() {
    return partitionInMemorySize;
  }

  public String getSpillFile() {
    return spillFile;
  }

  public int getPartitionBatchesCount() {
    return partitionBatchesCount;
  }

  public int getPartitionNum() {
    return partitionNum;
  }

  /**
   * Close the writer without deleting the spill file
   */
  public void closeWriter() { // no deletion !!
    closeWriterInternal(false);
    processingOuter = true; // After the spill file was closed
  }

  /**
   * If exists - close the writer for this partition
   *
   * @param doDeleteFile Also delete the associated file
   */
  private void closeWriterInternal(boolean doDeleteFile) {
    try {
      if (writer != null) {
        spillSet.close(writer);
      }
      if (doDeleteFile && spillFile != null) {
        spillSet.delete(spillFile);
      }
    } catch (IOException ioe) {
      throw UserException.resourceError(ioe)
        .message("IO Error while closing %s spill file %s",
          doDeleteFile ? "and deleting" : "",
          spillFile)
        .build(logger);
    }
    spillFile = null;
    writer = null;
    partitionBatchesCount = 0;
  }

  /**
   * Creates the hash table and join helper for this partition.
   * This method should only be called after all the build side records
   * have been consumed.
   */
  public void buildContainersHashTableAndHelper() throws SchemaChangeException {
    if (isSpilled) { return; } // no building for spilled partitions
    containers = new ArrayList<>();
    hashTable.updateInitialCapacity((int) getNumInMemoryRecords());
    for (int curr = 0; curr < partitionBatchesCount; curr++) {
      VectorContainer nextBatch = tmpBatchesList.get(curr);
      final int currentRecordCount = nextBatch.getRecordCount();

      // For every incoming build batch, we create a matching helper batch
      if (! semiJoin) { hjHelper.addNewBatch(currentRecordCount); }

      // Holder contains the global index where the key is hashed into using the hash table
      final IndexPointer htIndex = new IndexPointer();

      assert nextBatch != null;
      assert probeBatch != null;

      hashTable.updateIncoming(nextBatch, probeBatch);

      IntVector HV_vector = (IntVector) nextBatch.getLast();

      for (int recInd = 0; recInd < currentRecordCount; recInd++) {
        int hashCode = HV_vector.getAccessor().get(recInd);
        try {
          hashTable.put(recInd, htIndex, hashCode, BATCH_SIZE);
        } catch (RetryAfterSpillException RE) {
          throw new OutOfMemoryException("HT put");
        } // Hash Join does not retry
        /* Use the global index returned by the hash table, to store
         * the current record index and batch index. This will be used
         * later when we probe and find a match.
         */
        if (! semiJoin) { hjHelper.setCurrentIndex(htIndex.value, curr /* buildBatchIndex */, recInd); }
      }

      containers.add(nextBatch);
    }
    outerBatchAllocNotNeeded = true; // the inner is whole in memory, no need for an outer batch
  }

  public void getStats(HashTableStats newStats) {
    hashTable.getStats(newStats);
  }

  /**
   * Frees memory allocated to the {@link HashTable} and {@link HashJoinHelper}.
   */
  private void clearHashTableAndHelper() {
    if (hashTable != null) {
      hashTable.clear();
      hashTable = null;
    }
    if (hjHelper != null) {
      hjHelper.clear();
      hjHelper = null;
    }
  }

  private void freeCurrentBatchAndHVVector() {
    if (currentBatch != null) {
      currentBatch.clear();
      currentBatch = null;
    }
    if (currHVVector != null) {
      currHVVector.clear();
      currHVVector = null;
    }
  }

  /**
   * Free all in-memory allocated structures.
   * @param deleteFile - whether to delete the spill file or not
   */
  public void cleanup(boolean deleteFile) {
    freeCurrentBatchAndHVVector();
    if (containers != null && !containers.isEmpty()) {
      for (VectorContainer vc : containers) {
        vc.clear();
      }
    }
    while (tmpBatchesList.size() > 0) {
      VectorContainer vc = tmpBatchesList.remove(0);
      vc.clear();
    }
    closeWriterInternal(deleteFile);
    clearHashTableAndHelper();
    if (containers != null) {
      containers.clear();
    }
  }

  public void close() {
    cleanup(true);
  }

  /**
   * Creates a debugging string containing information about memory usage.
   * @return A debugging string.
   */
  public String makeDebugString() {
    return String.format("[hashTable = %s]",
      hashTable == null ? "None": hashTable.makeDebugString());
  }
}
