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
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.io.OutputStream;
import java.util.List;

import org.apache.drill.exec.proto.GeneralRPCProtos.CompleteRpcMessage;
import org.apache.drill.exec.proto.GeneralRPCProtos.RpcHeader;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

/**
 * Converts an RPCMessage into wire format.
 */
class RpcEncoder extends MessageToMessageEncoder<OutboundRpcMessage>{
  final org.slf4j.Logger logger;

  static final int HEADER_TAG = makeTag(CompleteRpcMessage.HEADER_FIELD_NUMBER, WireFormat.WIRETYPE_LENGTH_DELIMITED);
  static final int PROTOBUF_BODY_TAG = makeTag(CompleteRpcMessage.PROTOBUF_BODY_FIELD_NUMBER, WireFormat.WIRETYPE_LENGTH_DELIMITED);
  static final int RAW_BODY_TAG = makeTag(CompleteRpcMessage.RAW_BODY_FIELD_NUMBER, WireFormat.WIRETYPE_LENGTH_DELIMITED);
  static final int HEADER_TAG_LENGTH = getRawVarintSize(HEADER_TAG);
  static final int PROTOBUF_BODY_TAG_LENGTH = getRawVarintSize(PROTOBUF_BODY_TAG);
  static final int RAW_BODY_TAG_LENGTH = getRawVarintSize(RAW_BODY_TAG);

  public RpcEncoder(String name) {
    this.logger = org.slf4j.LoggerFactory.getLogger(RpcEncoder.class.getCanonicalName() + "-" + name);
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, OutboundRpcMessage msg, List<Object> out) throws Exception {
    if (RpcConstants.EXTRA_DEBUGGING) {
      logger.debug("Rpc Encoder called with msg {}", msg);
    }

    if (!ctx.channel().isOpen()) {
      //output.add(ctx.alloc().buffer(0));
      logger.debug("Channel closed, skipping encode.");
      msg.release();
      return;
    }

    try{
      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug("Encoding outbound message {}", msg);
      }
      // first we build the RpcHeader
      RpcHeader header = RpcHeader.newBuilder() //
          .setMode(msg.mode) //
          .setCoordinationId(msg.coordinationId) //
          .setRpcType(msg.rpcType).build();

      // figure out the full length
      int headerLength = header.getSerializedSize();
      int protoBodyLength = msg.pBody.getSerializedSize();
      int rawBodyLength = msg.getRawBodySize();
      int fullLength = //
          HEADER_TAG_LENGTH + getRawVarintSize(headerLength) + headerLength +   //
          PROTOBUF_BODY_TAG_LENGTH + getRawVarintSize(protoBodyLength) + protoBodyLength; //

      if (rawBodyLength > 0) {
        fullLength += (RAW_BODY_TAG_LENGTH + getRawVarintSize(rawBodyLength) + rawBodyLength);
      }

      ByteBuf buf = ctx.alloc().buffer();
      OutputStream os = new ByteBufOutputStream(buf);
      CodedOutputStream cos = CodedOutputStream.newInstance(os);

      // write full length first (this is length delimited stream).
      cos.writeRawVarint32(fullLength);

      // write header
      cos.writeRawVarint32(HEADER_TAG);
      cos.writeRawVarint32(headerLength);
      header.writeTo(cos);

      // write protobuf body length and body
      cos.writeRawVarint32(PROTOBUF_BODY_TAG);
      cos.writeRawVarint32(protoBodyLength);
      msg.pBody.writeTo(cos);

      // if exists, write data body and tag.
      if (msg.getRawBodySize() > 0) {
        if(RpcConstants.EXTRA_DEBUGGING) {
          logger.debug("Writing raw body of size {}", msg.getRawBodySize());
        }

        cos.writeRawVarint32(RAW_BODY_TAG);
        cos.writeRawVarint32(rawBodyLength);
        cos.flush(); // need to flush so that dbody goes after if cos is caching.

        final CompositeByteBuf cbb = ctx.alloc().compositeBuffer(msg.dBodies.length + 1);
        cbb.addComponent(buf);
        int bufLength = buf.readableBytes();
        for (ByteBuf b : msg.dBodies) {
          cbb.addComponent(b);
          bufLength += b.readableBytes();
        }
        cbb.writerIndex(bufLength);
        out.add(cbb);
      } else {
        cos.flush();
        out.add(buf);
      }

      if (RpcConstants.SOME_DEBUGGING) {
        logger.debug("Wrote message length {}:{} bytes (head:body).  Message: " + msg, getRawVarintSize(fullLength), fullLength);
      }
      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug("Sent message.  Ending writer index was {}.", buf.writerIndex());
      }
    } finally {
      // make sure to release Rpc Messages underlying byte buffers.
      //msg.release();
    }
  }

  /** Makes a tag value given a field number and wire type, copied from WireFormat since it isn't public.  */
  static int makeTag(final int fieldNumber, final int wireType) {
    return (fieldNumber << 3) | wireType;
  }

  public static int getRawVarintSize(int value) {
    int count = 0;
    while (true) {
      if ((value & ~0x7F) == 0) {
        count++;
        return count;
      } else {
        count++;
        value >>>= 7;
      }
    }
  }

}
