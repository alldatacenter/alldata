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

package org.apache.celeborn.plugin.flink.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.shaded.netty4.io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.network.protocol.Message;
import org.apache.celeborn.common.network.util.FrameDecoder;
import org.apache.celeborn.plugin.flink.protocol.ReadData;

public class TransportFrameDecoderWithBufferSupplier extends ChannelInboundHandlerAdapter
    implements FrameDecoder {
  public static final Logger logger =
      LoggerFactory.getLogger(TransportFrameDecoderWithBufferSupplier.class);
  private int msgSize = -1;
  private int bodySize = -1;
  private Message.Type curType = Message.Type.UNKNOWN_TYPE;
  private ByteBuf headerBuf = Unpooled.buffer(HEADER_SIZE, HEADER_SIZE);
  private io.netty.buffer.CompositeByteBuf bodyBuf = null;
  private ByteBuf externalBuf = null;
  private final ByteBuf msgBuf = Unpooled.buffer(8);
  private Message curMsg = null;
  private int remainingSize = -1;

  private final ConcurrentHashMap<Long, Supplier<ByteBuf>> bufferSuppliers;

  public TransportFrameDecoderWithBufferSupplier(
      ConcurrentHashMap<Long, Supplier<ByteBuf>> bufferSuppliers) {
    this.bufferSuppliers = bufferSuppliers;
  }

  private void copyByteBuf(io.netty.buffer.ByteBuf source, ByteBuf target, int targetSize) {
    int bytes = Math.min(source.readableBytes(), targetSize - target.readableBytes());
    target.writeBytes(source.readSlice(bytes).nioBuffer());
  }

  private void decodeHeader(io.netty.buffer.ByteBuf buf, ChannelHandlerContext ctx) {
    copyByteBuf(buf, headerBuf, HEADER_SIZE);
    if (!headerBuf.isWritable()) {
      msgSize = headerBuf.readInt();
      if (msgBuf.capacity() < msgSize) {
        msgBuf.capacity(msgSize);
      }
      msgBuf.clear();
      curType = Message.Type.decode(headerBuf.nioBuffer());
      // type byte is read
      headerBuf.readByte();
      bodySize = headerBuf.readInt();
      decodeMsg(buf, ctx);
    }
  }

  private void decodeMsg(io.netty.buffer.ByteBuf buf, ChannelHandlerContext ctx) {
    if (msgBuf.readableBytes() < msgSize) {
      copyByteBuf(buf, msgBuf, msgSize);
    }
    if (msgBuf.readableBytes() == msgSize) {
      curMsg = MessageDecoderExt.decode(curType, msgBuf, false);
      if (bodySize <= 0) {
        ctx.fireChannelRead(curMsg);
        clear();
      }
    }
  }

  private io.netty.buffer.ByteBuf decodeBody(
      io.netty.buffer.ByteBuf buf, ChannelHandlerContext ctx) {
    if (bodyBuf == null) {
      if (buf.readableBytes() >= bodySize) {
        io.netty.buffer.ByteBuf body = buf.retain().readSlice(bodySize);
        curMsg.setBody(body);
        ctx.fireChannelRead(curMsg);
        clear();
        return buf;
      } else {
        bodyBuf = buf.alloc().compositeBuffer(Integer.MAX_VALUE);
      }
    }
    int remaining = bodySize - bodyBuf.readableBytes();
    io.netty.buffer.ByteBuf next;
    if (remaining >= buf.readableBytes()) {
      next = buf;
      buf = null;
    } else {
      next = buf.retain().readSlice(remaining);
    }
    bodyBuf.addComponent(next).writerIndex(bodyBuf.writerIndex() + next.readableBytes());
    if (bodyBuf.readableBytes() == bodySize) {
      curMsg.setBody(bodyBuf);
      ctx.fireChannelRead(curMsg);
      clear();
    }
    return buf;
  }

  private io.netty.buffer.ByteBuf decodeBodyCopyOut(
      io.netty.buffer.ByteBuf buf, ChannelHandlerContext ctx) {
    if (remainingSize > 0) {
      dropUnusedBytes(buf);
      return buf;
    }

    ReadData readData = (ReadData) curMsg;
    long streamId = readData.getStreamId();
    if (externalBuf == null) {
      Supplier<ByteBuf> supplier = bufferSuppliers.get(streamId);
      if (supplier == null) {
        return needDropUnusedBytes(streamId, buf);
      } else {
        try {
          externalBuf = supplier.get();
        } catch (Exception e) {
          return needDropUnusedBytes(streamId, buf);
        }
      }
    }

    copyByteBuf(buf, externalBuf, bodySize);
    if (externalBuf.readableBytes() == bodySize) {
      ((ReadData) curMsg).setFlinkBuffer(externalBuf);
      ctx.fireChannelRead(curMsg);
      clear();
    }
    return buf;
  }

  private io.netty.buffer.ByteBuf needDropUnusedBytes(
      long streamId, io.netty.buffer.ByteBuf byteBuf) {
    logger.warn("Need drop unused bytes, streamId: {}, bodySize: {}", streamId, bodySize);
    remainingSize = bodySize;
    dropUnusedBytes(byteBuf);
    return byteBuf;
  }

  private void dropUnusedBytes(io.netty.buffer.ByteBuf source) {
    if (source.readableBytes() > 0) {
      if (remainingSize > source.readableBytes()) {
        remainingSize = remainingSize - source.readableBytes();
        source.skipBytes(source.readableBytes());
      } else {
        source.skipBytes(remainingSize);
        clear();
      }
    }
  }

  public void channelRead(ChannelHandlerContext ctx, Object data) {
    io.netty.buffer.ByteBuf nettyBuf = (io.netty.buffer.ByteBuf) data;
    try {
      while (nettyBuf != null && nettyBuf.isReadable()) {
        if (headerBuf.isWritable()) {
          decodeHeader(nettyBuf, ctx);
        } else if (curMsg == null) {
          decodeMsg(nettyBuf, ctx);
        } else if (bodySize > 0) {
          if (curMsg.needCopyOut()) {
            // Only read data will enter this branch
            nettyBuf = decodeBodyCopyOut(nettyBuf, ctx);
          } else {
            nettyBuf = decodeBody(nettyBuf, ctx);
          }
        }
      }
    } finally {
      if (nettyBuf != null) {
        nettyBuf.release();
      }
    }
  }

  private void clear() {
    externalBuf = null;
    curMsg = null;
    curType = Message.Type.UNKNOWN_TYPE;
    headerBuf.clear();
    bodyBuf = null;
    bodySize = -1;
    remainingSize = -1;
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    super.channelInactive(ctx);
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    clear();

    headerBuf.release();
    msgBuf.release();
    super.handlerRemoved(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);
  }
}
