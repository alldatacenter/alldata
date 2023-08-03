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
import java.util.List;

import org.apache.flink.runtime.io.network.buffer.BufferCompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.BufferPoolFactory;
import org.apache.flink.runtime.io.network.partition.ResultPartition;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionManager;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.util.function.SupplierWithException;

import org.apache.celeborn.common.CelebornConf;

/** Factory class to create {@link RemoteShuffleResultPartition}. */
public class RemoteShuffleResultPartitionFactory
    extends AbstractRemoteShuffleResultPartitionFactory {

  public RemoteShuffleResultPartitionFactory(
      CelebornConf celebornConf,
      ResultPartitionManager partitionManager,
      BufferPoolFactory bufferPoolFactory,
      int networkBufferSize) {

    super(celebornConf, partitionManager, bufferPoolFactory, networkBufferSize);
  }

  @Override
  ResultPartition createRemoteShuffleResultPartitionInternal(
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
      RemoteShuffleDescriptor rsd) {
    return new RemoteShuffleResultPartition(
        taskNameWithSubtaskAndId,
        partitionIndex,
        id,
        type,
        numSubpartitions,
        maxParallelism,
        networkBufferSize,
        partitionManager,
        bufferCompressor,
        bufferPoolFactories.get(0),
        new RemoteShuffleOutputGate(
            rsd,
            numSubpartitions,
            networkBufferSize,
            bufferPoolFactories.get(1),
            celebornConf,
            numMappers));
  }
}
