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
package org.apache.drill.exec.expr;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Holder class that contains batch naming, batch  and record index. Batch index is used when batch is hyper container.
 * Used to distinguish batches in non-equi conditions during expression materialization.
 * Mostly used for nested loop join which allows non equi-join.
 *
 * BatchReference instance can be created during batch initialization
 * (ex: instance of {@link org.apache.drill.exec.record.AbstractRecordBatch})
 * since naming of batches used won't change during data processing.
 * Though information from batch reference will be used during schema build (i.e. once per OK_NEW_SCHEMA).
 *
 * Example:
 * BatchReference{batchName='leftBatch', batchIndex='leftIndex', recordIndex='leftIndex'}
 * BatchReference{batchName='rightContainer', batchIndex='rightBatchIndex', recordIndex='rightRecordIndexWithinBatch'}
 *
 */
public final class BatchReference {

  private final String batchName;

  private final String batchIndex;

  private final String recordIndex;

  public BatchReference(String batchName, String recordIndex) {
    // when batch index is not indicated, record index value will be set instead
    this(batchName, recordIndex, recordIndex);
  }

  public BatchReference(String batchName, String batchIndex, String recordIndex) {
    Preconditions.checkNotNull(batchName, "Batch name should not be null.");
    Preconditions.checkNotNull(batchIndex, "Batch index should not be null.");
    Preconditions.checkNotNull(recordIndex, "Record index should not be null.");
    this.batchName = batchName;
    this.batchIndex = batchIndex;
    this.recordIndex = recordIndex;
  }

  public String getBatchName() {
    return batchName;
  }

  public String getBatchIndex() {
    return batchIndex;
  }

  public String getRecordIndex() {
    return recordIndex;
  }

  @Override
  public String toString() {
    return "BatchReference{" +
        "batchName='" + batchName + '\'' +
        ", batchIndex='" + batchIndex + '\'' +
        ", recordIndex='" + recordIndex + '\'' +
        '}';
  }
}
