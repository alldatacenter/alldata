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
package org.apache.drill.exec.store.mapr.db;

import com.mapr.db.impl.IdCodec;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.exec.physical.impl.join.RowKeyJoin;
import org.apache.drill.exec.record.AbstractRecordBatch.BatchState;
import org.apache.drill.exec.vector.ValueVector;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.ByteBuffer;

/**
 * A RestrictedMapRDBSubScanSpec encapsulates a join instance which contains the ValueVectors of row keys and
 * is associated with this sub-scan and also exposes an iterator type interface over the row key vectors.
 */
public class RestrictedMapRDBSubScanSpec extends MapRDBSubScanSpec {

  /**
   * The RowKeyJoin instance (specific to one minor fragment) which will supply this
   * subscan with the set of rowkeys. For efficiency, we keep a reference to this
   * join rather than making another copy of the rowkeys.
   */
  private RowKeyJoin rjbatch = null;

  /**
   * The following are needed to maintain internal state of iteration over the set
   * of row keys
   */
  private ValueVector rowKeyVector = null; // the current row key value vector
  private int currentIndex = 0;  // the current index within the row key vector
  private int maxOccupiedIndex = -1; // max occupied index within a row key vector

  public RestrictedMapRDBSubScanSpec(String tableName, String regionServer, byte[] serializedFilter, String userName) {
    super(tableName, null, regionServer, null, null, serializedFilter, null, userName);
  }
  /* package */ RestrictedMapRDBSubScanSpec() {
    // empty constructor, to be used with builder pattern;
  }

  public void setJoinForSubScan(RowKeyJoin rjbatch) {
    this.rjbatch = rjbatch;
  }

  @JsonIgnore
  public RowKeyJoin getJoinForSubScan() {
    return rjbatch;
  }

  @JsonIgnore
  private void init(Pair<ValueVector, Integer> b) {
    this.maxOccupiedIndex = b.getRight();
    this.rowKeyVector = b.getLeft();
    this.currentIndex = 0;
  }

  /**
   * Return {@code true} if a valid rowkey batch is available, {@code false} otherwise
   */
  @JsonIgnore
  public boolean readyToGetRowKey() {
    return rjbatch != null && rjbatch.hasRowKeyBatch();
  }

  /**
   * Return {@code true} if the row key join is in the build schema phase
   */
  @JsonIgnore
  public boolean isBuildSchemaPhase() {
    return rjbatch.getBatchState() == BatchState.BUILD_SCHEMA;
  }

  /**
   * Returns {@code true} if the iteration has more row keys.
   * (In other words, returns {@code true} if {@link #nextRowKey} would
   * return a non-null row key)
   * @return {@code true} if the iteration has more row keys
   */
  @JsonIgnore
  public boolean hasRowKey() {
    if (rowKeyVector != null && currentIndex <= maxOccupiedIndex) {
      return true;
    }

    if (rjbatch != null) {
      Pair<ValueVector, Integer> currentBatch = rjbatch.nextRowKeyBatch();

      // note that the hash table could be null initially during the BUILD_SCHEMA phase
      if (currentBatch != null) {
        init(currentBatch);
        return true;
      }
    }

    return false;
  }

  @JsonIgnore
  public int getMaxRowKeysToBeRead() {
    if (rjbatch != null) {
      Pair<ValueVector, Integer> currentBatch = rjbatch.nextRowKeyBatch();

      // note that the currentBatch could be null initially during the BUILD_SCHEMA phase
      if (currentBatch != null) {
        init(currentBatch);
      }
    }
    return maxOccupiedIndex + 1;
  }

  /**
   * Returns number of rowKeys that can be read.
   * Number of rowKeys returned will be numRowKeysToRead at the most i.e. it
   * will be less than numRowKeysToRead if only that many exist in the currentBatch.
   */
  @JsonIgnore
  public int hasRowKeys(int numRowKeysToRead) {
    int numKeys = 0;

    // if there is pending rows from the current batch, read them first
    // in chunks of numRowsToRead rows
    if (rowKeyVector != null && currentIndex <= maxOccupiedIndex) {
        numKeys = Math.min(numRowKeysToRead, maxOccupiedIndex - currentIndex + 1);
        return numKeys;
    }

    // otherwise, get the next batch of rowkeys
    if (rjbatch != null) {
      Pair<ValueVector, Integer> currentBatch = rjbatch.nextRowKeyBatch();

      // note that the currentBatch could be null initially during the BUILD_SCHEMA phase
      if (currentBatch != null) {
        init(currentBatch);
        numKeys = Math.min(numRowKeysToRead, maxOccupiedIndex - currentIndex + 1);
      }
    }

    return numKeys;
  }

  /**
   * Returns ids of rowKeys to be read.
   * Number of rowKey ids returned will be numRowKeysToRead at the most i.e. it
   * will be less than numRowKeysToRead if only that many exist in the currentBatch.
   */
  @JsonIgnore
  public ByteBuffer[] getRowKeyIdsToRead(int numRowKeysToRead) {

    int numKeys = hasRowKeys(numRowKeysToRead);
    if (numKeys == 0) {
      return null;
    }

    int index = 0;
    final ByteBuffer[] rowKeyIds = new ByteBuffer[numKeys];

    while (index < numKeys) {
      Object o = rowKeyVector.getAccessor().getObject(currentIndex + index);
      rowKeyIds[index++] = IdCodec.encode(o.toString());
    }

    updateRowKeysRead(numKeys);
    return rowKeyIds;
  }

  /**
   * updates the index to reflect number of keys read.
   */
  @JsonIgnore
  public void updateRowKeysRead(int numKeys) {
    currentIndex += numKeys;
  }

}
