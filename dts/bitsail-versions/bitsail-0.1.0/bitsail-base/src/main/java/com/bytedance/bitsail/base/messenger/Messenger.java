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

package com.bytedance.bitsail.base.messenger;

import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.messenger.context.MessengerContext;

import java.io.Closeable;
import java.io.Serializable;

/**
 * an interface for collecting statistics in job runtime
 * including two kinds of statistics:
 *
 * @param <T> type of record
 */
public abstract class Messenger<T> implements Serializable, Closeable {

  protected MessengerContext messengerContext;

  public Messenger(MessengerContext messengerContext) {
    this.messengerContext = messengerContext;
  }

  /**
   * Open messenger for reporting statistics.
   */
  public abstract void open();

  /**
   * Report a succeeded record.
   *
   * @param message The record.
   */
  public abstract void addSuccessRecord(T message);

  /**
   * Report a failed record.
   *
   * @param message   The record.
   * @param throwable Cause for which the record failed.
   */
  public abstract void addFailedRecord(T message, Throwable throwable);

  /**
   * Update how many splits are finished when a job has multiple splits to process.
   */
  public void recordSplitProgress() {

  }

  /**
   * @return The number of succeeded records.
   */
  public abstract long getSuccessRecords();

  /**
   * @return The bytes number of succeeded records.
   */
  public abstract long getSuccessRecordBytes();

  /**
   * @return The number of failed records.
   */
  public abstract long getFailedRecords();

  /**
   * Commit collected statistics to somewhere when tasks finish.
   */
  public abstract void commit();

  /**
   * Restore statistics from process result.
   *
   * @param processResult Process result returned by a finished job.
   */
  public abstract void restoreMessengerCounter(ProcessResult<?> processResult);
}
