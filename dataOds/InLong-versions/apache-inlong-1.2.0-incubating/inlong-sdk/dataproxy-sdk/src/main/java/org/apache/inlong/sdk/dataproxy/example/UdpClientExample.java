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

package org.apache.inlong.sdk.dataproxy.example;

import static org.apache.inlong.sdk.dataproxy.ConfigConstants.FLAG_ALLOW_COMPRESS;
import static org.apache.inlong.sdk.dataproxy.ConfigConstants.FLAG_ALLOW_ENCRYPT;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Random;
import org.apache.inlong.sdk.dataproxy.codec.EncodeObject;
import org.apache.inlong.sdk.dataproxy.config.EncryptConfigEntry;
import org.apache.inlong.sdk.dataproxy.config.EncryptInfo;
import org.apache.inlong.sdk.dataproxy.network.SequentialID;
import org.apache.inlong.sdk.dataproxy.network.Utils;
import org.apache.inlong.sdk.dataproxy.utils.EncryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

public class UdpClientExample {

    private static final Logger logger = LoggerFactory.getLogger(UdpClientExample.class);

    private static SequentialID idGenerator = new SequentialID(Utils.getLocalIp());

    public static void main(String[] args) {
        long sentCount = 10;
        String groupId = "test_group_id";
        String streamId = "test_stream_id";
        String busIp = "127.0.0.1";
        int busPort = 46802;
        String attr = "";
        UdpClientExample demo = new UdpClientExample();
        Channel channel = demo.initUdpChannel();
        /*
         * It is recommended to use msg type 7. For others, please refer to the official related
         * documents
         * Therefore, use type 7 to assemble the message.
         * For other types, please refer to the sdk source code
         */
        try {
            int count = 0;
            while (count < sentCount) {
                if (count % 1000 == 0) {
                    long seqId = idGenerator.getNextInt();
                    long dt = System.currentTimeMillis() / 1000;
                    EncodeObject encodeObject =
                            demo.getEncodeObject(7, false,
                                    false, false, dt, seqId, groupId,
                                    streamId, attr);
                    ByteBuf buffer = demo.getSendBuf(encodeObject);
                    demo.sendUdpMessage(channel, busIp, busPort, buffer);
                    TimeUnit.SECONDS.sleep(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean sendUdpMessage(Channel channel, String ip, int port, ByteBuf msg) {
        try {
            channel.writeAndFlush(new DatagramPacket(msg, new InetSocketAddress(ip, port))).sync();
            logger.info("send = [{}/{}]", ip, port);
        } catch (InterruptedException e) {
            logger.info("send has exception e = {}", e);
        }
        return true;
    }

    private EncodeObject getEncodeObject(int msgType, boolean isCompress, boolean isReport,
            boolean isGroupIdTransfer, long dt, long seqId, String groupId, String streamId,
            String attr) throws UnsupportedEncodingException {
        EncodeObject encodeObject =
                new EncodeObject(getRandomString(5).getBytes("UTF-8"), msgType,
                        isCompress,
                        isReport, isGroupIdTransfer, dt, seqId, groupId, streamId, attr);
        return encodeObject;
    }

    public ByteBuf getSendBuf(EncodeObject message) {
        ByteBuf buf = null;
        try {
            if (message.getMsgtype() == 7) {
                buf = writeToBuf7(message);
            }
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();
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
            if (object.getBodyBytes() != null && object.getBodyBytes().length != 0) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                ByteBuffer databuffer = ByteBuffer.allocate(4);
                databuffer.putInt(object.getBodyBytes().length);
                out.write(databuffer.array());
                out.write(object.getBodyBytes());
                body = out.toByteArray();
            }

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
                        endAttr = endAttr + "_userName=" + object.getUserName() + "&_encyVersion="
                                + encryptInfo.getVersion() + "&_encyDesKey="
                                + encryptInfo.getRsaEncryptedKey();
                        body = EncryptUtil.desEncrypt(body, encryptInfo.getDesKey());
                    }
                }
                if (!object.isGroupIdTransfer()) {
                    if (Utils.isNotBlank(endAttr)) {
                        endAttr = endAttr + "&";
                    }
                    endAttr = (endAttr + "bid=" + object.getGroupId() + "&tid="
                            + object.getStreamId());
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
                totalLength = totalLength + body.length
                        + endAttr.getBytes("utf8").length;
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
            int len = Snappy.compress(out.toByteArray(), 0,
                    out.size(), tmpData, 0);
            body = new byte[len];
            System.arraycopy(tmpData, 0, body, 0, len);
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();
        }
        return body;
    }

    public Channel initUdpChannel() {
        Channel channel = null;
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                    protected void channelRead0(ChannelHandlerContext var1,
                            DatagramPacket dmsg) throws Exception {
                        String msg = dmsg.content().toString(StandardCharsets.UTF_8);
                        System.out.println("from server:" + msg);
                    }
                });
        try {
            channel = bootstrap.bind(0).sync().channel();
        } catch (Exception e) {
            logger.error("Connection has exception e = {}", e);
        }
        return channel;
    }

    public static String getRandomString(int length) {
        StringBuffer sb = new StringBuffer();
        String string = "i am bus test client!";
        for (int i = 0; i < length; i++) {
            int number = new Random().nextInt(string.length());
            sb.append(string.charAt(number));
        }
        return sb.toString();
    }
}
