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

package com.netease.arctic.flink.write.hidden;

import com.netease.arctic.flink.shuffle.ShuffleHelper;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.LogDataJsonSerialization;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.runtime.taskexecutor.GlobalAggregateManager;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.apache.iceberg.relocated.com.google.common.base.Preconditions.checkNotNull;

/**
 * This is a global flip committer used by every log writer operator.
 */
public class GlobalFlipCommitter {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalFlipCommitter.class);

  private static final String AGGREGATE_NAME = "flip-committer";
  private final GlobalAggregateManager aggregateManager;
  private final FlipCommitFunction flipCommitFunction;

  public GlobalFlipCommitter(
      GlobalAggregateManager aggregateManager,
      FlipCommitFunction flipCommitFunction) {
    this.aggregateManager = aggregateManager;
    this.flipCommitFunction = flipCommitFunction;
  }

  public boolean commit(int subtaskId, LogData<RowData> logData) throws IOException {
    Long committedEpicNo = aggregateManager.updateGlobalAggregate(
        AGGREGATE_NAME,
        new CommitRequest(subtaskId, logData),
        flipCommitFunction
    );
    return committedEpicNo != null && committedEpicNo == logData.getEpicNo();
  }

  public boolean hasCommittedFlip(LogData<RowData> logData) throws IOException {
    Long committedEpicNo = aggregateManager.updateGlobalAggregate(
        AGGREGATE_NAME,
        new CommitRequest(null, logData, true),
        flipCommitFunction
    );
    return committedEpicNo != null && committedEpicNo == logData.getEpicNo();
  }

  static class FlipCommitFunction implements AggregateFunction<CommitRequest, LogGlobalState, Long> {
    private static final long serialVersionUID = 6399278898504357412L;
    private final int numberOfTasks;
    private final LogDataJsonSerialization<RowData> logDataJsonSerialization;
    private final LogMsgFactory<RowData> factory;
    private final Properties producerConfig;
    private final String topic;
    private final ShuffleHelper helper;
    private transient LogMsgFactory.Producer<RowData> producer;

    public FlipCommitFunction(
        int numberOfTasks,
        Schema schema,
        LogData.FieldGetterFactory<RowData> fieldGetterFactory,
        LogMsgFactory<RowData> factory,
        Properties producerConfig,
        String topic,
        ShuffleHelper helper) {
      this.numberOfTasks = numberOfTasks;
      this.factory = checkNotNull(factory);
      this.logDataJsonSerialization = new LogDataJsonSerialization<>(
          checkNotNull(schema),
          checkNotNull(fieldGetterFactory)
      );
      this.producerConfig = producerConfig;
      this.topic = topic;
      this.helper = helper;
    }

    @Override
    public LogGlobalState createAccumulator() {
      return new LogGlobalState();
    }

    @Override
    public LogGlobalState add(CommitRequest value, LogGlobalState globalState) {
      if (value.checkCommitted) {
        return globalState;
      }
      LOG.info("receive CommitRequest={}.", value);
      NavigableMap<Long, SubAccumulator> accumulator = globalState.accumulators;
      Long epicNo = value.logRecord.getEpicNo();
      accumulator.compute(epicNo, (cpId, subAccumulator) -> {
        subAccumulator = subAccumulator == null ? new SubAccumulator() : subAccumulator;
        if (!subAccumulator.hasCommittedFlip) {
          subAccumulator.add(value.subtaskId, value);
        }
        return subAccumulator;
      });

      SubAccumulator subAccumulator = globalState.accumulators.get(epicNo);
      if (subAccumulator.taskIds.size() == numberOfTasks) {
        // this sync step, wait for sent records to topic.
        try {
          LOG.info("already receive {} commit requests. The last subtask received is {}.",
              numberOfTasks, value.subtaskId);
          sendFlip(subAccumulator, value);
          LOG.info("sent flip messages success, cost {}ms.", subAccumulator.cost.time());
        } catch (Exception e) {
          LOG.error("sending flip messages to topic failed, subAccumulator:{}.", subAccumulator, e);
          throw new RuntimeException(e);
        }
      } else {
        LOG.info("As of now, global state has received a total of {} commit requests which are {}.",
            subAccumulator.taskIds.size(),
            Arrays.toString(subAccumulator.taskIds.toArray(new Integer[0])));
      }
      return globalState;
    }

    private void sendFlip(SubAccumulator subAccumulator, CommitRequest value) throws Exception {
      if (null == producer) {
        producer =
            factory.createProducer(
                producerConfig,
                topic,
                logDataJsonSerialization,
                helper);
        producer.open();
      }

      producer.sendToAllPartitions(value.logRecord);
      subAccumulator.committed();
    }

    @Override
    public Long getResult(LogGlobalState globalState) {
      // find the maximum epic number and has already committed flip message to log queue.
      Optional<Long> result = globalState.accumulators
          .descendingMap()
          .entrySet()
          .stream()
          .filter(entry -> entry.getValue().hasCommittedFlip)
          .findFirst()
          .map(Map.Entry::getKey);
      return result.orElse(null);
    }

    @Override
    public LogGlobalState merge(LogGlobalState a, LogGlobalState b) {
      b.accumulators.forEach((cpId, acc) -> a.accumulators.compute(cpId, (key, subAccumulator) -> {
        subAccumulator = subAccumulator == null ? new SubAccumulator() : subAccumulator;
        if (!subAccumulator.hasCommittedFlip) {
          subAccumulator.merge(acc);
        }
        return subAccumulator;
      }));
      return a;
    }
  }

  static class CommitRequest implements Serializable {
    private static final long serialVersionUID = 5469815741394678192L;
    private final Integer subtaskId;
    private final LogData<RowData> logRecord;
    // TURE means check committerFunction has sent flip to topic whether.
    private final boolean checkCommitted;

    private CommitRequest(Integer subtaskId, LogData<RowData> logRecord) {
      this.subtaskId = subtaskId;
      this.logRecord = logRecord;
      this.checkCommitted = false;
    }

    private CommitRequest(Integer subtaskId, LogData<RowData> logRecord, Boolean checkCommitted) {
      this.subtaskId = subtaskId;
      this.logRecord = logRecord;
      this.checkCommitted = checkCommitted;
    }

    @Override
    public String toString() {
      return "CommitRequest{subtaskId=" +
          subtaskId +
          ", flip message=" +
          logRecord.toString() +
          "}";
    }
  }

  static class LogGlobalState implements Serializable {
    private static final long serialVersionUID = 9132207718335661833L;
    // this map keys mean epicNo, which is not exactly equal to checkpoint id
    private final NavigableMap<Long, SubAccumulator> accumulators;

    public LogGlobalState() {
      accumulators = new ConcurrentSkipListMap<>();
    }
  }

  private static class SubAccumulator implements Serializable {
    private static final long serialVersionUID = 1252547231163598559L;
    private final Set<Integer> taskIds = new CopyOnWriteArraySet<>();
    private CommitRequest commitRequest = null;
    // TRUE means has already sent flip msg to topic successfully.
    private volatile boolean hasCommittedFlip = false;
    // Mark how long it took to collect all commit requests.
    private Cost cost = new Cost();

    void add(int taskId, CommitRequest commitRequest) {
      this.taskIds.add(taskId);
      if (null == this.commitRequest && null != commitRequest) {
        this.commitRequest = commitRequest;
      }
      cost.markStart();
    }

    void committed() {
      this.hasCommittedFlip = true;
      cost.markEnd();
    }

    void merge(SubAccumulator subAccumulator) {
      this.taskIds.addAll(subAccumulator.taskIds);
      this.commitRequest = subAccumulator.commitRequest;
    }

    static class Cost implements Serializable {
      private static final long serialVersionUID = 1L;
      Long start;
      Long end;

      long time() {
        return end - start;
      }

      void markStart() {
        if (start == null) {
          start = System.currentTimeMillis();
        }
      }

      void markEnd() {
        if (end == null) {
          end = System.currentTimeMillis();
        }
      }
    }
  }
}
