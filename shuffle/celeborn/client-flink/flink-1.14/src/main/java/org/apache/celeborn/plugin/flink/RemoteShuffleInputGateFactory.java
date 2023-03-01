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

package org.apache.celeborn.plugin.flink;

import java.io.IOException;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.deployment.InputGateDeploymentDescriptor;
import org.apache.flink.runtime.io.network.buffer.BufferDecompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.BufferPoolFactory;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.util.function.SupplierWithException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.plugin.flink.config.PluginConf;
import org.apache.celeborn.plugin.flink.utils.Utils;

/** Factory class to create {@link RemoteShuffleInputGate}. */
public class RemoteShuffleInputGateFactory {

  public static final int MIN_BUFFERS_PER_GATE = 16;

  private static final Logger LOG = LoggerFactory.getLogger(RemoteShuffleInputGateFactory.class);

  /** Number of max concurrent reading channels. */
  private final int numConcurrentReading;

  /** Codec used for compression / decompression. */
  private static final String compressionCodec = "LZ4";

  /** Network buffer size. */
  private final int networkBufferSize;

  /**
   * Network buffer pool used for shuffle read buffers. {@link BufferPool}s will be created from it
   * and each of them will be used by a channel exclusively.
   */
  private final NetworkBufferPool networkBufferPool;

  /** Sum of buffers. */
  private final int numBuffersPerGate;

  private CelebornConf celebornConf;

  public RemoteShuffleInputGateFactory(
      Configuration flinkConf,
      CelebornConf conf,
      NetworkBufferPool networkBufferPool,
      int networkBufferSize) {
    this.celebornConf = conf;
    long configuredMemorySize =
        org.apache.celeborn.common.util.Utils.byteStringAsBytes(
            PluginConf.getValue(flinkConf, PluginConf.MEMORY_PER_INPUT_GATE));
    if (configuredMemorySize < MIN_BUFFERS_PER_GATE) {
      throw new IllegalArgumentException(
          String.format(
              "Insufficient network memory per input gate, please increase %s to at " + "least %s.",
              PluginConf.MEMORY_PER_INPUT_GATE.name,
              PluginConf.getValue(flinkConf, PluginConf.MIN_MEMORY_PER_GATE)));
    }

    this.numBuffersPerGate = Utils.checkedDownCast(configuredMemorySize / networkBufferSize);
    if (numBuffersPerGate < MIN_BUFFERS_PER_GATE) {
      throw new IllegalArgumentException(
          String.format(
              "Insufficient network memory per input gate, please increase %s to at "
                  + "least %d bytes.",
              PluginConf.MEMORY_PER_INPUT_GATE.name, networkBufferSize * MIN_BUFFERS_PER_GATE));
    }

    this.networkBufferSize = networkBufferSize;
    this.numConcurrentReading =
        Integer.valueOf(PluginConf.getValue(flinkConf, PluginConf.NUM_CONCURRENT_READINGS));
    this.networkBufferPool = networkBufferPool;
  }

  /** Create {@link RemoteShuffleInputGate} from {@link InputGateDeploymentDescriptor}. */
  public RemoteShuffleInputGate create(
      String owningTaskName, int gateIndex, InputGateDeploymentDescriptor igdd) {
    LOG.info(
        "Create input gate -- number of buffers per input gate={}, "
            + "number of concurrent readings={}.",
        numBuffersPerGate,
        numConcurrentReading);

    SupplierWithException<BufferPool, IOException> bufferPoolFactory =
        createBufferPoolFactory(networkBufferPool, numBuffersPerGate);
    BufferDecompressor bufferDecompressor =
        new BufferDecompressor(networkBufferSize, compressionCodec);

    return createInputGate(owningTaskName, gateIndex, igdd, bufferPoolFactory, bufferDecompressor);
  }

  // For testing.
  RemoteShuffleInputGate createInputGate(
      String owningTaskName,
      int gateIndex,
      InputGateDeploymentDescriptor igdd,
      SupplierWithException<BufferPool, IOException> bufferPoolFactory,
      BufferDecompressor bufferDecompressor) {
    return new RemoteShuffleInputGate(
        this.celebornConf, owningTaskName, gateIndex, igdd, bufferPoolFactory, bufferDecompressor);
  }

  private SupplierWithException<BufferPool, IOException> createBufferPoolFactory(
      BufferPoolFactory bufferPoolFactory, int numBuffers) {
    return () -> bufferPoolFactory.createBufferPool(numBuffers, numBuffers);
  }
}
