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

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.source.AbstractSource;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.common.monitor.MonitorIndex;
import org.apache.inlong.common.monitor.MonitorIndexExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMessageFactory
        extends ChannelInitializer<SocketChannel> {

    private static final Logger LOG = LoggerFactory.getLogger(ServerMessageFactory.class);

    private static final int DEFAULT_READ_IDLE_TIME = 70 * 60 * 1000;

    private static long MAX_CHANNEL_MEMORY_SIZE = 1024 * 1024;

    private static long MAX_TOTAL_MEMORY_SIZE = 1024 * 1024;

    private static int MSG_LENGTH_LEN = 4;

    private AbstractSource source;

    private ChannelProcessor processor;

    private ChannelGroup allChannels;

    private String protocolType;

    private ServiceDecoder serviceDecoder;

    private String messageHandlerName;

    private int maxConnections = Integer.MAX_VALUE;

    private int maxMsgLength;

    private boolean isCompressed;

    private String name;

    private String topic;

    private String attr;

    private boolean filterEmptyMsg;

    private MonitorIndex monitorIndex;

    private MonitorIndexExt monitorIndexExt;

    /**
     * get server factory
     *
     * @param source
     * @param allChannels
     * @param protocol
     * @param serviceDecoder
     * @param messageHandlerName
     * @param topic
     * @param attr
     * @param filterEmptyMsg
     * @param maxCons
     * @param isCompressed
     * @param monitorIndex
     * @param monitorIndexExt
     * @param name
     */
    public ServerMessageFactory(AbstractSource source, ChannelGroup allChannels, String protocol,
            ServiceDecoder serviceDecoder, String messageHandlerName, Integer maxMsgLength,
            String topic, String attr, Boolean filterEmptyMsg, Integer maxCons,
            Boolean isCompressed, MonitorIndex monitorIndex, MonitorIndexExt monitorIndexExt,
            String name) {
        this.source = source;
        this.processor = source.getChannelProcessor();
        this.allChannels = allChannels;
        this.topic = topic;
        this.attr = attr;
        this.filterEmptyMsg = filterEmptyMsg;
        int cores = Runtime.getRuntime().availableProcessors();
        this.protocolType = protocol;
        this.serviceDecoder = serviceDecoder;
        this.messageHandlerName = messageHandlerName;
        this.name = name;
        this.maxConnections = maxCons;
        this.maxMsgLength = maxMsgLength;
        this.isCompressed = isCompressed;
        this.monitorIndex = monitorIndex;
        this.monitorIndexExt = monitorIndexExt;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        if (this.protocolType
                .equalsIgnoreCase(ConfigConstants.TCP_PROTOCOL)) {
            ch.pipeline().addLast("messageDecoder", new LengthFieldBasedFrameDecoder(
                    this.maxMsgLength, 0,
                    MSG_LENGTH_LEN, 0, 0, true));
            ch.pipeline().addLast("readTimeoutHandler",
                    new ReadTimeoutHandler(DEFAULT_READ_IDLE_TIME, TimeUnit.MILLISECONDS));
        }

        if (processor != null) {
            try {
                Class<? extends ChannelInboundHandlerAdapter> clazz
                        = (Class<? extends ChannelInboundHandlerAdapter>) Class
                        .forName(messageHandlerName);

                Constructor<?> ctor = clazz.getConstructor(
                        AbstractSource.class, ServiceDecoder.class, ChannelGroup.class,
                        String.class, String.class, Boolean.class,
                        Integer.class, Boolean.class, MonitorIndex.class,
                        MonitorIndexExt.class, String.class);

                ChannelInboundHandlerAdapter messageHandler = (ChannelInboundHandlerAdapter) ctor
                        .newInstance(source, serviceDecoder, allChannels, topic, attr,
                                filterEmptyMsg, maxConnections,
                                isCompressed,  monitorIndex, monitorIndexExt, protocolType
                        );

                ch.pipeline().addLast("messageHandler", messageHandler);
            } catch (Exception e) {
                LOG.info("SimpleChannelHandler.newInstance  has error:" + name, e);
            }
        }
    }
}
