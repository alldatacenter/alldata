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

package com.bytedance.bitsail.flink.core.reader;

import com.bytedance.bitsail.base.connector.reader.DataReaderDAGBuilder;
import com.bytedance.bitsail.base.extension.ParallelismComputable;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;

import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.streaming.api.datastream.DataStream;

/**
 * Created 2022/4/21
 */
public abstract class FlinkDataReaderDAGBuilder<T> implements DataReaderDAGBuilder, ParallelismComputable {

  public abstract DataStream<T> addSource(FlinkExecutionEnviron executionEnviron,
                                          int readerParallelism) throws Exception;

  public BaseStatistics getStatistics(BaseStatistics baseStatistics) throws Exception {
    return new BaseStatistics() {
      @Override
      public long getTotalInputSize() {
        return BaseStatistics.SIZE_UNKNOWN;
      }

      @Override
      public long getNumberOfRecords() {
        return BaseStatistics.NUM_RECORDS_UNKNOWN;
      }

      @Override
      public float getAverageRecordWidth() {
        return BaseStatistics.AVG_RECORD_BYTES_UNKNOWN;
      }
    };
  }
}
