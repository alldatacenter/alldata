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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.directory;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * choose the specific time strategy to generate default time
 */
@Slf4j
public class DefaultTimeManager {

  public static DefaultTimeStrategy getDefaultTimeStrategy(BitSailConfiguration jobConf) {
    if (!jobConf.fieldExists(FileSystemCommonOptions.CommitOptions.DEFAULT_TIME_STRATEGY)) {
      return isBinlogType(jobConf) ? DefaultTimeStrategy.ON_COMMIT_TIME : DefaultTimeStrategy.ON_SYSTEM_TIME;
    }

    String defaultTimeStrategy = jobConf.get(FileSystemCommonOptions.CommitOptions.DEFAULT_TIME_STRATEGY);
    if (StreamingFileSystemValidator.DEFAULT_TIME_STRATEGY_ON_COMMIT_TIME.equalsIgnoreCase(defaultTimeStrategy)) {
      return DefaultTimeStrategy.ON_COMMIT_TIME;
    }
    return DefaultTimeStrategy.ON_SYSTEM_TIME;
  }

  private static boolean isBinlogType(BitSailConfiguration jobConf) {
    String dumpType = jobConf.get(FileSystemSinkOptions.HDFS_DUMP_TYPE);
    if (StringUtils.isEmpty(dumpType)) {
      log.info("can not find dump type for partition computer, the default binlog type is false");
      return false;
    }

    Set<String> binlogType = ImmutableSet.of(StreamingFileSystemValidator.HDFS_DUMP_TYPE_BINLOG);
    return binlogType.contains(dumpType.toLowerCase());
  }

  public enum DefaultTimeStrategy {
    /**
     * put defaultTime on system time
     */
    ON_SYSTEM_TIME {
      @Override
      public long getDefaultTime(long latestJobCommitTimestamp) {
        return System.currentTimeMillis();
      }
    },
    /**
     * put defaultTime on watermark time
     */
    ON_COMMIT_TIME {
      @Override
      public long getDefaultTime(long latestJobCommitTimestamp) {
        return latestJobCommitTimestamp > 0 ? latestJobCommitTimestamp : System.currentTimeMillis();
      }
    },
    /**
     * put defaultTime on specific time
     */
    ON_SPECIFIC_TIME {
      @Override
      public long getDefaultTime(long latestJobCommitTimestamp) {
        throw new RuntimeException("ON_SPECIFIC_TIME: not supported yet");
      }
    };

    public long getDefaultTime(long latestJobCommitTimestamp) {
      return latestJobCommitTimestamp;
    }
  }
}
