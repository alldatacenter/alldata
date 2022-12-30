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

package com.bytedance.bitsail.base.runtime;

import com.bytedance.bitsail.base.connector.reader.DataReaderDAGBuilder;
import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.runtime.metrics.BitSailClientMetricsPlugin;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.Pair;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BitSailClientMetricsPluginTest {

  private final ProcessResult<?> processResult = ProcessResult.builder()
      .jobSuccessInputRecordCount(10)
      .jobSuccessInputRecordBytes(100)
      .jobFailedInputRecordCount(11)
      .jobSuccessOutputRecordCount(30)
      .jobSuccessOutputRecordBytes(300)
      .jobFailedOutputRecordCount(33)
      .taskRetryCount(5)
      .build();

  @Test
  public void testWrappedName() {
    List<String> names = Lists.newArrayList("1", "2", "3");
    String wrappedName = BitSailClientMetricsPlugin.getWrappedName(names, name -> "n" + name);
    Assert.assertEquals("[n1,n2,n3]", wrappedName);
  }

  @Test
  public void testMetricsPlugin() {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    jobConf.set(CommonOptions.JOB_ID, -1L);
    jobConf.set(CommonOptions.METRICS_REPORTER_TYPE, "nop");

    BitSailClientMetricsPlugin metricsPlugin = new BitSailClientMetricsPlugin();
    metricsPlugin.configure(jobConf, getReaders(), getWriters());
    metricsPlugin.start();
    metricsPlugin.onSuccessComplete(processResult);

    List<Pair<String, String>> metricTags = metricsPlugin.getAllMetricTags();
    List<Pair<String, String>> expectedTags = Arrays.asList(
        Pair.newPair("source", "[reader_0,reader_1,reader_2]"),
        Pair.newPair("target", "[writer_0,writer_1,writer_2]"),
        Pair.newPair("job", "-1")
    );

    AtomicInteger foundTagCount = new AtomicInteger(0);
    expectedTags.forEach(expectedTag -> {
      for (Pair<String, String> metricTag : metricTags) {
        if (metricTag.equals(expectedTag)) {
          foundTagCount.incrementAndGet();
          break;
        }
      }
    });
    Assert.assertEquals(expectedTags.size(), foundTagCount.get());
  }

  private List<DataReaderDAGBuilder> getReaders() {
    return IntStream.range(0, 3).boxed()
        .map(i -> {
          DataReaderDAGBuilder reader = Mockito.mock(DataReaderDAGBuilder.class);
          PowerMockito.when(reader.getReaderName()).thenReturn("reader_" + i);
          return reader;
        })
        .collect(Collectors.toList());
  }

  private List<DataWriterDAGBuilder> getWriters() {
    return IntStream.range(0, 3).boxed()
        .map(i -> {
          DataWriterDAGBuilder reader = Mockito.mock(DataWriterDAGBuilder.class);
          PowerMockito.when(reader.getWriterName()).thenReturn("writer_" + i);
          return reader;
        })
        .collect(Collectors.toList());
  }
}
