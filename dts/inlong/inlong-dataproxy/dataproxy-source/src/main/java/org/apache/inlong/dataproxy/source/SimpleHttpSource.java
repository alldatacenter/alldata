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

package org.apache.inlong.dataproxy.source;

import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.utils.ConfStringUtils;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.flume.Context;
import org.apache.flume.conf.Configurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Simple tcp source
 */
public class SimpleHttpSource extends BaseSource implements Configurable {

    private static final Logger logger = LoggerFactory.getLogger(SimpleHttpSource.class);

    private ServerBootstrap bootstrap;
    private boolean tcpNoDelay;
    private int highWaterMark;

    public SimpleHttpSource() {
        super();
        ConfigManager.getInstance().regIPVisitConfigChgCallback(this);
    }

    @Override
    public void configure(Context context) {
        logger.info("Source {} context is {}", getName(), context);
        super.configure(context);
        // get tcp no-delay parameter
        this.tcpNoDelay = context.getBoolean(SourceConstants.SRCCXT_TCP_NO_DELAY,
                SourceConstants.VAL_DEF_TCP_NO_DELAY);
        // get tcp high watermark
        this.highWaterMark = ConfStringUtils.getIntValue(context,
                SourceConstants.SRCCXT_TCP_HIGH_WATER_MARK, SourceConstants.VAL_DEF_TCP_HIGH_WATER_MARK);
        Preconditions.checkArgument((this.highWaterMark >= SourceConstants.VAL_MIN_TCP_HIGH_WATER_MARK),
                SourceConstants.VAL_DEF_TCP_HIGH_WATER_MARK + " must be >= "
                        + SourceConstants.VAL_MIN_TCP_HIGH_WATER_MARK);
    }

    @Override
    public synchronized void startSource() {
        logger.info("start " + this.getName());
        // build accept group
        this.acceptorGroup = new NioEventLoopGroup(maxAcceptThreads,
                new DefaultThreadFactory(this.getName() + "-boss-group"));
        // build worker group
        this.workerGroup = new NioEventLoopGroup(maxWorkerThreads,
                new DefaultThreadFactory(this.getName() + "-worker-group"));
        // init boostrap
        bootstrap = new ServerBootstrap();
        if (conLinger >= 0) {
            bootstrap.option(ChannelOption.SO_LINGER, conLinger);
        }
        bootstrap.option(ChannelOption.SO_BACKLOG, conBacklog);
        bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddress);
        bootstrap.childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, tcpNoDelay);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, maxRcvBufferSize);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, maxSendBufferSize);
        bootstrap.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, highWaterMark);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.group(acceptorGroup, workerGroup);
        bootstrap.childHandler(this.getChannelInitializerFactory());
        try {
            if (srcHost == null) {
                channelFuture = bootstrap.bind(new InetSocketAddress(srcPort)).sync();
            } else {
                channelFuture = bootstrap.bind(new InetSocketAddress(srcHost, srcPort)).sync();
            }
        } catch (Exception e) {
            logger.error("Source {} bind ({}:{}) error, program will exit! e = {}",
                    this.getName(), srcHost, srcPort, e);
            System.exit(-1);
        }
        ConfigManager.getInstance().addSourceReportInfo(
                srcHost, String.valueOf(srcPort), getProtocolName().toUpperCase());
        logger.info("Source {} started at ({}:{})!", this.getName(), srcHost, srcPort);
    }

    @Override
    public synchronized void stop() {
        super.stop();
    }

    @Override
    public String getProtocolName() {
        return SourceConstants.SRC_PROTOCOL_TYPE_HTTP;
    }
}
