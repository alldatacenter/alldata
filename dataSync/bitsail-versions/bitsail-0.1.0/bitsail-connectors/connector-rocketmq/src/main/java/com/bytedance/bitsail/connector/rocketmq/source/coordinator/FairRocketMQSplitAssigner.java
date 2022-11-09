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

package com.bytedance.bitsail.connector.rocketmq.source.coordinator;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.base.source.split.SplitAssigner;

import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FairRocketMQSplitAssigner implements SplitAssigner<MessageQueue> {

  private BitSailConfiguration readerConfiguration;

  private AtomicInteger atomicInteger;

  public Map<MessageQueue, String> rocketMQSplitIncrementMapping;

  public FairRocketMQSplitAssigner(BitSailConfiguration readerConfiguration,
                                   Map<MessageQueue, String> rocketMQSplitIncrementMapping) {
    this.readerConfiguration = readerConfiguration;
    this.rocketMQSplitIncrementMapping = rocketMQSplitIncrementMapping;
    this.atomicInteger = new AtomicInteger(CollectionUtils
        .size(rocketMQSplitIncrementMapping.keySet()));
  }

  @Override
  public String assignSplitId(MessageQueue messageQueue) {
    if (!rocketMQSplitIncrementMapping.containsKey(messageQueue)) {
      rocketMQSplitIncrementMapping.put(messageQueue, String.valueOf(atomicInteger.getAndIncrement()));
    }
    return rocketMQSplitIncrementMapping.get(messageQueue);
  }

  @Override
  public int assignToReader(String splitId, int totalParallelism) {
    return splitId.hashCode() % totalParallelism;
  }
}
