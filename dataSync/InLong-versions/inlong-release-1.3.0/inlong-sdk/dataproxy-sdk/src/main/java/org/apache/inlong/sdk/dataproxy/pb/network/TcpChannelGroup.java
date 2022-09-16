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

package org.apache.inlong.sdk.dataproxy.pb.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * TcpChannelGroup
 */
public class TcpChannelGroup {

    public static final Logger LOG = LoggerFactory.getLogger(TcpChannelGroup.class);

    private String bid;
    private int senderThreadNum;
    private ClientBootstrap client = new ClientBootstrap();
    private Set<IpPort> currentIpLists = new HashSet<>();
    // for send
    private List<LinkedBlockingQueue<TcpChannel>> channelQueues = new ArrayList<>();
    // for close
    private List<List<TcpChannel>> channelLists = new ArrayList<>();
    private int mIndex = 0;
    // for callback
    private ConcurrentHashMap<Object, TcpChannel> channelMap = new ConcurrentHashMap<>();
    private AtomicLong channelId = new AtomicLong(0);

    /**
     * Constructor
     * 
     * @param bid
     * @param senderThreadNum
     * @param decoder
     * @param clientHandler
     */
    public TcpChannelGroup(String bid, int senderThreadNum, ChannelUpstreamHandler decoder,
            SimpleChannelHandler clientHandler) {
        this.bid = bid;
        this.senderThreadNum = senderThreadNum;
        LOG.info("TcpChannelGroup netty thread pool size is " + senderThreadNum);

        client.setFactory(new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool(),
                this.senderThreadNum));

        client.setPipelineFactory(() -> {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("decoder", decoder);
            pipeline.addLast("encoder", new ByteArrayToBinaryEncoder());
            pipeline.addLast("handler", clientHandler);
            return pipeline;
        });
        client.setOption("tcpNoDelay", false);
        client.setOption("child.tcpNoDelay", false);
        client.setOption("keepAlive", true);
        client.setOption("child.keepAlive", true);
        client.setOption("reuseAddr", false);
        //
        channelQueues.add(new LinkedBlockingQueue<>());
        channelQueues.add(new LinkedBlockingQueue<>());
        channelLists.add(new ArrayList<>());
        channelLists.add(new ArrayList<>());
    }

    /**
     * updateConfig
     * 
     * @param ipList
     */
    public void updateConfig(Set<IpPort> ipList) {
        boolean hasIpChange = !currentIpLists.equals(ipList);
        LOG.info("TcpChannelGroup updateConfig bid:{},hasIpChange:{},currentIpLists:{},ipLists:{}",
                bid, hasIpChange, currentIpLists, ipList);
        // clear old channel
        int oldIndex = mIndex ^ 0x01;
        this.channelQueues.get(oldIndex).clear();
        for (TcpChannel tcpChannel : this.channelLists.get(oldIndex)) {
            LOG.info(String.format("TcpChannelGroup updateConfig bid:%s,disconnect and close:%s", bid, tcpChannel));
            this.channelMap.remove(tcpChannel.getChannel().getAttachment());
            tcpChannel.close();
        }
        this.channelLists.get(oldIndex).clear();

        // no ip change
        if (!hasIpChange) {
            // reset status
            for (Entry<Object, TcpChannel> entry : this.channelMap.entrySet()) {
                LOG.info("TcpChannelGroup channel status index:{},"
                        + "isConnected:{},isHasException:{},isReconnectFail:{},availablePermits:{}",
                        entry.getValue().getChannel().getAttachment(),
                        entry.getValue().getChannel().isConnected(),
                        entry.getValue().isHasException(),
                        entry.getValue().isReconnectFail(),
                        entry.getValue().getPackToken().availablePermits());
                entry.getValue().setHasException(false);
                entry.getValue().setReconnectFail(false);
            }
            return;
        }

        // change ip list
        int newIndex = mIndex ^ 0x01;
        LinkedBlockingQueue<TcpChannel> newChannelQueue = this.channelQueues.get(newIndex);
        List<TcpChannel> newChannelList = this.channelLists.get(newIndex);
        for (IpPort ipPortObj : ipList) {
            // connect
            try {
                ChannelFuture future = client.connect(ipPortObj.addr).await();
                Channel channel = future.getChannel();
                Long newChannelId = this.channelId.getAndIncrement();
                channel.setAttachment(newChannelId);
                TcpChannel tcpChannel = new TcpChannel(channel, ipPortObj);
                newChannelQueue.add(tcpChannel);
                newChannelList.add(tcpChannel);
                this.channelMap.put(newChannelId, tcpChannel);
            } catch (Throwable ex) {
                LOG.error(String.format("bid:%s,ipPort:%s,connect failed:%s", bid, ipPortObj, ex.getMessage()),
                        ex);
            }
        }
        this.currentIpLists = ipList;
        this.mIndex = newIndex;
    }

    /**
     * send
     * 
     * @param  dataBuf
     * @return
     */
    public TcpResult send(ChannelBuffer dataBuf) {
        LinkedBlockingQueue<TcpChannel> channelQueue = this.channelQueues.get(mIndex);
        TcpChannel tcpChannel = this.getTcpChannel(channelQueue);
        try {
            // can not get channel of connect success
            if (tcpChannel == null) {
                return new TcpResult("", 0, false, "can not acquire a channel");
            }
            tcpChannel.acquireUninterruptibly();
            // write data
            ChannelFuture t = tcpChannel.getChannel().write(dataBuf).sync().await();
            // write success
            if (!t.isSuccess()) {
                // write fail
                tcpChannel.setHasException(true);
                channelQueue.offer(tcpChannel);
                // print error log
                String errorMessage = (t.getCause() != null) ? t.getCause().getMessage() : "write fail";
                LOG.error(String.format("bid:%s,write failed:%s", bid, errorMessage), t.getCause());
                return new TcpResult(tcpChannel.getIpPort(), false, errorMessage);
            }
            channelQueue.offer(tcpChannel);
            TcpResult result = new TcpResult(tcpChannel.getIpPort(), true, "");
            result.channelId = (Long) tcpChannel.getChannel().getAttachment();
            return result;
        } catch (Throwable ex) {
            LOG.error(String.format("bid:%s,netty send failed:%s", bid, ex.getMessage()), ex);
            if (tcpChannel != null) {
                channelQueue.offer(tcpChannel);
            }
            return new TcpResult("", 0, false, ex.getMessage());
        }
    }

    /**
     * getTcpChannel
     * 
     * @return
     */
    private TcpChannel getTcpChannel(LinkedBlockingQueue<TcpChannel> channelQueue) {
        TcpChannel tcpChannel = null;
        try {
            // get tcp channel
            for (int i = 0; i < channelQueue.size(); i++) {
                tcpChannel = channelQueue.take();
                // skip the channel that reconnect fail
                if (tcpChannel.isReconnectFail()) {
                    channelQueue.offer(tcpChannel);
                    tcpChannel = null;
                    continue;
                }
                // reconnect disconnect channel or exception channel
                if (!tcpChannel.getChannel().isConnected() || tcpChannel.isHasException()) {
                    this.reconnect(tcpChannel);
                }
                // return the channel of connect success
                if (tcpChannel.getChannel().isConnected()) {
                    tcpChannel.setHasException(false);
                    break;
                }
                // reconnect fail
                LOG.info("reconnect fail,channel:{}", tcpChannel);
                tcpChannel.setReconnectFail(true);
                tcpChannel = null;
            }
            return tcpChannel;
        } catch (Throwable ex) {
            LOG.error(String.format("bid:%s,get channel error:%s", bid, ex.getMessage()), ex);
            if (tcpChannel != null) {
                channelQueue.offer(tcpChannel);
            }
            return null;
        }
    }

    /**
     * reconnect
     * 
     * @param tcpChannel
     */
    private void reconnect(TcpChannel tcpChannel) {
        try {
            synchronized (tcpChannel) {
                Channel oldChannel = tcpChannel.getChannel();
                if (oldChannel != null && oldChannel.isOpen()) {
                    return;
                }
                LOG.info("reconnect channel:{}", tcpChannel);
                ChannelFuture future = client.connect(tcpChannel.getIpPort().addr).await();
                Channel newChannel = future.getChannel();
                tcpChannel.setChannel(newChannel);
                newChannel.setAttachment(oldChannel.getAttachment());
                if (oldChannel != null) {
                    oldChannel.disconnect();
                    oldChannel.close();
                }
            }
        } catch (Throwable ex) {
            LOG.error("reconnect failed:" + ex.getMessage(), ex);
        }
    }

    /**
     * get bid
     * 
     * @return the bid
     */
    public String getBid() {
        return bid;
    }

    /**
     * close
     */
    public void close() {
        //
        for (LinkedBlockingQueue<TcpChannel> channelQueue : this.channelQueues) {
            channelQueue.clear();
        }
        this.channelQueues.clear();
        //
        for (List<TcpChannel> channelList : this.channelLists) {
            for (TcpChannel tcpChannel : channelList) {
                LOG.info("TcpChannelGroup close bid:{},disconnect and close:{}", bid, tcpChannel);
                tcpChannel.close();
            }
        }
        //
        this.channelMap.clear();
    }

    /**
     * mark exception channel
     * 
     * @param channel
     */
    public void exceptionChannel(Channel channel) {
        TcpChannel tcpChannel = this.channelMap.get(channel.getAttachment());
        if (tcpChannel != null) {
            tcpChannel.setHasException(true);
        }
    }

    /**
     * releaseChannel
     * 
     * @param channel
     */
    public void releaseChannel(Channel channel) {
        TcpChannel tcpChannel = this.channelMap.get(channel.getAttachment());
        if (tcpChannel != null) {
            tcpChannel.release();
        }
    }
//    public static void main(String[] args) {
//        String[] data = new String[]{"10.56.81.205:46801",
//            "10.56.81.211:46801",
//            "10.56.20.85:46801",
//            "10.56.20.21:46801",
//            "10.56.83.143:46801",
//            "10.56.82.37:46801",
//            "10.56.83.80:46801",
//            "10.56.82.11:46801",
//            "10.56.15.221:46801",
//            "10.56.21.85:46801",
//            "10.56.16.20:46801",
//            "10.56.21.17:46801",
//            "10.56.21.79:46801",
//            "10.56.15.195:46801",
//            "10.56.16.38:46801",
//            "10.56.20.80:46801",
//            "10.56.82.38:46801",
//            "10.56.209.205:46801",
//            "10.56.84.17:46801",
//            "10.56.15.230:46801",
//            "10.56.82.12:46801",
//            "10.56.15.220:46801",
//            "10.56.21.20:46801",
//            "10.56.82.40:46801",
//            "10.56.15.212:46801"};
//    }
}
