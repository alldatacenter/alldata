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

package org.apache.inlong.sdk.dataproxy.pb;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * SdkSenderClientHandler
 */
public class SdkSenderClientHandler extends SimpleChannelHandler {

    public static final Logger LOG = LoggerFactory.getLogger(SdkSenderClientHandler.class);

    private SdkProxyChannelManager manager;

    /**
     * Constructor
     * 
     * @param manager
     */
    public SdkSenderClientHandler(SdkProxyChannelManager manager) {
        this.manager = manager;
    }

    /**
     * channelConnected
     * 
     * @param  ctx
     * @param  e
     * @throws Exception
     */
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        LOG.info("proxyClusterId:{},connect success:{}", manager.getProxyClusterId(), e);
    }

    /**
     * messageReceived
     * 
     * @param  ctx
     * @param  e
     * @throws Exception
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        try {
            manager.onMessageReceived(ctx, e);
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    /**
     * exceptionCaught
     * 
     * @param  ctx
     * @param  e
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        LOG.error("proxyClusterId:{},exceptionCaught error:{}", manager.getProxyClusterId(),
                e.getCause());
        if (e.getCause() != null) {
            LOG.error(e.getCause().getMessage(), e.getCause());
        }
        try {
            Channel channel = this.getChannel(ctx, e);
            manager.setChannelException(channel);
            super.exceptionCaught(ctx, e);
        } catch (Throwable ex) {
            ex.printStackTrace();
            LOG.error(ex.getMessage(), ex);
        }
    }

    /**
     * channelDisconnected
     * 
     * @param  ctx
     * @param  e
     * @throws Exception
     */
    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        LOG.info("proxyClusterId:{},channel disconnect:{}", manager.getProxyClusterId(), e);
        try {
            Channel channel = this.getChannel(ctx, e);
            manager.setChannelException(channel);
            super.channelDisconnected(ctx, e);
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    /**
     * channelClosed
     * 
     * @param  ctx
     * @param  e
     * @throws Exception
     */
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        LOG.info("proxyClusterId:{},connect close:{}", manager.getProxyClusterId(), e);
        try {
            Channel channel = this.getChannel(ctx, e);
            manager.setChannelException(channel);
            super.channelClosed(ctx, e);
        } catch (Throwable ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    /**
     * writeComplete
     * 
     * @param  ctx
     * @param  e
     * @throws Exception
     */
    @Override
    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
        super.writeComplete(ctx, e);
    }

    /**
     * getChannel
     * 
     * @param  ctx
     * @param  e
     * @return
     */
    private Channel getChannel(ChannelHandlerContext ctx, ChannelEvent e) {
        Channel channel = ctx.getChannel();
        if (channel == null) {
            channel = e.getChannel();
        }
        return channel;
    }
}
