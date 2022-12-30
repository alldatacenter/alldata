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

package com.bytedance.bitsail.base.dirty;

import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

/**
 * Created 2020/4/23.
 */
public abstract class AbstractDirtyCollector implements Closeable, Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractDirtyCollector.class);

  private static final long serialVersionUID = -1415097228124142427L;
  /**
   * Max number of dirty records that can be collected.
   */
  private final int maxDirtyRecordsNum;
  /**
   * Sample ratio when collecting dirty records.
   */
  private final double dirtySampleRatio;
  /**
   * A random machine to determine whether to collect.
   */
  private final Random dirtySampleRandom;
  protected BitSailConfiguration jobConf;
  protected int taskId;
  protected long jobId;
  protected boolean isRunning;
  /**
   * Number of dirty records that has been collected.
   */
  private int dirtyCount;

  public AbstractDirtyCollector(BitSailConfiguration jobConf,
                                int taskId) {
    this.jobConf = jobConf;
    this.taskId = taskId;
    this.isRunning = true;

    this.maxDirtyRecordsNum = jobConf.get(CommonOptions.DirtyRecordOptions.DIRTY_COLLECTOR_SIZE);
    this.dirtyCount = 0;

    this.dirtySampleRatio = jobConf.get(CommonOptions.DirtyRecordOptions.DIRTY_SAMPLE_RATIO);
    this.dirtySampleRandom = new Random();
  }

  /**
   * Collect dirty records.
   *
   * @param dirtyObj       A dirty record.
   * @param e              Exception of why the record fails.
   * @param processingTime Processing timestamp for the record.
   */
  public void collectDirty(Object dirtyObj, Throwable e, long processingTime) {
    if (isRunning && shouldSample() && !Objects.isNull(dirtyObj)) {
      try {
        collect(dirtyObj, e, processingTime);
        dirtyCount++;
        triggerShutdown();
      } catch (Throwable throwable) {
        LOG.error("Collect dirty row failed.", throwable);
      }
    }
  }

  /**
   * Stop collecting dirty records once collect enough dirty records.
   */
  private void triggerShutdown() throws IOException {
    if (dirtyCount == maxDirtyRecordsNum) {
      close();
      this.isRunning = false;
    }
  }

  /**
   * Collect exceptional message and dirty record into dirty collector.
   *
   * @param obj dirty record object
   * @param e   cause by dirty message's exception
   */
  protected abstract void collect(Object obj, Throwable e, long processingTime) throws IOException;

  /**
   * store dirty records to somewhere
   */
  public void storeDirtyRecords() {

  }

  /**
   * Trigger rolling policy check is should roll or not.
   *
   * @param processingTime processing time
   */
  public void onProcessingTime(long processingTime) throws IOException {

  }

  /**
   * Clear history records
   */
  public void clear() throws IOException {

  }

  /**
   * During job running period, dirty records were reported and summarized in process result.
   * Once job finishes, dirty records are restored from process result and be logged.
   *
   * @param processResult Process result of the job.
   */
  public void restoreDirtyRecords(ProcessResult<?> processResult) {

  }

  /**
   * Check should sample record.
   */
  protected boolean shouldSample() {
    return dirtySampleRandom.nextDouble() <= dirtySampleRatio;
  }
}
