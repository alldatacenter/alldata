/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.audit.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

public class Decoder extends MessageToMessageDecoder<ByteBuf> {

    // Maximum return packet size
    private static final int MAX_RESPONSE_LENGTH = 8 * 1024 * 1024;

    /**
     * decoding
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer,
            List<Object> out) throws Exception {
        // Every time you need to read the complete package (that is, read to the end of the package),
        // otherwise only the first one will be parsed correctly,
        // which will adversely affect the parsing of the subsequent package
        buffer.markReaderIndex();
        // Packet composition: 4 bytes length content + ProtocolBuffer content
        int totalLen = buffer.readInt();
        // Respond to abnormal channel, interrupt in time to avoid stuck
        if (totalLen > MAX_RESPONSE_LENGTH) {
            ctx.channel().close();
            return;
        }
        // If the package is not complete, continue to wait for the return package
        if (buffer.readableBytes() < totalLen) {
            buffer.resetReaderIndex();
            return;
        }
        byte[] returnBuffer = new byte[totalLen];
        buffer.readBytes(returnBuffer, 0, totalLen);
        out.add(returnBuffer);
    }
}
