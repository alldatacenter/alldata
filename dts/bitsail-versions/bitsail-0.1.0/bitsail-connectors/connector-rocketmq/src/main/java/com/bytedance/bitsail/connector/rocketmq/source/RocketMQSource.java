/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.connector.rocketmq.source;

import com.bytedance.bitsail.base.connector.reader.v1.Boundedness;
import com.bytedance.bitsail.base.connector.reader.v1.Source;
import com.bytedance.bitsail.base.connector.reader.v1.SourceReader;
import com.bytedance.bitsail.base.connector.reader.v1.SourceSplitCoordinator;
import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.execution.Mode;
import com.bytedance.bitsail.base.extension.ParallelismComputable;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.connector.rocketmq.option.RocketMQSourceOptions;
import com.bytedance.bitsail.connector.rocketmq.source.coordinator.RocketMQSourceSplitCoordinator;
import com.bytedance.bitsail.connector.rocketmq.source.reader.RocketMQSourceReader;
import com.bytedance.bitsail.connector.rocketmq.source.split.RocketMQSplit;
import com.bytedance.bitsail.connector.rocketmq.source.split.RocketMQState;
import com.bytedance.bitsail.connector.rocketmq.utils.RocketMQUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.Collection;
import java.util.UUID;

public class RocketMQSource
    implements Source<Row, RocketMQSplit, RocketMQState>, ParallelismComputable {

  private static final String SOURCE_INSTANCE_NAME_TEMPLATE = "rmq_source:%s_%s_%s:%s";

  private static final int DEFAULT_ROCKETMQ_PARALLELISM_THRESHOLD = 4;

  private BitSailConfiguration readerConfiguration;

  private BitSailConfiguration commonConfiguration;

  @Override
  public void configure(ExecutionEnviron execution, BitSailConfiguration readerConfiguration) {
    this.readerConfiguration = readerConfiguration;
    this.commonConfiguration = execution.getCommonConfiguration();
  }

  @Override
  public Boundedness getSourceBoundedness() {
    return Mode.BATCH.equals(Mode.getJobRunMode(commonConfiguration.get(CommonOptions.JOB_TYPE))) ?
        Boundedness.BOUNDEDNESS :
        Boundedness.UNBOUNDEDNESS;
  }

  @Override
  public SourceReader<Row, RocketMQSplit> createReader(SourceReader.Context readerContext) {
    return new RocketMQSourceReader(
        readerConfiguration,
        readerContext,
        getSourceBoundedness());
  }

  @Override
  public SourceSplitCoordinator<RocketMQSplit, RocketMQState> createSplitCoordinator(SourceSplitCoordinator
                                                                                         .Context<RocketMQSplit, RocketMQState> coordinatorContext) {
    return new RocketMQSourceSplitCoordinator(
        coordinatorContext,
        readerConfiguration,
        getSourceBoundedness());
  }

  @Override
  public String getReaderName() {
    return "rocketmq";
  }

  @Override
  public ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConfiguration,
                                                BitSailConfiguration rocketmqConfiguration,
                                                ParallelismAdvice upstreamAdvice) throws Exception {
    String cluster = rocketmqConfiguration.get(RocketMQSourceOptions.CLUSTER);
    String topic = rocketmqConfiguration.get(RocketMQSourceOptions.TOPIC);
    String consumerGroup = rocketmqConfiguration.get(RocketMQSourceOptions.CONSUMER_GROUP);
    DefaultLitePullConsumer consumer = RocketMQUtils.prepareRocketMQConsumer(rocketmqConfiguration, String.format(SOURCE_INSTANCE_NAME_TEMPLATE,
        cluster,
        topic,
        consumerGroup,
        UUID.randomUUID()
    ));
    try {
      consumer.start();
      Collection<MessageQueue> messageQueues = consumer.fetchMessageQueues(topic);
      int adviceParallelism = Math.max(CollectionUtils.size(messageQueues) / DEFAULT_ROCKETMQ_PARALLELISM_THRESHOLD, 1);

      return ParallelismAdvice.builder()
          .adviceParallelism(adviceParallelism)
          .enforceDownStreamChain(true)
          .build();
    } finally {
      consumer.shutdown();
    }
  }
}
