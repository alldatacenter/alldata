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
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.drill.exec.proto.GeneralRPCProtos.RpcHeader;

/**
 * Converts a previously length adjusted buffer into an RpcMessage.
 */
class RpcDecoder extends MessageToMessageDecoder<ByteBuf> {
  final org.slf4j.Logger logger;

  private final AtomicLong messageCounter = new AtomicLong();

  public RpcDecoder(String name) {
    this.logger = org.slf4j.LoggerFactory.getLogger(RpcDecoder.class.getCanonicalName() + "-" + name);
  }


  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
    if (!ctx.channel().isOpen()) {
      return;
    }

    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug("Inbound rpc message received.");
    }

    // now, we know the entire message is in the buffer and the buffer is constrained to this message. Additionally,
    // this process should avoid reading beyond the end of this buffer so we inform the ByteBufInputStream to throw an
    // exception if be go beyond readable bytes (as opposed to blocking).
    final ByteBufInputStream is = new ByteBufInputStream(buffer, buffer.readableBytes());

    // read the rpc header, saved in delimited format.
    checkTag(is, RpcEncoder.HEADER_TAG);
    final RpcHeader header = RpcHeader.parseDelimitedFrom(is);

    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug(" post header read index {}", buffer.readerIndex());
    }

    // read the protobuf body into a buffer.
    checkTag(is, RpcEncoder.PROTOBUF_BODY_TAG);
    final int pBodyLength = readRawVarint32(is);
    final ByteBuf pBody = buffer.slice(buffer.readerIndex(), pBodyLength);
    buffer.skipBytes(pBodyLength);
    pBody.retain(1);
    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug("Read protobuf body of length {} into buffer {}.", pBodyLength, pBody);
    }

    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug("post protobufbody read index {}", buffer.readerIndex());
    }

    ByteBuf dBody = null;
    int dBodyLength = 0;

    // read the data body.
    if (buffer.readableBytes() > 0) {

      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug("Reading raw body, buffer has {} bytes available, is available {}.", buffer.readableBytes(), is.available());
      }
      checkTag(is, RpcEncoder.RAW_BODY_TAG);
      dBodyLength = readRawVarint32(is);
      if (buffer.readableBytes() != dBodyLength) {
        throw new CorruptedFrameException(String.format("Expected to receive a raw body of %d bytes but received a buffer with %d bytes.", dBodyLength, buffer.readableBytes()));
      }
      dBody = buffer.slice();
      dBody.retain(1);
      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug("Read raw body of {}", dBody);
      }

    }else{
      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug("No need to read raw body, no readable bytes left.");
      }
    }


    // return the rpc message.
    InboundRpcMessage m = new InboundRpcMessage(header.getMode(), header.getRpcType(), header.getCoordinationId(),
        pBody, dBody);

    // move the reader index forward so the next rpc call won't try to work with it.
    buffer.skipBytes(dBodyLength);
    messageCounter.incrementAndGet();
    if (RpcConstants.SOME_DEBUGGING) {
      logger.debug("Inbound Rpc Message Decoded {}.", m);
    }
    out.add(m);

  }

  private void checkTag(ByteBufInputStream is, int expectedTag) throws IOException {
    int actualTag = readRawVarint32(is);
    if (actualTag != expectedTag) {
      throw new CorruptedFrameException(String.format("Expected to read a tag of %d but actually received a value of %d.  Happened after reading %d message.", expectedTag, actualTag, messageCounter.get()));
    }
  }

  // Taken from CodedInputStream and modified to enable ByteBufInterface.
  public static int readRawVarint32(ByteBufInputStream is) throws IOException {
    byte tmp = is.readByte();
    if (tmp >= 0) {
      return tmp;
    }
    int result = tmp & 0x7f;
    if ((tmp = is.readByte()) >= 0) {
      result |= tmp << 7;
    } else {
      result |= (tmp & 0x7f) << 7;
      if ((tmp = is.readByte()) >= 0) {
        result |= tmp << 14;
      } else {
        result |= (tmp & 0x7f) << 14;
        if ((tmp = is.readByte()) >= 0) {
          result |= tmp << 21;
        } else {
          result |= (tmp & 0x7f) << 21;
          result |= (tmp = is.readByte()) << 28;
          if (tmp < 0) {
            // Discard upper 32 bits.
            for (int i = 0; i < 5; i++) {
              if (is.readByte() >= 0) {
                return result;
              }
            }
            throw new CorruptedFrameException("Encountered a malformed varint.");
          }
        }
      }
    }
    return result;
  }

}
