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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Optional;

import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.celeborn.common.protocol.PartitionLocation;
import org.apache.celeborn.plugin.flink.readclient.FlinkShuffleClientImpl;

public class RemoteShuffleOutputGateSuiteJ {
  private RemoteShuffleOutputGate remoteShuffleOutputGate = mock(RemoteShuffleOutputGate.class);
  private FlinkShuffleClientImpl shuffleClient = mock(FlinkShuffleClientImpl.class);
  private static final int BUFFER_SIZE = 20;
  private NetworkBufferPool networkBufferPool;
  private BufferPool bufferPool;

  @Before
  public void setup() throws IOException {
    remoteShuffleOutputGate.flinkShuffleClient = shuffleClient;
    networkBufferPool = new NetworkBufferPool(10, BUFFER_SIZE);
    bufferPool = networkBufferPool.createBufferPool(10, 10);
  }

  @Test
  public void TestSimpleWriteData() throws IOException, InterruptedException {

    PartitionLocation partitionLocation =
        new PartitionLocation(
            1, 0, "localhost", 123, 245, 789, 238, PartitionLocation.Mode.PRIMARY);
    when(shuffleClient.registerMapPartitionTask(anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
        .thenAnswer(t -> partitionLocation);
    doNothing()
        .when(remoteShuffleOutputGate.flinkShuffleClient)
        .pushDataHandShake(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any());

    remoteShuffleOutputGate.handshake(true);

    when(remoteShuffleOutputGate.flinkShuffleClient.regionStart(
            anyInt(), anyInt(), anyInt(), any(), anyInt(), anyBoolean()))
        .thenAnswer(t -> Optional.empty());
    remoteShuffleOutputGate.regionStart(false);

    remoteShuffleOutputGate.write(bufferPool.requestBuffer(), 0);

    doNothing()
        .when(remoteShuffleOutputGate.flinkShuffleClient)
        .regionFinish(anyInt(), anyInt(), anyInt(), any());
    remoteShuffleOutputGate.regionFinish();

    doNothing()
        .when(remoteShuffleOutputGate.flinkShuffleClient)
        .mapperEnd(anyInt(), anyInt(), anyInt(), anyInt());
    remoteShuffleOutputGate.finish();

    doNothing().when(remoteShuffleOutputGate.flinkShuffleClient).shutdown();
    remoteShuffleOutputGate.close();
  }

  @Test
  public void testNettyPoolTransform() {
    Buffer buffer = bufferPool.requestBuffer();
    ByteBuf byteBuf = buffer.asByteBuf();
    byteBuf.writeByte(1);
    Assert.assertEquals(1, byteBuf.refCnt());
    io.netty.buffer.ByteBuf celebornByteBuf =
        io.netty.buffer.Unpooled.wrappedBuffer(byteBuf.nioBuffer());
    Assert.assertEquals(1, celebornByteBuf.refCnt());
    celebornByteBuf.release();
    byteBuf.release();
    Assert.assertEquals(0, byteBuf.refCnt());
    Assert.assertEquals(0, celebornByteBuf.refCnt());
  }
}
