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
import com.bytedance.bitsail.connector.legacy.hudi.common.HudiWriteOptions;
import com.bytedance.bitsail.connector.legacy.hudi.compact.CompactionPlanEvent;
import com.bytedance.bitsail.connector.legacy.hudi.compact.CompactionPlanSourceFunction;
import com.bytedance.bitsail.connector.legacy.hudi.configuration.FlinkOptions;
import com.bytedance.bitsail.connector.legacy.hudi.util.CompactionUtil;
import com.bytedance.bitsail.connector.legacy.hudi.util.StreamerUtil;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;
import com.bytedance.bitsail.flink.core.reader.FlinkDataReaderDAGBuilder;

import lombok.Getter;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.hudi.avro.model.HoodieCompactionPlan;
import org.apache.hudi.client.HoodieFlinkWriteClient;
import org.apache.hudi.common.table.HoodieTableMetaClient;
import org.apache.hudi.common.table.timeline.HoodieInstant;
import org.apache.hudi.common.table.timeline.HoodieTimeline;
import org.apache.hudi.common.util.CompactionUtils;
import org.apache.hudi.common.util.Option;
import org.apache.hudi.table.HoodieFlinkTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class HudiCompactSourceDAGBuilder<T extends CompactionPlanEvent> extends FlinkDataReaderDAGBuilder<T> {
  private static final Logger LOG = LoggerFactory.getLogger(HudiCompactSourceDAGBuilder.class);

  @Getter
  private Configuration conf;

  @Getter
  private BitSailConfiguration jobConf;

  /**
   * Meta Client.
   */
  private HoodieTableMetaClient metaClient;

  /**
   * Write Client.
   */
  private HoodieFlinkWriteClient<?> writeClient;

  /**
   * The hoodie table.
   */
  private HoodieFlinkTable<?> table;

  private HoodieCompactionPlan compactionPlan = null;

  private String compactionInstantTime;

  private boolean shouldRun;

  @Override
  public void configure(ExecutionEnviron execution, BitSailConfiguration readerConfiguration) throws Exception {
    jobConf = execution.getGlobalConfiguration();
    Map<String, String> properties = jobConf.getFlattenMap("job.reader.");
    conf = FlinkOptions.fromMap(properties);
    this.metaClient = StreamerUtil.createMetaClient(conf);
    conf.setString(FlinkOptions.TABLE_NAME, metaClient.getTableConfig().getTableName());
    CompactionUtil.setAvroSchema(conf, metaClient);
    CompactionUtil.inferChangelogMode(conf, metaClient);
    this.writeClient = StreamerUtil.createWriteClient(conf);
    this.table = writeClient.getHoodieTable();
    table.getMetaClient().reloadActiveTimeline();
    HoodieTimeline timeline = table.getActiveTimeline().filterPendingCompactionTimeline();
    Option<HoodieInstant> requested = timeline.firstInstant();
    this.shouldRun = requested.isPresent();
    if (shouldRun) {
      this.compactionInstantTime = requested.get().getTimestamp();

      HoodieInstant inflightInstant = HoodieTimeline.getCompactionInflightInstant(compactionInstantTime);
      if (timeline.containsInstant(inflightInstant)) {
        LOG.info("Rollback inflight compaction instant: [" + compactionInstantTime + "]");
        table.rollbackInflightCompaction(inflightInstant);
        table.getMetaClient().reloadActiveTimeline();
      }
      this.compactionPlan = CompactionUtils.getCompactionPlan(
          table.getMetaClient(), compactionInstantTime);
    }
  }

  @Override
  public boolean validate() {
    if (!shouldRun) {
      // do nothing.
      LOG.info("No compaction plan scheduled. Skip the job.");
      return false;
    }
    return true;
  }

  @Override
  public String getReaderName() {
    return "hudi_compaction_source";
  }

  @Override
  public ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConf, BitSailConfiguration selfConf, ParallelismAdvice upstreamAdvice) throws Exception {
    int parallelism;
    if (validate() && this.compactionPlan != null) {
      parallelism = calculateParallelism(this.compactionPlan.getOperations().size(),
          jobConf.get(HudiWriteOptions.COMPACTION_FILE_PER_TASK),
          jobConf.get(HudiWriteOptions.COMPACTION_MAX_PARALLELISM));
    } else {
      parallelism = 1;
    }
    return ParallelismAdvice.builder()
      .adviceParallelism(parallelism)
      .enforceDownStreamChain(false)
      .build();
  }

  public static int calculateParallelism(int totalFileSize, int filePerTask, int maxParallelism) {
    return Math.min((int) Math.ceil(totalFileSize / (float) filePerTask), maxParallelism);
  }

  @Override
  public DataStream<T> addSource(FlinkExecutionEnviron executionEnviron, int readerParallelism) throws Exception {
    // Mark instant as compaction inflight
    HoodieInstant instant = HoodieTimeline.getCompactionRequestedInstant(compactionInstantTime);
    table.getActiveTimeline().transitionCompactionRequestedToInflight(instant);
    table.getMetaClient().reloadActiveTimeline();

    return executionEnviron.getExecutionEnvironment()
      .addSource(new CompactionPlanSourceFunction(compactionPlan, compactionInstantTime))
      .setParallelism(1)
      .name("compaction_source")
      .uid("uid_compaction_source")
      .rebalance();
  }
}
