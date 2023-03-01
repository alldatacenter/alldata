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

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferCompressor;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.runtime.io.network.partition.ResultPartitionID;
import org.apache.flink.runtime.io.network.partition.ResultPartitionManager;
import org.apache.flink.runtime.io.network.partition.ResultPartitionType;
import org.apache.flink.util.function.SupplierWithException;
import org.junit.Before;
import org.junit.Test;

import org.apache.celeborn.plugin.flink.buffer.SortBuffer;

public class RemoteShuffleResultPartitionSuiteJ {
  private BufferCompressor bufferCompressor = new BufferCompressor(32 * 1024, "lz4");
  private RemoteShuffleOutputGate remoteShuffleOutputGate = mock(RemoteShuffleOutputGate.class);

  @Before
  public void setup() {}

  @Test
  public void tesSimpleFlush() throws IOException, InterruptedException {
    List<SupplierWithException<BufferPool, IOException>> bufferPool = createBufferPoolFactory();
    RemoteShuffleResultPartition remoteShuffleResultPartition =
        new RemoteShuffleResultPartition(
            "test",
            0,
            new ResultPartitionID(),
            ResultPartitionType.BLOCKING,
            2,
            2,
            32 * 1024,
            new ResultPartitionManager(),
            bufferCompressor,
            bufferPool.get(0),
            remoteShuffleOutputGate);
    remoteShuffleResultPartition.setup();
    doNothing().when(remoteShuffleOutputGate).regionStart(anyBoolean());
    doNothing().when(remoteShuffleOutputGate).regionFinish();
    when(remoteShuffleOutputGate.getBufferPool()).thenReturn(bufferPool.get(1).get());
    SortBuffer sortBuffer = remoteShuffleResultPartition.getUnicastSortBuffer();
    ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[] {1, 2, 3});
    sortBuffer.append(byteBuffer, 0, Buffer.DataType.DATA_BUFFER);
    remoteShuffleResultPartition.flushSortBuffer(sortBuffer, true);
  }

  private List<SupplierWithException<BufferPool, IOException>> createBufferPoolFactory() {
    NetworkBufferPool networkBufferPool =
        new NetworkBufferPool(256 * 8, 32 * 1024, Duration.ofMillis(1000));

    int numBuffersPerPartition = 64 * 1024 / 32;
    int numForResultPartition = numBuffersPerPartition * 7 / 8;
    int numForOutputGate = numBuffersPerPartition - numForResultPartition;

    List<SupplierWithException<BufferPool, IOException>> factories = new ArrayList<>();
    factories.add(
        () -> networkBufferPool.createBufferPool(numForResultPartition, numForResultPartition));
    factories.add(() -> networkBufferPool.createBufferPool(numForOutputGate, numForOutputGate));
    return factories;
  }
}
