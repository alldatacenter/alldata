/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.dataproxy.source;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleUdpSource
        extends BaseSource
        implements EventDrivenSource, Configurable {

    private static final Logger logger = LoggerFactory
            .getLogger(SimpleUdpSource.class);

    private static int UPD_BUFFER_DEFAULT_SIZE = 8192;

    private Bootstrap bootstrap;

    public SimpleUdpSource() {
        super();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void startSource() {
        // setup Netty server
        bootstrap = new Bootstrap();
        logger.info("Set max workers : {} ;",maxThreads);
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.option(ChannelOption.SO_RCVBUF, receiveBufferSize);
        bootstrap.option(ChannelOption.SO_SNDBUF, sendBufferSize);
        ChannelInitializer fac = this.getChannelInitializerFactory();
        bootstrap.handler(fac);
        try {
            if (host == null) {
                channelFuture = bootstrap.bind(new InetSocketAddress(port)).sync();
            } else {

                channelFuture = bootstrap.bind(new InetSocketAddress(host, port)).sync();
            }
        } catch (Exception e) {
            logger.error("Simple UDP Source error bind host {} port {}, program will exit!",
                    new Object[] { host, port});
            System.exit(-1);
            //throw new FlumeException(e.getMessage());
        }
        logger.info("Simple UDP Source started at host {}, port {}", host, port);
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void configure(Context context) {
        super.configure(context);
        try {
            maxThreads = context.getInteger(ConfigConstants.MAX_THREADS, 32);
        } catch (NumberFormatException e) {
            logger.warn("Simple UDP Source max-threads property must specify an integer value.",
                    context.getString(ConfigConstants.MAX_THREADS));
        }
    }

    @Override
    public String getProtocolName() {
        return "udp";
    }
}
