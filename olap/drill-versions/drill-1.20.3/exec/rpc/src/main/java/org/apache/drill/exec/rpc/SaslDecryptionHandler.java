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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.apache.drill.exec.exception.OutOfMemoryException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Handler to Decrypt the input ByteBuf. It expects input to be in format where it has length of the bytes to
 * decode in network order and actual encrypted bytes. The handler reads the length and then reads the
 * required bytes to pass it to unwrap function for decryption. The decrypted buffer is copied to a new
 * ByteBuf and added to out list.
 * <p>
 * Example:
 * <li>Input - [EBLN1, EB1, EBLN2, EB2] --> ByteBuf with repeated combination of encrypted byte length
 *             in network order (EBLNx) and encrypted bytes (EB)
 * <li>Output - [DB1] --> Decrypted ByteBuf of first chunk.(EB1)
 * </p>
 */
class SaslDecryptionHandler extends MessageToMessageDecoder<ByteBuf> {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(
      SaslDecryptionHandler.class.getCanonicalName());

  private final SaslCodec saslCodec;

  private final int maxWrappedSize;

  private final OutOfMemoryHandler outOfMemoryHandler;

  private final byte[] encodedMsg;

  private final ByteBuffer lengthOctets;

  SaslDecryptionHandler(SaslCodec saslCodec, int maxWrappedSize, OutOfMemoryHandler oomHandler) {
    this.saslCodec = saslCodec;
    this.outOfMemoryHandler = oomHandler;
    this.maxWrappedSize = maxWrappedSize;

    // Allocate the byte array of maxWrappedSize to reuse for each encoded packet received on this connection.
    // Size of this buffer depends upon the configuration encryption.sasl.max_wrapped_size
    encodedMsg = new byte[maxWrappedSize];
    lengthOctets = ByteBuffer.allocate(RpcConstants.LENGTH_FIELD_LENGTH);
    lengthOctets.order(ByteOrder.BIG_ENDIAN);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    super.handlerAdded(ctx);
    logger.trace("Added " + RpcConstants.SASL_DECRYPTION_HANDLER + " handler");
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    super.handlerRemoved(ctx);
    logger.trace("Removed " + RpcConstants.SASL_DECRYPTION_HANDLER + " handler");
  }

  public void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws IOException {

    if (!ctx.channel().isOpen()) {
      logger.trace("Channel closed before decoding the message of {} bytes", msg.readableBytes());
      msg.skipBytes(msg.readableBytes());
      return;
    }

    try {
      if(logger.isTraceEnabled()) {
        logger.trace("Trying to decrypt the encrypted message of size: {} with maxWrappedSize", msg.readableBytes());
      }


      // All the encrypted blocks are prefixed with it's length in network byte order (or BigEndian format). Netty's
      // default Byte order of ByteBuf is Little Endian, so we cannot just do msg.getInt() as that will read the 4
      // octets in little endian format.
      //
      // We will read the length of one complete encrypted chunk and decode that.
      msg.getBytes(msg.readerIndex(), lengthOctets.array(), 0, RpcConstants.LENGTH_FIELD_LENGTH);
      final int wrappedMsgLength = lengthOctets.getInt(0);
      msg.skipBytes(RpcConstants.LENGTH_FIELD_LENGTH);

      // Since lengthBasedFrameDecoder will ensure we have enough bytes it's good to have this check here.
      assert(msg.readableBytes() == wrappedMsgLength);

      // Uncomment the below code if msg can contain both of Direct and Heap ByteBuf. Currently Drill only supports
      // DirectByteBuf so the below condition will always be false. If the msg are always HeapByteBuf then in
      // addition also remove the allocation of encodedMsg from constructor.
      /*if (msg.hasArray()) {
        wrappedMsg = msg.array();
      } else {
      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug("The input bytebuf is not backed by a byte array so allocating a new one");
      }*/

      // Check if the wrappedMsgLength doesn't exceed agreed upon maxWrappedSize. As per SASL RFC 2222/4422 we
      // should close the connection since it represents a security attack.
      if (wrappedMsgLength > maxWrappedSize) {
        throw new RpcException(String.format("Received encoded buffer size: %d is larger than negotiated " +
            "maxWrappedSize: %d. Closing the connection as this is unexpected.", wrappedMsgLength, maxWrappedSize));
      }

      final byte[] wrappedMsg = encodedMsg;
      // Copy the wrappedMsgLength of bytes into the byte array
      msg.getBytes(msg.readerIndex(), wrappedMsg, 0, wrappedMsgLength);
      //}

      // SASL library always copies the origMsg internally to a new byte array
      // and return another new byte array after decrypting the message. The memory for this
      // will be Garbage collected by JVM since SASL Library releases it's reference after
      // returning the byte array.
      final byte[] decodedMsg = saslCodec.unwrap(wrappedMsg, 0, wrappedMsgLength);

      if(logger.isTraceEnabled()) {
        logger.trace("Successfully decrypted incoming message. Length after decryption: {}", decodedMsg.length);
      }

      // Update the msg reader index since we have decrypted this chunk
      msg.skipBytes(wrappedMsgLength);

      // Allocate a new Bytebuf to copy the decrypted chunk.
      final ByteBuf decodedMsgBuf = ctx.alloc().buffer(decodedMsg.length);
      decodedMsgBuf.writeBytes(decodedMsg);

      // Add the decrypted chunk to output buffer for next handler to take care of it.
      out.add(decodedMsgBuf);

    } catch (OutOfMemoryException e) {
      logger.warn("Failure allocating buffer on incoming stream due to memory limits.");
      msg.resetReaderIndex();
      outOfMemoryHandler.handle();
    } catch (IOException e) {
      logger.error("Something went wrong while unwrapping the message: {} with MaxEncodeSize: {} and " +
          "error: {}", msg, maxWrappedSize, e.getMessage());
      throw e;
    }
  }
}
