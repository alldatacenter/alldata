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

package org.apache.celeborn.plugin.flink.buffer;

import java.nio.ByteBuffer;

import org.apache.flink.shaded.netty4.io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.flink.shaded.netty4.io.netty.buffer.UnpooledDirectByteBuf;

/**
 * We use {@link UnpooledDirectByteBuf} directly to reduce one copy from {@link Buffer} to Netty's
 * {@link org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf} when transmitting data over the
 * network.
 */
public class Buffer extends UnpooledDirectByteBuf {

  private final ByteBuffer buffer;

  private final BufferRecycler recycler;

  public Buffer(ByteBuffer buffer, BufferRecycler recycler, int readableBytes) {
    super(UnpooledByteBufAllocator.DEFAULT, buffer, buffer.capacity());

    this.buffer = buffer;
    this.recycler = recycler;
    writerIndex(readableBytes);
  }

  @Override
  protected void deallocate() {
    buffer.clear();
    recycler.recycle(buffer);
  }
}
