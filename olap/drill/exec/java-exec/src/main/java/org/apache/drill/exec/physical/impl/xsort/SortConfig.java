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
package org.apache.drill.exec.physical.impl.xsort;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortConfig {

  private static final Logger logger = LoggerFactory.getLogger(SortConfig.class);

  /**
   * Smallest allowed output batch size. The smallest output batch
   * created even under constrained memory conditions.
   */
  public static final int MIN_MERGED_BATCH_SIZE = 256 * 1024;

  /**
   * In the bizarre case where the user gave us an unrealistically low
   * spill file size, set a floor at some bare minimum size. (Note that,
   * at this size, big queries will create a huge number of files, which
   * is why the configuration default is one the order of hundreds of MB.)
   */

  public static final long MIN_SPILL_FILE_SIZE = 1 * 1024 * 1024;

  public static final int DEFAULT_SPILL_BATCH_SIZE = 8 * 1024 * 1024;
  public static final int MIN_SPILL_BATCH_SIZE = 256 * 1024;
  public static final int MIN_MERGE_BATCH_SIZE = 256 * 1024;

  public static final int DEFAULT_MERGE_LIMIT = 128;
  public static final int MIN_MERGE_LIMIT = 2;

  private final long maxMemory;

  /**
   * Maximum number of spilled runs that can be merged in a single pass.
   */

  private final int mergeLimit;

  /**
   * Target size of the first-generation spill files.
   */
  private final long spillFileSize;

  private final int spillBatchSize;

  private final int mergeBatchSize;

  private final int bufferedBatchLimit;

  /**
   * Limit the size of the in-memory merge return batches.
   * Primarily for testing.
   */

  private final int mSortBatchSize;

  public SortConfig(DrillConfig config, OptionManager options) {
    // Optional configured memory limit, typically used only for testing.

    maxMemory = config.getBytes(ExecConstants.EXTERNAL_SORT_MAX_MEMORY);

    // Optional limit on the number of spilled runs to merge in a single
    // pass. Limits the number of open file handles. Must allow at least
    // two batches to merge to make progress.

    int limit = config.getInt(ExecConstants.EXTERNAL_SORT_MERGE_LIMIT);
    if (limit > 0) {
      mergeLimit = Math.max(limit, MIN_MERGE_LIMIT);
    } else {
      mergeLimit = Integer.MAX_VALUE;
    }

    // Limits the size of first-generation spill files.
    // Ensure the size is reasonable.

    spillFileSize = Math.max(config.getBytes(ExecConstants.EXTERNAL_SORT_SPILL_FILE_SIZE), MIN_SPILL_FILE_SIZE);
    spillBatchSize = (int) Math.max(config.getBytes(ExecConstants.EXTERNAL_SORT_SPILL_BATCH_SIZE), MIN_SPILL_BATCH_SIZE);

    // Set the target output batch size. Use the maximum size, but only if
    // this represents less than 10% of available memory. Otherwise, use 10%
    // of memory, but no smaller than the minimum size. In any event, an
    // output batch can contain no fewer than a single record.

    // get the output batch size from context.
    // Size of the batch sent downstream from the sort operator during
    // the merge phase. Default value is 16M.
    // Don't change defaults unless you know what you are doing,
    // larger sizes can result in memory fragmentation, smaller sizes
    // in excessive operator iterator overhead.
    mergeBatchSize = (int) Math.max(options.getOption(ExecConstants.OUTPUT_BATCH_SIZE_VALIDATOR), MIN_MERGE_BATCH_SIZE);

    // Limit on in-memory batches, primarily for testing.

    int value = config.getInt(ExecConstants.EXTERNAL_SORT_BATCH_LIMIT);
    if (value == 0) {
      bufferedBatchLimit = Integer.MAX_VALUE;
    } else {
      bufferedBatchLimit = Math.max(value, 2);
    }

    // Limit on memory merge batch size; primarily for testing

    if (config.hasPath(ExecConstants.EXTERNAL_SORT_MSORT_MAX_BATCHSIZE)) {
      mSortBatchSize = Math.max(1,
            Math.min(Character.MAX_VALUE,
                     config.getInt(ExecConstants.EXTERNAL_SORT_MSORT_MAX_BATCHSIZE)));
    } else {
      mSortBatchSize = Character.MAX_VALUE;
    }

    logConfig();
  }

  private void logConfig() {
    logger.debug("Config: " +
                 "spill file size = {}, spill batch size = {}, " +
                 "merge batch size = {}, mSort batch size = {}",
                  spillFileSize, spillBatchSize,
                  mergeBatchSize, mSortBatchSize);
  }

  @Override
  public String toString() {
    return "SortConfig[spillFileSize=" + spillFileSize
        + ", spillBatchSize=" + spillBatchSize
        + ", mergeBatchSize=" + mergeBatchSize
        + ", mSortBatchSize=" + mSortBatchSize
        + "]";
  }

  public long maxMemory() { return maxMemory; }
  public int mergeLimit() { return mergeLimit; }
  public long spillFileSize() { return spillFileSize; }
  public int spillBatchSize() { return spillBatchSize; }
  public int mergeBatchSize() { return mergeBatchSize; }
  public int getBufferedBatchLimit() { return bufferedBatchLimit; }
  public int getMSortBatchSize() { return mSortBatchSize; }
}
