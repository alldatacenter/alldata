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
import com.bytedance.bitsail.base.extension.ParallelismComputable;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.flink.core.option.FlinkCommonOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FlinkParallelismAdvisor {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkParallelismAdvisor.class);

  private final BitSailConfiguration commonConfiguration;
  private final List<BitSailConfiguration> readerConfigurations;
  private final List<BitSailConfiguration> writerConfigurations;

  private final UnionParallelismStrategy unionParallelismStrategy;
  private final Map<DataReaderDAGBuilder, ParallelismAdvice> readerParallelismMap;
  private final Map<DataWriterDAGBuilder, ParallelismAdvice> writerParallelismMap;

  private final int userConfigGlobalParallelism;

  private ParallelismAdvice unionParallelismAdvice;

  public FlinkParallelismAdvisor(BitSailConfiguration commonConf,
                                 List<BitSailConfiguration> readerConfs,
                                 List<BitSailConfiguration> writerConfs) {
    this.commonConfiguration = commonConf;
    this.readerConfigurations = readerConfs;
    this.writerConfigurations = writerConfs;

    this.unionParallelismStrategy = UnionParallelismStrategy.valueOf(commonConf.get(FlinkCommonOptions.FLINK_UNION_PARALLELISM_STRATEGY).toUpperCase());
    this.readerParallelismMap = new HashMap<>();
    this.writerParallelismMap = new HashMap<>();

    this.userConfigGlobalParallelism = commonConf.get(CommonOptions.GLOBAL_PARALLELISM_NUM);
  }

  /**
   * there are 5 steps to compute the parallelism advice for each reader and writer
   * 1. get parallelism advice from each reader
   * 2. compute parallelism advice for the union operator of readers
   * 3. get parallelism advice from each writer (with union parallelism advice as upstream advice)
   * 4. try to chain the reader and writer when job.common.parallelism_CHIAN=true
   * 5. overwrite the parallelism options in configurations
   */
  public <T> void advice(List<DataReaderDAGBuilder> readerDAGBuilders,
                         List<DataWriterDAGBuilder> writerDAGBuilders) throws Exception {
    Preconditions.checkState(readerDAGBuilders.size() == readerConfigurations.size());
    Preconditions.checkState(writerDAGBuilders.size() == writerConfigurations.size());

    int readersNum = readerDAGBuilders.size();
    int writersNum = writerDAGBuilders.size();
    ParallelismAdvice firstAdvice = ParallelismAdvice.builder().adviceParallelism(userConfigGlobalParallelism).build();

    List<ParallelismAdvice> readerParallelismAdvices = new ArrayList<>();
    for (int i = 0; i < readersNum; ++i) {
      Preconditions.checkState(readerDAGBuilders.get(i) instanceof ParallelismComputable);
      ParallelismAdvice advice = ((ParallelismComputable) readerDAGBuilders.get(i))
          .getParallelismAdvice(commonConfiguration, readerConfigurations.get(i), firstAdvice);
      readerParallelismMap.put(readerDAGBuilders.get(i), advice);
      readerParallelismAdvices.add(advice);
    }

    unionParallelismAdvice = computeUnionParallelismAdvice(readerParallelismAdvices);

    List<ParallelismAdvice> writerParallelismAdvices = new ArrayList<>();
    for (int i = 0; i < writersNum; ++i) {
      Preconditions.checkState(writerDAGBuilders.get(i) instanceof ParallelismComputable);
      ParallelismAdvice advice = ((ParallelismComputable) writerDAGBuilders.get(i))
          .getParallelismAdvice(commonConfiguration, writerConfigurations.get(i), unionParallelismAdvice);
      writerParallelismMap.put(writerDAGBuilders.get(i), advice);
      writerParallelismAdvices.add(advice);
    }

    tryParallelismChain(readerParallelismAdvices, writerParallelismAdvices);

    for (int i = 0; i < readerConfigurations.size(); ++i) {
      readerConfigurations.get(i).set(ReaderOptions.BaseReaderOptions.READER_PARALLELISM_NUM,
          readerParallelismAdvices.get(i).getAdviceParallelism());
    }
    for (int i = 0; i < writerConfigurations.size(); ++i) {
      writerConfigurations.get(i).set(WriterOptions.BaseWriterOptions.WRITER_PARALLELISM_NUM,
          writerParallelismAdvices.get(i).getAdviceParallelism());
    }
  }

  /**
   * compute a parallelism advice for the union operator
   * 1. when there is only one reader, the union parallelism will be the reader parallelism
   * 2. when there is multiple reader, the union parallelism is computed based on {@link UnionParallelismStrategy}
   */
  public ParallelismAdvice computeUnionParallelismAdvice(List<ParallelismAdvice> readerParallelismAdvices) {
    if (readerParallelismAdvices.size() == 1) {
      return readerParallelismAdvices.get(0);
    } else {
      return ParallelismAdvice.builder()
          .adviceParallelism(unionParallelismStrategy.computeUnionParallelism(readerParallelismAdvices))
          .enforceDownStreamChain(false)
          .build();
    }
  }

  /**
   * when parallelism_CHIAN=true, make reader and writer use the same parallelism
   * need to satisfy the following conditions:
   * 1. only one reader and one writer
   * 2. user does not set both job.reader.reader_parallelism_num and job.writer.writer_parallelism_num
   */
  public void tryParallelismChain(List<ParallelismAdvice> readerAdvices, List<ParallelismAdvice> writerAdvices) {
    boolean enableParallelismChain = commonConfiguration.get(FlinkCommonOptions.FLINK_PARALLELISM_CHAIN);
    if (!enableParallelismChain) {
      return;
    }

    boolean singleChain = readerParallelismMap.size() == 1 && writerParallelismMap.size() == 1;
    boolean userConfigured = readerConfigurations.get(0).fieldExists(ReaderOptions.BaseReaderOptions.READER_PARALLELISM_NUM)
        && writerConfigurations.get(0).fieldExists(WriterOptions.BaseWriterOptions.WRITER_PARALLELISM_NUM);
    boolean enforceWriterParallelism = unionParallelismAdvice.isEnforceDownStreamChain();

    if (!singleChain) {
      LOG.warn("parallelism_CHIAN does not work when reader or writer num > 1");
      return;
    }
    if (userConfigured) {
      LOG.warn("parallelism_CHIAN does not work when user has configured reader and writer parallelism");
      return;
    }
    if (enforceWriterParallelism) {
      LOG.warn("parallelism_CHIAN can not work when writer parallelism is enforced by reader");
      return;
    }
    LOG.info("Parallelism chain enabled.");

    int userConfigMinParallelism = commonConfiguration.get(FlinkCommonOptions.FLINK_MIN_PARALLELISM);
    int userConfigMaxParallelism = commonConfiguration.get(FlinkCommonOptions.FLINK_MAX_PARALLELISM);

    DataReaderDAGBuilder readerDAGBuilder = readerParallelismMap.keySet().iterator().next();
    DataWriterDAGBuilder writerDAGBuilder = writerParallelismMap.keySet().iterator().next();
    int adviceReaderParallelism = readerParallelismMap.get(readerDAGBuilder).getAdviceParallelism();
    int adviceWriterParallelism = writerParallelismMap.get(writerDAGBuilder).getAdviceParallelism();

    int chainParallelism = Math.min(adviceReaderParallelism, adviceWriterParallelism);
    chainParallelism = Math.min(userConfigMaxParallelism, Math.max(userConfigMinParallelism, chainParallelism));

    ParallelismAdvice newReaderParallelismAdvice = readerParallelismMap.get(readerDAGBuilder);
    ParallelismAdvice newWriterParallelismAdvice = writerParallelismMap.get(writerDAGBuilder);

    newReaderParallelismAdvice.setAdviceParallelism(chainParallelism);
    newWriterParallelismAdvice.setAdviceParallelism(chainParallelism);

    readerParallelismMap.put(readerDAGBuilder, newReaderParallelismAdvice);
    writerParallelismMap.put(writerDAGBuilder, newWriterParallelismAdvice);

    readerAdvices.set(0, newReaderParallelismAdvice);
    writerAdvices.set(0, newWriterParallelismAdvice);
  }

  public void display() {
    LOG.info("reader parallelisms:");
    readerParallelismMap.forEach((reader, advice) -> {
      String readerName = reader.getReaderName();
      int parallelism = advice.getAdviceParallelism();
      LOG.info("reader {} has parallelism: {}", readerName, parallelism);
    });

    LOG.info("writer parallelisms:");
    writerParallelismMap.forEach((writer, advice) -> {
      String writerName = writer.getWriterName();
      int parallelism = advice.getAdviceParallelism();
      LOG.info("writer {} has parallelism: {}", writerName, parallelism);
    });
  }

  public List<Integer> getReaderParallelismList() {
    return readerParallelismMap.values().stream().map(advice -> advice.getAdviceParallelism()).collect(Collectors.toList());
  }

  public List<Integer> getWriterParallelismList() {
    return writerParallelismMap.values().stream().map(advice -> advice.getAdviceParallelism()).collect(Collectors.toList());
  }

  public <T> int getAdviceReaderParallelism(DataReaderDAGBuilder readerDAGBuilder) {
    return readerParallelismMap.get(readerDAGBuilder).getAdviceParallelism();
  }

  public <T> int getAdviceWriterParallelism(DataWriterDAGBuilder writerDAGBuilder) {
    return writerParallelismMap.get(writerDAGBuilder).getAdviceParallelism();
  }

  public int getGlobalParallelism() {
    return unionParallelismAdvice.getAdviceParallelism();
  }
}
