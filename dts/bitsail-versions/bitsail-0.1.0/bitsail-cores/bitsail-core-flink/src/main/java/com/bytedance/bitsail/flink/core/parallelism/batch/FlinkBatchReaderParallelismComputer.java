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

package com.bytedance.bitsail.flink.core.parallelism.batch;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.flink.core.option.FlinkCommonOptions;

import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:MagicNumber")
public class FlinkBatchReaderParallelismComputer {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkBatchReaderParallelismComputer.class);

  private final int userConfigMinParallelism;
  private final int userConfigMaxParallelism;

  private final Float configParallelismRatio;

  public FlinkBatchReaderParallelismComputer(BitSailConfiguration commonConfiguration) {
    userConfigMinParallelism = commonConfiguration.get(FlinkCommonOptions.FLINK_MIN_PARALLELISM);
    userConfigMaxParallelism = commonConfiguration.get(FlinkCommonOptions.FLINK_MAX_PARALLELISM);
    configParallelismRatio = commonConfiguration.get(FlinkCommonOptions.FLINK_PARALLELISM_RATIO);
  }

  public int adviceReaderParallelism(BaseStatistics statistics,
                                     int adviceReaderMinParallelism,
                                     int adviceReaderMaxParallelism) throws Exception {
    Preconditions.checkNotNull(statistics, "Input statistics should not be null.");

    long numberOfRecords = statistics.getNumberOfRecords();
    long totalInputSize = statistics.getTotalInputSize();

    int adviceParallelismAccordingByRecords = Integer.MAX_VALUE;
    int adviceParallelismAccordingBySize = Integer.MAX_VALUE;

    if (numberOfRecords != BaseStatistics.NUM_RECORDS_UNKNOWN) {
      adviceParallelismAccordingByRecords = advisePerRecordsNumber(numberOfRecords);
      LOG.info("Input number records: {}, advise parallelism: {}.",
          numberOfRecords, adviceParallelismAccordingByRecords);
    }

    if (totalInputSize != BaseStatistics.SIZE_UNKNOWN) {
      adviceParallelismAccordingBySize = advisePerFileSize(totalInputSize);
      LOG.info("Input size: {}, advise parallelism: {}.",
          totalInputSize, adviceParallelismAccordingBySize);

      if (adviceReaderMaxParallelism != Integer.MAX_VALUE) {
        adviceReaderMinParallelism = balanceParallelism((int) (Math.ceil(adviceReaderMaxParallelism / 400.0) * 8));
      }
    }

    if (numberOfRecords == BaseStatistics.NUM_RECORDS_UNKNOWN
        && totalInputSize == BaseStatistics.SIZE_UNKNOWN) {
      adviceParallelismAccordingByRecords = 1;
      adviceParallelismAccordingBySize = 1;
    }

    final int adviceReaderParallelism = Math
        .max(adviceReaderMinParallelism, (int) (Math
                .min(adviceParallelismAccordingByRecords, adviceParallelismAccordingBySize) * configParallelismRatio));

    LOG.info("Reader advice parallelism according by records is: {}.", adviceParallelismAccordingByRecords);
    LOG.info("Reader advice parallelism according by size is: {}.", adviceParallelismAccordingBySize);
    LOG.info("Reader advice min parallelism: {}.", adviceReaderMinParallelism);
    LOG.info("Reader advice max parallelism: {}.", adviceReaderMaxParallelism);
    LOG.info("Reader advice parallelism: {}.", adviceReaderParallelism);

    return balanceParallelism(Math.min(adviceReaderMaxParallelism, adviceReaderParallelism));
  }

  private int advisePerRecordsNumber(long numberOfRecords) {
    Preconditions.checkArgument(numberOfRecords >= 0);

    return balanceParallelism((int) (Math.ceil(numberOfRecords / 5e7) * 8));
  }

  private int advisePerFileSize(long inputSize) {
    Preconditions.checkArgument(inputSize >= 0);

    return balanceParallelism((int) (Math.ceil(inputSize / 1e10) * 8));
  }

  private int balanceParallelism(int parallelism) {
    return Math.min(userConfigMaxParallelism, Math.max(userConfigMinParallelism, parallelism));
  }
}
