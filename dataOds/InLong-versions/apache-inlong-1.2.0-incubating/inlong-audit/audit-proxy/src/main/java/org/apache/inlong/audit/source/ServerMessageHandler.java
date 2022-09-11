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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
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

/**
 * Server message handler
 *
 */
public class ServerMessageHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServerMessageHandler.class);

    private static final String DEFAULT_REMOTE_IP_VALUE = "0.0.0.0";

    private AbstractSource source;
    private final ChannelGroup allChannels;
    private int maxConnections = Integer.MAX_VALUE;

    private final ChannelProcessor processor;
    private final ServiceDecoder serviceDecoder;

    private final Gson gson = new Gson();

    public ServerMessageHandler(AbstractSource source, ServiceDecoder serviceDecoder,
                                ChannelGroup allChannels, Integer maxCons) {
        this.source = source;
        this.processor = source.getChannelProcessor();
        this.serviceDecoder = serviceDecoder;
        this.allChannels = allChannels;
        this.maxConnections = maxCons;

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (allChannels.size() - 1 >= maxConnections) {
            logger.warn("refuse to connect , and connections=" + (allChannels.size() - 1)
                    + ", maxConnections="
                    + maxConnections + ",channel is " + ctx.channel());
            ctx.channel().disconnect();
            ctx.channel().close();
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
        logger.debug("message received");
        if (msg == null) {
            logger.warn("get null message event, just skip");
            return;
        }
        ByteBuf cb = (ByteBuf) msg;
        int len = cb.readableBytes();
        if (len == 0) {
            logger.warn("receive message skip empty msg.");
            cb.clear();
            return;
        }
        Channel remoteChannel = ctx.channel();
        BaseCommand cmd = null;
        try {
            cmd = serviceDecoder.extractData(cb, remoteChannel);
        } catch (Exception ex) {
            logger.error("extractData has error e {}", ex);
            throw new IOException(ex.getCause());
        }

        if (cmd == null) {
            logger.warn("receive message extractData is null");
            return;
        }
        ByteBuf channelBuffer = null;
        switch (cmd.getType()) {
            case PING:
                checkArgument(cmd.hasPing());
                channelBuffer  = Commands.getPongChannelBuffer();
                break;
            case PONG:
                checkArgument(cmd.hasPong());
                channelBuffer  = Commands.getPingChannelBuffer();
                break;
            case AUDITREQUEST:
                checkArgument(cmd.hasAuditRequest());
                AuditReply auditReply = handleRequest(cmd.getAuditRequest());
                channelBuffer  = Commands.getAuditReplylBuffer(auditReply);
                break;
            case AUDITREPLY:
                checkArgument(cmd.hasAuditReply());
                break;
            default:
                channelBuffer = null;
        }
        if (channelBuffer != null) {
            writeResponse(remoteChannel, channelBuffer);
        }
    }

    private AuditReply handleRequest(AuditRequest auditRequest) {
        AuditReply reply = null;
        if (auditRequest != null) {
            List<AuditMessageBody> bodyList = auditRequest.getMsgBodyList();
            if (bodyList != null) {
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

                    byte[] body = null;
                    try {
                        body = gson.toJson(auditData).getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        logger.error("UnsupportedEncodingException = {}", e);
                    }
                    if (body != null) {
                        Event event = null;
                        try {
                            event = EventBuilder.withBody(body, null);
                            processor.processEvent(event);
                        } catch (Throwable ex) {
                            logger.error("Error writing to controller,data will discard.", ex);
                            errorMsgBody++;
                        }
                    }
                }
                if (errorMsgBody != 0) {
                    reply = AuditReply.newBuilder().setRequestId(auditRequest.getRequestId())
                            .setMessage("Error writing to controller,data "
                            + "will discard. error body num = "
                            + errorMsgBody).setRspCode(RSP_CODE.FAILED).build();
                }
            }
        }
        if (reply == null) {
            reply = AuditReply.newBuilder().setRequestId(auditRequest.getRequestId())
                    .setRspCode(RSP_CODE.SUCCESS).build();
        }
        return reply;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("exception caught", cause);
    }

    private void writeResponse(Channel remoteChannel, ByteBuf buffer) throws Exception {
        if (remoteChannel.isWritable()) {
            remoteChannel.writeAndFlush(buffer);
        } else {
            logger.warn(
                    "the send buffer2 is full, so disconnect it!please check remote client"
                            + "; Connection info:" + remoteChannel);
            throw new Exception(new Throwable(
                    "the send buffer2 is full,so disconnect it!please check remote client, Connection info:"
                            + remoteChannel));
        }
    }
}
