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

package com.bytedance.bitsail.connector.legacy.hudi.dag;

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.connector.legacy.hudi.compact.CompactFunction;
import com.bytedance.bitsail.connector.legacy.hudi.compact.CompactionCommitEvent;
import com.bytedance.bitsail.connector.legacy.hudi.compact.CompactionCommitSink;
import com.bytedance.bitsail.connector.legacy.hudi.compact.CompactionPlanEvent;
import com.bytedance.bitsail.connector.legacy.hudi.configuration.FlinkOptions;
import com.bytedance.bitsail.connector.legacy.hudi.util.CompactionUtil;
import com.bytedance.bitsail.connector.legacy.hudi.util.StreamerUtil;
import com.bytedance.bitsail.flink.core.writer.FlinkDataWriterDAGBuilder;

import lombok.Getter;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.operators.ProcessOperator;
import org.apache.hudi.common.table.HoodieTableMetaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class HudiCompactSinkDAGBuilder<OUT extends CompactionPlanEvent> extends FlinkDataWriterDAGBuilder<OUT> {
  private static final Logger LOG = LoggerFactory.getLogger(HudiSinkFunctionDAGBuilder.class);

  @Getter
  private Configuration conf;

  @Getter
  private BitSailConfiguration jobConf;

  @Override
  public void configure(ExecutionEnviron execution, BitSailConfiguration writerConfiguration) throws Exception {
    jobConf = execution.getGlobalConfiguration();
    Map<String, String> properties = jobConf.getFlattenMap("job.writer.");
    conf = FlinkOptions.fromMap(properties);
    HoodieTableMetaClient metaClient = StreamerUtil.createMetaClient(conf);
    conf.setString(FlinkOptions.TABLE_NAME, metaClient.getTableConfig().getTableName());
    CompactionUtil.setAvroSchema(conf, metaClient);
    CompactionUtil.inferChangelogMode(conf, metaClient);
  }

  @Override
  public ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConf,
                                                BitSailConfiguration writerConf,
                                                ParallelismAdvice upstreamAdvice) {
    int adviceWriterParallelism = upstreamAdvice.getAdviceParallelism();
    if (writerConf.fieldExists(WriterOptions.BaseWriterOptions.WRITER_PARALLELISM_NUM) && !upstreamAdvice.isEnforceDownStreamChain()) {
      adviceWriterParallelism = writerConf.get(WriterOptions.BaseWriterOptions.WRITER_PARALLELISM_NUM);
    }
    return ParallelismAdvice.builder()
        .adviceParallelism(adviceWriterParallelism)
        .enforceDownStreamChain(false)
        .build();
  }

  @Override
  public void addWriter(DataStream<OUT> dataStream, int parallelism) {
    dataStream
        .transform("compact_task",
        TypeInformation.of(CompactionCommitEvent.class),
          new ProcessOperator<OUT, CompactionCommitEvent>(new CompactFunction(conf)))
        .name("compaction_process")
        .uid("uid_compaction_process")
        .setParallelism(parallelism)
        .addSink(new CompactionCommitSink(conf))
        .name("compaction_commits")
        .uid("uid_compaction_commits")
        .setParallelism(1);
  }

  @Override
  public String getWriterName() {
    return "hudi_compaction_sink";
  }
}
