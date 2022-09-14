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

package org.apache.inlong.sdk.dataproxy.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.inlong.sdk.dataproxy.codec.EncodeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler extends SimpleChannelInboundHandler<EncodeObject> {
    private static final Logger logger = LoggerFactory
            .getLogger(ClientHandler.class);

    private final Sender sender;
    private final ClientMgr clientMgr;

    public ClientHandler(Sender sender, ClientMgr clientMgr) {
        this.sender = sender;
        this.clientMgr = clientMgr;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, EncodeObject e) {
        try {
            EncodeObject encodeObject = e;
            logger.debug("Channel = {} , msgType = {}", ctx.channel(), encodeObject.getMsgtype());
            if (encodeObject.getMsgtype() != 8) {
                sender.notifyFeedback(ctx.channel(), encodeObject);
            } else {
                clientMgr.notifyHBAck(ctx.channel(), encodeObject.getLoad());
            }
        } catch (Exception ex) {
            logger.error("error :", ex);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        logger.error("this channel {} has error! , reason is {} ", ctx.channel(), e.getCause());
        try {
            clientMgr.setConnectionFrozen(ctx.channel());
        } catch (Exception e1) {
            logger.error("exceptionCaught error :", e1);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        // clientMgr.resetClient(e.getChannel());
        logger.info("ClientHandler channelDisconnected {}", ctx.channel());
        try {
            sender.notifyConnectionDisconnected(ctx.channel());
        } catch (Exception e1) {
            logger.error("exceptionCaught error {}", e1.getMessage());
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        // clientMgr.resetClient(e.getChannel());
        logger.info("ClientHandler channelDisconnected {}", ctx.channel());
        try {
            sender.notifyConnectionDisconnected(ctx.channel());
        } catch (Exception e1) {
            logger.error("exceptionCaught error {}", e1.getMessage());
        }
    }
}
