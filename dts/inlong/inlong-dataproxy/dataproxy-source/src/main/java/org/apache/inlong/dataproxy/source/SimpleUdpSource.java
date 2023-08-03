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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.flume.Context;
import org.apache.flume.conf.Configurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class SimpleUdpSource extends BaseSource implements Configurable {

    private static final Logger logger = LoggerFactory
            .getLogger(SimpleUdpSource.class);

    private Bootstrap bootstrap;

    public SimpleUdpSource() {
        super();
    }

    @Override
    public void configure(Context context) {
        logger.info("Source {} context is {}", getName(), context);
        super.configure(context);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void startSource() {
        // setup Netty server
        logger.info("start " + this.getName());
        bootstrap = new Bootstrap();
        bootstrap.channel(NioDatagramChannel.class);
        if (conLinger >= 0) {
            bootstrap.option(ChannelOption.SO_LINGER, conLinger);
        }
        bootstrap.option(ChannelOption.SO_BACKLOG, conBacklog);
        bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddress);
        bootstrap.option(ChannelOption.SO_RCVBUF, maxRcvBufferSize);
        bootstrap.option(ChannelOption.SO_SNDBUF, maxSendBufferSize);
        bootstrap.handler(this.getChannelInitializerFactory());
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
    public void stop() {
        super.stop();
    }

    @Override
    public String getProtocolName() {
        return SourceConstants.SRC_PROTOCOL_TYPE_UDP;
    }
}
