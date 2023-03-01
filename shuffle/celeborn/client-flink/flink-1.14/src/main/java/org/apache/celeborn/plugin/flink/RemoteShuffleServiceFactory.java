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

package org.apache.celeborn.plugin.flink;

import static org.apache.flink.runtime.io.network.metrics.NettyShuffleMetricFactory.registerShuffleMetrics;

import java.time.Duration;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.NettyShuffleEnvironmentOptions;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.io.network.api.writer.ResultPartitionWriter;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.runtime.io.network.partition.ResultPartitionManager;
import org.apache.flink.runtime.io.network.partition.consumer.IndexedInputGate;
import org.apache.flink.runtime.shuffle.ShuffleEnvironment;
import org.apache.flink.runtime.shuffle.ShuffleEnvironmentContext;
import org.apache.flink.runtime.shuffle.ShuffleMaster;
import org.apache.flink.runtime.shuffle.ShuffleMasterContext;
import org.apache.flink.runtime.shuffle.ShuffleServiceFactory;
import org.apache.flink.runtime.util.ConfigurationParserUtils;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.plugin.flink.utils.FlinkUtils;

public class RemoteShuffleServiceFactory
    implements ShuffleServiceFactory<
        RemoteShuffleDescriptor, ResultPartitionWriter, IndexedInputGate> {

  @Override
  public ShuffleMaster<RemoteShuffleDescriptor> createShuffleMaster(
      ShuffleMasterContext shuffleMasterContext) {
    return new RemoteShuffleMaster(shuffleMasterContext);
  }

  @Override
  public ShuffleEnvironment<ResultPartitionWriter, IndexedInputGate> createShuffleEnvironment(
      ShuffleEnvironmentContext shuffleEnvironmentContext) {
    Configuration configuration = shuffleEnvironmentContext.getConfiguration();
    int bufferSize = ConfigurationParserUtils.getPageSize(configuration);
    final int numBuffers =
        calculateNumberOfNetworkBuffers(
            shuffleEnvironmentContext.getNetworkMemorySize(), bufferSize);

    ResultPartitionManager resultPartitionManager = new ResultPartitionManager();
    MetricGroup metricGroup = shuffleEnvironmentContext.getParentMetricGroup();

    Duration requestSegmentsTimeout =
        Duration.ofMillis(
            configuration.getLong(
                NettyShuffleEnvironmentOptions
                    .NETWORK_EXCLUSIVE_BUFFERS_REQUEST_TIMEOUT_MILLISECONDS));
    NetworkBufferPool networkBufferPool =
        new NetworkBufferPool(numBuffers, bufferSize, requestSegmentsTimeout);

    registerShuffleMetrics(metricGroup, networkBufferPool);
    CelebornConf celebornConf = FlinkUtils.toCelebornConf(configuration);
    RemoteShuffleResultPartitionFactory resultPartitionFactory =
        new RemoteShuffleResultPartitionFactory(
            configuration, celebornConf, resultPartitionManager, networkBufferPool, bufferSize);
    RemoteShuffleInputGateFactory inputGateFactory =
        new RemoteShuffleInputGateFactory(
            configuration, celebornConf, networkBufferPool, bufferSize);

    return new RemoteShuffleEnvironment(
        networkBufferPool,
        resultPartitionManager,
        resultPartitionFactory,
        inputGateFactory,
        celebornConf);
  }

  private static int calculateNumberOfNetworkBuffers(MemorySize memorySize, int bufferSize) {
    long numBuffersLong = memorySize.getBytes() / bufferSize;
    if (numBuffersLong > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "The given number of memory bytes ("
              + memorySize.getBytes()
              + ") corresponds to more than MAX_INT pages.");
    }
    return (int) numBuffersLong;
  }
}
