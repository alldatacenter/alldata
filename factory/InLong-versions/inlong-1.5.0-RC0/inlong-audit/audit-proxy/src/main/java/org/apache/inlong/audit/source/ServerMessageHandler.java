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

package org.apache.inlong.audit.source;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import org.apache.flume.Event;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.AbstractSource;
import org.apache.inlong.audit.protocol.AuditApi.AuditMessageBody;
import org.apache.inlong.audit.protocol.AuditApi.AuditReply;
import org.apache.inlong.audit.protocol.AuditApi.AuditReply.RSP_CODE;
import org.apache.inlong.audit.protocol.AuditApi.AuditRequest;
import org.apache.inlong.audit.protocol.AuditApi.BaseCommand;
import org.apache.inlong.audit.protocol.AuditData;
import org.apache.inlong.audit.protocol.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Server message handler
 */
public class ServerMessageHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMessageHandler.class);
    private static final Gson GSON = new Gson();

    private final ChannelGroup allChannels;
    private final ChannelProcessor processor;
    private final ServiceDecoder serviceDecoder;
    private final int maxConnections;

    public ServerMessageHandler(AbstractSource source, ServiceDecoder serviceDecoder,
            ChannelGroup allChannels, Integer maxCons) {
        this.processor = source.getChannelProcessor();
        this.serviceDecoder = serviceDecoder;
        this.allChannels = allChannels;
        this.maxConnections = maxCons;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (allChannels.size() - 1 >= maxConnections) {
            ctx.channel().disconnect();
            ctx.channel().close();
            LOGGER.warn("refuse to connect to channel: {}, connections={}, maxConnections={}",
                    ctx.channel(), allChannels.size() - 1, maxConnections);
        }
        allChannels.add(ctx.channel());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();
        allChannels.remove(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            LOGGER.warn("get null message event, just skip");
            return;
        }
        ByteBuf buf = (ByteBuf) msg;
        int len = buf.readableBytes();
        if (len == 0) {
            LOGGER.warn("receive message skip empty msg");
            buf.clear();
            return;
        }
        Channel remoteChannel = ctx.channel();
        BaseCommand cmd;
        try {
            cmd = serviceDecoder.extractData(buf, remoteChannel);
        } catch (Exception ex) {
            LOGGER.error("extract data error: ", ex);
            throw new IOException(ex);
        }
        if (cmd == null) {
            LOGGER.warn("extract data from received msg is null");
            return;
        }

        ByteBuf channelBuffer = null;
        switch (cmd.getType()) {
            case PING:
                checkArgument(cmd.hasPing());
                channelBuffer = Commands.getPongChannelBuffer();
                break;
            case PONG:
                checkArgument(cmd.hasPong());
                channelBuffer = Commands.getPingChannelBuffer();
                break;
            case AUDIT_REQUEST:
                checkArgument(cmd.hasAuditRequest());
                AuditReply auditReply = handleRequest(cmd.getAuditRequest());
                channelBuffer = Commands.getAuditReplyBuffer(auditReply);
                break;
            case AUDIT_REPLY:
                checkArgument(cmd.hasAuditReply());
                break;
            default:
        }
        if (channelBuffer != null) {
            writeResponse(remoteChannel, channelBuffer);
        }
    }

    private AuditReply handleRequest(AuditRequest auditRequest) throws Exception {
        if (auditRequest == null) {
            throw new Exception("audit request cannot be null");
        }
        AuditReply reply = AuditReply.newBuilder()
                .setRequestId(auditRequest.getRequestId())
                .setRspCode(RSP_CODE.SUCCESS)
                .build();
        List<AuditMessageBody> bodyList = auditRequest.getMsgBodyList();
        int errorMsgBody = 0;
        for (AuditMessageBody auditMessageBody : bodyList) {
            AuditData auditData = new AuditData();
            auditData.setIp(auditRequest.getMsgHeader().getIp());
            auditData.setThreadId(auditRequest.getMsgHeader().getThreadId());
            auditData.setDockerId(auditRequest.getMsgHeader().getDockerId());
            auditData.setPacketId(auditRequest.getMsgHeader().getPacketId());
            auditData.setSdkTs(auditRequest.getMsgHeader().getSdkTs());

            auditData.setLogTs(auditMessageBody.getLogTs());
            auditData.setAuditId(auditMessageBody.getAuditId());
            auditData.setCount(auditMessageBody.getCount());
            auditData.setDelay(auditMessageBody.getDelay());
            auditData.setInlongGroupId(auditMessageBody.getInlongGroupId());
            auditData.setInlongStreamId(auditMessageBody.getInlongStreamId());
            auditData.setSize(auditMessageBody.getSize());

            try {
                byte[] body = GSON.toJson(auditData).getBytes(StandardCharsets.UTF_8);
                Event event = EventBuilder.withBody(body, null);
                processor.processEvent(event);
            } catch (Throwable ex) {
                LOGGER.error("writing data error, discard it: ", ex);
                errorMsgBody++;
            }
        }

        if (errorMsgBody != 0) {
            reply = reply.toBuilder()
                    .setMessage("writing data error, discard it, error body count=" + errorMsgBody)
                    .setRspCode(RSP_CODE.FAILED)
                    .build();
        }

        return reply;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("exception caught", cause);
    }

    private void writeResponse(Channel channel, ByteBuf buffer) throws Exception {
        if (channel.isWritable()) {
            channel.writeAndFlush(buffer);
            return;
        }

        String msg = String.format("remote channel=%s is not writable, please check remote client!", channel);
        LOGGER.warn(msg);
        throw new Exception(msg);
    }

}
