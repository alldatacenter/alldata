/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.connector.base.source;

import com.bytedance.bitsail.base.connector.reader.v1.Source;
import com.bytedance.bitsail.base.connector.reader.v1.SourceReader;
import com.bytedance.bitsail.base.connector.reader.v1.SourceSplitCoordinator;
import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.extension.ParallelismComputable;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;

public abstract class SimpleSourceBase<T>
    implements Source<T, SimpleSourceSplit, SimpleSourceState>, ParallelismComputable {

  private static final ParallelismAdvice PARALLELISM_ADVICE =
      ParallelismAdvice.builder()
          .adviceParallelism(1)
          .enforceDownStreamChain(false)
          .build();

  private transient ExecutionEnviron executionEnviron;

  private BitSailConfiguration readerConfiguration;

  @Override
  public void configure(ExecutionEnviron execution, BitSailConfiguration readerConfiguration) {
    this.readerConfiguration = readerConfiguration;
    this.executionEnviron = execution;
  }

  @Override
  public ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConfiguration,
                                                BitSailConfiguration jobConfiguration,
                                                ParallelismAdvice parallelismAdvice) throws Exception {
    return PARALLELISM_ADVICE;
  }

  @Override
  public SourceSplitCoordinator<SimpleSourceSplit, SimpleSourceState> createSplitCoordinator(
      SourceSplitCoordinator.Context<SimpleSourceSplit, SimpleSourceState> coordinatorContext) {
    return new SimpleSourceSplitCoordinator(readerConfiguration,
        coordinatorContext);
  }

  @Override
  public SimpleSourceReaderBase<T> createReader(SourceReader.Context readerContext) {
    return createSimpleReader(readerConfiguration, readerContext);
  }

  public abstract SimpleSourceReaderBase<T> createSimpleReader(BitSailConfiguration readerConfiguration,
                                                               SourceReader.Context readerContext);
}
