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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.sdk.dataproxy.pb.context.ProfileEvent;
import org.apache.inlong.sdk.dataproxy.pb.context.SdkSinkContext;
import org.apache.inlong.sdk.dataproxy.pb.dispatch.DispatchManager;
import org.apache.inlong.sdk.dataproxy.pb.dispatch.DispatchProfile;
import org.apache.inlong.sdk.dataproxy.pb.network.IpPort;
import org.jboss.netty.channel.ChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

/**
 * ProxySdkSink
 */
public class ProxySdkSink extends AbstractSink implements Configurable {

    public static final Logger LOG = LoggerFactory.getLogger(ProxySdkSink.class);

    private Context parentContext;
    private SdkSinkContext context;
    private DispatchManager dispatchManager;
    private LinkedBlockingQueue<DispatchProfile> dispatchQueue = new LinkedBlockingQueue<>();
    //
    protected Timer sinkTimer;
    protected Timer processTimer;
    private final ConcurrentHashMap<String, SdkProxyChannelManager> proxyManagers = new ConcurrentHashMap<>();
    private final List<SdkProxyChannelManager> deletingProxyManager = new ArrayList<>();

    /**
     * start
     */
    @Override
    public void start() {
        try {
            this.context = new SdkSinkContext(parentContext, getChannel());
            if (getChannel() == null) {
                LOG.error("channel is null");
            }
            this.context.start();
            this.dispatchManager = new DispatchManager(parentContext, dispatchQueue);
            this.reload();
            this.setReloadTimer();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        super.start();
    }

    /**
     * setReloadTimer
     */
    protected void setReloadTimer() {
        sinkTimer = new Timer(true);
        // reload config
        TimerTask reloadTask = new TimerTask() {

            public void run() {
                reload();
            }
        };
        sinkTimer.schedule(reloadTask, new Date(System.currentTimeMillis() + this.context.getReloadInterval()),
                this.context.getReloadInterval());
        // output overtime data
        TimerTask dispatchTimeoutTask = new TimerTask() {

            public void run() {
                dispatchManager.setNeedOutputOvertimeData();
            }
        };
        sinkTimer.schedule(dispatchTimeoutTask,
                new Date(System.currentTimeMillis() + this.dispatchManager.getDispatchTimeout()),
                this.dispatchManager.getDispatchTimeout());
        // output process data
        processTimer = new Timer(true);
        TimerTask processTask = new TimerTask() {

            public void run() {
                try {
                    outputProxyQueue();
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        };
        processTimer.schedule(processTask, new Date(System.currentTimeMillis() + this.context.getProcessInterval()),
                this.context.getProcessInterval());
    }

    /**
     * reload
     */
    public void reload() {
        try {
            LOG.info("All proxy managers start status,proxy size:{},proxys:{},metricItemSize:{}",
                    proxyManagers.size(), proxyManagers.keySet(), context.getMetricItemSet().getItemMap().size());
            // stop old proxy
            for (SdkProxyChannelManager proxyManager : this.deletingProxyManager) {
                proxyManager.close();
            }
            this.deletingProxyManager.clear();
            // create new proxy
            for (Entry<String, Set<IpPort>> entry : this.context.getProxyIpListMap().entrySet()) {
                if (!this.proxyManagers.containsKey(entry.getKey())) {
                    SdkProxyChannelManager proxyManager = new SdkProxyChannelManager(entry.getKey(), context);
                    this.proxyManagers.put(entry.getKey(), proxyManager);
                    proxyManager.start();
                }
            }
            // remove proxy
            Set<String> deletingProxys = new HashSet<>();
            for (Entry<String, SdkProxyChannelManager> entry : this.proxyManagers.entrySet()) {
                if (!this.context.getProxyIpListMap().containsKey(entry.getKey())) {
                    deletingProxys.add(entry.getKey());
                }
            }
            for (String proxy : deletingProxys) {
                this.deletingProxyManager.add(this.proxyManagers.remove(proxy));
            }
            LOG.info("All proxy managers end status,proxy size:{},proxys:{},metricItemSize:{}",
                    proxyManagers.size(), proxyManagers.keySet(), context.getMetricItemSet().getItemMap().size());
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        try {
            for (Entry<String, SdkProxyChannelManager> entry : this.proxyManagers.entrySet()) {
                entry.getValue().close();
            }
            this.sinkTimer.cancel();
            this.context.close();
            super.stop();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        LOG.info("start to configure:{}, context:{}.", this.getClass().getSimpleName(), context.toString());
        this.parentContext = context;
    }

    /**
     * process
     * 
     * @return                        Status
     * @throws EventDeliveryException
     */
    @Override
    public Status process() throws EventDeliveryException {
        Channel channel = getChannel();
        Transaction tx = channel.getTransaction();
        tx.begin();
        try {
            Event event = channel.take();
            if (event == null) {
                tx.commit();
                return Status.BACKOFF;
            }
            if (!(event instanceof ProfileEvent)) {
                tx.commit();
                this.context.addSendFailMetric();
                return Status.READY;
            }
            //
            ProfileEvent profileEvent = (ProfileEvent) event;
            this.dispatchManager.addEvent(profileEvent);
            tx.commit();
            return Status.READY;
        } catch (Throwable t) {
            LOG.error("Process event failed!" + this.getName(), t);
            try {
                tx.rollback();
            } catch (Throwable e) {
                LOG.error("Channel take transaction rollback exception:" + getName(), e);
            }
            return Status.BACKOFF;
        } finally {
            tx.close();
        }
    }

    /**
     * outputProxyQueue
     */
    private void outputProxyQueue() {
        DispatchProfile dispatchProfile = this.dispatchQueue.poll();
        while (dispatchProfile != null) {
            String uid = dispatchProfile.getUid();
            String proxyClusterId = context.getProxyClusterId(uid);
            if (proxyClusterId == null) {
                // monitor
                LOG.error("can not find uid:{}", uid);
                this.context.addSendResultMetric(dispatchProfile, uid, false, 0);
                ChannelException ex = new ChannelException(String.format("can not find proxyClusterId:%s", uid));
                dispatchProfile.getEvents().forEach((pEvent) -> {
                    try {
                        pEvent.getProfile().getCallback().onException(ex);
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                });
                dispatchProfile = this.dispatchQueue.poll();
                continue;
            }
            SdkProxyChannelManager proxyManager = this.proxyManagers.get(proxyClusterId);
            if (proxyManager == null) {
                // monitor 007
                LOG.error("can not find proxy:{},proxyManagers:{}", proxyClusterId, JSON.toJSONString(proxyManagers));
                this.context.addSendResultMetric(dispatchProfile, uid, false, 0);
                ChannelException ex = new ChannelException(
                        String.format("can not find proxyClusterId manager:%s", proxyClusterId));
                dispatchProfile.getEvents().forEach((pEvent) -> {
                    try {
                        pEvent.getProfile().getCallback().onException(ex);
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                });
                dispatchProfile = this.dispatchQueue.poll();
                continue;
            }
            proxyManager.offerDispatchQueue(dispatchProfile);
            dispatchProfile = this.dispatchQueue.poll();
        }
    }
}
