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

package com.bytedance.bitsail.base.messenger.checker;

import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.messenger.common.MessengerGroup;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.io.Serializable;
import java.util.List;

/**
 * check if there are too many dirty records
 */
@Slf4j
public class DirtyRecordChecker implements Serializable {

  /**
   * Maximum number of dirty records allowed.
   * Skip this check if {@code failedCountThreshold} is negative.
   */
  private final long failedCountThreshold;
  /**
   * Maximum percentage of dirty records allowed.
   * Skip this check if {@code failedPercentageThreshold} is negative.
   */
  private final double failedPercentageThreshold;

  public DirtyRecordChecker(BitSailConfiguration commonConf) {
    this.failedCountThreshold = commonConf.get(CommonOptions.DIRTY_RECORDS_COUNT_THRESHOLD);
    this.failedPercentageThreshold = commonConf.get(CommonOptions.DIRTY_RECORDS_PERCENTAGE_THRESHOLD);
  }

  /**
   * Gather all dirty records into one string.
   */
  public static String formatDirty(List<String> sampleDirtyRecords) {
    if (CollectionUtils.isEmpty(sampleDirtyRecords)) {
      return null;
    }
    return Joiner.on("\n").join(sampleDirtyRecords);
  }

  /**
   * Check if dirty records num exceeds threshold when job finishes.
   *
   * @param processResult  Process result returned by the job.
   * @param messengerGroup Message group indicates reader or writer.
   */
  public void check(ProcessResult<?> processResult, MessengerGroup messengerGroup) {
    if (processResult == null) {
      return;
    }

    long failed;
    long succeeded;
    List<String> sampleDirtyRecords;

    if (MessengerGroup.READER.equals(messengerGroup)) {
      failed = processResult.getJobFailedInputRecordCount();
      succeeded = processResult.getJobSuccessInputRecordCount();
      sampleDirtyRecords = processResult.getInputDirtyRecords();
    } else {
      failed = processResult.getJobFailedOutputRecordCount();
      succeeded = processResult.getJobSuccessOutputRecordCount();
      sampleDirtyRecords = processResult.getOutputDirtyRecords();
    }

    final long total = failed + succeeded;
    log.info("Found {} success records.", succeeded);
    if (total == 0) {
      return;
    }

    // log all dirty records found
    String formatDirtyStr = formatDirty(sampleDirtyRecords);
    if (failed > 0) {
      log.info("Found {} dirty records, threshold {}. They are:\n{}",
          failed, failedCountThreshold, formatDirtyStr);
    }

    // throw exception when there are too many dirty records and failedCountThreshold > 0
    if (failedCountThreshold >= 0 && failed > failedCountThreshold) {
      throw BitSailException.asBitSailException(CommonErrorCode.TOO_MANY_DIRTY_RECORDS,
          "Too many dirty records found. Failed " + failed + ", threshold " + failedCountThreshold +
              ". They are:\n" + formatDirtyStr);
    }

    // throw exception when there are too many dirty records and failedPercentageThreshold > 0
    if (failedPercentageThreshold >= 0 && failed > 0 && ((double) failed / total) > failedPercentageThreshold) {
      throw BitSailException.asBitSailException(CommonErrorCode.TOO_MANY_DIRTY_RECORDS,
          "Too high dirty records percentage found. Failed "
              + failed + ", success " + succeeded + ", threshold " + failedPercentageThreshold +
              ". They are:\n" + formatDirtyStr);
    }
  }
}
