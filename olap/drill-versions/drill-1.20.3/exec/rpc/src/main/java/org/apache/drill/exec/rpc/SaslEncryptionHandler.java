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
import org.apache.drill.exec.exception.OutOfMemoryException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;


/**
 * Handler to wrap the input Composite ByteBuf components separately and append the encrypted length for each
 * component in the output ByteBuf. If there are multiple components in the input ByteBuf then each component will be
 * encrypted individually and added to output ByteBuf with it's length prepended.
 * <p>
 * Example:
 * <li>Input ByteBuf  --> [B1,B2] - 2 component ByteBuf of 16K byte each.
 * <li>Output ByteBuf --> [[EBLN1, EB1], [EBLN2, EB2]] - List of ByteBuf's with each ByteBuf containing
 *                    Encrypted Byte Length (EBLNx) in network order as per SASL RFC and Encrypted Bytes (EBx).
 * </p>
 */
class SaslEncryptionHandler extends MessageToMessageEncoder<ByteBuf> {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(
      SaslEncryptionHandler.class.getCanonicalName());

  private final SaslCodec saslCodec;

  private final int wrapSizeLimit;

  private byte[] origMsgBuffer;

  private final ByteBuffer lengthOctets;

  private final OutOfMemoryHandler outOfMemoryHandler;

  /**
   * We don't provide preference to allocator to use heap buffer instead of direct buffer.
   * Drill uses it's own buffer allocator which doesn't support heap buffer allocation. We use
   * Drill buffer allocator in the channel.
   */
  SaslEncryptionHandler(SaslCodec saslCodec, final int wrapSizeLimit, final OutOfMemoryHandler oomHandler) {
    this.saslCodec = saslCodec;
    this.wrapSizeLimit = wrapSizeLimit;
    this.outOfMemoryHandler = oomHandler;

    // The maximum size of the component will be wrapSizeLimit. Since this is maximum size, we can allocate once
    // and reuse it for each component encode.
    origMsgBuffer = new byte[this.wrapSizeLimit];
    lengthOctets = ByteBuffer.allocate(RpcConstants.LENGTH_FIELD_LENGTH);
    lengthOctets.order(ByteOrder.BIG_ENDIAN);
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    super.handlerAdded(ctx);
    logger.trace("Added " + RpcConstants.SASL_ENCRYPTION_HANDLER + " handler!");
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    super.handlerRemoved(ctx);
    logger.trace("Removed " + RpcConstants.SASL_ENCRYPTION_HANDLER + " handler");
  }

  public void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws IOException {

    if (!ctx.channel().isOpen()) {
      logger.debug("In " + RpcConstants.SASL_ENCRYPTION_HANDLER + " and channel is not open. " +
          "So releasing msg memory before encryption.");
      msg.release();
      return;
    }

    try {
      // If encryption is enabled then this handler will always get ByteBuf of type Composite ByteBuf
      assert(msg instanceof CompositeByteBuf);

      final CompositeByteBuf cbb = (CompositeByteBuf) msg;
      final int numComponents = cbb.numComponents();

      // Get all the components inside the Composite ByteBuf for encryption
      for(int currentIndex = 0; currentIndex < numComponents; ++currentIndex) {
        final ByteBuf component = cbb.component(currentIndex);

        // Each component ByteBuf size should not be greater than wrapSizeLimit since ChunkCreationHandler
        // will break the RPC message into chunks of wrapSizeLimit.
        if (component.readableBytes() > wrapSizeLimit) {
          throw new RpcException(String.format("Component Chunk size: %d is greater than the wrapSizeLimit: %d",
              component.readableBytes(), wrapSizeLimit));
        }

        // Uncomment the below code if msg can contain both of Direct and Heap ByteBuf. Currently Drill only supports
        // DirectByteBuf so the below condition will always be false. If the msg are always HeapByteBuf then in
        // addition also remove the allocation of origMsgBuffer from constructor.
        /*if (component.hasArray()) {
          origMsg = component.array();
        } else {

        if (RpcConstants.EXTRA_DEBUGGING) {
          logger.trace("The input bytebuf is not backed by a byte array so allocating a new one");
        }*/
        final byte[] origMsg = origMsgBuffer;
        component.getBytes(component.readerIndex(), origMsg, 0, component.readableBytes());
        //}

        if (logger.isTraceEnabled()) {
          logger.trace("Trying to encrypt chunk of size:{} with wrapSizeLimit:{}",
              component.readableBytes(), wrapSizeLimit);
        }

        // Length to encrypt will be component length not origMsg length since that can be greater.
        final byte[] wrappedMsg = saslCodec.wrap(origMsg, 0, component.readableBytes());

        if(logger.isTraceEnabled()) {
          logger.trace("Successfully encrypted message, original size: {} Final Size: {}",
              component.readableBytes(), wrappedMsg.length);
        }

        // Allocate the buffer (directByteBuff) for copying the encrypted byte array and 4 octets for length of the
        // encrypted message. This is preferred since later on if the passed buffer is not in direct memory then it
        // will be copied by the channel into a temporary direct memory which will be cached to the thread. The size
        // of that temporary direct memory will be size of largest message send.
        final ByteBuf encryptedBuf = ctx.alloc().buffer(wrappedMsg.length + RpcConstants.LENGTH_FIELD_LENGTH);

        // Based on SASL RFC 2222/4422 we should have starting 4 octet as the length of the encrypted buffer in network
        // byte order. SASL framework provided by JDK doesn't do that by default and leaves it upto application. Whereas
        // Cyrus SASL implementation of sasl_encode does take care of this.
        lengthOctets.putInt(wrappedMsg.length);
        encryptedBuf.writeBytes(lengthOctets.array());

        // reset the position for re-use in next round
        lengthOctets.rewind();

        // Write the encrypted bytes inside the buffer
        encryptedBuf.writeBytes(wrappedMsg);

        // Update the msg and component reader index
        msg.skipBytes(component.readableBytes());
        component.skipBytes(component.readableBytes());

        // Add the encrypted buffer into the output to send it on wire.
        out.add(encryptedBuf);
      }
    } catch (OutOfMemoryException e) {
      logger.warn("Failure allocating buffer on incoming stream due to memory limits.");
      msg.resetReaderIndex();
      outOfMemoryHandler.handle();
    } catch (IOException e) {
      logger.error("Something went wrong while wrapping the message: {} with MaxRawWrapSize: {}, " +
          "and error: {}", msg, wrapSizeLimit, e.getMessage());
      throw e;
    }
  }
}
