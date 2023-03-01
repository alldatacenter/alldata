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

import static org.apache.flink.runtime.io.network.buffer.Buffer.DataType.DATA_BUFFER;
import static org.apache.flink.runtime.io.network.buffer.Buffer.DataType.EVENT_BUFFER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.runtime.io.network.buffer.Buffer;
import org.apache.flink.runtime.io.network.buffer.BufferPool;
import org.apache.flink.runtime.io.network.buffer.NetworkBufferPool;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.celeborn.plugin.flink.buffer.BufferPacker;
import org.apache.celeborn.plugin.flink.utils.BufferUtils;

public class BufferPackSuitJ {
  private static final int BUFFER_SIZE = 20 + 16;

  private NetworkBufferPool networkBufferPool;

  private BufferPool bufferPool;

  @Before
  public void setup() throws Exception {
    networkBufferPool = new NetworkBufferPool(10, BUFFER_SIZE);
    bufferPool = networkBufferPool.createBufferPool(10, 10);
  }

  @After
  public void tearDown() {
    bufferPool.lazyDestroy();
    assertEquals(10, networkBufferPool.getNumberOfAvailableMemorySegments());
    networkBufferPool.destroy();
  }

  @Test
  public void testPackEmptyBuffers() throws Exception {
    List<Buffer> buffers = requestBuffers(3);
    setCompressed(buffers, true, true, false);
    setDataType(buffers, EVENT_BUFFER, DATA_BUFFER, DATA_BUFFER);

    Integer subIdx = 2;

    List<ByteBuf> output = new ArrayList<>();
    BufferPacker.BiConsumerWithException<ByteBuf, Integer, InterruptedException> ripeBufferHandler =
        (ripe, sub) -> {
          assertEquals(subIdx, sub);
          output.add(ripe);
        };

    BufferPacker packer = new BufferPacker(ripeBufferHandler);
    packer.process(buffers.get(0), subIdx);
    packer.process(buffers.get(1), subIdx);
    packer.process(buffers.get(2), subIdx);
    assertTrue(output.isEmpty());

    packer.drain();
    assertEquals(0, output.size());
  }

  @Test
  public void testPartialBuffersForSameSubIdx() throws Exception {
    List<Buffer> buffers = requestBuffers(3);
    setCompressed(buffers, true, true, false);
    setDataType(buffers, EVENT_BUFFER, DATA_BUFFER, DATA_BUFFER);

    List<Pair<ByteBuf, Integer>> output = new ArrayList<>();
    BufferPacker.BiConsumerWithException<ByteBuf, Integer, InterruptedException> ripeBufferHandler =
        (ripe, sub) -> output.add(Pair.of(ripe, sub));
    BufferPacker packer = new BufferPacker(ripeBufferHandler);
    fillBuffers(buffers, 0, 1, 2);

    packer.process(buffers.get(0), 2);
    packer.process(buffers.get(1), 2);
    assertEquals(0, output.size());

    packer.process(buffers.get(2), 2);
    assertEquals(1, output.size());

    packer.drain();
    assertEquals(2, output.size());

    List<Buffer> unpacked = new ArrayList<>();
    output.forEach(
        pair -> {
          assertEquals(Integer.valueOf(2), pair.getRight());
          unpacked.addAll(BufferPacker.unpack(pair.getLeft()));
        });
    checkIfCompressed(unpacked, true, true, false);
    checkDataType(unpacked, EVENT_BUFFER, DATA_BUFFER, DATA_BUFFER);
    verifyBuffers(unpacked, 0, 1, 2);
    unpacked.forEach(Buffer::recycleBuffer);
  }

  @Test
  public void testPartialBuffersForMultipleSubIdx() throws Exception {
    List<Buffer> buffers = requestBuffers(3);
    setCompressed(buffers, true, true, false);
    setDataType(buffers, EVENT_BUFFER, DATA_BUFFER, DATA_BUFFER);

    List<Pair<ByteBuf, Integer>> output = new ArrayList<>();
    BufferPacker.BiConsumerWithException<ByteBuf, Integer, InterruptedException> ripeBufferHandler =
        (ripe, sub) -> output.add(Pair.of(ripe, sub));
    BufferPacker packer = new BufferPacker(ripeBufferHandler);
    fillBuffers(buffers, 0, 1, 2);

    packer.process(buffers.get(0), 0);
    packer.process(buffers.get(1), 1);
    assertEquals(1, output.size());

    packer.process(buffers.get(2), 1);
    assertEquals(1, output.size());

    packer.drain();
    assertEquals(2, output.size());

    List<Buffer> unpacked = new ArrayList<>();
    for (int i = 0; i < output.size(); i++) {
      Pair<ByteBuf, Integer> pair = output.get(i);
      assertEquals(Integer.valueOf(i), pair.getRight());
      unpacked.addAll(BufferPacker.unpack(pair.getLeft()));
    }

    checkIfCompressed(unpacked, true, true, false);
    checkDataType(unpacked, EVENT_BUFFER, DATA_BUFFER, DATA_BUFFER);
    verifyBuffers(unpacked, 0, 1, 2);
    unpacked.forEach(Buffer::recycleBuffer);
  }

  @Test
  public void testUnpackedBuffers() throws Exception {
    List<Buffer> buffers = requestBuffers(3);
    setCompressed(buffers, true, true, false);
    setDataType(buffers, EVENT_BUFFER, DATA_BUFFER, DATA_BUFFER);

    List<Pair<ByteBuf, Integer>> output = new ArrayList<>();
    BufferPacker.BiConsumerWithException<ByteBuf, Integer, InterruptedException> ripeBufferHandler =
        (ripe, sub) -> output.add(Pair.of(ripe, sub));
    BufferPacker packer = new BufferPacker(ripeBufferHandler);
    fillBuffers(buffers, 0, 1, 2);

    packer.process(buffers.get(0), 0);
    packer.process(buffers.get(1), 1);
    assertEquals(1, output.size());

    packer.process(buffers.get(2), 2);
    assertEquals(2, output.size());

    packer.drain();
    assertEquals(3, output.size());

    List<Buffer> unpacked = new ArrayList<>();
    for (int i = 0; i < output.size(); i++) {
      Pair<ByteBuf, Integer> pair = output.get(i);
      assertEquals(Integer.valueOf(i), pair.getRight());
      unpacked.addAll(BufferPacker.unpack(pair.getLeft()));
    }

    checkIfCompressed(unpacked, true, true, false);
    checkDataType(unpacked, EVENT_BUFFER, DATA_BUFFER, DATA_BUFFER);
    verifyBuffers(unpacked, 0, 1, 2);
    unpacked.forEach(Buffer::recycleBuffer);
  }

  @Test
  public void testFailedToHandleRipeBufferAndClose() throws Exception {
    List<Buffer> buffers = requestBuffers(1);
    setCompressed(buffers, false);
    setDataType(buffers, DATA_BUFFER);
    fillBuffers(buffers, 0);

    BufferPacker.BiConsumerWithException<ByteBuf, Integer, InterruptedException> ripeBufferHandler =
        (ripe, sub) -> {
          // ripe.release();
          throw new RuntimeException("Test");
        };
    BufferPacker packer = new BufferPacker(ripeBufferHandler);
    System.out.println(buffers.get(0).refCnt());
    packer.process(buffers.get(0), 0);
    try {
      packer.drain();
    } catch (RuntimeException e) {
      e.printStackTrace();
    } catch (Exception e) {
      throw e;
    }

    // this should never throw any exception
    packer.close();
    assertEquals(0, bufferPool.bestEffortGetNumOfUsedBuffers());
  }

  private List<Buffer> requestBuffers(int n) {
    List<Buffer> buffers = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      Buffer buffer = bufferPool.requestBuffer();
      buffers.add(buffer);
    }
    return buffers;
  }

  private void setCompressed(List<Buffer> buffers, boolean... values) {
    for (int i = 0; i < buffers.size(); i++) {
      buffers.get(i).setCompressed(values[i]);
    }
  }

  private void setDataType(List<Buffer> buffers, Buffer.DataType... values) {
    for (int i = 0; i < buffers.size(); i++) {
      buffers.get(i).setDataType(values[i]);
    }
  }

  private void checkIfCompressed(List<Buffer> buffers, boolean... values) {
    for (int i = 0; i < buffers.size(); i++) {
      assertEquals(values[i], buffers.get(i).isCompressed());
    }
  }

  private void checkDataType(List<Buffer> buffers, Buffer.DataType... values) {
    for (int i = 0; i < buffers.size(); i++) {
      assertEquals(values[i], buffers.get(i).getDataType());
    }
  }

  private void fillBuffers(List<Buffer> buffers, int... ints) {
    for (int i = 0; i < buffers.size(); i++) {
      Buffer buffer = buffers.get(i);
      ByteBuf target = buffer.asByteBuf();
      BufferUtils.setBufferHeader(target, buffer.getDataType(), buffer.isCompressed(), 4);
      target.writerIndex(BufferUtils.HEADER_LENGTH);
      target.writeInt(ints[i]);
    }
  }

  private void verifyBuffers(List<Buffer> buffers, int... expects) {
    for (int i = 0; i < buffers.size(); i++) {
      ByteBuf actual = buffers.get(i).asByteBuf();
      assertEquals(expects[i], actual.getInt(0));
    }
  }
}
