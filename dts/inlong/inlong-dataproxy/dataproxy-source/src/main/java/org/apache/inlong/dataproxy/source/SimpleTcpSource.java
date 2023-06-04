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

import com.google.common.base.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.util.Iterator;

import org.apache.flume.Context;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.holder.ConfigUpdateCallback;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.utils.AddressUtils;
import org.apache.inlong.dataproxy.utils.EventLoopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple tcp source
 *
 */
public class SimpleTcpSource extends BaseSource
        implements
            Configurable,
            ConfigUpdateCallback,
            EventDrivenSource {

    private static final Logger logger = LoggerFactory.getLogger(SimpleTcpSource.class);

    private static int TRAFFIC_CLASS_TYPE_0 = 0;

    private static int TRAFFIC_CLASS_TYPE_96 = 96;

    private static int HIGH_WATER_MARK_DEFAULT_VALUE = 64 * 1024;

    private boolean tcpNoDelay = true;

    private boolean keepAlive = true;

    private int highWaterMark;

    private int trafficClass;

    protected String topic;

    private ServerBootstrap bootstrap;

    public SimpleTcpSource() {
        super();
        ConfigManager.getInstance().regIPVisitConfigChgCallback(this);
    }

    @Override
    public synchronized void startSource() {
        logger.info("start " + this.getName());

        logger.info("Set max workers : {} ;", maxThreads);

        acceptorThreadFactory = new DefaultThreadFactory("tcpSource-nettyBoss-threadGroup");

        this.acceptorGroup = EventLoopUtil.newEventLoopGroup(
                acceptorThreads, false, acceptorThreadFactory);

        this.workerGroup = EventLoopUtil
                .newEventLoopGroup(maxThreads, enableBusyWait,
                        new DefaultThreadFactory("tcpSource-nettyWorker-threadGroup"));

        bootstrap = new ServerBootstrap();

        bootstrap.childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, tcpNoDelay);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, keepAlive);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, receiveBufferSize);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, sendBufferSize);
        // serverBootstrap.childOption("child.trafficClass", trafficClass);
        bootstrap.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, highWaterMark);
        bootstrap.channel(EventLoopUtil.getServerSocketChannelClass(workerGroup));
        EventLoopUtil.enableTriggeredMode(bootstrap);
        bootstrap.group(acceptorGroup, workerGroup);
        logger.info("load msgFactory=" + msgFactoryName
                + " and serviceDecoderName=" + serviceDecoderName);

        ChannelInitializer fac = this.getChannelInitializerFactory();
        bootstrap.childHandler(fac);
        try {
            if (host == null) {
                channelFuture = bootstrap.bind(new InetSocketAddress(port)).sync();
            } else {
                channelFuture = bootstrap.bind(new InetSocketAddress(host, port)).sync();
            }
        } catch (Exception e) {
            logger.error("Simple TCP Source error bind host {} port {},program will exit! e = {}",
                    host, port, e);
            System.exit(-1);
        }
        ConfigManager.getInstance().addSourceReportInfo(
                host, String.valueOf(port), getProtocolName().toUpperCase());
        logger.info("Simple TCP Source started at host {}, port {}", host, port);
    }

    @Override
    public synchronized void stop() {
        super.stop();
    }

    @Override
    public void configure(Context context) {
        logger.info("context is {}", context);
        super.configure(context);
        tcpNoDelay = context.getBoolean(ConfigConstants.TCP_NO_DELAY, true);
        keepAlive = context.getBoolean(ConfigConstants.KEEP_ALIVE, true);
        highWaterMark = context.getInteger(ConfigConstants.HIGH_WATER_MARK, HIGH_WATER_MARK_DEFAULT_VALUE);
        receiveBufferSize = context.getInteger(ConfigConstants.RECEIVE_BUFFER_SIZE, RECEIVE_BUFFER_DEFAULT_SIZE);
        if (receiveBufferSize > RECEIVE_BUFFER_MAX_SIZE) {
            receiveBufferSize = RECEIVE_BUFFER_MAX_SIZE;
        }
        Preconditions.checkArgument(receiveBufferSize > BUFFER_SIZE_MUST_THAN,
                "receiveBufferSize must be > 0");

        sendBufferSize = context.getInteger(ConfigConstants.SEND_BUFFER_SIZE, SEND_BUFFER_DEFAULT_SIZE);
        if (sendBufferSize > SEND_BUFFER_MAX_SIZE) {
            sendBufferSize = SEND_BUFFER_MAX_SIZE;
        }
        Preconditions.checkArgument(sendBufferSize > BUFFER_SIZE_MUST_THAN,
                "sendBufferSize must be > 0");

        trafficClass = context.getInteger(ConfigConstants.TRAFFIC_CLASS, TRAFFIC_CLASS_TYPE_0);
        Preconditions.checkArgument((trafficClass == TRAFFIC_CLASS_TYPE_0
                || trafficClass == TRAFFIC_CLASS_TYPE_96),
                "trafficClass must be == 0 or == 96");

        try {
            maxThreads = context.getInteger(ConfigConstants.MAX_THREADS, 32);
        } catch (NumberFormatException e) {
            logger.warn("Simple TCP Source max-threads property must specify an integer value. {}",
                    context.getString(ConfigConstants.MAX_THREADS));
        }
    }

    @Override
    public String getProtocolName() {
        return "tcp";
    }

    @Override
    public void update() {
        // check current all links
        if (ConfigManager.getInstance().needChkIllegalIP()) {
            int cnt = 0;
            Channel channel;
            String strRemoteIP;
            long startTime = System.currentTimeMillis();
            Iterator<Channel> iterator = allChannels.iterator();
            while (iterator.hasNext()) {
                channel = iterator.next();
                strRemoteIP = AddressUtils.getChannelRemoteIP(channel);
                if (strRemoteIP == null) {
                    continue;
                }
                if (ConfigManager.getInstance().isIllegalIP(strRemoteIP)) {
                    channel.disconnect();
                    channel.close();
                    allChannels.remove(channel);
                    cnt++;
                    logger.error(strRemoteIP + " is Illegal IP, so disconnect it !");
                }
            }
            logger.info("Channel check, {} disconnects {} Illegal channels, waist {} ms",
                    getName(), cnt, (System.currentTimeMillis() - startTime));
        }
    }

}
