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

package org.apache.inlong.audit.send;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenderHandler extends SimpleChannelInboundHandler<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SenderHandler.class);
    private final SenderManager manager;

    /**
     * Constructor
     */
    public SenderHandler(SenderManager manager) {
        this.manager = manager;
    }

    /**
     * Message Received
     */
    @Override
    public void channelRead0(io.netty.channel.ChannelHandlerContext ctx, byte[] e) {
        try {
            manager.onMessageReceived(ctx, e);
        } catch (Throwable ex) {
            LOGGER.error("channelRead0 error: ", ex);
        }
    }

    /**
     * Caught exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        try {
            manager.onExceptionCaught(ctx, e);
        } catch (Throwable ex) {
            LOGGER.error("caught exception: ", ex);
        }
    }

    /**
     * Disconnected channel
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        try {
            super.channelInactive(ctx);
        } catch (Throwable ex) {
            LOGGER.error("channelInactive error: ", ex);
        }
    }

    /**
     * Closed channel
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        try {
            super.channelUnregistered(ctx);
        } catch (Throwable ex) {
            LOGGER.error("channelUnregistered error: ", ex);
        }
    }
}
