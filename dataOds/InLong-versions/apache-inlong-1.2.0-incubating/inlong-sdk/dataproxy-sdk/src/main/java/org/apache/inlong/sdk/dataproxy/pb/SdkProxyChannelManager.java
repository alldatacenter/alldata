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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.ResponseInfo;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.ResultCode;
import org.apache.inlong.sdk.dataproxy.SendResult;
import org.apache.inlong.sdk.dataproxy.pb.context.SdkProfile;
import org.apache.inlong.sdk.dataproxy.pb.context.SdkSinkContext;
import org.apache.inlong.sdk.dataproxy.pb.dispatch.DispatchProfile;
import org.apache.inlong.sdk.dataproxy.pb.network.IpPort;
import org.apache.inlong.sdk.dataproxy.pb.network.TcpChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * SdkProxyChannelManager
 */
public class SdkProxyChannelManager {

    public static final Logger LOG = LoggerFactory.getLogger(SdkProxyChannelManager.class);
    public static final int DEFAULT_LENGTH_FIELD_OFFSET = 0;
    public static final int DEFAULT_LENGTH_FIELD_LENGTH = 4;
    public static final int DEFAULT_LENGTH_ADJUSTMENT = -4;
    public static final int DEFAULT_INITIAL_BYTES_TO_STRIP = 0;
    public static final boolean DEFAULT_FAIL_FAST = true;

    // proxyClusterId
    private String proxyClusterId;
    private SdkSinkContext context;
    //
    private LinkedBlockingQueue<DispatchProfile> proxyDispatchQueue = new LinkedBlockingQueue<>();
    private AtomicLong offerCounter = new AtomicLong(0);
    private ConcurrentHashMap<String, AtomicLong> offerGroupCounter = new ConcurrentHashMap<>();
    private AtomicLong takeCounter = new AtomicLong(0);
    private Timer reloadTimer;
    private List<SdkChannelWorker> workers = new ArrayList<>();
    private TcpChannelGroup sender;
    //
    private AtomicLong sdkPackId = new AtomicLong(RandomUtils.nextLong());
    private ConcurrentHashMap<Long, SdkProfile> profileMap = new ConcurrentHashMap<>();

    /**
     * Constructor
     * 
     * @param proxyClusterId
     * @param context
     */
    public SdkProxyChannelManager(String proxyClusterId, SdkSinkContext context) {
        this.proxyClusterId = proxyClusterId;
        this.context = context;
    }

    /**
     * start
     */
    public void start() {
        try {
            LOG.info("start to SdkProxyChannelManager:{}", this.proxyClusterId);
            SdkSenderClientHandler clientHandler = new SdkSenderClientHandler(this);
            this.sender = new TcpChannelGroup(proxyClusterId, context.getMaxThreads(),
                    new LengthFieldBasedFrameDecoder(SdkSinkContext.MAX_RESPONSE_LENGTH, DEFAULT_LENGTH_FIELD_OFFSET,
                            DEFAULT_LENGTH_FIELD_LENGTH,
                            DEFAULT_LENGTH_ADJUSTMENT, DEFAULT_INITIAL_BYTES_TO_STRIP, DEFAULT_FAIL_FAST),
                    clientHandler);
            //
            for (int i = 0; i < context.getMaxThreads(); i++) {
                SdkChannelWorker worker = new SdkChannelWorker(this, i);
                this.workers.add(worker);
                worker.start();
            }
            //
            this.reload();
            this.setReloadTimer();
        } catch (Exception e) {
            LOG.error("proxyClusterId:{},error:{}", proxyClusterId, e);
        }
    }

    /**
     * setReloadTimer
     */
    private void setReloadTimer() {
        reloadTimer = new Timer(true);
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                try {
                    reload();
                } catch (Exception e) {
                    LOG.error("proxyClusterId:{},error:{}", proxyClusterId, e);
                }
            }
        };
        reloadTimer.schedule(task, new Date(System.currentTimeMillis() + context.getReloadInterval()),
                context.getReloadInterval());
    }

    /**
     * nextPackId
     * 
     * @return
     */
    public long nextPackId() {
        return this.sdkPackId.getAndIncrement();
    }

    /**
     * reload
     */
    public void reload() {
        try {
            Set<IpPort> ipPortList = context.getProxyIpListMap().get(proxyClusterId);
            this.sender.updateConfig(ipPortList);
            //
            this.clearTimeoutPack();
        } catch (Exception e) {
            LOG.error("proxyClusterId:{},error:{}", proxyClusterId, e);
        }

    }

    /**
     * clearTimeoutPack
     */
    private void clearTimeoutPack() {
        LOG.info("ProxyClusterIdChannelManager clearTimeoutPack proxyClusterId:{},queueSize:{},waitingSize:{},"
                + "offerCount:{},takeCount:{},offerGroupCount:{}",
                proxyClusterId, this.proxyDispatchQueue.size(), profileMap.size(),
                offerCounter.getAndSet(0), takeCounter.getAndSet(0), offerGroupCounter);
        offerGroupCounter.clear();
        long currentTime = System.currentTimeMillis();
        List<Long> timeoutPackId = new ArrayList<>(this.profileMap.size());
        for (Entry<Long, SdkProfile> entry : this.profileMap.entrySet()) {
            if (currentTime - entry.getValue().getSendTime() > context.getSdkPackTimeout()) {
                timeoutPackId.add(entry.getKey());
            }
        }
        if (timeoutPackId.size() > 0) {
            LOG.info("clearTimeoutPack timeoutSize:{}",
                    timeoutPackId.size());
            for (Long tdbankPackId : timeoutPackId) {
                SdkProfile tProfile = this.profileMap.remove(tdbankPackId);
                if (tProfile != null) {
                    this.offerDispatchQueue(tProfile.getDispatchProfile());
                }
            }
        }
    }

    /**
     * putWaitCompletedProfile
     * 
     * @param tProfile
     */
    public void putWaitCompletedProfile(SdkProfile tProfile) {
        this.profileMap.put(tProfile.getSdkPackId(), tProfile);
    }

    /**
     * removeWaitCompletedProfile
     * 
     * @param tProfile
     */
    public void removeWaitCompletedProfile(SdkProfile tProfile) {
        this.profileMap.remove(tProfile.getSdkPackId());
    }

    /**
     * clearChannel
     * 
     * @param channel
     */
    public void setChannelException(Channel channel) {
        SocketAddress socketAddress = channel.getRemoteAddress();
        if (!(socketAddress instanceof InetSocketAddress)) {
            return;
        }
        //
        InetSocketAddress addr = (InetSocketAddress) socketAddress;
        IpPort ipPort = new IpPort(addr);
        List<Long> packIds = new ArrayList<>(this.profileMap.size());
        for (Entry<Long, SdkProfile> entry : this.profileMap.entrySet()) {
            SdkProfile tProfile = entry.getValue();
            if (ipPort.equals(tProfile.getIpPort())) {
                packIds.add(entry.getKey());
            }
        }
        LOG.warn("proxyClusterId:{},clear channel:local:{},remote:{},profile size:{}",
                proxyClusterId, channel.getLocalAddress(), channel.getRemoteAddress(), packIds.size());
        //
        for (Long packId : packIds) {
            SdkProfile tProfile = this.profileMap.remove(packId);
            if (tProfile != null) {
                this.offerDispatchQueue(tProfile.getDispatchProfile());
                this.context.addSendResultMetric(tProfile.getDispatchProfile(), proxyClusterId, false,
                        tProfile.getSendTime());
            }
        }
        //
        this.sender.exceptionChannel(channel);
        this.sender.releaseChannel(channel);
    }

    /**
     * parseInetSocketAddress
     * 
     * @param  channel
     * @return
     */
    public static InetSocketAddress parseInetSocketAddress(Channel channel) {
        InetSocketAddress destAddr = null;
        if (channel.getRemoteAddress() instanceof InetSocketAddress) {
            destAddr = (InetSocketAddress) channel.getRemoteAddress();
        } else if (channel.getRemoteAddress() != null) {
            String sendIp = channel.getRemoteAddress().toString();
            destAddr = new InetSocketAddress(sendIp, 0);
        } else {
            destAddr = new InetSocketAddress("127.0.0.1", 0);
        }
        return destAddr;
    }

    /**
     * onMessageReceived
     * 
     * @param ctx
     * @param e
     */
    public void onMessageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        // parse channel buffer
        if (!(e.getMessage() instanceof ChannelBuffer)) {
            LOG.error("proxyClusterId:{},onMessageReceived e.getMessage:{}", proxyClusterId, e.getMessage());
            this.setChannelException(e.getChannel());
            return;
        }
        // read bytes
        ChannelBuffer nettyBuffer = (ChannelBuffer) e.getMessage();
        int responseLength = nettyBuffer.readInt();
        // check pack version
        int packVersion = nettyBuffer.readShort();
        if (packVersion != SdkSinkContext.PACK_VERSION) {
            LOG.error("proxyClusterId:{},Error result from:{},error pack version:{}",
                    proxyClusterId, String.valueOf(e.getChannel().getRemoteAddress()), packVersion);
            this.setChannelException(e.getChannel());
            return;
        }
        byte[] responseBytes = new byte[responseLength - SdkSinkContext.PACK_VERSION_LENGTH];
        nettyBuffer.readBytes(responseBytes);
        // unpack
        ResponseInfo responseInfo = null;
        try {
            responseInfo = ResponseInfo.parseFrom(responseBytes);
        } catch (Exception ex) {
            LOG.error("proxyClusterId:{},Error result from:{},parseFrom exception:{}",
                    proxyClusterId, String.valueOf(e.getChannel().getRemoteAddress()), ex);
            this.setChannelException(e.getChannel());
            return;
        }
        // check result
        ResultCode result = responseInfo.getResult();
        if (result != ResultCode.SUCCUSS) {
            LOG.error("proxyClusterId:{},Error result from:{},resultCode:{}",
                    proxyClusterId, String.valueOf(e.getChannel().getRemoteAddress()), result.toString());
            this.setChannelException(e.getChannel());
            return;
        }
        // parse sdkPackId
        long sdkPackId = responseInfo.getPackId();
        // get SdkProfile
        SdkProfile tProfile = this.profileMap.remove(sdkPackId);
        if (tProfile == null) {
            LOG.error("proxyClusterId:%s,Can not find MessageDispatchProfile by sdkPackId:%d,from:%s",
                    proxyClusterId, sdkPackId, String.valueOf(e.getChannel().getRemoteAddress()));
            this.setChannelException(e.getChannel());
            return;
        }

        // onMessageOK
        this.onMessageOK(tProfile, e.getChannel());
    }

    /**
     * onMessageOK
     * 
     * @param tProfile
     * @param channel
     */
    private void onMessageOK(SdkProfile tProfile, Channel channel) {
        this.context.addSendResultMetric(tProfile.getDispatchProfile(), proxyClusterId, true, tProfile.getSendTime());
        this.sender.releaseChannel(channel);
        tProfile.getDispatchProfile().getEvents().forEach((pEvent) -> {
            try {
                pEvent.getProfile().getCallback().onMessageAck(SendResult.OK);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }

    /**
     * close
     */
    public void close() {
        LOG.info("begin to close proxyClusterId:{}.", proxyClusterId);
        this.reloadTimer.cancel();
        for (SdkChannelWorker worker : this.workers) {
            worker.close();
        }
        this.workers.clear();
        this.profileMap.clear();
        this.sender.close();
        LOG.info("end to close proxyClusterId:{}.", proxyClusterId);
    }

    /**
     * get proxyClusterId
     * 
     * @return the proxyClusterId
     */
    public String getProxyClusterId() {
        return proxyClusterId;
    }

    /**
     * get context
     * 
     * @return the context
     */
    public SdkSinkContext getContext() {
        return context;
    }

    /**
     * offerDispatchQueue
     * 
     * @param  profile
     * @return
     */
    public boolean offerDispatchQueue(DispatchProfile profile) {
        offerCounter.incrementAndGet();
        String key = new Exception().getStackTrace()[1].toString();
        AtomicLong value = offerGroupCounter.computeIfAbsent(key, k -> new AtomicLong(0));
        value.incrementAndGet();
        return this.proxyDispatchQueue.offer(profile);
    }

    /**
     * takeDispatchQueue
     * 
     * @return                      DispatchProfile
     * @throws InterruptedException
     */
    public DispatchProfile takeDispatchQueue() throws InterruptedException {
        takeCounter.incrementAndGet();
        return this.proxyDispatchQueue.take();
    }

    /**
     * get sender
     * 
     * @return the sender
     */
    public TcpChannelGroup getSender() {
        return sender;
    }

}
