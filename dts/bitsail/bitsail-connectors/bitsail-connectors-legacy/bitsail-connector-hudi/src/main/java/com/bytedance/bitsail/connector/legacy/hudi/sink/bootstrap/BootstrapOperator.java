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

package com.bytedance.bitsail.connector.legacy.hudi.sink.bootstrap;

import com.bytedance.bitsail.connector.legacy.hudi.configuration.FlinkOptions;
import com.bytedance.bitsail.connector.legacy.hudi.configuration.HadoopConfigurations;
import com.bytedance.bitsail.connector.legacy.hudi.sink.bootstrap.aggregate.BootstrapAggFunction;
import com.bytedance.bitsail.connector.legacy.hudi.sink.meta.CkpMetadata;
import com.bytedance.bitsail.connector.legacy.hudi.util.FlinkTables;
import com.bytedance.bitsail.connector.legacy.hudi.util.StreamerUtil;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.KeyGroupRangeAssignment;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.StateSnapshotContext;
import org.apache.flink.runtime.taskexecutor.GlobalAggregateManager;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.hudi.common.fs.FSUtils;
import org.apache.hudi.common.model.FileSlice;
import org.apache.hudi.common.model.HoodieAvroRecord;
import org.apache.hudi.common.model.HoodieKey;
import org.apache.hudi.common.model.HoodieRecord;
import org.apache.hudi.common.model.HoodieRecordGlobalLocation;
import org.apache.hudi.config.HoodieWriteConfig;
import org.apache.hudi.table.HoodieTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * The operator to load index from existing hoodieTable.
 *
 * <p>Each subtask of the function triggers the index bootstrap when the first element came in,
 * the record cannot be sent until all the index records have been sent.
 *
 * <p>The output records should then shuffle by the recordKey and thus do scalable write.
 */
public class BootstrapOperator<I, O extends HoodieRecord<?>>
    extends AbstractStreamOperator<O> implements OneInputStreamOperator<I, O> {

  private static final Logger LOG = LoggerFactory.getLogger(BootstrapOperator.class);
  protected final Configuration conf;
  private final Pattern pattern;
  protected HoodieTable<?, ?, ?, ?> hoodieTable;
  protected transient org.apache.hadoop.conf.Configuration hadoopConf;
  protected transient HoodieWriteConfig writeConfig;
  private CkpMetadata ckpMetadata;
  private transient GlobalAggregateManager aggregateManager;
  private transient ListState<String> instantState;
  private String lastInstantTime;

  public BootstrapOperator(Configuration conf) {
    this.conf = conf;
    this.pattern = Pattern.compile(conf.getString(FlinkOptions.INDEX_PARTITION_REGEX));
  }

  @SuppressWarnings("unchecked")
  public static HoodieRecord generateHoodieRecord(HoodieKey hoodieKey, FileSlice fileSlice) {
    HoodieRecord hoodieRecord = new HoodieAvroRecord(hoodieKey, null);
    hoodieRecord.setCurrentLocation(new HoodieRecordGlobalLocation(hoodieKey.getPartitionPath(), fileSlice.getBaseInstantTime(), fileSlice.getFileId()));
    hoodieRecord.seal();

    return hoodieRecord;
  }

  @Override
  public void snapshotState(StateSnapshotContext context) throws Exception {
    lastInstantTime = this.ckpMetadata.lastPendingInstant();
    instantState.update(Collections.singletonList(lastInstantTime));
  }

  @Override
  public void initializeState(StateInitializationContext context) throws Exception {
    ListStateDescriptor<String> instantStateDescriptor = new ListStateDescriptor<>(
        "instantStateDescriptor",
        Types.STRING
    );
    instantState = context.getOperatorStateStore().getListState(instantStateDescriptor);

    if (context.isRestored()) {
      Iterator<String> instantIterator = instantState.get().iterator();
      if (instantIterator.hasNext()) {
        lastInstantTime = instantIterator.next();
      }
    }

    this.hadoopConf = HadoopConfigurations.getHadoopConf(this.conf);
    this.writeConfig = StreamerUtil.getHoodieClientConfig(this.conf, true);
    this.hoodieTable = FlinkTables.createTable(writeConfig, hadoopConf, getRuntimeContext());
    this.ckpMetadata = CkpMetadata.getInstance(hoodieTable.getMetaClient().getFs(), this.writeConfig.getBasePath());
    this.aggregateManager = getRuntimeContext().getGlobalAggregateManager();

    preLoadIndexRecords();
  }

  /**
   * Load the index records before {@link #processElement}.
   */
  protected void preLoadIndexRecords() throws Exception {
    String basePath = hoodieTable.getMetaClient().getBasePath();
    int taskID = getRuntimeContext().getIndexOfThisSubtask();
    LOG.info("Start loading records in table {} into the index state, taskId = {}", basePath, taskID);
    for (String partitionPath : FSUtils.getAllFoldersWithPartitionMetaFile(FSUtils.getFs(basePath, hadoopConf), basePath)) {
      if (pattern.matcher(partitionPath).matches()) {
        loadRecords(partitionPath);
      }
    }

    LOG.info("Finish sending index records, taskId = {}.", getRuntimeContext().getIndexOfThisSubtask());

    // wait for the other bootstrap tasks finish bootstrapping.
    waitForBootstrapReady(getRuntimeContext().getIndexOfThisSubtask());
  }

  /**
   * Wait for other bootstrap tasks to finish the index bootstrap.
   */
  @SuppressWarnings("checkstyle:MagicNumber")
  private void waitForBootstrapReady(int taskID) {
    int taskNum = getRuntimeContext().getNumberOfParallelSubtasks();
    int readyTaskNum = 1;
    while (taskNum != readyTaskNum) {
      try {
        readyTaskNum = aggregateManager.updateGlobalAggregate(BootstrapAggFunction.NAME, taskID, new BootstrapAggFunction());
        LOG.info("Waiting for other bootstrap tasks to complete, taskId = {}.", taskID);

        TimeUnit.SECONDS.sleep(5);
      } catch (Exception e) {
        LOG.warn("Update global task bootstrap summary error", e);
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void processElement(StreamRecord<I> element) throws Exception {
    output.collect((StreamRecord<O>) element);
  }

  /**
   * Loads all the indices of give partition path into the backup state.
   *
   * @param partitionPath The partition path
   */
  @SuppressWarnings("unchecked")
  protected void loadRecords(String partitionPath) throws Exception {
    // no ops
  }

  protected boolean shouldLoadFile(String fileId,
                                   int maxParallelism,
                                   int parallelism,
                                   int taskID) {
    return KeyGroupRangeAssignment.assignKeyToParallelOperator(
        fileId, maxParallelism, parallelism) == taskID;
  }

  @VisibleForTesting
  public boolean isAlreadyBootstrap() throws Exception {
    return instantState.get().iterator().hasNext();
  }
}
