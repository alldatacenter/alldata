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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.ThreadFactory;
import org.apache.inlong.audit.util.EventLoopUtil;
import org.apache.inlong.audit.util.IpPort;
import org.apache.inlong.audit.util.SenderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class SenderGroup {
    private static final Logger logger = LoggerFactory.getLogger(SenderGroup.class);
    // maximum number of sending
    public static final int MAX_SEND_TIMES = 3;
    public static final int DEFAULT_WAIT_TIMES = 10000;
    public static final int WAIT_INTERVAL = 1;
    public static final int DEFAULT_SYNCH_REQUESTS = 1;
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 16777216;
    public static final int DEFAULT_SEND_BUFFER_SIZE = 16777216;

    private Bootstrap client;
    private List<LinkedBlockingQueue<SenderChannel>> channelGroups = new ArrayList<>();
    private int mIndex = 0;
    private List<SenderChannel> deleteChannels = new ArrayList<>();
    private ConcurrentHashMap<String, SenderChannel> totalChannels = new ConcurrentHashMap<>();

    private int senderThreadNum;
    private int waitChannelTimes = DEFAULT_WAIT_TIMES;
    private int waitChannelIntervalMs = WAIT_INTERVAL;
    private int maxSynchRequest = DEFAULT_SYNCH_REQUESTS;
    private boolean hasSendError = false;

    /**
     * constructor
     *
     * @param senderThreadNum
     * @param clientHandler
     */
    public SenderGroup(int senderThreadNum, SimpleChannelInboundHandler clientHandler) {
        this.senderThreadNum = senderThreadNum;

        ThreadFactory selfDefineFactory  = new DefaultThreadFactory("audit-client-io",
                Thread.currentThread().isDaemon());

        EventLoopGroup eventLoopGroup = EventLoopUtil.newEventLoopGroup(this.senderThreadNum,
                false, selfDefineFactory);
        client = new Bootstrap();
        client.group(eventLoopGroup);
        client.channel(EventLoopUtil.getClientSocketChannelClass(eventLoopGroup));
        client.option(ChannelOption.SO_KEEPALIVE, true);
        client.option(ChannelOption.TCP_NODELAY, true);
        client.option(ChannelOption.SO_REUSEADDR, true);
        client.option(ChannelOption.SO_RCVBUF, DEFAULT_RECEIVE_BUFFER_SIZE);
        client.option(ChannelOption.SO_SNDBUF, DEFAULT_SEND_BUFFER_SIZE);
        client.handler(new ClientPipelineFactory(clientHandler));

        /*
         * init add two list for update config
         */
        channelGroups.add(new LinkedBlockingQueue<>());
        channelGroups.add(new LinkedBlockingQueue<>());
    }

    /**
     * send data
     *
     * @param dataBuf
     * @return
     */
    public SenderResult send(ByteBuf dataBuf) {
        LinkedBlockingQueue<SenderChannel> channels = channelGroups.get(mIndex);
        SenderChannel channel = null;
        try {
            if (channels.size() <= 0) {
                logger.error("channels is empty");
                return new SenderResult("channels is empty", 0, false);
            }
            boolean isOk = false;
            for (int tryIndex = 0; tryIndex < waitChannelTimes; tryIndex++) {
                channels = channelGroups.get(mIndex);
                for (int i = 0; i < channels.size(); i++) {
                    channel = channels.poll();
                    if (channel.tryAcquire()) {
                        isOk = true;
                        break;
                    }
                    channels.offer(channel);
                    channel = null;
                }
                if (isOk) {
                    break;
                }
                try {
                    Thread.sleep(waitChannelIntervalMs);
                } catch (Throwable e) {
                    logger.error(e.getMessage());
                }
            }
            if (channel == null) {
                logger.error("can not get a channel");
                return new SenderResult("can not get a channel", 0, false);
            }
            ChannelFuture t = null;
            if (channel.getChannel().isWritable()) {
                t = channel.getChannel().writeAndFlush(dataBuf).sync().await();
                if (!t.isSuccess()) {
                    if (!channel.getChannel().isActive()) {
                        reconnect(channel);
                    }
                    t = channel.getChannel().writeAndFlush(dataBuf).sync().await();
                }
            } else {
                reconnect(channel);
                t = channel.getChannel().writeAndFlush(dataBuf).sync().await();
            }
            return new SenderResult(channel.getIpPort().ip, channel.getIpPort().port, t.isSuccess());
        } catch (Throwable ex) {
            logger.error(ex.getMessage());
            this.setHasSendError(true);
            return new SenderResult(ex.getMessage(), 0, false);
        } finally {
            if (channel != null) {
                channel.release();
                channels.offer(channel);
            }
        }
    }

    /**
     * release channel
     */
    public void release(String ipPort) {
        SenderChannel channel = this.totalChannels.get(ipPort);
        if (channel != null) {
            channel.release();
        }
    }

    /**
     * release channel
     */
    public void release(InetSocketAddress addr) {
        String destIp = addr.getHostName();
        int destPort = addr.getPort();
        String ipPort = IpPort.getIpPortKey(destIp, destPort);
        SenderChannel channel = this.totalChannels.get(ipPort);
        if (channel != null) {
            channel.release();
        }
    }

    /**
     * update config
     *
     * @param ipLists
     */
    public void updateConfig(Set<String> ipLists) {
        try {
            for (SenderChannel dc : deleteChannels) {
                dc.getChannel().disconnect();
                dc.getChannel().close();
            }
            deleteChannels.clear();
            int newIndex = mIndex ^ 0x01;
            LinkedBlockingQueue<SenderChannel> newChannels = this.channelGroups.get(newIndex);
            newChannels.clear();
            for (String ipPort : ipLists) {
                SenderChannel channel = totalChannels.get(ipPort);
                if (channel != null) {
                    newChannels.add(channel);
                    continue;
                }
                try {
                    IpPort ipPortObj = IpPort.parseIpPort(ipPort);
                    if (ipPortObj == null) {
                        continue;
                    }
                    ChannelFuture future = client.connect(ipPortObj.addr).await();
                    channel = new SenderChannel(future.channel(), ipPortObj, maxSynchRequest);
                    newChannels.add(channel);
                    totalChannels.put(ipPort, channel);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }

            for (Entry<String, SenderChannel> entry : totalChannels.entrySet()) {
                if (!ipLists.contains(entry.getKey())) {
                    deleteChannels.add(entry.getValue());
                }
            }
            for (SenderChannel dc : deleteChannels) {
                totalChannels.remove(dc.getIpPort().key);
            }
            this.mIndex = newIndex;
        } catch (Throwable e) {
            logger.error("Update Sender Ip Failed." + e.getMessage());
        }
    }

    /**
     * reconnect
     *
     * @param channel
     */
    private void reconnect(SenderChannel channel) {
        try {
            synchronized (channel) {
                if (channel.getChannel().isOpen()) {
                    return;
                }

                Channel oldChannel = channel.getChannel();
                ChannelFuture future = client.connect(channel.getIpPort().addr).await();
                Channel newChannel = future.channel();
                channel.setChannel(newChannel);
                oldChannel.disconnect();
                oldChannel.close();
            }
        } catch (Throwable e) {
            logger.error("reconnect failed." + e.getMessage());
        }
    }

    /**
     * get hasSendError
     *
     * @return the hasSendError
     */
    public boolean isHasSendError() {
        return hasSendError;
    }

    /**
     * set hasSendError
     *
     * @param hasSendError the hasSendError to set
     */
    public void setHasSendError(boolean hasSendError) {
        this.hasSendError = hasSendError;
    }

}

