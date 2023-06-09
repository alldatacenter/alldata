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
package org.apache.drill.exec.store.hive.readers.inspectors;

import org.apache.drill.exec.store.hive.readers.HiveDefaultRecordReader;

/**
 * Parent class for records inspectors which responsible for counting of processed records
 * and managing free and used value holders.
 */
public abstract class AbstractRecordsInspector {

  private int processedRecordCount;

  /**
   * Checks if current number of processed records does not exceed max batch size.
   *
   * @return true if reached max number of records in batch
   */
  public boolean isBatchFull() {
    return processedRecordCount >= HiveDefaultRecordReader.TARGET_RECORD_COUNT;
  }

  /**
   * @return number of processed records
   */
  public int getProcessedRecordCount() {
    return processedRecordCount;
  }

  /**
   * Increments current number of processed records.
   */
  public void incrementProcessedRecordCount() {
    processedRecordCount++;
  }

  /**
   * When batch of data was sent, number of processed records should be reset.
   */
  public void reset() {
    processedRecordCount = 0;
  }

  /**
   * Returns value holder where next value will be written.
   *
   * @return value holder
   */
  public abstract Object getValueHolder();

  /**
   * @return value holder with written value
   */
  public abstract Object getNextValue();

}
