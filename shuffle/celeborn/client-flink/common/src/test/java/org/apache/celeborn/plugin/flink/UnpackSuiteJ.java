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
import java.util.Queue;

import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.celeborn.plugin.flink.buffer.BufferPacker;

public class UnpackSuiteJ {

  private NetworkBufferPool networkBufferPool;
  private BufferPool bufferPool;

  @Before
  public void setup() throws IOException {
    networkBufferPool = new NetworkBufferPool(10, 128);
    bufferPool = networkBufferPool.createBufferPool(10, 10);
  }

  @After
  public void tearDown() {
    bufferPool.lazyDestroy();
    networkBufferPool.destroy();
  }

  @Test
  public void testUnpack() throws IOException, InterruptedException {
    Buffer buffer = bufferPool.requestBuffer();
    ByteBuf byteBuf = buffer.asByteBuf();
    byteBuf.writerIndex(0);
    byteBuf.writeInt(0);
    byteBuf.writeInt(1);
    byteBuf.writeInt(2);
    byteBuf.writeInt(3);
    byteBuf.writeByte((byte) Buffer.DataType.DATA_BUFFER.ordinal());
    byteBuf.writeBoolean(true);
    byteBuf.writeInt(3);
    byteBuf.writeBytes(new byte[] {1, 2, 3});
    byteBuf.writeByte((byte) Buffer.DataType.EVENT_BUFFER.ordinal());
    byteBuf.writeBoolean(false);
    byteBuf.writeInt(5);
    byteBuf.writeBytes(new byte[] {1, 2, 3, 4, 5});

    Queue<Buffer> bufferQueue = BufferPacker.unpack(byteBuf);
    Assert.assertEquals(2, bufferQueue.size());
    Buffer buffer1 = bufferQueue.poll();
    Assert.assertEquals(buffer1.getDataType(), Buffer.DataType.DATA_BUFFER);
    Assert.assertEquals(buffer1.isCompressed(), true);
    Assert.assertEquals(buffer1.getSize(), 3);

    Buffer buffer2 = bufferQueue.poll();
    Assert.assertEquals(buffer2.getDataType(), Buffer.DataType.EVENT_BUFFER);
    Assert.assertEquals(buffer2.isCompressed(), false);
    Assert.assertEquals(buffer2.getSize(), 5);
  }
}
