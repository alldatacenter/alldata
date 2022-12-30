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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.partitionfirst.PartitionFirstCommitFunction;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.partitionfirst.PartitionFirstStateProcessFunction;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.partitionlast.PartitionLastCommitFunction;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.partitionlast.PartitionLastStateProcessFunction;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PartitionStrategyFactory {

  public static AbstractPartitionStateProcessFunction getPartitionStateProcessFunction(BitSailConfiguration jobConf) {
    PartitionStrategyEnum partitionStrategyEnum = getPartitionStrategyEnum(jobConf);
    log.info("Partition strategy is {}.", partitionStrategyEnum);
    switch (partitionStrategyEnum) {
      case PARTITION_LAST:
        return new PartitionLastStateProcessFunction();
      case PARTITION_FIRST:
        return new PartitionFirstStateProcessFunction();
      default:
        throw new RuntimeException("Unknown partition strategy " + partitionStrategyEnum);
    }
  }

  public static AbstractPartitionCommitFunction getPartitionStrategyCommitFunction(BitSailConfiguration jobConf, int numberOfTasks) {
    PartitionStrategyEnum partitionStrategyEnum = getPartitionStrategyEnum(jobConf);
    switch (partitionStrategyEnum) {
      case PARTITION_FIRST:
        return new PartitionFirstCommitFunction(jobConf, numberOfTasks);
      case PARTITION_LAST:
        return new PartitionLastCommitFunction(jobConf, numberOfTasks);
      default:
        throw new RuntimeException("Unknown partition strategy " + partitionStrategyEnum);
    }
  }

  private static PartitionStrategyEnum getPartitionStrategyEnum(BitSailConfiguration jobConf) {
    String partitionStrategyString =
        jobConf.getUnNecessaryOption(FileSystemCommonOptions.CommitOptions.COMMIT_PARTITION_STRATEGY,
            PartitionStrategyEnum.PARTITION_LAST.toString());
    return PartitionStrategyEnum.valueOf(partitionStrategyString.toUpperCase());
  }

  private enum PartitionStrategyEnum {
    PARTITION_FIRST,
    PARTITION_LAST
  }
}

