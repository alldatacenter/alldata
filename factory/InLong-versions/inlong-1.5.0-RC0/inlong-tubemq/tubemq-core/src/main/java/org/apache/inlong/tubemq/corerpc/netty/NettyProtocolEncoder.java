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

package org.apache.inlong.tubemq.corerpc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.inlong.tubemq.corerpc.RpcConstants;
import org.apache.inlong.tubemq.corerpc.RpcDataPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyProtocolEncoder extends MessageToMessageEncoder<RpcDataPack> {

    private static final Logger logger = LoggerFactory.getLogger(NettyProtocolEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext chx, RpcDataPack msg, List<Object> out) {
        RpcDataPack dataPack = msg;
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream()) {
            byteOut.write(getPackHeader(dataPack).array());
            List<ByteBuffer> origs = dataPack.getDataLst();
            Iterator<ByteBuffer> iter = origs.iterator();
            while (iter.hasNext()) {
                ByteBuffer entry = iter.next();
                byteOut.write(getLengthHeader(entry).array());
                byteOut.write(getLengthBody(entry));
            }
            byte[] body = byteOut.toByteArray();
            ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(body.length);
            buf.writeBytes(body);
            out.add(buf);
        } catch (IOException e) {
            logger.error("encode has exception ", e);
        }
    }

    private ByteBuffer getPackHeader(RpcDataPack dataPack) {
        ByteBuffer header = ByteBuffer.allocate(12);
        header.putInt(RpcConstants.RPC_PROTOCOL_BEGIN_TOKEN);
        header.putInt(dataPack.getSerialNo());
        header.putInt(dataPack.getDataLst().size());
        header.flip();
        return header;
    }

    private ByteBuffer getLengthHeader(ByteBuffer buf) {
        ByteBuffer header = ByteBuffer.allocate(4);
        header.putInt(buf.limit());
        header.flip();
        return header;
    }

    private byte[] getLengthBody(ByteBuffer buf) {
        return Arrays.copyOf(buf.array(), buf.limit());
    }
}
