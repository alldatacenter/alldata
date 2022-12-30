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

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.Serializable;
import java.util.Arrays;

/**
 * test job  in low volume data mode
 */
@Slf4j
public class LowVolumeTestChecker implements Serializable {

  private final long threshold;

  public LowVolumeTestChecker(BitSailConfiguration configuration) {
    this.threshold = configuration.get(CommonOptions.LOW_VOLUME_TEST_COUNT_THRESHOLD);
    log.info("Low Volume test is {}, threshold: {}", (threshold > 0 ? "enabled" : "disabled"), threshold);
  }

  /**
   * check if records handled is enough
   */
  public boolean check(long successRecords, long failRecords) {
    if (threshold <= 0) {
      return false;
    }

    val totalCount = successRecords + failRecords;

    val result = totalCount >= threshold;

    if (result) {
      log.info("Reached threshold of low volume test, total count {}, threshold {}", totalCount, threshold);
    }
    return result;
  }

  /**
   * return at most 100 InputSplits in low volume data mode
   */
  public <T> T[] restrictSplitsNumber(T[] splits) {
    if (threshold > 0) {
      final int restrictedLength = Math.min(splits.length, 100);
      log.info("Restricting input splits length to {}", restrictedLength);
      return Arrays.copyOf(splits, restrictedLength);
    }

    return splits;
  }
}
