/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.rpc;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.min;

/**
 * Handler that converts an input ByteBuf into chunk size ByteBuf's and add it to the
 * CompositeByteBuf as individual components. If encryption is enabled, this is always
 * added in the channel pipeline.
 */
class ChunkCreationHandler extends MessageToMessageEncoder<ByteBuf> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(
      ChunkCreationHandler.class.getCanonicalName());

  private final int chunkSize;

  ChunkCreationHandler(int chunkSize) {
    checkArgument(chunkSize > 0);
    this.chunkSize = chunkSize;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    super.handlerAdded(ctx);
    logger.trace("Added " + RpcConstants.CHUNK_CREATION_HANDLER + " handler!");
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    super.handlerRemoved(ctx);
    logger.trace("Removed " + RpcConstants.CHUNK_CREATION_HANDLER + " handler");
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {

    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug("ChunkCreationHandler called with msg {} of size {} with chunkSize {}",
          msg, msg.readableBytes(), chunkSize);
    }

    if (!ctx.channel().isOpen()) {
      logger.debug("Channel closed, skipping encode inside {}.", RpcConstants.CHUNK_CREATION_HANDLER);
      msg.release();
      return;
    }

    // Calculate the number of chunks based on configured chunk size and input msg size
    int numChunks = (int) Math.ceil((double) msg.readableBytes() / chunkSize);

    // Initialize a composite buffer to hold numChunks chunk.
    final CompositeByteBuf cbb = ctx.alloc().compositeBuffer(numChunks);

    int cbbWriteIndex = 0;
    int currentChunkLen = min(msg.readableBytes(), chunkSize);

    // Create slices of chunkSize from input msg and add it to the composite buffer.
    while (numChunks > 0) {
      final ByteBuf chunkBuf = msg.slice(msg.readerIndex(), currentChunkLen);
      chunkBuf.retain();
      cbb.addComponent(chunkBuf);
      cbbWriteIndex += currentChunkLen;
      msg.skipBytes(currentChunkLen);
      --numChunks;
      currentChunkLen = min(msg.readableBytes(), chunkSize);
    }

    // Update the writerIndex of composite byte buffer. Netty doesn't do it automatically.
    cbb.writerIndex(cbbWriteIndex);

    // Add the final composite bytebuf into output buffer.
    out.add(cbb);
  }
}