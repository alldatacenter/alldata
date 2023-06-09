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

import org.apache.drill.exec.record.RecordBatch;

/**
 * This class predicts the sizes of batches given an input batch.
 *
 * <h4>Invariants</h4>
 * <ul>
 *   <li>The {@link BatchSizePredictor} assumes that a {@link RecordBatch} is in a state where it can return a valid record count.</li>
 * </ul>
 */
public interface BatchSizePredictor {
  /**
   * Gets the batchSize computed in the call to {@link #updateStats()}. Returns 0 if {@link #hadDataLastTime()} is false.
   * @return Gets the batchSize computed in the call to {@link #updateStats()}. Returns 0 if {@link #hadDataLastTime()} is false.
   * @throws IllegalStateException if {@link #updateStats()} was never called.
   */
  long getBatchSize();

  /**
   * Gets the number of records computed in the call to {@link #updateStats()}. Returns 0 if {@link #hadDataLastTime()} is false.
   * @return Gets the number of records computed in the call to {@link #updateStats()}. Returns 0 if {@link #hadDataLastTime()} is false.
   * @throws IllegalStateException if {@link #updateStats()} was never called.
   */
  int getNumRecords();

  /**
   * True if the input batch had records in the last call to {@link #updateStats()}. False otherwise.
   * @return True if the input batch had records in the last call to {@link #updateStats()}. False otherwise.
   */
  boolean hadDataLastTime();

  /**
   * This method can be called multiple times to collect stats about the latest data in the provided record batch. These
   * stats are used to predict batch sizes. If the batch currently has no data, this method is a noop. This method must be
   * called at least once before {@link #predictBatchSize(int, boolean)}.
   */
  void updateStats();

  /**
   * Predicts the size of a batch using the current collected stats.
   * @param desiredNumRecords The number of records contained in the batch whose size we want to predict.
   * @param reserveHash Whether or not to include a column containing hash values.
   * @return The size of the predicted batch.
   * @throws IllegalStateException if {@link #hadDataLastTime()} is false or {@link #updateStats()} was not called.
   */
  long predictBatchSize(int desiredNumRecords, boolean reserveHash);

  /**
   * A factory for creating {@link BatchSizePredictor}s.
   */
  interface Factory {
    /**
     * Creates a predictor with a batch whose data needs to be used to predict other batch sizes.
     * @param batch The batch whose size needs to be predicted.
     * @param fragmentationFactor A constant used to predict value vector doubling.
     * @param safetyFactor A constant used to leave padding for unpredictable incoming batches.
     */
    BatchSizePredictor create(RecordBatch batch, double fragmentationFactor, double safetyFactor);
  }
}
