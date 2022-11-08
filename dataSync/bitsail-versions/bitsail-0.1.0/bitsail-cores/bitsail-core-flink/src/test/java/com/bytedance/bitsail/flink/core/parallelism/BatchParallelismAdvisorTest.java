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
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.ReaderOptions.BaseReaderOptions;
import com.bytedance.bitsail.common.option.WriterOptions.BaseWriterOptions;
import com.bytedance.bitsail.flink.core.option.FlinkCommonOptions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;

public class BatchParallelismAdvisorTest extends BaseParallelismTest {

  private BitSailConfiguration commonConf;
  private BitSailConfiguration readerConf;
  private BitSailConfiguration writerConf;
  private FlinkParallelismAdvisor advisor;

  @Before
  public void initConf() {
    commonConf = BitSailConfiguration.newDefault();
    readerConf = BitSailConfiguration.newDefault();
    writerConf = BitSailConfiguration.newDefault();
  }

  @Test
  public void testSelfParallelismAdvice() throws Exception {
    int readerParallelism = 3;
    int writerParallelism = 100;
    readerConf.set(BaseReaderOptions.READER_PARALLELISM_NUM, readerParallelism);
    writerConf.set(BaseWriterOptions.WRITER_PARALLELISM_NUM, writerParallelism);

    DataReaderDAGBuilder reader = getBatchReader(true);
    DataWriterDAGBuilder writer = getBatchWriter();

    advisor = new FlinkParallelismAdvisor(commonConf, asList(readerConf), asList(writerConf));
    advisor.advice(asList(reader), asList(writer));

    Assert.assertEquals(readerParallelism, advisor.getAdviceReaderParallelism(reader));
    Assert.assertEquals(writerParallelism, advisor.getAdviceWriterParallelism(writer));
  }

  @Test
  public void testSpeculatedReaderParallelism() throws Exception {
    writerConf.set(BaseWriterOptions.WRITER_PARALLELISM_NUM, 100);

    DataReaderDAGBuilder reader = getBatchReader(false);
    DataWriterDAGBuilder writer = getBatchWriter();

    advisor = new FlinkParallelismAdvisor(commonConf, asList(readerConf), asList(writerConf));
    advisor.advice(asList(reader), asList(writer));

    Assert.assertEquals((int) (Math.ceil(mockReaderTotalInputSize / 1e10) * 8), advisor.getAdviceReaderParallelism(reader));
    Assert.assertEquals(100, advisor.getAdviceWriterParallelism(writer));
  }

  @Test
  public void testSpeculatedWriterParallelism() throws Exception {
    readerConf.set(BaseReaderOptions.READER_PARALLELISM_NUM, 32);

    DataReaderDAGBuilder reader = getBatchReader(false);
    DataWriterDAGBuilder writer = getBatchWriter();

    advisor = new FlinkParallelismAdvisor(commonConf, asList(readerConf), asList(writerConf));
    advisor.advice(asList(reader), asList(writer));

    Assert.assertEquals(32, advisor.getAdviceReaderParallelism(reader));
    Assert.assertEquals(mockWriterMaxParallelism, advisor.getAdviceWriterParallelism(writer));
  }

  @Test
  public void testUserConfigParallelism() throws Exception {
    readerConf.set(BaseReaderOptions.READER_PARALLELISM_NUM, 32);
    writerConf.set(BaseWriterOptions.WRITER_PARALLELISM_NUM, 1024);

    DataReaderDAGBuilder reader = getBatchReader(false);
    DataWriterDAGBuilder writer = getBatchWriter();

    advisor = new FlinkParallelismAdvisor(commonConf, asList(readerConf), asList(writerConf));
    advisor.advice(asList(reader), asList(writer));

    Assert.assertEquals(32, advisor.getAdviceReaderParallelism(reader));
    Assert.assertEquals(1024, advisor.getAdviceWriterParallelism(writer));
  }

  @Test
  public void testParallelismChain() throws Exception {
    commonConf.set(FlinkCommonOptions.FLINK_PARALLELISM_CHAIN, true);
    readerConf.set(BaseReaderOptions.READER_PARALLELISM_NUM, 32);

    DataReaderDAGBuilder reader = getBatchReader(false);
    DataWriterDAGBuilder writer = getBatchWriter();

    advisor = new FlinkParallelismAdvisor(commonConf, asList(readerConf), asList(writerConf));
    advisor.advice(asList(reader), asList(writer));

    Assert.assertEquals(mockWriterMaxParallelism, advisor.getAdviceReaderParallelism(reader));
    Assert.assertEquals(mockWriterMaxParallelism, advisor.getAdviceWriterParallelism(writer));
  }
}
