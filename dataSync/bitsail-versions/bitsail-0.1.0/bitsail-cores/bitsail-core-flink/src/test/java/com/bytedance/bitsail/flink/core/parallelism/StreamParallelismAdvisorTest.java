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

package com.bytedance.bitsail.flink.core.parallelism;

import com.bytedance.bitsail.base.connector.reader.DataReaderDAGBuilder;
import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.flink.core.option.FlinkCommonOptions;
import com.bytedance.bitsail.flink.core.reader.FlinkDataReaderDAGBuilder;
import com.bytedance.bitsail.flink.core.writer.FlinkDataWriterDAGBuilder;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class StreamParallelismAdvisorTest<T> {

  private final List<Integer> readerParallelisms = Arrays.asList(1, 3, 5);
  private final List<DataReaderDAGBuilder> readers = readerParallelisms.stream()
      .map(parallelism -> (DataReaderDAGBuilder) getMockedReader(parallelism))
      .collect(Collectors.toList());
  private final DataWriterDAGBuilder writer = getMockedWriter(7);

  private static FlinkDataReaderDAGBuilder getMockedReader(int parallelism) {
    FlinkDataReaderDAGBuilder reader = mock(FlinkDataReaderDAGBuilder.class);
    ParallelismAdvice advice = ParallelismAdvice.builder()
        .adviceParallelism(parallelism)
        .enforceDownStreamChain(false)
        .build();
    try {
      doReturn(advice).when(reader).getParallelismAdvice(
          any(BitSailConfiguration.class),
          any(BitSailConfiguration.class),
          any(ParallelismAdvice.class));
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, "failed to getMockedReader");
    }
    return reader;
  }

  private static FlinkDataWriterDAGBuilder getMockedWriter(int parallelism) {
    FlinkDataWriterDAGBuilder writer = mock(FlinkDataWriterDAGBuilder.class);
    ParallelismAdvice advice = ParallelismAdvice.builder()
        .adviceParallelism(parallelism)
        .enforceDownStreamChain(false)
        .build();
    try {
      doReturn(advice).when(writer).getParallelismAdvice(
          any(BitSailConfiguration.class),
          any(BitSailConfiguration.class),
          any(ParallelismAdvice.class));
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, "failed to getMockedWriter");
    }
    return writer;
  }

  @Test
  public <T> void testMultiSourceUnionMax() throws Exception {
    FlinkParallelismAdvisor advisor = new FlinkParallelismAdvisor(
        BitSailConfiguration.newDefault(),
        Collections.nCopies(3, BitSailConfiguration.newDefault()),
        Arrays.asList(BitSailConfiguration.newDefault())
    );
    advisor.advice(readers, Arrays.asList(writer));

    Assert.assertEquals(3, advisor.getReaderParallelismList().size());
    Assert.assertEquals(1, advisor.getWriterParallelismList().size());
    Assert.assertEquals(7, advisor.getAdviceWriterParallelism(writer));
    for (int i = 0; i < 3; ++i) {
      Assert.assertEquals(readerParallelisms.get(i).longValue(), advisor.getAdviceReaderParallelism(readers.get(i)));
    }
    Assert.assertEquals(5, advisor.getGlobalParallelism());
  }

  @Test
  public <T> void testMultiSourceUnionMin() throws Exception {
    BitSailConfiguration commonConf = BitSailConfiguration.newDefault();
    commonConf.set(FlinkCommonOptions.FLINK_UNION_PARALLELISM_STRATEGY, "min");

    FlinkParallelismAdvisor advisor = new FlinkParallelismAdvisor(
        commonConf,
        Collections.nCopies(3, BitSailConfiguration.newDefault()),
        Arrays.asList(BitSailConfiguration.newDefault())
    );
    advisor.advice(readers, Arrays.asList(writer));

    Assert.assertEquals(1, advisor.getGlobalParallelism());
  }
}
