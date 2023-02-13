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

package org.apache.inlong.dataproxy.dispatch;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.dataproxy.utils.MessageUtils;
import org.apache.inlong.sdk.commons.protocol.ProxyEvent;
import org.apache.inlong.sdk.commons.protocol.ProxyPackEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DispatchManager
 */
public class DispatchManager {

    public static final Logger LOG = LoggerFactory.getLogger(DispatchManager.class);
    public static final String KEY_DISPATCH_TIMEOUT = "dispatchTimeout";
    public static final String KEY_DISPATCH_MAX_PACKCOUNT = "dispatchMaxPackCount";
    public static final String KEY_DISPATCH_MAX_PACKSIZE = "dispatchMaxPackSize";
    public static final long DEFAULT_DISPATCH_TIMEOUT = 2000;
    public static final long DEFAULT_DISPATCH_MAX_PACKCOUNT = 256;
    public static final long DEFAULT_DISPATCH_MAX_PACKSIZE = 327680;
    public static final long MINUTE_MS = 60L * 1000;

    private final long dispatchTimeout;
    private final long maxPackCount;
    private final long maxPackSize;
    private final ArrayList<LinkedBlockingQueue<DispatchProfile>> dispatchQueues;

    private final AtomicInteger sendIndex = new AtomicInteger();

    private ConcurrentHashMap<String, DispatchProfile> profileCache = new ConcurrentHashMap<>();
    // flag that manager need to output overtime data.
    private AtomicBoolean needOutputOvertimeData = new AtomicBoolean(false);
    private AtomicLong inCounter = new AtomicLong(0);
    private AtomicLong outCounter = new AtomicLong(0);

    /**
     * Constructor
     * 
     * @param context
     * @param dispatchQueues
     */
    public DispatchManager(Context context, ArrayList<LinkedBlockingQueue<DispatchProfile>> dispatchQueues) {
        this.dispatchQueues = dispatchQueues;
        this.dispatchTimeout = context.getLong(KEY_DISPATCH_TIMEOUT, DEFAULT_DISPATCH_TIMEOUT);
        this.maxPackCount = context.getLong(KEY_DISPATCH_MAX_PACKCOUNT, DEFAULT_DISPATCH_MAX_PACKCOUNT);
        this.maxPackSize = context.getLong(KEY_DISPATCH_MAX_PACKSIZE, DEFAULT_DISPATCH_MAX_PACKSIZE);
    }

    /**
     * addEvent
     * 
     * @param event
     */
    public void addEvent(ProxyEvent event) {
        // parse
        String eventUid = event.getUid();
        long dispatchTime = event.getMsgTime() - event.getMsgTime() % MINUTE_MS;
        String dispatchKey = eventUid + "." + dispatchTime;
        // find dispatch profile
        DispatchProfile dispatchProfile = this.profileCache.get(dispatchKey);
        if (dispatchProfile == null) {
            dispatchProfile = new DispatchProfile(eventUid, event.getInlongGroupId(), event.getInlongStreamId(),
                    dispatchTime);
            this.profileCache.put(dispatchKey, dispatchProfile);
        }
        // add event
        boolean addResult = dispatchProfile.addEvent(event, maxPackCount, maxPackSize);
        addSendIndex(event, dispatchProfile);
        if (!addResult) {
            DispatchProfile newDispatchProfile = new DispatchProfile(eventUid, event.getInlongGroupId(),
                    event.getInlongStreamId(), dispatchTime);
            DispatchProfile oldDispatchProfile = this.profileCache.put(dispatchKey, newDispatchProfile);
            this.dispatchQueues.get(dispatchProfile.getSendIndex() % dispatchQueues.size())
                    .offer(dispatchProfile);
            outCounter.addAndGet(dispatchProfile.getCount());
            newDispatchProfile.addEvent(event, maxPackCount, maxPackSize);
        }
        inCounter.incrementAndGet();
    }

    private void addSendIndex(Event event, DispatchProfile dispatchProfile) {
        if ((MessageUtils.isSyncSendForOrder(event))) {
            String partitionKey = event.getHeaders().get(AttributeConstants.MESSAGE_PARTITION_KEY);
            int sendIndex = Math.abs(partitionKey.hashCode());
            dispatchProfile.setOrder(true);
            dispatchProfile.setSendIndex(sendIndex);
        } else {
            dispatchProfile.setSendIndex(sendIndex.incrementAndGet());
        }
    }

    /**
     * addPackEvent
     * @param packEvent
     */
    public void addPackEvent(ProxyPackEvent packEvent) {
        String eventUid = packEvent.getUid();
        long dispatchTime = packEvent.getMsgTime() - packEvent.getMsgTime() % MINUTE_MS;
        DispatchProfile dispatchProfile = new DispatchProfile(eventUid, packEvent.getInlongGroupId(),
                packEvent.getInlongStreamId(), dispatchTime);
        // callback
        DispatchProfileCallback callback = new DispatchProfileCallback(packEvent.getEvents().size(),
                packEvent.getCallback());
        dispatchProfile.setCallback(callback);
        // offer queue
        for (ProxyEvent event : packEvent.getEvents()) {
            inCounter.incrementAndGet();
            boolean addResult = dispatchProfile.addEvent(event, maxPackCount, maxPackSize);
            addSendIndex(event, dispatchProfile);
            // dispatch profile is full
            if (!addResult) {
                outCounter.addAndGet(dispatchProfile.getCount());
                this.dispatchQueues.get(dispatchProfile.getSendIndex() % dispatchQueues.size())
                        .offer(dispatchProfile);
                dispatchProfile = new DispatchProfile(eventUid, event.getInlongGroupId(), event.getInlongStreamId(),
                        dispatchTime);
                dispatchProfile.setCallback(callback);
                dispatchProfile.addEvent(event, maxPackCount, maxPackSize);
            }
        }
        // last dispatch profile
        if (dispatchProfile.getEvents().size() > 0) {
            outCounter.addAndGet(dispatchProfile.getCount());
            this.dispatchQueues.get(dispatchProfile.getSendIndex() % dispatchQueues.size())
                    .offer(dispatchProfile);
        }
    }

    /**
     * outputOvertimeData
     * 
     * @return
     */
    public void outputOvertimeData() {
        if (!needOutputOvertimeData.getAndSet(false)) {
            return;
        }
        LOG.debug("start to outputOvertimeData profileCacheSize:{},dispatchQueueSize:{}",
                profileCache.size(), dispatchQueues.stream().mapToInt(LinkedBlockingQueue::size).sum());
        long currentTime = System.currentTimeMillis();
        long createThreshold = currentTime - dispatchTimeout;
        List<String> removeKeys = new ArrayList<>();
        long eventCount = 0;
        for (Entry<String, DispatchProfile> entry : this.profileCache.entrySet()) {
            DispatchProfile dispatchProfile = entry.getValue();
            eventCount += dispatchProfile.getCount();
            if (!dispatchProfile.isTimeout(createThreshold)) {
                continue;
            }
            removeKeys.add(entry.getKey());
        }
        // output
        removeKeys.forEach((key) -> {
            DispatchProfile dispatchProfile = this.profileCache.remove(key);
            if (dispatchProfile != null) {
                this.dispatchQueues.get(dispatchProfile.getSendIndex() % dispatchQueues.size())
                        .offer(dispatchProfile);
                outCounter.addAndGet(dispatchProfile.getCount());
            }
        });
        LOG.debug("end to outputOvertimeData profileCacheSize:{},dispatchQueueSize:{},eventCount:{},"
                + "inCounter:{},outCounter:{}",
                profileCache.size(), dispatchQueues.stream().mapToInt(LinkedBlockingQueue::size).sum(), eventCount,
                inCounter.getAndSet(0), outCounter.getAndSet(0));
    }

    /**
     * get dispatchTimeout
     * 
     * @return the dispatchTimeout
     */
    public long getDispatchTimeout() {
        return dispatchTimeout;
    }

    /**
     * get maxPackCount
     * 
     * @return the maxPackCount
     */
    public long getMaxPackCount() {
        return maxPackCount;
    }

    /**
     * get maxPackSize
     * 
     * @return the maxPackSize
     */
    public long getMaxPackSize() {
        return maxPackSize;
    }

    /**
     * setNeedOutputOvertimeData
     */
    public void setNeedOutputOvertimeData() {
        this.needOutputOvertimeData.getAndSet(true);
    }
}
