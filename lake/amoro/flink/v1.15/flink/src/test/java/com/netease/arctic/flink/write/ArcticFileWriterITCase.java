/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.write;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.data.FileNameRules;
import com.netease.arctic.flink.FlinkTestBase;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.util.ArcticUtils;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ExecutionOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.minicluster.MiniCluster;
import org.apache.flink.runtime.minicluster.MiniClusterConfiguration;
import org.apache.flink.runtime.state.CheckpointListener;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.Snapshot;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static com.netease.arctic.flink.read.TestArcticSource.tableRecords;

public class ArcticFileWriterITCase extends FlinkTestBase {

  public static final Logger LOG = LoggerFactory.getLogger(ArcticFileWriterITCase.class);

  private static final Map<String, CountDownLatch> LATCH_MAP = new ConcurrentHashMap<>();
  public ArcticTableLoader tableLoader;
  private String latchId;
  private int NUM_SOURCES = 4;
  private int NUM_RECORDS = 10000;

  public ArcticFileWriterITCase() {
    super(new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
      new BasicTableTestHelper(true, true));
  }

  @Before
  public void setup() {
    this.latchId = UUID.randomUUID().toString();
    // We wait for two successful checkpoints in sources before shutting down. This ensures that
    // the sink can commit its data.
    // We need to keep a "static" latch here because all sources need to be kept running
    // while we're waiting for the required number of checkpoints. Otherwise, we would lock up
    // because we can only do checkpoints while all operators are running.
    LATCH_MAP.put(latchId, new CountDownLatch(NUM_SOURCES * 2));
  }

  protected static final double FAILOVER_RATIO = 0.4;

  private static class StreamingExecutionTestSource extends RichParallelSourceFunction<RowData>
      implements CheckpointListener, CheckpointedFunction {

    private final String latchId;

    private final int numberOfRecords;

    /**
     * Whether the test is executing in a scenario that induces a failover. This doesn't mean
     * that this source induces the failover.
     */
    private final boolean isFailoverScenario;

    private ListState<Integer> nextValueState;

    private int nextValue;

    private volatile boolean isCanceled;

    private volatile boolean snapshottedAfterAllRecordsOutput;

    private volatile boolean isWaitingCheckpointComplete;

    private volatile boolean hasCompletedCheckpoint;

    public StreamingExecutionTestSource(
        String latchId, int numberOfRecords, boolean isFailoverScenario) {
      this.latchId = latchId;
      this.numberOfRecords = numberOfRecords;
      this.isFailoverScenario = isFailoverScenario;
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
      nextValueState =
          context.getOperatorStateStore()
              .getListState(new ListStateDescriptor<>("nextValue", Integer.class));

      if (nextValueState.get() != null && nextValueState.get().iterator().hasNext()) {
        nextValue = nextValueState.get().iterator().next();
      }
    }

    @Override
    public void run(SourceContext<RowData> ctx) throws Exception {
      if (isFailoverScenario && getRuntimeContext().getAttemptNumber() == 0) {
        // In the first execution, we first send a part of record...
        sendRecordsUntil((int) (numberOfRecords * FAILOVER_RATIO * 0.5), ctx);

        // Wait till the first part of data is committed.
        while (!hasCompletedCheckpoint) {
          Thread.sleep(50);
        }

        // Then we write the second part of data...
        sendRecordsUntil((int) (numberOfRecords * FAILOVER_RATIO), ctx);

        // And then trigger the failover.
        if (getRuntimeContext().getIndexOfThisSubtask() == 0) {
          throw new RuntimeException("Designated Exception");
        } else {
          while (true) {
            Thread.sleep(50);
          }
        }
      } else {
        // If we are not going to trigger failover or we have already triggered failover,
        // run until finished.
        sendRecordsUntil(numberOfRecords, ctx);

        // Wait the last checkpoint to commit all the pending records.
        isWaitingCheckpointComplete = true;
        CountDownLatch latch = LATCH_MAP.get(latchId);
        latch.await();
      }
    }

    private void sendRecordsUntil(int targetNumber, SourceContext<RowData> ctx) {
      while (!isCanceled && nextValue < targetNumber) {
        synchronized (ctx.getCheckpointLock()) {
          ctx.collect(GenericRowData.of(
              nextValue++,
              StringData.fromString(""),
              LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),
              TimestampData.fromLocalDateTime(LocalDateTime.now()))
          );
        }
      }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
      nextValueState.update(Collections.singletonList(nextValue));

      if (isWaitingCheckpointComplete) {
        snapshottedAfterAllRecordsOutput = true;
      }
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {
      if (isWaitingCheckpointComplete && snapshottedAfterAllRecordsOutput) {
        CountDownLatch latch = LATCH_MAP.get(latchId);
        latch.countDown();
      }

      hasCompletedCheckpoint = true;
    }

    @Override
    public void cancel() {
      isCanceled = true;
    }
  }

  protected JobGraph createJobGraph(ArcticTableLoader tableLoader, TableSchema tableSchema, boolean triggerFailover) {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    Configuration config = new Configuration();
    config.set(ExecutionOptions.RUNTIME_MODE, RuntimeExecutionMode.STREAMING);
    env.configure(config, getClass().getClassLoader());

    env.enableCheckpointing(10, CheckpointingMode.EXACTLY_ONCE);

    if (triggerFailover) {
      env.setRestartStrategy(RestartStrategies.fixedDelayRestart(1, Time.milliseconds(100)));
    } else {
      env.setRestartStrategy(RestartStrategies.noRestart());
    }

    DataStreamSource<RowData> source = env.addSource(
            new StreamingExecutionTestSource(latchId, NUM_RECORDS, triggerFailover))
        .setParallelism(4);
    ArcticTable table = ArcticUtils.loadArcticTable(tableLoader);
    FlinkSink
        .forRowData(source)
        .context(Optional::of)
        .table(table)
        .tableLoader(tableLoader)
        .flinkSchema(tableSchema)
        .build();

    StreamGraph streamGraph = env.getStreamGraph();
    return streamGraph.getJobGraph();
  }

  @Test
  public void testWrite() throws Exception {
    tableLoader = ArcticTableLoader.of(TableTestHelper.TEST_TABLE_ID, catalogBuilder);

    JobGraph jobGraph = createJobGraph(tableLoader, FLINK_SCHEMA, true);
    final Configuration config = new Configuration();
    config.setString(RestOptions.BIND_PORT, "18081-19000");
    final MiniClusterConfiguration cfg =
        new MiniClusterConfiguration.Builder()
            .setNumTaskManagers(1)
            .setNumSlotsPerTaskManager(NUM_SOURCES)
            .setConfiguration(config)
            .build();

    try (MiniCluster miniCluster = new MiniCluster(cfg)) {
      miniCluster.start();
      miniCluster.executeJobBlocking(jobGraph);
    }

    KeyedTable keyedTable = tableLoader.loadArcticTable().asKeyedTable();
    checkResult(keyedTable, NUM_RECORDS * NUM_SOURCES);
  }

  public static void checkResult(KeyedTable keyedTable, int exceptedSize) {
    keyedTable.refresh();
    Snapshot crt = keyedTable.changeTable().currentSnapshot();

    Stack<Snapshot> snapshots = new Stack<>();
    while (crt != null) {
      snapshots.push(crt);
      if (crt.parentId() == null) {
        break;
      }
      crt = keyedTable.changeTable().snapshot(crt.parentId());
    }

    Set<String> paths = new HashSet<>();
    long maxTxId = -1;
    while (!snapshots.isEmpty()) {
      Snapshot snapshot = snapshots.pop();
      long minTxIdInSnapshot = Integer.MAX_VALUE;
      long maxTxIdInSnapshot = -1;
      for (DataFile addedFile : snapshot.addedDataFiles(keyedTable.io())) {
        String path = addedFile.path().toString();
        Assert.assertFalse(paths.contains(path));
        paths.add(path);
        LOG.info("add file: {}", addedFile.path());

        long txId = FileNameRules.parseChange(path, snapshot.sequenceNumber()).transactionId();
        minTxIdInSnapshot = Math.min(minTxIdInSnapshot, txId);
        maxTxIdInSnapshot = Math.max(maxTxIdInSnapshot, txId);
      }
      Assert.assertTrue(maxTxId <= minTxIdInSnapshot);

      maxTxId = maxTxIdInSnapshot;
    }

    Assert.assertEquals(exceptedSize, tableRecords(keyedTable).size());
  }
}