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

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.flink.shuffle.ShuffleKey;
import com.netease.arctic.flink.shuffle.ShuffleRulePolicy;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.util.ArcticUtils;
import com.netease.arctic.table.ArcticTable;
import org.apache.commons.lang.ArrayUtils;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.StateSnapshotContext;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.BoundedOneInput;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.flink.sink.TaskWriterFactory;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is arctic table includes writing file data to un keyed table and keyed table.
 */
public class ArcticFileWriter extends AbstractStreamOperator<WriteResult>
    implements OneInputStreamOperator<RowData, WriteResult>, BoundedOneInput {

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(ArcticFileWriter.class);

  private final ShuffleRulePolicy<RowData, ShuffleKey> shuffleRule;

  private final TaskWriterFactory<RowData> taskWriterFactory;
  private final int minFileSplitCount;
  private final ArcticTableLoader tableLoader;
  private final boolean submitEmptySnapshot;

  private transient org.apache.iceberg.io.TaskWriter<RowData> writer;
  private transient int subTaskId;
  private transient int attemptId;
  /**
   * Load table in runtime, because that table's refresh method will be invoked in serialization.
   * And it will set {@link org.apache.hadoop.security.UserGroupInformation#authenticationMethod} to KERBEROS
   * if Arctic's table is KERBEROS enabled. It will cause ugi relevant exception when deploy to yarn cluster.
   */
  private transient ArcticTable table;

  public ArcticFileWriter(
      ShuffleRulePolicy<RowData, ShuffleKey> shuffleRule,
      TaskWriterFactory<RowData> taskWriterFactory,
      int minFileSplitCount,
      ArcticTableLoader tableLoader,
      boolean upsert,
      boolean submitEmptySnapshot) {
    this.shuffleRule = shuffleRule;
    this.taskWriterFactory = taskWriterFactory;
    this.minFileSplitCount = minFileSplitCount;
    this.tableLoader = tableLoader;
    this.submitEmptySnapshot = submitEmptySnapshot;
    LOG.info("ArcticFileWriter is created with minFileSplitCount: {}, upsert: {}, submitEmptySnapshot: {}",
        minFileSplitCount, upsert, submitEmptySnapshot);
  }

  @Override
  public void open() {
    this.attemptId = getRuntimeContext().getAttemptNumber();
    table = ArcticUtils.loadArcticTable(tableLoader);

    long mask = getMask(subTaskId);
    initTaskWriterFactory(mask);

    this.writer = table.io().doAs(taskWriterFactory::create);
  }

  @Override
  public void initializeState(StateInitializationContext context) throws Exception {
    super.initializeState(context);

    this.subTaskId = getRuntimeContext().getIndexOfThisSubtask();
  }

  @Override
  public void snapshotState(StateSnapshotContext context) throws Exception {
    super.snapshotState(context);
  }

  private void initTaskWriterFactory(long mask) {
    if (taskWriterFactory instanceof ArcticRowDataTaskWriterFactory) {
      ((ArcticRowDataTaskWriterFactory) taskWriterFactory).setMask(mask);
    }
    taskWriterFactory.initialize(subTaskId, attemptId);
  }

  private long getMask(int subTaskId) {
    Set<DataTreeNode> initRootNodes;
    if (shuffleRule != null) {
      initRootNodes = shuffleRule.getSubtaskTreeNodes().get(subTaskId);
    } else {
      if (table.isKeyedTable()) {
        initRootNodes = IntStream.range(0, minFileSplitCount).mapToObj(index ->
            DataTreeNode.of(minFileSplitCount - 1, index)).collect(Collectors.toSet());
      } else {
        initRootNodes = Sets.newHashSet();
        initRootNodes.add(DataTreeNode.of(0, 0));
      }
    }

    return initRootNodes.iterator().next().mask();
  }

  @Override
  public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
    table.io().doAs(() -> {
      completeAndEmitFiles();

      this.writer = null;
      return null;
    });
  }

  @Override
  public void endInput() throws Exception {
    table.io().doAs(() -> {
      completeAndEmitFiles();
      return null;
    });
  }

  private void completeAndEmitFiles() throws IOException {
    // For bounded stream, it may don't enable the checkpoint mechanism so we'd better to emit the remaining
    // completed files to downstream before closing the writer so that we won't miss any of them.
    if (writer != null) {
      emit(writer.complete());
    }
  }

  @Override
  public void processElement(StreamRecord<RowData> element) throws Exception {
    RowData row = element.getValue();
    table.io().doAs(() -> {
      if (writer == null) {
        this.writer = taskWriterFactory.create();
      }
      writer.write(row);
      return null;
    });
  }

  @Override
  public void close() throws Exception {
    super.close();
    if (writer != null) {
      table.io().doAs(() -> {
        writer.close();
        return null;
      });
      writer = null;
    }
  }

  private void emit(WriteResult writeResult) {
    if (shouldEmit(writeResult)) {
      // Only emit a non-empty WriteResult to committer operator, thus avoiding submitting too much empty snapshots.
      output.collect(new StreamRecord<>(writeResult));
    }
  }

  /**
   * Whether to emit the WriteResult.
   *
   * @param writeResult the WriteResult to emit
   * @return true if the WriteResult should be emitted, or the WriteResult isn't empty,
   * false only if the WriteResult is empty and the submitEmptySnapshot is false.
   */
  private boolean shouldEmit(WriteResult writeResult) {
    return submitEmptySnapshot || (writeResult != null &&
        (!ArrayUtils.isEmpty(writeResult.dataFiles()) ||
            !ArrayUtils.isEmpty(writeResult.deleteFiles()) ||
            !ArrayUtils.isEmpty(writeResult.referencedDataFiles())));
  }

  @VisibleForTesting
  public TaskWriter<RowData> getWriter() {
    return writer;
  }
}
