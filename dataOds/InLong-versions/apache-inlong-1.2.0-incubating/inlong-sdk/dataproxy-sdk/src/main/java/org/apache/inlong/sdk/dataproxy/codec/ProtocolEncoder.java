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

import static org.apache.inlong.sdk.dataproxy.ConfigConstants.FLAG_ALLOW_AUTH;
import static org.apache.inlong.sdk.dataproxy.ConfigConstants.FLAG_ALLOW_COMPRESS;
import static org.apache.inlong.sdk.dataproxy.ConfigConstants.FLAG_ALLOW_ENCRYPT;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Iterator;

import java.util.List;
import org.apache.inlong.sdk.dataproxy.config.EncryptConfigEntry;
import org.apache.inlong.sdk.dataproxy.config.EncryptInfo;
import org.apache.inlong.sdk.dataproxy.network.Utils;
import org.apache.inlong.sdk.dataproxy.utils.EncryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

public class ProtocolEncoder extends MessageToMessageEncoder<EncodeObject> {
    private static final Logger logger = LoggerFactory
            .getLogger(ProtocolEncoder.class);

    protected void encode(ChannelHandlerContext ctx,
            EncodeObject message, List<Object> out) throws Exception {
        ByteBuf buf = null;
        try {
            EncodeObject object = message;
            if (object.getMsgtype() == 3) {
                buf = writeToBuf3(object);
            }
            if (object.getMsgtype() == 5) {
                buf = writeToBuf5(object);
            }

            if (object.getMsgtype() == 7) {
                buf = writeToBuf7(object);
            }
            if (object.getMsgtype() == 8) {
                buf = writeToBuf8(object);
            }
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();
        }
        if (buf != null) {
            out.add(buf);
        } else {
            logger.warn("write buf is null !");
        }
    }

    private ByteBuf writeToBuf8(EncodeObject object) {
        ByteBuf buf = null;
        try {
            String endAttr = object.getCommonattr();
            if (object.isAuth()) {
                if (Utils.isNotBlank(endAttr)) {
                    endAttr = endAttr + "&";
                }
                long timestamp = System.currentTimeMillis();
                int nonce = new SecureRandom(String.valueOf(timestamp).getBytes()).nextInt(Integer.MAX_VALUE);
                endAttr = endAttr + "_userName=" + object.getUserName() + "&_clientIP=" + Utils.getLocalIp()
                        + "&_signature=" + Utils.generateSignature(object.getUserName(),
                        timestamp, nonce, object.getSecretKey())
                        + "&_timeStamp=" + timestamp + "&_nonce=" + nonce;
            }
            if (Utils.isNotBlank(object.getMsgUUID())) {
                if (Utils.isNotBlank(endAttr)) {
                    endAttr = endAttr + "&";
                }
                endAttr = endAttr + "msgUUID=" + object.getMsgUUID();
            }
            int msgType = 8;
            if (object.isAuth()) {
                msgType |= FLAG_ALLOW_AUTH;
            }
            int totalLength = 1 + 4 + 1 + 4 + 2 + endAttr.getBytes("utf8").length + 2;
            buf = ByteBufAllocator.DEFAULT.buffer(4 + totalLength);
            buf.writeInt(totalLength);
            buf.writeByte(msgType);
            buf.writeInt((int) object.getDt());
            buf.writeByte(1);
            buf.writeInt(0);
            buf.writeShort(endAttr.getBytes("utf8").length);
            if (endAttr.getBytes("utf8").length > 0) {
                buf.writeBytes(endAttr.getBytes("utf8"));
            }
            buf.writeShort(0xee01);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return buf;
    }

    private ByteBuf constructBody(byte[] body, EncodeObject object,
        int totalLength, int cnt) throws UnsupportedEncodingException {
        ByteBuf buf = null;
        if (body != null) {
            if (object.isCompress()) {
                body = processCompress(body);
            }
            String endAttr = object.getCommonattr();
            if (object.isEncrypt()) {
                EncryptConfigEntry encryptEntry = object.getEncryptEntry();
                if (encryptEntry != null) {
                    if (Utils.isNotBlank(endAttr)) {
                        endAttr = endAttr + "&";
                    }
                    EncryptInfo encryptInfo = encryptEntry.getRsaEncryptInfo();
                    endAttr = endAttr + "_userName=" + object.getUserName()
                        + "&_encyVersion=" + encryptInfo.getVersion()
                        + "&_encyDesKey=" + encryptInfo.getRsaEncryptedKey();
                    body = EncryptUtil.desEncrypt(body, encryptInfo.getDesKey());
                }
            }
            if (!object.isGroupIdTransfer()) {
                if (Utils.isNotBlank(endAttr)) {
                    endAttr = endAttr + "&";
                }
                endAttr = (endAttr + "groupId=" + object.getGroupId() + "&streamId=" + object.getStreamId());
            }
            if (Utils.isNotBlank(object.getMsgUUID())) {
                if (Utils.isNotBlank(endAttr)) {
                    endAttr = endAttr + "&";
                }
                endAttr = endAttr + "msgUUID=" + object.getMsgUUID();
            }

            int msgType = 7;
            if (object.isEncrypt()) {
                msgType |= FLAG_ALLOW_ENCRYPT;
            }
            if (object.isCompress()) {
                msgType |= FLAG_ALLOW_COMPRESS;
            }
            totalLength = totalLength + body.length + endAttr.getBytes("utf8").length;
            buf = ByteBufAllocator.DEFAULT.buffer(4 + totalLength);
            buf.writeInt(totalLength);
            buf.writeByte(msgType);
            buf.writeShort(object.getGroupIdNum());
            buf.writeShort(object.getStreamIdNum());
            String bitStr = object.isSupportLF() ? "1" : "0";
            bitStr += (object.getMessageKey().equals("minute")) ? "1" : "0";
            bitStr += (object.getMessageKey().equals("file")) ? "1" : "0";
            bitStr += !object.isGroupIdTransfer() ? "1" : "0";
            bitStr += object.isReport() ? "1" : "0";
            bitStr += "0";
            buf.writeShort(Integer.parseInt(bitStr, 2));
            buf.writeInt((int) object.getDt());

            buf.writeShort(cnt);
            buf.writeInt(Integer.valueOf(object.getMessageId()));

            buf.writeInt(body.length);
            buf.writeBytes(body);

            buf.writeShort(endAttr.getBytes("utf8").length);
            buf.writeBytes(endAttr.getBytes("utf8"));
            buf.writeShort(0xee01);
        }
        return buf;
    }

    private ByteBuf writeToBuf7(EncodeObject object) {
        ByteBuf buf = null;
        try {
            int totalLength = 1 + 2 + 2 + 2 + 4 + 2 + 4 + 4 + 2 + 2;
            byte[] body = null;
            int cnt = 1;

            if (object.getBodylist() != null && object.getBodylist().size() != 0) {
                if (object.getCnt() > 0) {
                    cnt = object.getCnt();
                } else {
                    cnt = object.getBodylist().size();
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Iterator<byte[]> iter = object.getBodylist().iterator();

                if (object.isSupportLF()) {
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    int len = object.getBodylist().size();
                    for (int i = 0; i < len - 1; i++) {
                        data.write(object.getBodylist().get(i));
                        data.write("\n".getBytes("utf8"));
                    }
                    data.write(object.getBodylist().get(len - 1));
                    ByteBuffer databuffer = ByteBuffer.allocate(4);
                    databuffer.putInt(data.toByteArray().length);
                    out.write(databuffer.array());
                    out.write(data.toByteArray());
                } else {
                    while (iter.hasNext()) {
                        byte[] entry = iter.next();
                        ByteBuffer databuffer = ByteBuffer.allocate(4);
                        databuffer.putInt(entry.length);
                        out.write(databuffer.array());
                        out.write(entry);
                    }
                }
                body = out.toByteArray();
            }
            //send single message one time
            if (object.getBodyBytes() != null && object.getBodyBytes().length != 0) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                ByteBuffer databuffer = ByteBuffer.allocate(4);
                databuffer.putInt(object.getBodyBytes().length);
                out.write(databuffer.array());
                out.write(object.getBodyBytes());
                body = out.toByteArray();
            }

            buf = constructBody(body, object, totalLength, cnt);
        } catch (Exception e) {
            logger.error("writeToBuf7 has {}", e);
        }
        return buf;
    }

    private ByteBuf writeToBuf5(EncodeObject object) {
        ByteBuf buf = null;
        try {
            int totalLength = 1 + 4 + 4;
            byte[] body = null;

            //send multiple  messages one time
            if (object.getBodylist() != null && object.getBodylist().size() != 0) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Iterator<byte[]> iter = object.getBodylist().iterator();
                while (iter.hasNext()) {
                    byte[] entry = iter.next();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(4);
                    byteBuffer.putInt(entry.length);
                    out.write(byteBuffer.array());
                    out.write(entry);
                }
                body = out.toByteArray();
            }
            //send single message one time
            if (object.getBodyBytes() != null && object.getBodyBytes().length != 0) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteBuffer byteBuffer = ByteBuffer.allocate(4);
                byteBuffer.putInt(object.getBodyBytes().length);
                out.write(byteBuffer.array());
                out.write(object.getBodyBytes());
                body = out.toByteArray();
            }
            if (body != null) {
                String msgAttrs = object.getAttributes();
                if (object.isCompress()) {
                    body = processCompress(body);
                }
                if (object.isEncrypt()) {
                    EncryptConfigEntry encryptEntry = object.getEncryptEntry();
                    if (encryptEntry != null) {
                        if (Utils.isNotBlank(msgAttrs)) {
                            msgAttrs = msgAttrs + "&";
                        }
                        EncryptInfo encryptInfo = encryptEntry.getRsaEncryptInfo();
                        msgAttrs = msgAttrs + "_userName=" + object.getUserName()
                                + "&_encyVersion=" + encryptInfo.getVersion()
                                + "&_encyDesKey=" + encryptInfo.getRsaEncryptedKey();
                        body = EncryptUtil.desEncrypt(body, encryptInfo.getDesKey());
                    }
                }
                if (Utils.isNotBlank(object.getMsgUUID())) {
                    if (Utils.isNotBlank(msgAttrs)) {
                        msgAttrs = msgAttrs + "&";
                    }
                    msgAttrs = msgAttrs + "msgUUID=" + object.getMsgUUID();
                }

                int msgType = 5;
                if (object.isEncrypt()) {
                    msgType |= FLAG_ALLOW_ENCRYPT;
                }
                totalLength = totalLength + body.length + msgAttrs.getBytes("utf8").length;
                buf = ByteBufAllocator.DEFAULT.buffer(4 + totalLength);
                buf.writeInt(totalLength);
                buf.writeByte(msgType);
                buf.writeInt(body.length);
                buf.writeBytes(body);
                buf.writeInt(msgAttrs.getBytes("utf8").length);
                buf.writeBytes(msgAttrs.getBytes("utf8"));
            }
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();
        }
        return buf;
    }

    /*private ChannelBuffer writeToBuf4(EncodeObject object) {
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        try {
            int totalLength = 1 + 4 + 4;
            byte[] body = null;

            //send single message one time
            if (object.getBodyBytes() != null && object.getBodyBytes().length != 0) {
                body = object.getBodyBytes();
            }
            totalLength = totalLength + body.length + object.getAttributes().getBytes("utf8").length;
            buf.writeInt(totalLength);
            buf.writeByte(4);
            buf.writeInt(body.length);
            buf.writeBytes(body);
            buf.writeInt(object.getAttributes().getBytes().length);
            buf.writeBytes(object.getAttributes().getBytes());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return buf;
    }
    */

    private ByteBuf writeToBuf3(EncodeObject object) {
        ByteBuf buf = null;
        try {
            int totalLength = 1 + 4 + 4;
            byte[] body = null;

            //send multiple  messages one time
            if (object.getBodylist() != null && object.getBodylist().size() != 0) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Iterator<byte[]> iter = object.getBodylist().iterator();
                while (iter.hasNext()) {
                    byte[] entry = iter.next();
                    out.write(entry);
                    out.write("\n".getBytes("utf8"));
                }
                body = out.toByteArray();
            }
            //send single message one time
            if (object.getBodyBytes() != null && object.getBodyBytes().length != 0) {
                body = object.getBodyBytes();
            }
            if (body != null) {
                String msgAttrs = object.getAttributes();
                if (object.isCompress()) {
                    body = processCompress(body);
                }
                if (object.isEncrypt()) {
                    EncryptConfigEntry encryptEntry = object.getEncryptEntry();
                    if (encryptEntry != null) {
                        if (Utils.isNotBlank(msgAttrs)) {
                            msgAttrs = msgAttrs + "&";
                        }
                        EncryptInfo encryptInfo = encryptEntry.getRsaEncryptInfo();
                        msgAttrs = msgAttrs + "_userName=" + object.getUserName()
                                + "&_encyVersion=" + encryptInfo.getVersion()
                                + "&_encyDesKey=" + encryptInfo.getRsaEncryptedKey();
                        body = EncryptUtil.desEncrypt(body, encryptInfo.getDesKey());
                    }
                }
                if (Utils.isNotBlank(object.getMsgUUID())) {
                    if (Utils.isNotBlank(msgAttrs)) {
                        msgAttrs = msgAttrs + "&";
                    }
                    msgAttrs = msgAttrs + "msgUUID=" + object.getMsgUUID();
                }

                int msgType = 3;
                if (object.isEncrypt()) {
                    msgType |= FLAG_ALLOW_ENCRYPT;
                }
                totalLength = totalLength + body.length + msgAttrs.getBytes("utf8").length;
                buf = ByteBufAllocator.DEFAULT.buffer(4 + totalLength);
                buf.writeInt(totalLength);
                buf.writeByte(msgType);
                buf.writeInt(body.length);
                buf.writeBytes(body);
                buf.writeInt(msgAttrs.getBytes("utf8").length);
                buf.writeBytes(msgAttrs.getBytes("utf8"));
            }
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();
        }
        return buf;
    }

    private byte[] processCompress(byte[] body) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(body);
            int guessLen = Snappy.maxCompressedLength(out.size());
            byte[] tmpData = new byte[guessLen];
            int len = Snappy.compress(out.toByteArray(), 0, out.size(),
                    tmpData, 0);
            body = new byte[len];
            System.arraycopy(tmpData, 0, body, 0, len);
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();
        }
        return body;
    }
}
