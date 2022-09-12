/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import static org.apache.inlong.tubemq.corebase.utils.AddressUtils.getRemoteAddressIP;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corerpc.RpcConstants;
import org.apache.inlong.tubemq.corerpc.RpcDataPack;
import org.apache.inlong.tubemq.corerpc.exception.UnknownProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyProtocolDecoder extends MessageToMessageDecoder<ByteBuf> {
    private static final Logger logger = LoggerFactory.getLogger(NettyProtocolDecoder.class);

    private static final ConcurrentHashMap<String, AtomicLong> errProtolAddrMap =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> errSizeAddrMap =
            new ConcurrentHashMap<>();
    private static AtomicLong lastProtolTime = new AtomicLong(0);
    private static AtomicLong lastSizeTime = new AtomicLong(0);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        if (buffer.readableBytes() < 12) {
            logger.warn("Decode buffer.readableBytes() < 12 !");
            return;
        }
        int frameToken = buffer.readInt();
        filterIllegalPkgToken(frameToken,
                RpcConstants.RPC_PROTOCOL_BEGIN_TOKEN, ctx.channel());
        int serialNo = buffer.readInt();
        int tmpListSize = buffer.readInt();
        filterIllegalPackageSize(true, tmpListSize,
                RpcConstants.MAX_FRAME_MAX_LIST_SIZE, ctx.channel());
        RpcDataPack dataPack = new RpcDataPack(serialNo, new ArrayList<ByteBuffer>());
        // get PackBody
        int i = 0;
        while (i < tmpListSize) {
            i++;
            if (buffer.readableBytes() < 4) {
                logger.warn("Decode buffer.readableBytes() < 4 !");
                break;
            }
            buffer.markReaderIndex();
            int length = buffer.readInt();
            filterIllegalPackageSize(false, length,
                    RpcConstants.RPC_MAX_BUFFER_SIZE, ctx.channel());
            ByteBuffer bb = ByteBuffer.allocate(length);
            buffer.readBytes(bb);
            bb.flip();
            dataPack.getDataLst().add(bb);
        }

        if (dataPack.getDataLst().size() == tmpListSize) {
            out.add(dataPack);
        } else {
            logger.warn("Decode dataPack.getDataLst().size()[{}] != tmpListSize [{}] !",
                    dataPack.getDataLst().size(), tmpListSize);
            return;
        }
    }

    private void filterIllegalPkgToken(int inParamValue,
                                       int allowTokenVal, Channel channel) throws UnknownProtocolException {
        if (inParamValue != allowTokenVal) {
            String rmtaddrIp = getRemoteAddressIP(channel);
            if (rmtaddrIp != null) {
                AtomicLong count = errProtolAddrMap.get(rmtaddrIp);
                if (count == null) {
                    AtomicLong tmpCount = new AtomicLong(0);
                    count = errProtolAddrMap.putIfAbsent(rmtaddrIp, tmpCount);
                    if (count == null) {
                        count = tmpCount;
                    }
                }
                count.incrementAndGet();
                long befTime = lastProtolTime.get();
                long curTime = System.currentTimeMillis();
                if (curTime - befTime > 180000) {
                    if (lastProtolTime.compareAndSet(befTime, System.currentTimeMillis())) {
                        logger.warn("[Abnormal Visit] OSS Tube visit list is :" + errProtolAddrMap.toString());
                        errProtolAddrMap.clear();
                    }
                }
            }
            throw new UnknownProtocolException(new StringBuilder(256)
                    .append("Unknown protocol exception for message frame, channel.address = ")
                    .append(channel.remoteAddress().toString()).toString());
        }
    }

    private void filterIllegalPackageSize(boolean isFrameSize, int inParamValue,
                                          int allowSize, Channel channel) throws UnknownProtocolException {
        if (inParamValue < 0 || inParamValue > allowSize) {
            String rmtaddrIp = getRemoteAddressIP(channel);
            if (rmtaddrIp != null) {
                AtomicLong count = errSizeAddrMap.get(rmtaddrIp);
                if (count == null) {
                    AtomicLong tmpCount = new AtomicLong(0);
                    count = errSizeAddrMap.putIfAbsent(rmtaddrIp, tmpCount);
                    if (count == null) {
                        count = tmpCount;
                    }
                }
                count.incrementAndGet();
                long befTime = lastSizeTime.get();
                long curTime = System.currentTimeMillis();
                if (curTime - befTime > 180000) {
                    if (lastSizeTime.compareAndSet(befTime, System.currentTimeMillis())) {
                        logger.warn("[Abnormal Visit] Abnormal BodySize visit list is :" + errSizeAddrMap.toString());
                        errSizeAddrMap.clear();
                    }
                }
            }
            StringBuilder sBuilder = new StringBuilder(256)
                    .append("Unknown protocol exception for message listSize! channel.address = ")
                    .append(channel.remoteAddress().toString());
            if (isFrameSize) {
                sBuilder.append(", Max list size=").append(allowSize)
                        .append(", request's list size=").append(inParamValue);
            } else {
                sBuilder.append(", Max buffer size=").append(allowSize)
                        .append(", request's buffer size=").append(inParamValue);
            }
            throw new UnknownProtocolException(sBuilder.toString());
        }
    }

}
