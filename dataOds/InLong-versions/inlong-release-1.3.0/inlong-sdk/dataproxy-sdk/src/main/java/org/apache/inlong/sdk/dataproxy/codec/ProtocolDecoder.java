/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.nio.charset.StandardCharsets;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolDecoder extends MessageToMessageDecoder<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx,
            ByteBuf buffer, List<Object> out) throws Exception {
        buffer.markReaderIndex();
        // totallen
        int totalLen = buffer.readInt();
        logger.debug("decode totalLen : {}", totalLen);
        if (totalLen != buffer.readableBytes()) {
            logger.error("totalLen is not equal readableBytes.total:" + totalLen
                    + ";readableBytes:" + buffer.readableBytes());
            buffer.resetReaderIndex();
            throw new Exception("totalLen is not equal readableBytes.total");
        }
        // msgtype
        int msgType = buffer.readByte() & 0x1f;

        if (msgType == 4) {
            logger.info("debug decode");
        }
        if (msgType == 3 | msgType == 5) {
            // bodylen
            int bodyLength = buffer.readInt();
            if (bodyLength >= totalLen) {
                logger.error("bodyLen is greater than totalLen.totalLen:" + totalLen
                        + ";bodyLen:" + bodyLength);
                buffer.resetReaderIndex();
                throw new Exception("bodyLen is greater than totalLen.totalLen");
            }
            byte[] bodyBytes = null;
            if (bodyLength > 0) {
                bodyBytes = new byte[bodyLength];
                buffer.readBytes(bodyBytes);
            }

            // attrlen
            int attrLength = buffer.readInt();
            byte[] attrBytes = null;
            if (attrLength > 0) {
                attrBytes = new byte[attrLength];
                buffer.readBytes(attrBytes);
            }
            EncodeObject object = new EncodeObject(bodyBytes, new String(attrBytes,
                    StandardCharsets.UTF_8));
            object.setMsgtype(5);
            out.add(object);
        } else if (msgType == 7) {

            int seqId = buffer.readInt();
            int attrLen = buffer.readShort();
            EncodeObject object = new EncodeObject();
            object.setMessageId(String.valueOf(seqId));

            if (attrLen == 4) {
                int errorValue = buffer.readInt();
                ErrorCode errorCode = ErrorCode.valueOf(errorValue);
                if (errorCode != null) {
                    object.setException(true);
                    object.setExceptionError(errorCode);
                }
            } else {
                byte[] attrContent = new byte[attrLen];
                buffer.readBytes(attrContent);
            }

            buffer.readShort();

            object.setMsgtype(msgType);
            out.add(object);

        } else if (msgType == 8) {
            int attrlen = buffer.getShort(4 + 1 + 4 + 1 + 4 + 2);
            buffer.skipBytes(13 + attrlen + 2);
            EncodeObject object = new EncodeObject();
            object.setMsgtype(8);
            object.setLoad(buffer.getShort(4 + 1 + 4 + 1 + 4));
            out.add(object);
        }
    }
}
