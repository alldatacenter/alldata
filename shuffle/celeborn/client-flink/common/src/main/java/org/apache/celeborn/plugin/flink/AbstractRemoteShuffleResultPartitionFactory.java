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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.runtime.deployment.ResultPartitionDeploymentDescriptor;
import org.apache.flink.runtime.io.network.buffer.BufferCompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.BufferPoolFactory;
import org.apache.flink.runtime.io.network.partition.ResultPartition;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionManager;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.runtime.shuffle.ShuffleDescriptor;
import org.apache.flink.util.function.SupplierWithException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.protocol.CompressionCodec;
import org.apache.celeborn.plugin.flink.utils.Utils;

/** Factory class to create {@link RemoteShuffleResultPartition}. */
public abstract class AbstractRemoteShuffleResultPartitionFactory {

  private static final Logger LOG =
      LoggerFactory.getLogger(AbstractRemoteShuffleResultPartitionFactory.class);

  public static final int MIN_BUFFERS_PER_PARTITION = 16;

  /** Not used and just for compatibility with Flink pluggable shuffle service. */
  protected final ResultPartitionManager partitionManager;

  /** Network buffer pool used for shuffle write buffers. */
  protected final BufferPoolFactory bufferPoolFactory;

  /** Network buffer size. */
  protected final int networkBufferSize;

  /**
   * Configured number of buffers for shuffle write, it contains two parts: sorting buffers and
   * transportation buffers.
   */
  protected final int numBuffersPerPartition;

  protected boolean supportFloatingBuffers;

  protected String compressionCodec;

  public AbstractRemoteShuffleResultPartitionFactory(
      CelebornConf celebornConf,
      ResultPartitionManager partitionManager,
      BufferPoolFactory bufferPoolFactory,
      int networkBufferSize) {
    long configuredMemorySize = celebornConf.clientFlinkMemoryPerResultPartition();
    long minConfiguredMemorySize = celebornConf.clientFlinkMemoryPerResultPartitionMin();
    if (configuredMemorySize < minConfiguredMemorySize) {
      throw new IllegalArgumentException(
          String.format(
              "Insufficient network memory per result partition, please increase %s "
                  + "to at least %s.",
              CelebornConf.CLIENT_MEMORY_PER_RESULT_PARTITION().key(), minConfiguredMemorySize));
    }

    this.numBuffersPerPartition = Utils.checkedDownCast(configuredMemorySize / networkBufferSize);
    this.supportFloatingBuffers = celebornConf.clientFlinkResultPartitionSupportFloatingBuffer();
    if (numBuffersPerPartition < MIN_BUFFERS_PER_PARTITION) {
      throw new IllegalArgumentException(
          String.format(
              "Insufficient network memory per partition, please increase %s to at "
                  + "least %d bytes.",
              CelebornConf.CLIENT_MEMORY_PER_RESULT_PARTITION().key(),
              networkBufferSize * MIN_BUFFERS_PER_PARTITION));
    }

    this.partitionManager = partitionManager;
    this.bufferPoolFactory = bufferPoolFactory;
    this.networkBufferSize = networkBufferSize;

    this.compressionCodec = celebornConf.shuffleCompressionCodec().name();
  }

  public ResultPartition create(
      String taskNameWithSubtaskAndId,
      int partitionIndex,
      ResultPartitionDeploymentDescriptor desc,
      CelebornConf celebornConf) {
    LOG.info(
        "Create result partition -- number of buffers per result partition={}, "
            + "number of subpartitions={}.",
        numBuffersPerPartition,
        desc.getNumberOfSubpartitions());

    return create(
        taskNameWithSubtaskAndId,
        partitionIndex,
        desc.getShuffleDescriptor().getResultPartitionID(),
        desc.getPartitionType(),
        desc.getNumberOfSubpartitions(),
        desc.getMaxParallelism(),
        createBufferPoolFactory(),
        desc.getShuffleDescriptor(),
        celebornConf,
        desc.getTotalNumberOfPartitions());
  }

  public ResultPartition create(
      String taskNameWithSubtaskAndId,
      int partitionIndex,
      ResultPartitionID id,
      ResultPartitionType type,
      int numSubpartitions,
      int maxParallelism,
      List<SupplierWithException<BufferPool, IOException>> bufferPoolFactories,
      ShuffleDescriptor shuffleDescriptor,
      CelebornConf celebornConf,
      int numMappers) {

    // in flink1.14/1.15, just support LZ4
    if (!compressionCodec.equals(CompressionCodec.LZ4.name())) {
      throw new IllegalStateException("Unknown CompressionMethod " + compressionCodec);
    }
    final BufferCompressor bufferCompressor =
        new BufferCompressor(networkBufferSize, compressionCodec);
    RemoteShuffleDescriptor rsd = (RemoteShuffleDescriptor) shuffleDescriptor;
    ResultPartition partition =
        createRemoteShuffleResultPartitionInternal(
            taskNameWithSubtaskAndId,
            partitionIndex,
            id,
            type,
            numSubpartitions,
            maxParallelism,
            bufferPoolFactories,
            celebornConf,
            numMappers,
            bufferCompressor,
            rsd);
    LOG.debug("{}: Initialized {}", taskNameWithSubtaskAndId, this);
    return partition;
  }

  abstract ResultPartition createRemoteShuffleResultPartitionInternal(
      String taskNameWithSubtaskAndId,
      int partitionIndex,
      ResultPartitionID id,
      ResultPartitionType type,
      int numSubpartitions,
      int maxParallelism,
      List<SupplierWithException<BufferPool, IOException>> bufferPoolFactories,
      CelebornConf celebornConf,
      int numMappers,
      BufferCompressor bufferCompressor,
      RemoteShuffleDescriptor rsd);

  /**
   * Used to create 2 buffer pools -- sorting buffer pool (7/8), transportation buffer pool (1/8).
   */
  private List<SupplierWithException<BufferPool, IOException>> createBufferPoolFactory() {
    int numForResultPartition = numBuffersPerPartition * 7 / 8;
    int numForOutputGate = numBuffersPerPartition - numForResultPartition;

    List<SupplierWithException<BufferPool, IOException>> factories = new ArrayList<>();
    if (supportFloatingBuffers) {
      factories.add(() -> bufferPoolFactory.createBufferPool(2, numForResultPartition));
      factories.add(() -> bufferPoolFactory.createBufferPool(2, numForOutputGate));
    } else {
      factories.add(
          () -> bufferPoolFactory.createBufferPool(numForResultPartition, numForResultPartition));
      factories.add(() -> bufferPoolFactory.createBufferPool(numForOutputGate, numForOutputGate));
    }
    return factories;
  }

  @VisibleForTesting
  int getNetworkBufferSize() {
    return networkBufferSize;
  }
}
