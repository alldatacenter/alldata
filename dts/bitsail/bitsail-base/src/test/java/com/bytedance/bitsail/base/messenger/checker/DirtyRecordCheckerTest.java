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
import com.bytedance.bitsail.common.option.CommonOptions;

import org.junit.Test;

public class DirtyRecordCheckerTest {

  @Test
  public void checkPass() {
    BitSailConfiguration commonConf = BitSailConfiguration.newDefault()
        .set(CommonOptions.DIRTY_RECORDS_COUNT_THRESHOLD, 100)
        .set(CommonOptions.DIRTY_RECORDS_PERCENTAGE_THRESHOLD, 0.00d);
    DirtyRecordChecker checker = new DirtyRecordChecker(commonConf);

    ProcessResult<?> result = ProcessResult.builder().jobFailedInputRecordCount(0).build();
    checker.check(result, MessengerGroup.READER);
  }

  @Test(expected = BitSailException.class)
  public void checkFailedCount() {
    BitSailConfiguration commonConf = BitSailConfiguration.newDefault()
        .set(CommonOptions.DIRTY_RECORDS_COUNT_THRESHOLD, 100)
        .set(CommonOptions.DIRTY_RECORDS_PERCENTAGE_THRESHOLD, 0.00d);
    DirtyRecordChecker checker = new DirtyRecordChecker(commonConf);

    ProcessResult<?> result = ProcessResult.builder().jobFailedInputRecordCount(101).build();
    checker.check(result, MessengerGroup.READER);
  }

  @Test(expected = BitSailException.class)
  public void checkFailedPercentage() {
    BitSailConfiguration commonConf = BitSailConfiguration.newDefault()
        .set(CommonOptions.DIRTY_RECORDS_COUNT_THRESHOLD, 100)
        .set(CommonOptions.DIRTY_RECORDS_PERCENTAGE_THRESHOLD, 0.50d);
    DirtyRecordChecker checker = new DirtyRecordChecker(commonConf);

    ProcessResult<?> result = ProcessResult.builder()
        .jobFailedInputRecordCount(50)
        .jobSuccessInputRecordBytes(1)
        .build();
    checker.check(result, MessengerGroup.READER);
  }

  @Test
  public void checkNotCheckDirtyRecords() {
    BitSailConfiguration commonConf = BitSailConfiguration.newDefault()
        .set(CommonOptions.DIRTY_RECORDS_COUNT_THRESHOLD, -1)
        .set(CommonOptions.DIRTY_RECORDS_PERCENTAGE_THRESHOLD, -1.0d);
    DirtyRecordChecker checker = new DirtyRecordChecker(commonConf);

    ProcessResult<?> result = ProcessResult.builder()
        .jobFailedInputRecordCount(50)
        .jobSuccessInputRecordBytes(1)
        .build();
    checker.check(result, MessengerGroup.READER);
  }
}